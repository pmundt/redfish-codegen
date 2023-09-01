package com.twardyece.dmtf.model.mapper;

import com.twardyece.dmtf.specification.IdentifierParseError;
import com.twardyece.dmtf.specification.VersionedSchemaIdentifier;
import com.twardyece.dmtf.text.SnakeCaseName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VersionedModelToMapper implements IModelTypeMapper {
    public VersionedModelToMapper() {}

    @Override
    public Optional<ModelMatchResult> matches(String name) {
        try {
            VersionedSchemaIdentifier identifier = new VersionedSchemaIdentifier(name);
            List<SnakeCaseName> module = new ArrayList<>();
            module.add(new SnakeCaseName(identifier.getModule()));
            module.add(identifier.getVersion());

            return Optional.of(new ModelMatchResult(module, identifier.getModel()));
        } catch (IdentifierParseError e) {
            return Optional.empty();
        }
    }
}
