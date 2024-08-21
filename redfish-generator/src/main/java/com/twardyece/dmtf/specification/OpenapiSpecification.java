package com.twardyece.dmtf.specification;

import com.twardyece.dmtf.model.mapper.PromotedSchemaModelTypeMapper;
import com.twardyece.dmtf.specification.file.FileList;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class OpenapiSpecification {
    private final Path specDirectory;
    private final List<Pattern> ignoredSchemaFiles;
    private final PromotedSchemaModelTypeMapper promotedSchemaModelTypeMapper;
    private static final Pattern SCHEMA_VERSION = Pattern.compile("([0-9]+)_([0-9]+)_([0-9]+)");
    private static final Pattern VERSIONED_SCHEMA_FILE = Pattern.compile("(?<name>[A-Z][A-Za-z]*).v(?<version>" + SCHEMA_VERSION + ").yaml");
    private static final Pattern UNVERSIONED_SCHEMA_PATTERN = Pattern.compile("(?<name>.*).yaml$");
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenapiSpecification.class);

    public OpenapiSpecification(Path specDirectory, Pattern[] ignoredSchemaFiles, PromotedSchemaModelTypeMapper promotedSchemaModelTypeMapper) {
        this.specDirectory = specDirectory;
        List<Pattern> ignoredSchemaFilesList = new ArrayList<>(List.of(ignoredSchemaFiles));
        ignoredSchemaFilesList.add(Pattern.compile("^openapi.yaml$"));
        this.ignoredSchemaFiles = ignoredSchemaFilesList;
        this.promotedSchemaModelTypeMapper = promotedSchemaModelTypeMapper;
    }

    private static void debugInformDuplicateSchemas(String schema, String file) {
        if (schema.endsWith("_1")) {
            LOGGER.warn("Duplicate schema " + schema + " encountered while parsing " + file);
        }
    }

    /**
     * The public entrypoint and mechanism to get a representation of the Redfish data model read from the filesystem.
     * First, parse the top-level OpenAPI document. Then, go through the list and parse a number of additional ones and
     * merge them into a final document.
     * @return OpenAPI  the Redfish data model as an OpenAPI document
     */
    public OpenAPI getRedfishDataModel() {
        // Read the top-level OpenAPI document first...
        Path openapiDirectory = Path.of(this.specDirectory + "/openapi");
        OpenAPI redfishDataModel = getOpenAPI(openapiDirectory + "/openapi.yaml");
        Components redfishComponents = redfishDataModel.getComponents();

        // Do the same for the other schema files we need to parse
        for (String file : getSchemaFiles(openapiDirectory)) {
            OpenAPI schemaDocument = getOpenAPI(openapiDirectory + "/" + file);
            if (null == schemaDocument.getComponents().getSchemas()) {
                continue;
            }

            // Merge the components
            for (Map.Entry<String, Schema> entry : schemaDocument.getComponents().getSchemas().entrySet()) {
                debugInformDuplicateSchemas(entry.getKey(), file);
                if (!redfishComponents.getSchemas().containsKey(entry.getKey())) {
                    redfishComponents.addSchemas(entry.getKey(), entry.getValue());
                }
            }
        }

        return promoteInlineSchemasFromComponents(promoteInlineSchemasFromPaths(redfishDataModel));
    }

    /**
     * There are some inline schemas in the data model that cannot be easily ignored or represented using types in the
     * Rust standard library. Primarily, these are inline schemas with named properties. They must be promoted to
     * "first-class schemas" so that they get generated definitions.
     * @param openAPI The OpenAPI document to mutate
     * @return The mutated OpenAPI document, with inline schemas promoted
     */
    private OpenAPI promoteInlineSchemasFromComponents(OpenAPI openAPI) {
        Map<String, Schema> pendingPromotions = new HashMap<>();
        openAPI.getComponents().getSchemas()
                .forEach((name, schema) -> Optional.ofNullable(schema.getProperties())
                        .ifPresent(properties -> {
                            HashMap<String, Schema> updatedProperties = new HashMap<>();
                            ((Map<String, Schema>)properties).forEach((propertyName, propertySchema) -> {
                                        if (isAlgebraicProductSchema(propertySchema)) {
                                            String ref = this.promotedSchemaModelTypeMapper.makeSchemaNameForProperty(name, propertyName);
                                            pendingPromotions.put(ref, propertySchema);
                                            updatedProperties.put(propertyName, new Schema().$ref("#/components/schemas/" + ref));
                                        } else if (null != propertySchema.getItems() && isAlgebraicProductSchema(propertySchema.getItems())) {
                                            String ref = this.promotedSchemaModelTypeMapper.makeSchemaNameForProperty(name, propertyName);
                                            pendingPromotions.put(ref, propertySchema.getItems());
                                            updatedProperties.put(propertyName, new ArraySchema().items(new Schema().$ref("#/components/schemas/" + ref)));
                                        }
                                    });
                            schema.getProperties().putAll(updatedProperties);
                        }));
        openAPI.getComponents().getSchemas().putAll(pendingPromotions);

        return openAPI;
    }

    /**
     * Some paths also contain inline schemas detailing response data. Identify these paths and promote them to
     * first-class schemas.
     * @param openAPI the OpenAPI document to check
     * @return The mutated OpenAPI document.
     */
    private OpenAPI promoteInlineSchemasFromPaths(OpenAPI openAPI) {
        Map<String, Schema> inlineSchemas = new HashMap<>();
        openAPI.getPaths()
                .forEach((path, pathItem) -> pathItem.readOperationsMap()
                        .forEach((httpMethod, operation) -> operation.getResponses()
                                .forEach((responseCode, apiResponse) -> Optional.ofNullable(apiResponse.getContent())
                                        .map(content -> content.get("application/json"))
                                        .map(MediaType::getSchema)
                                        .filter(schema -> schema.getProperties() != null)
                                        .ifPresent(schema -> {
                                            String name = path.replaceAll("/", "_")
                                                    + "_" + httpMethod.toString().toLowerCase()
                                                    + "_" + responseCode + "_response";
                                            inlineSchemas.put(name, schema);
                                            apiResponse.getContent().get("application/json")
                                                    .setSchema(new Schema<>().$ref("#/components/schemas/" + name));
                                        }))));
        openAPI.getComponents().getSchemas().putAll(inlineSchemas);
        return openAPI;
    }

    /**
     * Tests if a schema (presumably, an inline schema, though it doesn't matter) represents an algebraic product type.
     * This is important for detecting inline schemas that need to be promoted to first-class, named schemas.
     * @param schema The input schema
     * @return true if the schema represents an algebraic product type.
     */
    private static boolean isAlgebraicProductSchema(Schema schema) {
        return schema.getType() != null && schema.getType().equals("object") && schema.get$ref() == null && schema.getProperties() != null;
    }

    /**
     * Checks whether this schema file matches any patterns that we were instructed to ignore. If it does not, then this
     * schema file is "applied" to the next stage of specification parsing.
     * @param file A filename to check
     * @return true if we want to apply this schema file
     */
    private boolean isApplicableSchemaFile(String file) {
        for (Pattern pattern : ignoredSchemaFiles) {
            Matcher matcher = pattern.matcher(file);
            if (matcher.find()) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param file A filename to check
     * @return true if the filename refers to a versioned schema file
     */
    private static boolean isUnversionedSchema(String file) {
        Matcher matcher = VERSIONED_SCHEMA_FILE.matcher(file);
        return !matcher.find();
    }

    /**
     *
     * @param file The unversioned schema filename to match
     * @param versionedSchemas The list of versioned schemas
     * @return true if `file` does not share the name of a versioned file in versionedSchemas
     */
    private boolean hasNoCorrespondingVersionedSchema(String file, List<VersionedFileDiscovery.VersionedFile> versionedSchemas) {
        Matcher matcher = UNVERSIONED_SCHEMA_PATTERN.matcher(file);
        if (!matcher.find()) {
            return false;
        }

        String name = matcher.group("name");
        Optional<VersionedFileDiscovery.VersionedFile> versionedFile = versionedSchemas
                .stream()
                .filter((f) -> f.name.equals(name))
                .findFirst();
        return versionedFile.isEmpty();
    }

    /**
     * Obtain the list of schema files to parse and apply to the specification. This is not just a listing of the schema
     * directory, there are multiple stages of filtering applied.
     * @param openapiDirectory The path to the directory containing "openapi.yaml"
     * @return A list of the schemas that should be parsed.
     */
    private List<String> getSchemaFiles(Path openapiDirectory) {
        // 1: List the directory and filter out any files that match patterns we were instructed to discard.
        List<String> schemaFiles = Arrays.stream(Objects.requireNonNull(openapiDirectory.toFile().list()))
                .filter(this::isApplicableSchemaFile)
                .toList();

        // 2. For all versioned schemas, filter out all but the most recent version.
        VersionedFileDiscovery versionedFileDiscovery = new VersionedFileDiscovery(new FileList(schemaFiles, openapiDirectory.toString()));
        List<VersionedFileDiscovery.VersionedFile> versionedFiles = versionedFileDiscovery
                .getFiles(VERSIONED_SCHEMA_FILE, "name", "version", SCHEMA_VERSION);

        // 3. For all unversioned schemas, filter out those that have a corresponding versioned schema file.
        Stream<String> unversionedUniqueSchemas = schemaFiles
                .stream()
                .filter(OpenapiSpecification::isUnversionedSchema)
                .filter((f) -> hasNoCorrespondingVersionedSchema(f, versionedFiles));
        return Stream.concat(
                unversionedUniqueSchemas,
                versionedFiles.stream().map((f) -> f.file.getFileName().toString())
        ).toList();
    }

    private static OpenAPI getOpenAPI(String path) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(path, null, parseOptions);

        List<String> messages = result.getMessages();
        if (null != messages) {
            for (String message : messages) {
                LOGGER.warn(message);
            }
        }

        OpenAPI openAPI = result.getOpenAPI();
        if (null == openAPI) {
            throw new RuntimeException("Couldn't parse " + path);
        }
        return openAPI;
    }
}
