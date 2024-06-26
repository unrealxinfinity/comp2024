package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;

public class ConditionTypes extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("IfStatement", this::checkConditionType);
        addVisit("WhileStatement", this::checkConditionType);
    }

    private Void checkConditionType(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getJmmChild(0).getObject("type", Type.class).getName().equals("boolean")) {
            return null;
        }

        String message = "Condition expression is not boolean";
        Report report = NodeUtils.createSemanticError(jmmNode, message);
        addReport(report);

        return null;
    }
}
