package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class StaticPass extends AnalysisVisitor {
    
    protected void buildVisitor() {
        addVisit("This", this::checkThisUsage);
    }

    private Void checkThisUsage(JmmNode jmmNode, SymbolTable symbolTable) {
        JmmNode methodDecl = jmmNode.getAncestor(Kind.METHOD_DECL).get();
        if (methodDecl.getObject("isStatic", Boolean.class)) {
            String message = String.format("'this' used in a non-static context in method %s", methodDecl.get("name"));
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(report);
        }

        return null;
    }
}
