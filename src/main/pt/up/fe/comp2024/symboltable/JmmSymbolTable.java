package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmSymbolTable implements SymbolTable {

    private final String className;
    private final String superName;
    private final List<String> methods;
    private final List<String> imports;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;
    private final List<Symbol> fields;

    public JmmSymbolTable(String className, String superName,
                          List<String> methods, List<String> imports,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals, List<Symbol> fields) {
        this.className = className;
        this.superName = superName;
        this.methods = methods;
        this.imports = imports;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        this.fields = fields;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        // TODO: Simple implementation that needs to be expanded
        return new Type(TypeUtils.getIntTypeName(), false);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return Collections.unmodifiableList(params.get(methodSignature));
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.unmodifiableList(locals.get(methodSignature));
    }

}
