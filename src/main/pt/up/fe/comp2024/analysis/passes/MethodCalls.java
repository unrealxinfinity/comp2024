package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;
import java.util.Optional;

public class MethodCalls extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("SameClassCallExpr", this::checkSameClassTypes);
    }

    private Void checkSameClassTypes(JmmNode jmmNode, SymbolTable symbolTable) {
        Optional<List<Symbol>> params = symbolTable.getParametersTry(jmmNode.get("name"));
        if (params.isEmpty()) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Method does not exist");
            addReport(report);

            return null;
        }

        List<JmmNode> paramNodes = jmmNode.getChildren();

        for (int i = 0; i < paramNodes.size(); i++) {
            Symbol param = params.get().get(i);
            JmmNode paramNode = paramNodes.get(0);

            if (!param.getType().getName().equals(paramNode.get("type"))) {
                //|| param.getType().isArray() != paramNode.getJmmChild(0).getObject("isArray", Boolean.class)) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Parameter type mismatch");
                addReport(report);
            }
        }

        return null;
    }
}
