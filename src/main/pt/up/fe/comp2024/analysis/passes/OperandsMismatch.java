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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OperandsMismatch extends AnalysisVisitor {



    public void buildVisitor(){
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
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
            Report leftReport = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left type");
            addReport(leftReport);
        }
        if (!rightType.getName().equals(desiredType) || rightType.isArray()) {
            Report rightReport = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right type");
            addReport(rightReport);
        }

        return null;
    }
}
