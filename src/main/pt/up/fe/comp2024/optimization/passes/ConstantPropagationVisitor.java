package pt.up.fe.comp2024.optimization.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConstantPropagationVisitor extends AnalysisVisitor {
    private Map<String, Integer> intConstants;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignment);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_LITERAL, this::visitVarRef);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("IfStatement", this::visitIfElse);
    }

    private Void visitWhile(JmmNode jmmNode, SymbolTable symbolTable) {
        visit(jmmNode.getJmmChild(0), symbolTable);
        visit(jmmNode.getJmmChild(1), symbolTable);
        return null;
    }

    private Void visitIfElse(JmmNode jmmNode, SymbolTable symbolTable) {
        visit(jmmNode.getJmmChild(0), symbolTable);
        visit(jmmNode.getJmmChild(1), symbolTable);
        Map<String, Integer> temp = new HashMap<>(intConstants);
        visit(jmmNode.getJmmChild(2), symbolTable);
        intConstants.entrySet().removeIf(entry -> !Objects.equals(temp.get(entry.getKey()), entry.getValue()));
        return null;
    }

    private Void visitVarRef(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getParent().isInstance(Kind.ASSIGN_STMT) && jmmNode.getIndexOfSelf() == 0) return null;

        if (intConstants.containsKey(jmmNode.get("name"))) {
            JmmNode constant = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString(), jmmNode);
            constant.put("value", intConstants.get(jmmNode.get("name")).toString());
            jmmNode.replace(constant);
            Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(constant), NodeUtils.getColumn(constant), "Propagated a constant", null);
            addReport(report);
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode jmmNode, SymbolTable symbolTable) {
        intConstants = new HashMap<>();
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.isInstance("Stmt")) continue;
            visit(child, symbolTable);
        }
        return null;
    }

    private Void visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        if (!jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_LITERAL)) return null;
        if (!jmmNode.getJmmChild(1).isInstance(Kind.INTEGER_LITERAL)) {
            intConstants.remove(jmmNode.getJmmChild(0).get("name"));
            return null;
        }
        intConstants.put(jmmNode.getJmmChild(0).get("name"), Integer.parseInt(jmmNode.getJmmChild(1).get("value")));
        jmmNode.getParent().removeChild(jmmNode.getIndexOfSelf());
        return null;
    }
}
