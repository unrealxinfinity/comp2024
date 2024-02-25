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

    private static List<String> buildImports(JmmNode root) {
        return root.getChildren(IMPORT_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }
    private static Type returnType(JmmNode type){
        String typeName = type.get("name");
        if(typeName.equals(TypeUtils.getIntTypeName())) {
            return new Type(TypeUtils.getIntTypeName(), false);
        }
        else if(typeName.equals( TypeUtils.getIntArrayTypeName())){
            return new Type(TypeUtils.getIntTypeName(),true);
        }
        else if(typeName.equals(TypeUtils.getBoolTypeName())){
            return new Type(TypeUtils.getBoolTypeName(),false);
        }
        else if(typeName.equals(TypeUtils.getStringArrayTypeName())) {
            return new Type(TypeUtils.getStringTypeName(), true);
        }
        else if (typeName.equals( TypeUtils.getStringTypeName())){
            return new Type(TypeUtils.getStringTypeName(),true);
        }
        else if (typeName.equals(TypeUtils.getVoidTypeName())){
            return new Type(TypeUtils.getVoidTypeName(),false);
        }
        else{
            return new Type(typeName,false);
        }
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(
                    method -> map.put(
                        method.get("name"),
                        returnType( method.getChildren(TYPE).get(0))
                    )
                );

        return map;
    }

    private static List<Symbol> paramsAux(JmmNode methodDecl) {

        List<Symbol> l = new ArrayList<>();
        for (JmmNode child : methodDecl.getChildren(PARAM)) {
            l.add(new Symbol(new Type(child.getJmmChild(0).get("name"), false), child.get("name")));
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

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
