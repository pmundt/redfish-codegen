package com.twardyece.dmtf.model.mapper;

import com.twardyece.dmtf.text.PascalCaseName;
import com.twardyece.dmtf.text.SnakeCaseName;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PromotedSchemaModelTypeMapper implements IModelTypeMapper {
    private final Map<String, ModelMatchSpecification> types;
    public PromotedSchemaModelTypeMapper(Map<String, ModelMatchSpecification> types) {
        this.types = types;
    }

    @Override
    public Optional<ModelMatchSpecification> matchesType(String name) {
        ModelMatchSpecification match = this.types.get(name);
        if (match == null) {
            return Optional.empty();
        } else {
            // Make a deep copy of the match here.
            List<SnakeCaseName> pathCopy = new ArrayList<>();
            pathCopy.addAll(match.path());
            PascalCaseName modelCopy = new PascalCaseName(match.model());
            ModelMatchSpecification copy = new ModelMatchSpecification(pathCopy, modelCopy);
            return Optional.of(copy);
        }
    }

    /**
     * Construct the name of the promoted schema, given the name of the parent schema and the name of the property. This
     * is a simple procedure, but we keep it here to colocate it with the IModelTypeMapper implementation.
     * @param parentName The name of the parent schema containing the promoted inline schema.
     * @param propertyName The name of the property.
     * @return A name that can be used to uniquely identify this inline schema.
     */
    public String makeSchemaNameForProperty(String parentName, String propertyName) {
        final String name = parentName + "_" + propertyName;
        // NOTE: As of right now, there isn't a "smart" way to convert this schema name into a Rust type--mostly because
        //  we can't anticipate the pattern from name -> type. So, if there's a type that doesn't have an explicit mapping,
        //  we consider that a bug. In the future, we could allow breaking changes to the API and add more interesting
        //  logic here.
        if (!this.types.containsKey(name)) {
            throw new RuntimeException("An unaccounted promoted schema is discovered: " + name
                    + ". Please explicitly add this schema to the list.");
        }
        return name;
    }
}
