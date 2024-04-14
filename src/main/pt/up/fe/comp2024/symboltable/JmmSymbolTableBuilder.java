package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getJmmChild(root.getNumChildren()-1);
        //var classDecl = root.getJmmChild(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        Optional<String> superclass = classDecl.getOptional("superclass");

        String superName = superclass.orElse(null);

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);
        var imports = buildImports(root);

        return new JmmSymbolTable(className, superName, methods, imports, returnTypes, params, locals, fields);
    }

    private static String getImportName(JmmNode method) {
        List<String> full = method.getObjectAsList("name", String.class);
        return full.get(full.size()-1);
    }

    private static List<String> buildImports(JmmNode root) {
        return root.getChildren(IMPORT_DECL).stream()
                .map(JmmSymbolTableBuilder::getImportName)
                .toList();
    }
    private static Type getType(JmmNode type){
        String typeName = type.get("name");
        Boolean isArray = type.getObject("isArray", Boolean.class);
        return new Type(typeName, isArray);
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(
                    method -> map.put(
                        method.get("name"),
                        getType(method.getChildren(TYPE).get(0))
                    )
                );

        return map;
    }

    private static boolean isArrayOrVarargs(JmmNode node) {
        return node.getObject("isArray", Boolean.class) || node.getObject("isVarargs", Boolean.class);
    }

    private static List<Symbol> paramsAux(JmmNode methodDecl) {

        List<Symbol> l = new ArrayList<>();
        for (JmmNode child : methodDecl.getChildren(PARAM)) {
            Type type = new Type(child.getJmmChild(0).get("name"),
                isArrayOrVarargs(child.getJmmChild(0)));
            type.putObject("isVarargs", child.getJmmChild(0).getObject("isVarargs", Boolean.class));
            l.add(new Symbol(type, child.get("name")));
        }
        return l;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), paramsAux(method)));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren(VAR_DECL).stream()
                .map(var -> new Symbol(new Type(var.getJmmChild(0).get("name"), false), var.get("name")))
                .toList();
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getChildren(TYPE).get(0)), varDecl.get("name")))
                .toList();
    }

}
