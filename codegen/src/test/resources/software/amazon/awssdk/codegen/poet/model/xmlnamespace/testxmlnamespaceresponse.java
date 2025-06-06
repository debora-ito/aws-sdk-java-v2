package software.amazon.awssdk.services.protocolrestxml.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.XmlAttributesTrait;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class TestXmlNamespaceResponse extends ProtocolRestXmlResponse implements
                                                                            ToCopyableBuilder<TestXmlNamespaceResponse.Builder, TestXmlNamespaceResponse> {
    private static final SdkField<String> STRING_MEMBER_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("stringMember")
        .getter(getter(TestXmlNamespaceResponse::stringMember))
        .setter(setter(Builder::stringMember))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("stringMember")
                             .unmarshallLocationName("stringMember").build()).build();

    private static final SdkField<Integer> INTEGER_MEMBER_FIELD = SdkField
        .<Integer> builder(MarshallingType.INTEGER)
        .memberName("integerMember")
        .getter(getter(TestXmlNamespaceResponse::integerMember))
        .setter(setter(Builder::integerMember))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("integerMember")
                             .unmarshallLocationName("integerMember").build()).build();

    private static final SdkField<XmlNamespaceMember> XML_NAMESPACE_MEMBER_FIELD = SdkField
        .<XmlNamespaceMember> builder(MarshallingType.SDK_POJO)
        .memberName("xmlNamespaceMember")
        .getter(getter(TestXmlNamespaceResponse::xmlNamespaceMember))
        .setter(setter(Builder::xmlNamespaceMember))
        .constructor(XmlNamespaceMember::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("xmlNamespaceMember")
                             .unmarshallLocationName("xmlNamespaceMember").build(),
                XmlAttributesTrait.create(
                    Pair.of("xmlns:foo",
                            XmlAttributesTrait.AttributeAccessors.builder().attributeGetter((ignore) -> "http://bar")
                                                                 .build()),
                    Pair.of("foo:type",
                            XmlAttributesTrait.AttributeAccessors.builder()
                                                                 .attributeGetter(t -> ((XmlNamespaceMember) t).type()).build()))).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(STRING_MEMBER_FIELD,
                                                                                                   INTEGER_MEMBER_FIELD, XML_NAMESPACE_MEMBER_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final String stringMember;

    private final Integer integerMember;

    private final XmlNamespaceMember xmlNamespaceMember;

    private TestXmlNamespaceResponse(BuilderImpl builder) {
        super(builder);
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.xmlNamespaceMember = builder.xmlNamespaceMember;
    }

    /**
     * Returns the value of the StringMember property for this object.
     *
     * @return The value of the StringMember property for this object.
     */
    public final String stringMember() {
        return stringMember;
    }

    /**
     * Returns the value of the IntegerMember property for this object.
     *
     * @return The value of the IntegerMember property for this object.
     */
    public final Integer integerMember() {
        return integerMember;
    }

    /**
     * Returns the value of the XmlNamespaceMember property for this object.
     *
     * @return The value of the XmlNamespaceMember property for this object.
     */
    public final XmlNamespaceMember xmlNamespaceMember() {
        return xmlNamespaceMember;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        hashCode = 31 * hashCode + Objects.hashCode(integerMember());
        hashCode = 31 * hashCode + Objects.hashCode(xmlNamespaceMember());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TestXmlNamespaceResponse)) {
            return false;
        }
        TestXmlNamespaceResponse other = (TestXmlNamespaceResponse) obj;
        return Objects.equals(stringMember(), other.stringMember()) && Objects.equals(integerMember(), other.integerMember())
               && Objects.equals(xmlNamespaceMember(), other.xmlNamespaceMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("TestXmlNamespaceResponse").add("StringMember", stringMember())
                       .add("IntegerMember", integerMember()).add("XmlNamespaceMember", xmlNamespaceMember()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "stringMember":
                return Optional.ofNullable(clazz.cast(stringMember()));
            case "integerMember":
                return Optional.ofNullable(clazz.cast(integerMember()));
            case "xmlNamespaceMember":
                return Optional.ofNullable(clazz.cast(xmlNamespaceMember()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("stringMember", STRING_MEMBER_FIELD);
        map.put("integerMember", INTEGER_MEMBER_FIELD);
        map.put("xmlNamespaceMember", XML_NAMESPACE_MEMBER_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<TestXmlNamespaceResponse, T> g) {
        return obj -> g.apply((TestXmlNamespaceResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends ProtocolRestXmlResponse.Builder, SdkPojo, CopyableBuilder<Builder, TestXmlNamespaceResponse> {
        /**
         * Sets the value of the StringMember property for this object.
         *
         * @param stringMember
         *        The new value for the StringMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);

        /**
         * Sets the value of the IntegerMember property for this object.
         *
         * @param integerMember
         *        The new value for the IntegerMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder integerMember(Integer integerMember);

        /**
         * Sets the value of the XmlNamespaceMember property for this object.
         *
         * @param xmlNamespaceMember
         *        The new value for the XmlNamespaceMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder xmlNamespaceMember(XmlNamespaceMember xmlNamespaceMember);

        /**
         * Sets the value of the XmlNamespaceMember property for this object.
         *
         * This is a convenience method that creates an instance of the {@link XmlNamespaceMember.Builder} avoiding the
         * need to create one manually via {@link XmlNamespaceMember#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link XmlNamespaceMember.Builder#build()} is called immediately and its
         * result is passed to {@link #xmlNamespaceMember(XmlNamespaceMember)}.
         *
         * @param xmlNamespaceMember
         *        a consumer that will call methods on {@link XmlNamespaceMember.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #xmlNamespaceMember(XmlNamespaceMember)
         */
        default Builder xmlNamespaceMember(Consumer<XmlNamespaceMember.Builder> xmlNamespaceMember) {
            return xmlNamespaceMember(XmlNamespaceMember.builder().applyMutation(xmlNamespaceMember).build());
        }
    }

    static final class BuilderImpl extends ProtocolRestXmlResponse.BuilderImpl implements Builder {
        private String stringMember;

        private Integer integerMember;

        private XmlNamespaceMember xmlNamespaceMember;

        private BuilderImpl() {
        }

        private BuilderImpl(TestXmlNamespaceResponse model) {
            super(model);
            stringMember(model.stringMember);
            integerMember(model.integerMember);
            xmlNamespaceMember(model.xmlNamespaceMember);
        }

        public final String getStringMember() {
            return stringMember;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public final Integer getIntegerMember() {
            return integerMember;
        }

        public final void setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
        }

        @Override
        public final Builder integerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        public final XmlNamespaceMember.Builder getXmlNamespaceMember() {
            return xmlNamespaceMember != null ? xmlNamespaceMember.toBuilder() : null;
        }

        public final void setXmlNamespaceMember(XmlNamespaceMember.BuilderImpl xmlNamespaceMember) {
            this.xmlNamespaceMember = xmlNamespaceMember != null ? xmlNamespaceMember.build() : null;
        }

        @Override
        public final Builder xmlNamespaceMember(XmlNamespaceMember xmlNamespaceMember) {
            this.xmlNamespaceMember = xmlNamespaceMember;
            return this;
        }

        @Override
        public TestXmlNamespaceResponse build() {
            return new TestXmlNamespaceResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }
    }
}
