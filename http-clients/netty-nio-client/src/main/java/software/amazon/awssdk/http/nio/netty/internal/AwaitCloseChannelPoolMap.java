/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.nio.netty.internal;

import static software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration.CHANNEL_POOL_CLOSE_TIMEOUT_SECONDS;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpOrHttp2ChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;

/**
 * Implementation of {@link SdkChannelPoolMap} that awaits channel pools to be closed upon closing.
 */
@SdkInternalApi
public final class AwaitCloseChannelPoolMap extends SdkChannelPoolMap<URI, SimpleChannelPoolAwareChannelPool> {

    private static final NettyClientLogger log = NettyClientLogger.getLogger(AwaitCloseChannelPoolMap.class);

    private static final ChannelPoolHandler NOOP_HANDLER = new ChannelPoolHandler() {
        @Override
        public void channelReleased(Channel ch) throws Exception {
        }

        @Override
        public void channelAcquired(Channel ch) throws Exception {
        }

        @Override
        public void channelCreated(Channel ch) throws Exception {
        }
    };

    // IMPORTANT: If the default bootstrap provider is changed, ensure that the new implementation is compliant with
    // DNS resolver testing in BootstrapProviderTest, specifically that no caching of hostname lookups is taking place.
    private static final Function<Builder, BootstrapProvider> DEFAULT_BOOTSTRAP_PROVIDER =
        b -> new BootstrapProvider(b.sdkEventLoopGroup, b.configuration, b.sdkChannelOptions);

    private final Map<URI, Boolean> shouldProxyForHostCache = new ConcurrentHashMap<>();


    private final NettyConfiguration configuration;
    private final Protocol protocol;
    private final ProtocolNegotiation protocolNegotiation;
    private final long maxStreams;
    private final Duration healthCheckPingPeriod;
    private final int initialWindowSize;
    private final SslProvider sslProvider;
    private final ProxyConfiguration proxyConfiguration;
    private final BootstrapProvider bootstrapProvider;
    private final SslContextProvider sslContextProvider;
    private final Boolean useNonBlockingDnsResolver;

    private AwaitCloseChannelPoolMap(Builder builder, Function<Builder, BootstrapProvider> createBootStrapProvider) {
        this.configuration = builder.configuration;
        this.protocol = builder.protocol;
        this.protocolNegotiation = builder.protocolNegotiation;
        this.maxStreams = builder.maxStreams;
        this.healthCheckPingPeriod = builder.healthCheckPingPeriod;
        this.initialWindowSize = builder.initialWindowSize;
        this.sslProvider = builder.sslProvider;
        this.proxyConfiguration = builder.proxyConfiguration;
        this.bootstrapProvider = createBootStrapProvider.apply(builder);
        this.sslContextProvider = new SslContextProvider(configuration, protocol, protocolNegotiation, sslProvider);
        this.useNonBlockingDnsResolver = builder.useNonBlockingDnsResolver;
    }

    private AwaitCloseChannelPoolMap(Builder builder) {
        this(builder, DEFAULT_BOOTSTRAP_PROVIDER);
    }

    @SdkTestInternalApi
    AwaitCloseChannelPoolMap(Builder builder,
                             Map<URI, Boolean> shouldProxyForHostCache,
                             BootstrapProvider bootstrapProvider) {
        this(builder, bootstrapProvider == null ? DEFAULT_BOOTSTRAP_PROVIDER : b -> bootstrapProvider);

        if (shouldProxyForHostCache != null) {
            this.shouldProxyForHostCache.putAll(shouldProxyForHostCache);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected SimpleChannelPoolAwareChannelPool newPool(URI key) {
        SslContext sslContext = needSslContext(key) ? sslContextProvider.sslContext() : null;

        Bootstrap bootstrap = createBootstrap(key);

        AtomicReference<ChannelPool> channelPoolRef = new AtomicReference<>();

        ChannelPipelineInitializer pipelineInitializer = new ChannelPipelineInitializer(protocol,
                                                                                        protocolNegotiation,
                                                                                        sslContext,
                                                                                        sslProvider,
                                                                                        maxStreams,
                                                                                        initialWindowSize,
                                                                                        healthCheckPingPeriod,
                                                                                        channelPoolRef,
                                                                                        configuration,
                                                                                        key);

        BetterSimpleChannelPool tcpChannelPool;
        ChannelPool baseChannelPool;
        if (shouldUseProxyForHost(key)) {
            tcpChannelPool = new BetterSimpleChannelPool(bootstrap, NOOP_HANDLER);
            baseChannelPool = new Http1TunnelConnectionPool(bootstrap.config().group().next(), tcpChannelPool, sslContext,
                                            proxyAddress(key), proxyConfiguration.username(), proxyConfiguration.password(),
                                            key, pipelineInitializer, configuration);
        } else {
            tcpChannelPool = new BetterSimpleChannelPool(bootstrap, pipelineInitializer);
            baseChannelPool = tcpChannelPool;
        }

        SdkChannelPool wrappedPool = wrapBaseChannelPool(bootstrap, baseChannelPool);

        channelPoolRef.set(wrappedPool);
        return new SimpleChannelPoolAwareChannelPool(wrappedPool, tcpChannelPool);
    }

    @Override
    public void close() {
        log.trace(null, () -> "Closing channel pools");
        // If there is a new pool being added while we are iterating the pools, there might be a
        // race condition between the close call of the newly acquired pool and eventLoopGroup.shutdown and it
        // could cause the eventLoopGroup#shutdownGracefully to hang before it times out.
        // If a new pool is being added while super.close() is running, it might be left open because
        // the underlying pool map is a ConcurrentHashMap and it doesn't guarantee strong consistency for retrieval
        // operations. See https://github.com/aws/aws-sdk-java-v2/pull/1200#discussion_r277906715
        Collection<SimpleChannelPoolAwareChannelPool> channelPools = pools().values();
        super.close();

        try {
            CompletableFuture.allOf(channelPools.stream()
                                                .map(pool -> pool.underlyingSimpleChannelPool().closeFuture())
                                                .toArray(CompletableFuture[]::new))
                             .get(CHANNEL_POOL_CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private Bootstrap createBootstrap(URI poolKey) {
        String host = bootstrapHost(poolKey);
        int port = bootstrapPort(poolKey);
        return bootstrapProvider.createBootstrap(host, port, useNonBlockingDnsResolver);
    }


    private boolean shouldUseProxyForHost(URI remoteAddr) {
        if (proxyConfiguration == null || proxyConfiguration.host() == null) {
            return false;
        }


        return shouldProxyForHostCache.computeIfAbsent(remoteAddr, (uri) ->
           proxyConfiguration.nonProxyHosts().stream().noneMatch(h -> uri.getHost().matches(h))
        );
    }

    private String bootstrapHost(URI remoteHost) {
        if (shouldUseProxyForHost(remoteHost)) {
            return proxyConfiguration.host();
        }
        return remoteHost.getHost();
    }

    private int bootstrapPort(URI remoteHost) {
        if (shouldUseProxyForHost(remoteHost)) {
            return proxyConfiguration.port();
        }
        return remoteHost.getPort();
    }

    private URI proxyAddress(URI remoteHost) {
        if (!shouldUseProxyForHost(remoteHost)) {
            return null;
        }

        String scheme = proxyConfiguration.scheme();
        if (scheme == null) {
            scheme = "http";
        }

        try {
            return new URI(scheme, null, proxyConfiguration.host(), proxyConfiguration.port(), null, null,
                    null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to construct proxy URI", e);
        }
    }

    private SdkChannelPool wrapBaseChannelPool(Bootstrap bootstrap, ChannelPool channelPool) {

        // Wrap the channel pool such that the ChannelAttributeKey.CLOSE_ON_RELEASE flag is honored.
        channelPool = new HonorCloseOnReleaseChannelPool(channelPool);

        // Wrap the channel pool such that HTTP 2 channels won't be released to the underlying pool while they're still in use.
        SdkChannelPool sdkChannelPool = new HttpOrHttp2ChannelPool(channelPool,
                                                                   bootstrap.config().group(),
                                                                   configuration.maxConnections(),
                                                                   configuration);


        sdkChannelPool = new ListenerInvokingChannelPool(bootstrap.config().group(), sdkChannelPool, Arrays.asList(
            // Add a listener that disables auto reads on acquired connections.
            AutoReadDisableChannelPoolListener.create(),

            // Add a listener that ensures acquired channels are marked IN_USE and thus not eligible for certain idle timeouts.
            InUseTrackingChannelPoolListener.create(),

            // Add a listener that removes request-specific handlers with each request.
            HandlerRemovingChannelPoolListener.create(),

            // Add a listener that enables auto reads on released connections.
            AutoReadEnableChannelPoolListener.create()
        ));

        // Wrap the channel pool such that an individual channel can only be released to the underlying pool once.
        sdkChannelPool = new ReleaseOnceChannelPool(sdkChannelPool);

        // Wrap the channel pool to guarantee all channels checked out are healthy, and all unhealthy channels checked in are
        // closed.
        sdkChannelPool = new HealthCheckedChannelPool(bootstrap.config().group(), configuration, sdkChannelPool);

        // Wrap the channel pool such that if the Promise given to acquire(Promise) is done when the channel is acquired
        // from the underlying pool, the channel is closed and released.
        sdkChannelPool = new CancellableAcquireChannelPool(bootstrap.config().group().next(), sdkChannelPool);

        return sdkChannelPool;
    }

    private boolean needSslContext(URI targetAddress) {
        URI proxyAddress = proxyAddress(targetAddress);
        boolean needContext = targetAddress.getScheme().equalsIgnoreCase("https")
                              || proxyAddress != null && proxyAddress.getScheme().equalsIgnoreCase("https");

        return needContext;
    }

    public static class Builder {

        private SdkChannelOptions sdkChannelOptions;
        private SdkEventLoopGroup sdkEventLoopGroup;
        private NettyConfiguration configuration;
        private Protocol protocol;
        private ProtocolNegotiation protocolNegotiation;
        private long maxStreams;
        private int initialWindowSize;
        private Duration healthCheckPingPeriod;
        private SslProvider sslProvider;
        private ProxyConfiguration proxyConfiguration;
        private Boolean useNonBlockingDnsResolver;

        private Builder() {
        }

        public Builder sdkChannelOptions(SdkChannelOptions sdkChannelOptions) {
            this.sdkChannelOptions = sdkChannelOptions;
            return this;
        }

        public Builder sdkEventLoopGroup(SdkEventLoopGroup sdkEventLoopGroup) {
            this.sdkEventLoopGroup = sdkEventLoopGroup;
            return this;
        }

        public Builder configuration(NettyConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder protocolNegotiation(ProtocolNegotiation protocolNegotiation) {
            this.protocolNegotiation = protocolNegotiation;
            return this;
        }

        public Builder maxStreams(long maxStreams) {
            this.maxStreams = maxStreams;
            return this;
        }

        public Builder initialWindowSize(int initialWindowSize) {
            this.initialWindowSize = initialWindowSize;
            return this;
        }

        public Builder healthCheckPingPeriod(Duration healthCheckPingPeriod) {
            this.healthCheckPingPeriod = healthCheckPingPeriod;
            return this;
        }

        public Builder sslProvider(SslProvider sslProvider) {
            this.sslProvider = sslProvider;
            return this;
        }

        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public Builder useNonBlockingDnsResolver(Boolean useNonBlockingDnsResolver) {
            this.useNonBlockingDnsResolver = useNonBlockingDnsResolver;
            return this;
        }

        public AwaitCloseChannelPoolMap build() {
            return new AwaitCloseChannelPoolMap(this);
        }
    }
}
