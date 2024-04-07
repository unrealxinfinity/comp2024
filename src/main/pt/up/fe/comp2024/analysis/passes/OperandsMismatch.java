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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OperandsMismatch extends AnalysisVisitor {



    public void buildVisitor(){
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntLit);
        addVisit(Kind.BOOLEAN_LITERAL, this::visitBoolLit);
    }

    private Void visitBoolLit(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.put("type", "boolean");

        return null;
    }

    private Void visitIntLit(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.put("type", "int");

        return null;
    }

    private Void visitBinaryExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        String op = jmmNode.get("op");

        visit(left, symbolTable);
        visit(right, symbolTable);
        String leftType = left.get("type");
        String rightType = right.get("type");
        String desiredType;
        String returnedType;

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            desiredType = "int";
            returnedType = "int";
        }
        else if (op.equals("<")) {
            desiredType = "int";
            returnedType = "boolean";
        }
        else {
            desiredType = "boolean";
            returnedType = "boolean";
        }

        jmmNode.put("type", returnedType);

        if (!leftType.equals(desiredType)) {
            Report leftReport = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left type");
            addReport(leftReport);
        }
        if (!rightType.equals(desiredType)) {
            Report rightReport = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right type");
            addReport(rightReport);
        }

        return null;
    }
}
