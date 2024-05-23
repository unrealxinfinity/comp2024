package pt.up.fe.comp2024.optimization.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.*;

public class ConstantPropagationVisitor extends AnalysisVisitor {
    Map<String, JmmNode> constants;
    JmmNode endOfWhileConstant = null;

    int inConditional = 0;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignment);
        addVisit("WhileStatement", this::visitWhile);
        addVisit(Kind.VAR_REF_LITERAL, this::visitVarRef);
        addVisit("IfStatement", this::visitIf);
        addVisit("ClassFunctionCallExpr", this::visitCall);
        addVisit(Kind.RETURN_STMT, this::visitReturn);
    }

    private Void visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        if (inConditional == 0 && endOfWhileConstant != null) {
            constants.put(endOfWhileConstant.getJmmChild(0).get("name"), endOfWhileConstant);
            endOfWhileConstant = null;
        }
        return null;
    }

    private Void visitCall(JmmNode jmmNode, SymbolTable symbolTable) {
        if (inConditional == 0 && endOfWhileConstant != null) {
            constants.put(endOfWhileConstant.getJmmChild(0).get("name"), endOfWhileConstant);
            endOfWhileConstant = null;
        }
        constants.entrySet().removeIf(entry -> entry.getValue().getJmmChild(0).getObject("type", Type.class).getObject("level", Integer.class) == 0);
        return null;
    }

    private Void visitIf(JmmNode jmmNode, SymbolTable symbolTable) {
        visit(jmmNode.getJmmChild(0), symbolTable);

        visit(jmmNode.getJmmChild(1), symbolTable);
        Map<String, JmmNode> tempConstants = new HashMap<>(constants);
        visit(jmmNode.getJmmChild(2), symbolTable);

        Map<String, JmmNode> finalConstants = new HashMap<>();
        for (Map.Entry<String, JmmNode> elem : constants.entrySet()) {
            if (!tempConstants.containsKey(elem.getKey())) continue;
            int left = Integer.parseInt(elem.getValue().getJmmChild(1).get("value"));
            int right = Integer.parseInt(tempConstants.get(elem.getKey()).getJmmChild(1).get("value"));
            if (left != right) continue;

            finalConstants.put(elem.getKey(), elem.getValue());
        }

        constants = new HashMap<>(finalConstants);
        return null;
    }

    private Void visitVarRef(JmmNode jmmNode, SymbolTable symbolTable) {
        if (jmmNode.getParent().isInstance(Kind.ASSIGN_STMT) && jmmNode.getIndexOfSelf() == 0) return null;
        if (!constants.containsKey(jmmNode.get("name"))) return null;
        if (inConditional > 0) {
            Optional<JmmNode> assigningTo = jmmNode.getAncestor(Kind.ASSIGN_STMT);
            if (assigningTo.isPresent()) {
                String name = assigningTo.get().getJmmChild(0).get("name");
                if (name.equals(jmmNode.get("name"))) {
                    constants.remove(name);
                    return null;
                }
            }
        }
        JmmNode propagated = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString(), jmmNode);
        propagated.put("value", constants.get(jmmNode.get("name")).getJmmChild(1).get("value"));
        propagated.putObject("type", jmmNode.getObject("type", Type.class));
        jmmNode.replace(propagated);
        Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(propagated),
                NodeUtils.getColumn(propagated), "Propagated a constant", null);
        addReport(report);
        return null;
    }

    private Void visitWhile(JmmNode jmmNode, SymbolTable symbolTable) {
        if (inConditional == 0 && endOfWhileConstant != null) {
            constants.put(endOfWhileConstant.getJmmChild(0).get("name"), endOfWhileConstant);
            endOfWhileConstant = null;
        }
        inConditional++;
        visit(jmmNode.getJmmChild(1), symbolTable);
        inConditional--;
        visit(jmmNode.getJmmChild(0), symbolTable);
        return null;
    }

    private Void visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        if (inConditional == 0 && endOfWhileConstant != null) {
            constants.put(endOfWhileConstant.getJmmChild(0).get("name"), endOfWhileConstant);
            endOfWhileConstant = null;
        }
        if (inConditional == 0 && (jmmNode.getParent().getParent().isInstance("IfStatement") || jmmNode.getParent().getParent().isInstance("WhileStatement"))) return null;
        if (!jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_LITERAL)) return null;
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, symbolTable);
        }
        if (!jmmNode.getJmmChild(1).isInstance(Kind.INTEGER_LITERAL)) {
            constants.remove(jmmNode.getJmmChild(0).get("name"));
            return null;
        }
        if (inConditional > 0) {
            constants.remove(jmmNode.getJmmChild(0).get("name"));
            endOfWhileConstant = jmmNode;
            return null;
        }

        if (constants.containsKey(jmmNode.getJmmChild(0).get("name"))) {
            JmmNode toRemove = constants.get(jmmNode.getJmmChild(0).get("name"));
            //toRemove.getParent().removeChild(toRemove);
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
            //toRemove.getParent().removeChild(toRemove);
        }
        return null;
    }
}
