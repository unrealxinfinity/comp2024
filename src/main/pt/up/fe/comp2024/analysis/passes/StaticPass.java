package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;

import java.util.Optional;

public class StaticPass extends AnalysisVisitor {
    
    protected void buildVisitor() {
        addVisit(Kind.VAR_REF_LITERAL, this::checkFieldUsage);
        addVisit("This", this::checkThisUsage);
        addVisit("ClassFunctionCallExpr", this::checkCalls);
    }

    private Void checkFieldUsage(JmmNode jmmNode, SymbolTable symbolTable) {
        Type varType = jmmNode.getObject("type", Type.class);
        if (varType.getOptionalObject("level").isEmpty()) {
            return null;
        }

        int level = varType.getObject("level", Integer.class);
        JmmNode methodDecl = jmmNode.getAncestor(Kind.METHOD_DECL).get();
        if (level == 0 && methodDecl.getObject("isStatic", Boolean.class)) {
            String message = String.format("Field %s used in static method %s", jmmNode.get("name"), methodDecl.get("name"));
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }
        return null;
    }

    private Void checkCalls(JmmNode jmmNode, SymbolTable symbolTable) {
        Type objType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Boolean isStatic = ((JmmSymbolTable) symbolTable).isMethodStatic(jmmNode.get("name"));
        if (isStatic == null) {
            return null;
        }
        if (objType.getOptionalObject("isStatic").isPresent() && !isStatic) {
            String message = String.format("Non-static method called on class %s", objType.getName());
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }
        else if (objType.getOptionalObject("isStatic").isEmpty() && isStatic) {
            String message = String.format("Static method called on variable of type %s", objType.getName());
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }
        return null;
    }

    private Void checkThisUsage(JmmNode jmmNode, SymbolTable symbolTable) {
        Optional<JmmNode> methodDecl = jmmNode.getAncestor(Kind.METHOD_DECL);
        if (methodDecl.isEmpty()) {
            String message = "'this' used outside a method";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
            return null;
        }
        if (methodDecl.get().getObject("isStatic", Boolean.class)) {
            String message = String.format("'this' used in a non-static context in method %s", methodDecl.get().get("name"));
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }
}
