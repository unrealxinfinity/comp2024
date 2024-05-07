package pt.up.fe.comp2024.optimization.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantPropagationVisitor extends AnalysisVisitor {
    Map<String, JmmNode> constants;
    boolean inWhile;
    boolean inIfElse;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignment);
        addVisit("WhileStatement", this::visitWhile);
        addVisit(Kind.VAR_REF_LITERAL, this::visitVarRef);
    }

    private Void visitVarRef(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getParent().isInstance(Kind.ASSIGN_STMT) && jmmNode.getIndexOfSelf() == 0) return null;
        if (!constants.containsKey(jmmNode.get("name"))) return null;
        JmmNode propagated = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString(), jmmNode);
        propagated.put("value", constants.get(jmmNode.get("name")).getJmmChild(1).get("value"));
        jmmNode.replace(propagated);
        Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(propagated),
                NodeUtils.getColumn(propagated), "Propagated a constant", null);
        addReport(report);
        return null;
    }

    private Void visitWhile(JmmNode jmmNode, SymbolTable symbolTable) {
        visit(jmmNode.getJmmChild(1));
        visit(jmmNode.getJmmChild(0));
        return null;
    }

    private Void visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        if (!jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_LITERAL)) return null;
        if (!jmmNode.getJmmChild(1).isInstance(Kind.INTEGER_LITERAL)) {
            constants.remove(jmmNode.getJmmChild(0).get("name"));
            return null;
        }
        if (constants.containsKey(jmmNode.getJmmChild(0).get("name"))) {
            JmmNode toRemove = constants.get(jmmNode.getJmmChild(0).get("name"));
            toRemove.getParent().removeChild(toRemove);
        }
        constants.put(jmmNode.getJmmChild(0).get("name"), jmmNode);
        return null;
    }

    private Void visitMethodDecl(JmmNode jmmNode, SymbolTable symbolTable) {
        constants = new HashMap<>();
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.isInstance("Stmt")) continue;
            visit(child);
        }
        for (JmmNode toRemove : constants.values()) {
            toRemove.getParent().removeChild(toRemove);
        }
        return null;
    }
}
