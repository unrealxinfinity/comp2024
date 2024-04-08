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
import pt.up.fe.comp2024.ast.TypeUtils;

public class Assignments extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("AssignmentStatement", this::visitAssign);
        addVisit("ArrayAlterIndexStatement", this::visitArrayAssign);
    }

    private void checkLHS(JmmNode jmmNode) {
        if (jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_EXPR)) {
            String message = "LHS of assignment is not a variable";
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(report);
        }
    }

    private Void visitArrayAssign(JmmNode jmmNode, SymbolTable symbolTable) {
        Type lhsType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Type rhsType = jmmNode.getJmmChild(2).getObject("type", Type.class);
        Type indexType = jmmNode.getJmmChild(1).getObject("type", Type.class);

        checkLHS(jmmNode);

        if (!indexType.getName().equals("int") || indexType.isArray()) {
            String message = String.format("Index expression is of type %s", indexType.getName());
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(report);
            return null;
        }

        //if (rhsType.getName().equals(lhsType.getName()) && !rhsType.isArray() && lhsType.isArray() && indexType.getName().equals("int")) {
        //    return null;
        //}
        if (TypeUtils.areTypesAssignable(rhsType, lhsType, true, symbolTable)) {
            return null;
        }

        String message = String.format("Invalid assignment from type %s to %s", rhsType.getName(), lhsType.getName());
        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                NodeUtils.getLine(jmmNode),
                NodeUtils.getColumn(jmmNode),
                message);
        addReport(report);
        return null;
    }

    private Void visitAssign(JmmNode jmmNode, SymbolTable symbolTable) {
        Type lhsType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Type rhsType = jmmNode.getJmmChild(1).getObject("type", Type.class);

        checkLHS(jmmNode);

        //if (rhsType.getName().equals(lhsType.getName()) && rhsType.isArray() == lhsType.isArray()) {
        //    return null;
        //}
        if (TypeUtils.areTypesAssignable(rhsType, lhsType, false, symbolTable)) {
            return null;
        }

        String message = String.format("Invalid assignment from type %s to %s", rhsType.getName(), lhsType.getName());
        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                NodeUtils.getLine(jmmNode),
                NodeUtils.getColumn(jmmNode),
                message);
        addReport(report);
        return null;
    }
}