package com.twardyece.dmtf.specification;

import com.twardyece.dmtf.text.CaseConversion;
import com.twardyece.dmtf.text.PascalCaseName;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a mechanism to extract _just_ the model name from Redfish Data Model identifier. The pattern
 * matches identifiers that this instance can extract model names from, and the modelGroupName property identifies the
 * named capture group in the pattern that will contain the model name.
 */
public class SimpleModelIdentifierFactory {
    private final Pattern pattern;
    private final String modelGroupName;

    public SimpleModelIdentifierFactory(Pattern pattern, String modelGroupName) {
        this.pattern = pattern;
        this.modelGroupName = modelGroupName;
    }

    /**
     * Determine the model name in a Redfish Data Model identifier. For example, this function may convert the Redfish
     * Data Model identifier "odata-v4_etag" to "Etag" depending on the values of pattern and modelGroupName.
     * @param identifier The Redfish Data Model name.
     * @return a PascalCaseName containing the model name parsed from the Redfish Data Model.
     */
    public Optional<PascalCaseName> modelName(String identifier) {
        Matcher matcher = this.pattern.matcher(identifier);
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(CaseConversion.toPascalCase(matcher.group(this.modelGroupName)));
    }
}
