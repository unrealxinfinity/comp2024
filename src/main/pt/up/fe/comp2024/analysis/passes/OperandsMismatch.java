package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OperandsMismatch extends AnalysisVisitor {



    public void buildVisitor(){
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("LogicalExpr", this::visitLogicalExpr);
    }

    private Void visitLogicalExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        Type exprType = jmmNode.getObject("type", Type.class);

        if (exprType.getName().equals("boolean")) {
            return null;
        }

        String message = String.format("Expected type boolean, got %s", exprType.getName());
        Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                NodeUtils.getLine(jmmNode),
                NodeUtils.getColumn(jmmNode),
                message);
        addReport(report);

        return null;
    }

    private Void visitBinaryExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        String op = jmmNode.get("op");

        visit(left, symbolTable);
        visit(right, symbolTable);
        Type leftType = left.getObject("type", Type.class);
        Type rightType = right.getObject("type", Type.class);
        String desiredType;

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            desiredType = "int";
        }
        else if (op.equals("<")) {
            desiredType = "int";
        }
        else {
            desiredType = "boolean";
        }

        if (!leftType.getName().equals(desiredType) || leftType.isArray()) {
            String leftName;
            if (leftType.isArray()) {
                leftName = leftType.getName() + "[]";
            }
            else {
                leftName = leftType.getName();
            }
            String message = String.format("Expected type %s in left operand, got %s", desiredType, leftName);
            Report leftReport = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(leftReport);
        }
        if (!rightType.getName().equals(desiredType) || rightType.isArray()) {
            String message = String.format("Expected type %s in right operand, got %s", desiredType, rightType.getName());
            Report rightReport = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(rightReport);
        }

        return null;
    }
}
