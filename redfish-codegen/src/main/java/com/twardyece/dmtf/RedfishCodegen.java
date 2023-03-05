package com.twardyece.dmtf;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.twardyece.dmtf.api.*;
import com.twardyece.dmtf.model.ModelResolver;
import com.twardyece.dmtf.model.context.ModelContext;
import com.twardyece.dmtf.model.context.factory.*;
import com.twardyece.dmtf.model.mapper.IModelFileMapper;
import com.twardyece.dmtf.model.mapper.SimpleModelMapper;
import com.twardyece.dmtf.model.mapper.UnversionedModelMapper;
import com.twardyece.dmtf.model.mapper.VersionedModelMapper;
import com.twardyece.dmtf.openapi.DocumentParser;
import com.twardyece.dmtf.text.PascalCaseName;
import com.twardyece.dmtf.text.SnakeCaseName;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Redfish Code Generator for the Rust language
 * Based on swagger-parser v3
 */
public class RedfishCodegen {
    private String apiDirectory;
    // TODO: Seems like this is no longer being used. Consider whether to fix that, or remove it.
    private String crateDirectory;
    private ModelResolver modelResolver;
    private OpenAPI document;
    private FileFactory fileFactory;
    static final Logger LOGGER = LoggerFactory.getLogger(RedfishCodegen.class);

    RedfishCodegen(String apiDirectory, String crateDirectory) {
        this.apiDirectory = apiDirectory;
        this.crateDirectory = crateDirectory;

        IModelFileMapper[] mappers = new IModelFileMapper[4];
        mappers[0] = new VersionedModelMapper();
        mappers[1] = new SimpleModelMapper(Pattern.compile("Redfish(?<model>[a-zA-Z0-9]*)"), new SnakeCaseName("redfish"));
        mappers[2] = new SimpleModelMapper(Pattern.compile("odata-v4_(?<model>[a-zA-Z0-9]*)"), new SnakeCaseName("odata_v4"));
        mappers[3] = new UnversionedModelMapper();

        this.modelResolver = new ModelResolver(mappers);
        IModelContextFactory[] factories = new IModelContextFactory[4];
        factories[0] = new EnumContextFactory();
        factories[1] = new StructContextFactory(this.modelResolver);
        factories[2] = new TupleContextFactory(this.modelResolver);
        factories[3] = new UnionContextFactory(this.modelResolver);
        this.fileFactory = new FileFactory(new DefaultMustacheFactory(), factories);

        DocumentParser parser = new DocumentParser(this.apiDirectory + "/openapi.yaml");

        // The DocumentParser will automatically generate names for inlined schemas. Having run the tool and seen (in the
        // console output) that these schemas are assigned autogenerated names, we choose to assign more meaningful names
        // here.
        parser.addInlineSchemaNameMapping("RedfishError_error", "RedfishRedfishError");
        parser.addInlineSchemaNameMapping("_redfish_v1_odata_get_200_response", "odata-v4_ServiceDocument");
        parser.addInlineSchemaNameMapping("_redfish_v1_odata_get_200_response_value_inner", "odata-v4_Service");

        this.document = parser.parse();
    }

    public void generateModels() throws IOException {
        HashMap<String, ModuleContext> modules = new HashMap<>();
        for (Map.Entry<String, Schema> schema : this.document.getComponents().getSchemas().entrySet()) {
            RustType result = this.modelResolver.resolvePath(schema.getKey());
            if (null == result) {
                LOGGER.warn("no match for model " + schema.getValue().getName());
                continue;
            }

            ModuleFile<ModelContext> modelFile = this.fileFactory.makeModelFile(result, schema.getValue());
            if (null != modelFile) {
                modelFile.getContext().moduleContext.registerModel(modules);
                modelFile.generate();
            }
        }

        for (ModuleContext module : modules.values()) {
            ModuleFile file = this.fileFactory.makeModuleFile(module);
            file.generate();
        }
    }

    public void generateApis() throws IOException {
        PathMap map = new PathMap(this.document.getPaths());

        NameMapper[] mappers = new NameMapper[4];
        mappers[0] = new NameMapper(Pattern.compile("^(?<name>[A-Za-z0-9]+)$"), "name");
        mappers[1] = new NameMapper(Pattern.compile("^\\{(?<name>[A-Za-z0-9]+)\\}$"), "name");
        mappers[2] = new NameMapper(Pattern.compile("(?<=\\.)(?<name>[A-Za-z0-9]+)$"), "name");
        mappers[3] = new NameMapper(Pattern.compile("^\\$(?<name>metadata)$"), "name");
        EndpointResolver endpointResolver = new EndpointResolver(mappers);

        List<SnakeCaseName> apiModulePathComponents = new ArrayList<>();
        apiModulePathComponents.add(RustConfig.API_BASE_MODULE);
        CratePath apiModulePath = CratePath.crateLocal(apiModulePathComponents);
        ModuleContext apiModule = new ModuleContext(apiModulePath, null);
        int pathDepth = apiModulePath.getComponents().size();

        MustacheFactory factory = new DefaultMustacheFactory();
        Mustache template = factory.compile("templates/api.mustache");

        Map<PascalCaseName, PascalCaseName> traitNameOverrides = new HashMap<>();
        traitNameOverrides.put(new PascalCaseName("V1"), new PascalCaseName("ServiceRoot"));

        TraitContextFactory traitContextFactory = new TraitContextFactory(this.modelResolver, endpointResolver,
                traitNameOverrides);

        for (TraitContext trait : map.getTraits(traitContextFactory)) {
            if (trait.moduleContext.path.getComponents().size() == pathDepth + 1) {
                apiModule.addNamedSubmodule(trait.moduleContext.path.getLastComponent());
            }
            ModuleFile<TraitContext> file = new ModuleFile<>(trait.moduleContext.path, trait, template);
            file.generate();
        }

        ModuleFile apiFile = this.fileFactory.makeModuleFile(apiModule);
        apiFile.generate();
    }

    public void generateLib() throws IOException {
        ModuleContext context = new ModuleContext(CratePath.crateRoot(), null);
        context.addNamedSubmodule(RustConfig.API_BASE_MODULE);
        context.addNamedSubmodule(RustConfig.MODELS_BASE_MODULE);

        ModuleFile file = this.fileFactory.makeModuleFile(context);
        file.generate();
    }

    public static void main(String[] args) {
        Option apiDirectoryOption = new Option("apiDirectory", true, "Directory containing openapi resource files");
        apiDirectoryOption.setRequired(true);
        Option crateDirectoryOption = new Option("crateDirectory", true, "Directory containing Cargo.toml, into which output sources are written");
        crateDirectoryOption.setRequired(true);

        Options options = new Options();
        options.addOption(apiDirectoryOption);
        options.addOption(crateDirectoryOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine command = parser.parse(options, args);

            String apiDirectory = command.getOptionValue("apiDirectory");
            String crateDirectory = command.getOptionValue("crateDirectory");

            RedfishCodegen codegen = new RedfishCodegen(apiDirectory, crateDirectory);
            codegen.generateModels();
            codegen.generateApis();
            codegen.generateLib();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("RedfishCodegen", options);
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
