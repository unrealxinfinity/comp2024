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
        addVisit("AssignStmt", this::visitAssign);
        addVisit("ArrayAlterIndexStatement", this::visitArrayAssign);
    }

    private void checkLHS(JmmNode jmmNode) {
        boolean isRef = jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_LITERAL);
        boolean isIndexedRef = jmmNode.getJmmChild(0).isInstance("IndexedExpr")
                && jmmNode.getJmmChild(0).getJmmChild(0).isInstance(Kind.VAR_REF_LITERAL);
        if (!isRef && !isIndexedRef) {
            String message = "LHS of assignment is not a variable";
            Report report = NodeUtils.createSemanticError(jmmNode, message);
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
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
            return null;
        }

        //if (rhsType.getName().equals(lhsType.getName()) && !rhsType.isArray() && lhsType.isArray() && indexType.getName().equals("int")) {
        //    return null;
        //}
        if (TypeUtils.areTypesAssignable(rhsType, lhsType, true, symbolTable)) {
            if (rhsType.getOptionalObject("assumedType").isPresent()) {
                jmmNode.getJmmChild(1).putObject("type", new Type(lhsType.getName(), false));
            }
            return null;
        }

        String message = String.format("Invalid assignment from type %s to %s", getFullName(rhsType), getFullName(lhsType));
        Report report = NodeUtils.createSemanticError(jmmNode, message);
        addReport(report);
        return null;
    }

    private String getFullName(Type type) {
        String name;
        if (type.isArray()) {
            name = type.getName() + "[]";
        }
        else {
            name = type.getName();
        }
        return name;
    }

    private Void visitAssign(JmmNode jmmNode, SymbolTable symbolTable) {
        Type lhsType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        Type rhsType = jmmNode.getJmmChild(1).getObject("type", Type.class);

        checkLHS(jmmNode);

        //if (rhsType.getName().equals(lhsType.getName()) && rhsType.isArray() == lhsType.isArray()) {
        //    return null;
        //}
        if (TypeUtils.areTypesAssignable(rhsType, lhsType, false, symbolTable)) {
            if (rhsType.getOptionalObject("assumedType").isPresent()) {
                jmmNode.getJmmChild(1).putObject("type", new Type(lhsType.getName(), lhsType.isArray()));
            }
            return null;
        }


        String message = String.format("Invalid assignment from type %s to %s", getFullName(rhsType), getFullName(lhsType));
        Report report = NodeUtils.createSemanticError(jmmNode, message);
        addReport(report);
        return null;
    }
}
