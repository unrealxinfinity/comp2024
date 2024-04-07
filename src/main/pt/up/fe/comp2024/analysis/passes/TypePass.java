package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
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
        addVisit("NewArrayExpr", this::visitArrayExpr);
        addVisit("ArrayExpr", this::visitArrayExpr);
        addVisit("IndexedExpr", this::visitArrayExpr);
    }

    private Void visitArrayExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        Type type = new Type("int", true);
        jmmNode.putObject("type", type);
        return null;
    }

    private Void visitBinaryExpr(JmmNode jmmNode, SymbolTable symbolTable) {
        String op = jmmNode.get("op");

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<")) {
            jmmNode.putObject("type", new Type("int", false));
        }
        else {
            jmmNode.putObject("type", new Type("boolean", false));
        }

        return null;
    }

    private Void visitLen(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.putObject("type", new Type("int", false));

        return null;
    }

    private Void propagateType(JmmNode jmmNode, SymbolTable symbolTable) {
        visit(jmmNode.getJmmChild(0));

        jmmNode.putObject("type", jmmNode.getJmmChild(0).get("type"));

        return null;
    }

    private Void visitBoolLit(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.putObject("type", new Type("boolean", false));

        return null;
    }

    private Void visitIntLit(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.putObject("type", new Type("int", false));

        return null;
    }
}
