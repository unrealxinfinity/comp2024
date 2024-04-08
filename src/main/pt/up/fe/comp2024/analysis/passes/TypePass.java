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

import java.util.Optional;

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
        addVisit("NewClassExpr", this::visitNewObject);
        addVisit("ClassFunctionCallExpr", this::visitMethodCall);
        addVisit("This", this::visitThis);
        addVisit("SameClassCallExpr", this::visitThisCall);
    }

    private boolean checkReturnType(JmmNode jmmNode, SymbolTable symbolTable) {
        Optional<Type> returnType = symbolTable.getReturnTypeTry(jmmNode.get("name"));
        if (returnType.isEmpty()) {
            String message = String.format("Method %s does not exist", jmmNode.get("name"));
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(report);
            return false;
        }
        else {
            jmmNode.putObject("type", returnType.get());
            return true;
        }
    }

    private Void visitThisCall(JmmNode jmmNode, SymbolTable symbolTable) {
        if (symbolTable.getSuper() != null && !symbolTable.getMethods().contains(jmmNode.get("name"))) {
            Type assumed = new Type("assumed", false);
            assumed.putObject("assumedTypes", true);
            jmmNode.putObject("type", assumed);
            return null;
        }

        checkReturnType(jmmNode, symbolTable);

        return null;
    }

    private Void visitThis(JmmNode jmmNode, SymbolTable symbolTable) {
        jmmNode.putObject("type", new Type(symbolTable.getClassName(), false));
        return null;
    }

    private Void visitMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {
        Type objType = jmmNode.getJmmChild(0).getObject("type", Type.class);

        if (TypeUtils.isPrimitive(objType)) {
            String message = String.format("Calling method %s on primitive type %s", jmmNode.get("name"), objType.getName());
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC,
                    NodeUtils.getLine(jmmNode),
                    NodeUtils.getColumn(jmmNode),
                    message);
            addReport(report);
        }
        else if (objType.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper() == null) {
            if (checkReturnType(jmmNode, symbolTable)) {
                return null;
            }
        }
        else {
            Type assumed = new Type("assumed", false);
            assumed.putObject("assumedType", true);
            jmmNode.putObject("type", assumed);
        }

        return null;
    }

    private Void visitNewObject(JmmNode jmmNode, SymbolTable symbolTable) {
        Type type = new Type(jmmNode.get("name"), false);
        if (!jmmNode.get("name").equals(symbolTable.getClassName())) {
            type.putObject("assumedTypes", true);
        }
        jmmNode.putObject("type", type);

        return null;
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
