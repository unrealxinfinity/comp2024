package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;

public class ArrayExpressions extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("NewArrayExpr", this::visitNewArray);
        addVisit("ArrayExpr", this::visitArrayInitializer);
        addVisit("IndexedExpr", this::visitIndexedExpr);
    }

    private Void visitIndexedExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        Type varType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Type indexType = jmmNode.getJmmChild(1).getObject("type", Type.class);

        if (!varType.isArray()) {
            String message = "Indexed variable is not an array";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }
        if (!indexType.getName().equals("int") || indexType.isArray()) {
            String message = String.format("Index expression is of type %s", indexType.getName());
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }

    private Void visitNewArray(JmmNode jmmNode, SymbolTable symbolTable) {
        Type type = jmmNode.getJmmChild(0).getObject("type", Type.class);
        if (!type.getName().equals("int") || type.isArray()) {
            String message = "Invalid expression in array initializer";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }

    private Void visitArrayInitializer(JmmNode jmmNode, SymbolTable symbolTable) {
        for (JmmNode child : jmmNode.getChildren()) {
            Type type = child.getObject("type", Type.class);
            if (!type.getName().equals("int") || type.isArray()) {
                String message = "Invalid expression in array initializer";
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
                return null;
            }
        }

        return null;
    }

}
