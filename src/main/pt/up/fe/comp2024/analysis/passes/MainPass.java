package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class MainPass extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMain);
    }

    private Void visitMain(JmmNode jmmNode, SymbolTable symbolTable) {
        if (!jmmNode.get("name").equals("main")) {
            return null;
        }

        if (!jmmNode.getObject("isStatic", Boolean.class)) {
            String message = "main method is not static";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        if (symbolTable.getParameters("main").size() != 1) {
            String message = "main method needs exactly one parameter";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        JmmNode param = jmmNode.getJmmChild(1).getJmmChild(0);
        if (!param.isInstance("StringArrType")) {
            String message = "main method argument should be String[]";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }
}
