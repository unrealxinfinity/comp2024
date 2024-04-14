package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

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
                String message = String.format("Only last parameter of %s can be varargs", jmmNode.get("name"));
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
            }
        }

        return null;
    }

    private Void visitVarDecl(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getJmmChild(0).getObject("isVarargs", Boolean.class)) {
            String message = String.format("Variable %s cannot be varargs", jmmNode.get("name"));
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }


}
