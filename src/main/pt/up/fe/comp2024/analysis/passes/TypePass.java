package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class TypePass extends AnalysisVisitor {
    protected void buildVisitor() {
        addVisit(Kind.INTEGER_LITERAL, this::visitIntLit);
        addVisit(Kind.BOOLEAN_LITERAL, this::visitBoolLit);
        addVisit("ParensExpr", this::propagateType);
        addVisit("LengthFunctionExpr", this::visitLen);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("LogicalExpr", this::visitBoolLit);
    }

    private Void visitBinaryExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        String op = jmmNode.get("op");

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<")) {
            jmmNode.put("type", "int");
        }
        else {
            jmmNode.put("type", "boolean");
        }

        return null;
    }

    private Void visitLen(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.put("type", "int");

        return null;
    }

    private Void propagateType(JmmNode jmmNode, SymbolTable symbolTable) {
        visit(jmmNode.getJmmChild(0));

        jmmNode.put("type", jmmNode.getJmmChild(0).get("type"));

        return null;
    }

    private Void visitBoolLit(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.put("type", "boolean");

        return null;
    }

    private Void visitIntLit(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.put("type", "int");

        return null;
    }
}
