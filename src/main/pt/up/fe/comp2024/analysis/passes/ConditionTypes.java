package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;

public class ConditionTypes extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("IfStatement", this::checkConditionType);
        addVisit("WhileStatement", this::checkConditionType);
    }

    private Void checkConditionType(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getJmmChild(0).get("type").equals("boolean")) {
            return null;
        }

        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Condition is not boolean");
        addReport(report);

        return null;
    }
}
