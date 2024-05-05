package pt.up.fe.comp2024.optimization.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class ConstantFoldingVisitor extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private static int runArithmeticOp(String op, Integer op1, Integer op2) {
        switch (op) {
            case "+" -> {
                return op1 + op2;
            }
            case "-" -> {
                return op1 - op2;
            }
            case "*" -> {
                return op1 * op2;
            }
            case "/" -> {
                return op1 / op2;
            }
            default -> throw new IllegalArgumentException();
        }
    }

    private Void visitBinaryExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        String op = jmmNode.get("op");
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (!left.isInstance(Kind.INTEGER_LITERAL) || !right.isInstance(Kind.INTEGER_LITERAL)) return null;
            int op1 = Integer.parseInt(left.get("value"));
            int op2 = Integer.parseInt(right.get("value"));
            JmmNode folded = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString(), jmmNode);
            folded.put("value", Integer.toString(runArithmeticOp(op, op1, op2)));
            jmmNode.replace(folded);
            Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(folded), NodeUtils.getColumn(folded), "Folded a constant op", null);
            addReport(report);
        }

        return null;
    }
}
