package com.twardyece.dmtf.policies;

import com.twardyece.dmtf.ModuleFile;
import com.twardyece.dmtf.specification.ODataTypeIdentifier;
import com.twardyece.dmtf.model.context.ModelContext;
import com.twardyece.dmtf.model.context.StructContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ODataPropertyPolicy implements IModelGenerationPolicy {
    private ODataTypeIdentifier identifier;
    private static final List<String> immutableProperties;
    private static final String ODATA_TYPE = "#/components/schemas/odata-v4_type";
    private boolean alwaysDeserialize;
    public ODataPropertyPolicy(ODataTypeIdentifier identifier, boolean alwaysDeserialize) {
        this.identifier = identifier;
        this.alwaysDeserialize = alwaysDeserialize;
    }

    static {
        immutableProperties = new ArrayList<>();
        immutableProperties.add("#/components/schemas/Resource_Name");
        immutableProperties.add("#/components/schemas/Resource_Id");
        immutableProperties.add(ODATA_TYPE);
        immutableProperties.add("#/components/schemas/odata-v4_id");
        immutableProperties.add("#/components/schemas/odata-v4_etag");
        immutableProperties.add("#/components/schemas/odata-v4_context");
    }

    @Override
    public void apply(Map<String, ModuleFile<ModelContext>> models) {
        // For each ModelContext that contains a StructContext...
        for (Map.Entry<String, ModuleFile<ModelContext>> entry : models.entrySet()) {
            StructContext struct = entry.getValue().getContext().structContext;
            if (null != struct) {
                for (StructContext.Property property : struct.properties) {
                    // If the property.openapiType matches one of the few immutable properties, and we are not
                    // required to deserialize all properties, skip deserialization.
                    String openapiType = property.getOpenapiType();
                    if (null != openapiType && !this.alwaysDeserialize && isImmutable(openapiType)) {
                        property.setIsDeserialized(false);
                    }

                    // Set a default value for the odata-v4_Type property, if it exists.
                    if (null != openapiType && openapiType.equals(ODATA_TYPE)) {
                        // Assumption: the odata-v4_type Rust type manifestation is constructible as a tuple (String).
                        property.setDefaultValue(property.type() + "(\\\"" + this.identifier.identify(entry.getKey()) + "\\\".to_string())");
                    }
                }
            }
        }
    }

    private static boolean isImmutable(String propertyType) {
	return immutableProperties.contains(propertyType);
    }
}
