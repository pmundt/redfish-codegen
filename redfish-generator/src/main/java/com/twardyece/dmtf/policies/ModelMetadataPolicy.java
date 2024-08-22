package com.twardyece.dmtf.policies;

import com.twardyece.dmtf.ModuleFile;
import com.twardyece.dmtf.model.context.Metadata;
import com.twardyece.dmtf.model.context.ModelContext;
import com.twardyece.dmtf.specification.JsonSchemaIdentifier;

import java.util.Map;

public class ModelMetadataPolicy implements IModelGenerationPolicy {
    private final JsonSchemaIdentifier jsonSchemaIdentifier;

    public ModelMetadataPolicy(JsonSchemaIdentifier jsonSchemaIdentifier) {
        this.jsonSchemaIdentifier = jsonSchemaIdentifier;
    }

    @Override
    public void apply(Map<String, ModuleFile<ModelContext>> models) {
        models.forEach((name, model) -> this.jsonSchemaIdentifier.identify(name)
                .ifPresent(identifier -> model.getContext().metadata = new Metadata(identifier)));
    }
}
