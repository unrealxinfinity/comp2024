package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DupPass extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethod);
        addVisit(Kind.CLASS_DECL, this::visitClass);
    }

    private Void checkDuplicates(Set<String> set, String message, List<JmmNode> children, JmmNode parent) {
        for (JmmNode child : children) {
            if (set.contains(child.get("name"))) {
                String formatted = String.format(message, child.get("name"), parent.get("name"));
                Report report = NodeUtils.createSemanticError(parent, formatted);
                addReport(report);
            }

            set.add(child.get("name"));
        }

        return null;
    }

    private Void visitClass(JmmNode jmmNode, SymbolTable symbolTable) {
        Set<String> imports = new HashSet<>();
        Set<String> fields = new HashSet<>();

        for (JmmNode child : jmmNode.getJmmParent().getChildren(Kind.IMPORT_DECL)) {
            List<String> path = child.getObjectAsList("name", String.class);
            String className = path.get(path.size()-1);
            if (imports.contains(className)) {
                String message = String.format("Duplicated import %s", className);
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
            }
            imports.add(className);
        }

        checkDuplicates(fields, "Duplicated method %s in class %s", jmmNode.getChildren(Kind.METHOD_DECL), jmmNode);
        checkDuplicates(fields, "Duplicated field %s in class %s", jmmNode.getChildren(Kind.VAR_DECL), jmmNode);

        return null;
    }

    private Void visitMethod(JmmNode jmmNode, SymbolTable symbolTable) {
        Set<String> params = new HashSet<>();
        Set<String> locals = new HashSet<>();

        checkDuplicates(params, "Duplicated parameter %s in method %s", jmmNode.getChildren(Kind.PARAM), jmmNode);

        checkDuplicates(locals, "Duplicated local %s in method %s", jmmNode.getChildren(Kind.VAR_DECL), jmmNode);

        Type retType = symbolTable.getReturnType(jmmNode.get("name"));
        boolean isVoid = retType.getName().equals("void");
        boolean foundReturn = false;

        for (JmmNode stmt : jmmNode.getChildren("Stmt")) {
            if (foundReturn) {
                String message = String.format("Misplaced or duplicate return statement in method %s", jmmNode.get("name"));
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
            }
            if (!stmt.isInstance(Kind.RETURN_STMT)) continue;

            foundReturn = true;
        }
        if (!foundReturn && !isVoid) {
            String message = String.format("Method %s is lacking a return statement", jmmNode.get("name"));
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }
}
