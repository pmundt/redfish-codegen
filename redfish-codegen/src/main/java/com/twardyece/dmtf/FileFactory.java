package com.twardyece.dmtf;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.twardyece.dmtf.model.ModelResolver;
import com.twardyece.dmtf.model.context.ModelContext;
import com.twardyece.dmtf.model.ModelFile;
import com.twardyece.dmtf.model.context.factory.IModelContextFactory;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class FileFactory {
    private MustacheFactory factory;
    private Mustache modelTemplate;
    private Mustache moduleTemplate;
    private IModelContextFactory[] contextFactories;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileFactory.class);

    public FileFactory(MustacheFactory factory, IModelContextFactory[] contextFactories) {
        this.factory = factory;
        this.modelTemplate = factory.compile("templates/model.mustache");
        this.moduleTemplate = factory.compile("templates/module.mustache");
        this.contextFactories = contextFactories;
    }

    public ModelFile makeModelFile(RustType rustType, Schema schema) {
        for (IModelContextFactory factory : this.contextFactories) {
            ModelContext context = factory.makeModelContext(rustType, schema);
            if (null != context) {
                return new ModelFile(context, this.modelTemplate);
            }
        }
        LOGGER.error("No ModelContextFactory matching Rust type " + rustType);
        return null;
    }

    public ModuleFile makeModuleFile(ModuleContext context) {
        return new ModuleFile(context, this.moduleTemplate);
    }

    // TODO: Add a makeTraitFile method here?
}
