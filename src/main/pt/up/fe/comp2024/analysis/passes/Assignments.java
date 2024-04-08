package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

public class Assignments extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("AssignmentStatement", this::visitAssign);
        addVisit("ArrayAlterIndexStatement", this::visitArrayAssign);
    }

    private Void visitArrayAssign(JmmNode jmmNode, SymbolTable symbolTable) {
        Type lhsType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Type rhsType = jmmNode.getJmmChild(2).getObject("type", Type.class);
        Type indexType = jmmNode.getJmmChild(1).getObject("type", Type.class);

        if (jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_EXPR)) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "LHS is not a variable");
            addReport(report);
        }
        if (!indexType.getName().equals("int") || indexType.isArray()) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Index is not int");
            addReport(report);
        }

        //if (rhsType.getName().equals(lhsType.getName()) && !rhsType.isArray() && lhsType.isArray() && indexType.getName().equals("int")) {
        //    return null;
        //}
        if (TypeUtils.areTypesAssignable(rhsType, lhsType, true)) {
            return null;
        }

        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Invalid assignment");
        addReport(report);
        return null;
    }

    private Void visitAssign(JmmNode jmmNode, SymbolTable symbolTable) {
        Type lhsType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Type rhsType = jmmNode.getJmmChild(1).getObject("type", Type.class);

        if (jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_EXPR)) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "LHS is not a variable");
            addReport(report);
        }

        //if (rhsType.getName().equals(lhsType.getName()) && rhsType.isArray() == lhsType.isArray()) {
        //    return null;
        //}
        if (TypeUtils.areTypesAssignable(rhsType, lhsType, false)) {
            return null;
        }

        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Invalid assignment");
        addReport(report);
        return null;
    }
}
