package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;

public class VarargPass extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitMethodDecl(JmmNode jmmNode, SymbolTable symbolTable) {
        List<JmmNode> params = jmmNode.getChildren(Kind.PARAM);
        if (params.isEmpty()) return null;

        for (int i = 0; i < params.size() - 1; i++) {
            if (params.get(i).getJmmChild(0).getObject("isVarargs", Boolean.class)) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Only last parameter can be varargs");
                addReport(report);
            }
        }

        return null;
    }

    private Void visitVarDecl(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getJmmChild(0).getObject("isVarargs", Boolean.class)) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Variable cannot be varargs");
            addReport(report);
        }

        return null;
    }


}
