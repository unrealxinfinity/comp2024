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
        addVisit("LogicalExpr", this::visitNegation);
        addVisit("ParensExpr", this::propagateInt);
    }

    private Void propagateInt(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getJmmChild(0).isInstance(Kind.INTEGER_LITERAL)) {
            Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(jmmNode), NodeUtils.getColumn(jmmNode), "Folded a parenthesis", null);
            addReport(report);
            jmmNode.replace(jmmNode.getJmmChild(0));
        }
        return null;
    }

    private void replaceAndLog(JmmNode toReplace, JmmNode folded) {
        toReplace.replace(folded);
        Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(folded), NodeUtils.getColumn(folded), "Folded a constant op", null);
        addReport(report);
    }

    private Void visitNegation(JmmNode jmmNode, SymbolTable symbolTable) {
        JmmNode operand = jmmNode.getJmmChild(0);
        if (!operand.isInstance(Kind.BOOLEAN_LITERAL)) return null;
        boolean op1 = Boolean.parseBoolean(operand.get("value"));
        JmmNode folded = new JmmNodeImpl(Kind.BOOLEAN_LITERAL.toString(), jmmNode);
        folded.put("value", Boolean.toString(!op1));
        replaceAndLog(jmmNode, folded);
        return null;
    }

    private static String runArithmeticOp(String op, Integer op1, Integer op2) {
        switch (op) {
            case "+" -> {
                return Integer.toString(op1 + op2);
            }
            case "-" -> {
                return Integer.toString(op1 - op2);
            }
            case "*" -> {
                return Integer.toString(op1 * op2);
            }
            case "/" -> {
                return Integer.toString(op1 / op2);
            }
            case "<" -> {
                return Boolean.toString(op1 < op2);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    private Void visitBinaryExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        String op = jmmNode.get("op");
        //visit(jmmNode.getJmmChild(0));
        //visit(jmmNode.getJmmChild(1));
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        switch (op) {
            case "+", "-", "*", "/"/*, "<"*/ -> {
                if (!left.isInstance(Kind.INTEGER_LITERAL) || !right.isInstance(Kind.INTEGER_LITERAL)) return null;
                int op1 = Integer.parseInt(left.get("value"));
                int op2 = Integer.parseInt(right.get("value"));
                String kind = op.equals("<") ? Kind.BOOLEAN_LITERAL.toString() : Kind.INTEGER_LITERAL.toString();
                JmmNode folded = new JmmNodeImpl(kind, jmmNode);
                folded.put("value", runArithmeticOp(op, op1, op2));
                replaceAndLog(jmmNode, folded);
            }
            case "&&" -> {
                //if (!left.isInstance(Kind.BOOLEAN_LITERAL) || !right.isInstance(Kind.BOOLEAN_LITERAL)) return null;
                //boolean op1 = Boolean.parseBoolean(left.get("value"));
                //boolean op2 = Boolean.parseBoolean(right.get("value"));
                //JmmNode folded = new JmmNodeImpl(Kind.BOOLEAN_LITERAL.toString(), jmmNode);
                //folded.put("value", Boolean.toString(op1 && op2));
                //replaceAndLog(jmmNode, folded);
            }
        }

        return null;
    }
}
