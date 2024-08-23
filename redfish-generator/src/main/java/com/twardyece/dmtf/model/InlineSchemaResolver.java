package com.twardyece.dmtf.model;

import com.twardyece.dmtf.rust.RustType;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Optional;

public class InlineSchemaResolver {
    private static boolean isNullEnum(Schema schema) {
        final boolean typeIsString = schema.getType() != null && schema.getType().equals("string");
        final boolean isSingletonEnumOfNull = schema.getEnum() != null && schema.getEnum().size() == 1
                && (schema.getEnum().get(0) == null || schema.getEnum().get(0).equals("null"));
        return typeIsString && isSingletonEnumOfNull;
    }

    private static Optional<String> getInlineOptionalSchemaRef(Schema schema) {
        if (schema.getOneOf() == null || schema.getOneOf().size() != 2) {
            return Optional.empty();
        }
        Schema first = (Schema) schema.getOneOf().get(0);
        Schema second = (Schema) schema.getOneOf().get(1);

        if (isNullEnum(first)) {
            return Optional.of(second.get$ref());
        } else if (isNullEnum(second)) {
            return Optional.of(first.get$ref());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Resolves an inline "option" schema to the corresponding RustType. An inline "option" schema is one that is
     * captured as "oneOf" where one variant is a singleton enum where the only variant is null, and the other is a ref
     * to a first-class type. For these schemas, the first-class type is usually a tuple that contains String, so we
     * just resolve to the first-class type.
     * @param schema the (possibly) inline "string" schema
     * @return Optional.of(the corresponding RustType) if schema is an inline "string" schema, Optional.empty() otherwise.
     */
    public static Optional<RustType> resolveInlineOptionalSchema(Schema schema, ModelResolver modelResolver) {
        return getInlineOptionalSchemaRef(schema).map(ModelResolver::getSchemaIdentifier).map(modelResolver::resolvePath);
    }
}
