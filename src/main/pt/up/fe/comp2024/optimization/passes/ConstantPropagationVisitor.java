package pt.up.fe.comp2024.optimization.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.*;

public class ConstantPropagationVisitor extends AJmmVisitor<Boolean, Boolean> {
    Map<String, JmmNode> constants;
    Map<String, JmmNode> forbiddenConstants;
    List<Report> reports = new ArrayList<>();

    boolean lookingForForbiddens = false;

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignment);
        addVisit("WhileStatement", this::visitWhile);
        addVisit(Kind.VAR_REF_LITERAL, this::visitVarRef);
        addVisit("IfStatement", this::visitIf);
        addVisit("ClassFunctionCallExpr", this::visitCall);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode jmmNode, Boolean bool) {
        for (JmmNode child : jmmNode.getChildren()) visit(child, bool);
        return bool;
    }

    private Boolean visitCall(JmmNode jmmNode, Boolean bool) {
        for (JmmNode child : jmmNode.getChildren()) visit(child, bool);
        constants.entrySet().removeIf(entry -> entry.getValue().getJmmChild(0).getObject("type", Type.class).getObject("level", Integer.class) == 0);
        return null;
    }

    private Boolean visitIf(JmmNode jmmNode, Boolean bool) {
        visit(jmmNode.getJmmChild(0), bool);

        visit(jmmNode.getJmmChild(1), bool);
        Map<String, JmmNode> tempConstants = new HashMap<>(constants);
        visit(jmmNode.getJmmChild(2), bool);

        Map<String, JmmNode> finalConstants = new HashMap<>();
        for (Map.Entry<String, JmmNode> elem : constants.entrySet()) {
            if (!tempConstants.containsKey(elem.getKey())) continue;
            int left = Integer.parseInt(elem.getValue().getJmmChild(1).get("value"));
            int right = Integer.parseInt(tempConstants.get(elem.getKey()).getJmmChild(1).get("value"));
            if (left != right) continue;

            finalConstants.put(elem.getKey(), elem.getValue());
        }

        constants = new HashMap<>(finalConstants);
        return bool;
    }

    private Boolean visitVarRef(JmmNode jmmNode, Boolean bool) {
        if (jmmNode.getParent().isInstance(Kind.ASSIGN_STMT) && jmmNode.getIndexOfSelf() == 0) return null;
        if (lookingForForbiddens) {
            forbiddenConstants.put(jmmNode.get("name"), jmmNode);
            return bool;
        }
        if (forbiddenConstants.containsKey(jmmNode.get("name"))) return null;
        if (!constants.containsKey(jmmNode.get("name"))) return null;
        JmmNode propagated = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString(), jmmNode);
        propagated.put("value", constants.get(jmmNode.get("name")).getJmmChild(1).get("value"));
        propagated.putObject("type", jmmNode.getObject("type", Type.class));
        jmmNode.replace(propagated);
        Report report = Report.newLog(Stage.OPTIMIZATION, NodeUtils.getLine(jmmNode), NodeUtils.getColumn(jmmNode),
                "Propagated a constant", null);
        addReport(report);
        return true;
    }

    private Boolean visitWhile(JmmNode jmmNode, Boolean bool) {
        Map<String, JmmNode> tempConstants = new HashMap<>(constants);
        Map<String, JmmNode> tempForbiddens = new HashMap<>(forbiddenConstants);
        constants = new HashMap<>();

        lookingForForbiddens = true;
        visit(jmmNode.getJmmChild(0), bool);
        lookingForForbiddens = false;
        constants = new HashMap<>(tempConstants);

        visit(jmmNode.getJmmChild(1), bool);

        Map<String, JmmNode> afterConstants = new HashMap<>(constants);

        Map<String, JmmNode> finalConstants = new HashMap<>();
        for (Map.Entry<String, JmmNode> elem : constants.entrySet()) {
            if (!tempConstants.containsKey(elem.getKey())) continue;
            int left = Integer.parseInt(elem.getValue().getJmmChild(1).get("value"));
            int right = Integer.parseInt(tempConstants.get(elem.getKey()).getJmmChild(1).get("value"));
            if (left != right) continue;

            finalConstants.put(elem.getKey(), elem.getValue());
        }

        forbiddenConstants.keySet().removeIf(finalConstants::containsKey);
        constants = new HashMap<>(finalConstants);
        visit(jmmNode.getJmmChild(0), bool);
        forbiddenConstants.keySet().removeIf(key -> !tempForbiddens.containsKey(key));
        constants = new HashMap<>(finalConstants);
        return bool;
    }

    private Boolean visitAssignment(JmmNode jmmNode, Boolean bool) {
        if (!jmmNode.getJmmChild(0).isInstance(Kind.VAR_REF_LITERAL)) return null;
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, bool);
        }
        if (!jmmNode.getJmmChild(1).isInstance(Kind.INTEGER_LITERAL)) {
            constants.remove(jmmNode.getJmmChild(0).get("name"));
            return bool;
        }

        constants.put(jmmNode.getJmmChild(0).get("name"), jmmNode);
        return bool;
    }

    private Boolean visitMethodDecl(JmmNode jmmNode, Boolean bool) {
        constants = new HashMap<>();
        forbiddenConstants = new HashMap<>();
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.isInstance("Stmt")) continue;
            visit(child);
        }
        return bool;
    }

    protected void addReport(Report report) {
        reports.add(report);
    }

    protected List<Report> getReports() {
        return reports;
    }


    public List<Report> analyze(JmmNode root) {
        // Visit the node
        visit(root, false);

        // Return reports
        return getReports();
    }
}
