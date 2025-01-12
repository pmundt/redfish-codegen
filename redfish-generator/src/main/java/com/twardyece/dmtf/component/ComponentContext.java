package com.twardyece.dmtf.component;

import com.twardyece.dmtf.ModuleContext;
import com.twardyece.dmtf.rust.RustType;
import com.twardyece.dmtf.text.PascalCaseName;
import com.twardyece.dmtf.text.SnakeCaseName;
import io.swagger.v3.oas.models.PathItem;

import java.util.*;

public class ComponentContext implements Comparable<ComponentContext> {
    public ModuleContext moduleContext;
    public RustType rustType;
    public final RustType baseRegistry;
    public Map<PathItem.HttpMethod, Operation> operationMap;
    public List<Subcomponent> subcomponents;
    public List<Supercomponent> owningComponents;
    public final List<Action> actions;
    public final List<String> paths;
    public PrivilegeRegistry.OperationPrivilegeMapping defaultPrivileges;
    public final List<SubordinatePrivilegeOverride> subordinatePrivilegeOverrides;

    public ComponentContext(RustType rustType, RustType baseRegistry) {
        this.moduleContext = new ModuleContext(rustType.getPath());
        this.rustType = rustType;
        this.baseRegistry = baseRegistry;
        this.operationMap = new HashMap<>();
        this.subcomponents = new ArrayList<>();
        this.owningComponents = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.subordinatePrivilegeOverrides = new ArrayList<>();
    }

    public void addPath(String path) { this.paths.add(path); }
    public PascalCaseName componentName() {
        return new PascalCaseName(this.rustType.getName());
    }
    public Collection<Operation> operations() { return this.operationMap.values(); }
    public boolean hasOwningComponents() { return !this.owningComponents.isEmpty(); }
    public boolean isCollection() { return this.rustType.getName().toString().endsWith("Collection"); }
    public List<PrivilegedOperation> privilegedOperations() {
        List<PrivilegedOperation> privilegedOperations = new ArrayList<>();
        this.operationMap
                .entrySet()
                .stream()
                .filter((entry) -> entry.getValue().requiresAuth)
                .map((entry) -> new PrivilegedOperation(operationNameForMethod(entry.getKey())))
                .forEach(privilegedOperations::add);
        if (!this.operationMap.containsKey(PathItem.HttpMethod.POST) && !this.actions.isEmpty()) {
            privilegedOperations.add(new PrivilegedOperation(operationNameForMethod(PathItem.HttpMethod.POST)));
        }

        return privilegedOperations;
    }

    public static class Operation {
        public PascalCaseName pascalCaseName;
        public boolean requiresAuth;
        public Operation(PascalCaseName pascalCaseName, boolean requiresAuth) {
            this.pascalCaseName = pascalCaseName;
            this.requiresAuth = requiresAuth;
        }

        public SnakeCaseName snakeCaseName() { return new SnakeCaseName(this.pascalCaseName); }
        public String upperSnakeCaseName() { return this.pascalCaseName.toString().toUpperCase(); }
        public boolean isPost() { return this.pascalCaseName.toString().equals("Post"); }
    }

    public record Supercomponent(PascalCaseName componentName, RustType componentType) {}
    public record Subcomponent(SnakeCaseName snakeCaseName, PascalCaseName pascalCaseName, RustType componentType,
                               String componentPath) {}
    public record Action(SnakeCaseName snakeCaseName, PascalCaseName pascalCaseName) {}
    public record SubordinatePrivilegeOverride(RustType owningComponent,
                                               PascalCaseName owningComponentName,
                                               PrivilegeRegistry.OperationPrivilegeMapping privileges) {}
    public record PrivilegedOperation(PascalCaseName privilege) {}

    @Override
    public String toString() { return this.rustType.toString(); }

    @Override
    public int compareTo(ComponentContext o) { return this.rustType.compareTo(o.rustType); }

    @Override
    public int hashCode() {
        return this.rustType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ComponentContext) {
            return this.rustType.equals(((ComponentContext) o).rustType);
        } else {
            return false;
        }
    }

    public static PascalCaseName operationNameForMethod(PathItem.HttpMethod method) {
        PascalCaseName name = null;
        switch (method) {
            case GET -> name = new PascalCaseName("Get");
            case HEAD -> name = new PascalCaseName("Head");
            case POST -> name = new PascalCaseName("Post");
            case PUT -> name = new PascalCaseName("Put");
            case PATCH -> name = new PascalCaseName("Patch");
            case DELETE -> name = new PascalCaseName("Delete");
        }

        return name;
    }
}
