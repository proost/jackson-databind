package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

public class BeanCoercionTest extends BaseMapTest
{
    static class Bean {
        public String a;
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testPOJOFromEmptyString() throws Exception
    {
        // first, verify default settings which do not accept empty String:
        assertFalse(MAPPER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));

        // should be ok to enable dynamically
        _verifyFromEmptyPass(MAPPER.reader()
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));

    }

    public void testPOJOFromEmptyGlobalConfig() throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h ->
                    h.setCoercion(CoercionInputShape.EmptyString,
                            CoercionAction.AsNull))
                .build();
        assertNull(_verifyFromEmptyPass(mapper));

        // Then coerce as empty
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h ->
                h.setCoercion(CoercionInputShape.EmptyString,
                        CoercionAction.AsEmpty))
            .build();
        Bean b = _verifyFromEmptyPass(mapper);
        assertNotNull(b);

        // and finally, "try convert", which aliases to 'null'
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h ->
                h.setCoercion(CoercionInputShape.EmptyString,
                        CoercionAction.TryConvert))
            .build();
        assertNull(_verifyFromEmptyPass(mapper));
    }

    public void testPOJOFromEmptyLogicalTypeConfig() throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.AsNull))
                .build();
        assertNull(_verifyFromEmptyPass(mapper));

        // Then coerce as empty
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.AsEmpty))
                .build();
        Bean b = _verifyFromEmptyPass(mapper);
        assertNotNull(b);

        // But also make fail again with 2-level settings
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> h.setCoercion(CoercionInputShape.EmptyString,
                        CoercionAction.AsNull))
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.Fail))
                .build();
        _verifyFromEmptyFail(mapper);
    }

    public void testPOJOFromEmptyPhysicalTypeConfig() throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = jsonMapperBuilder()
                .withCoercionConfig(Bean.class,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.AsNull))
                .build();
        assertNull(_verifyFromEmptyPass(mapper));

        // Then coerce as empty
        mapper = jsonMapperBuilder()
                .withCoercionConfig(Bean.class,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.AsEmpty))
                .build();
        Bean b = _verifyFromEmptyPass(mapper);
        assertNotNull(b);

        // But also make fail again with 2-level settings, with physical having precedence
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.POJO,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.AsEmpty))
                .withCoercionConfig(Bean.class,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString,
                                CoercionAction.Fail))
                .build();
        _verifyFromEmptyFail(mapper);
    }

    private Bean _verifyFromEmptyPass(ObjectMapper m) throws Exception {
        return _verifyFromEmptyPass(m.reader());
    }

    private Bean _verifyFromEmptyPass(ObjectReader r) throws Exception
    {
        return r.forType(Bean.class)
                .readValue(quote(""));
    }

    private void _verifyFromEmptyFail(ObjectMapper m) throws Exception
    {
        try {
            m.readValue(quote(""), Bean.class);
            fail("Should not accept Empty String for POJO with passed settings");
        } catch (JsonProcessingException e) {
            _verifyFailMessage(e);
        }
    }

    private void _verifyFailMessage(JsonProcessingException e)
    {
        verifyException(e, "Cannot deserialize value of type ");
        verifyException(e, " from empty String ");
        assertValidLocation(e.getLocation());
    }
}