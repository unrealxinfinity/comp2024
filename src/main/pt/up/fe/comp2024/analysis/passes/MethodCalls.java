package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MethodCalls extends AnalysisVisitor {

    private boolean isChecking = false;

    protected void buildVisitor() {
        addVisit("SameClassCallExpr", this::checkSameClassTypes);
        addVisit("ClassFunctionCallExpr", this::checkClassTypes);
    }

    private Void checkClassTypes(JmmNode jmmNode, SymbolTable symbolTable) {
        Type objType = jmmNode.getJmmChild(0).getObject("type", Type.class);

        if (objType.getName().equals(symbolTable.getClassName())) {
            isChecking = true;
            checkSameClassTypes(jmmNode, symbolTable);
            isChecking = false;
        }

        return null;
    }

    private boolean isArrayOrVarargs(Symbol symbol) {
        return symbol.getType().isArray() || symbol.getType().getObject("isVarargs", Boolean.class);
    }

    private boolean arrayCondition(Symbol symbol, JmmNode paramNode) {
        if (symbol.getType().getObject("isVarargs", Boolean.class)) {
            return true;
        }
        else {
            return symbol.getType().isArray() == paramNode.getObject("type", Type.class).isArray();
        }
    }

    private Void checkSameClassTypes(JmmNode jmmNode, SymbolTable symbolTable) {
        if (!symbolTable.getMethods().contains(jmmNode.get("name"))) {
            if (symbolTable.getSuper() == null) {
                String message = String.format("Method %s does not exist", jmmNode.get("name"));
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
            }

            return null;
        }
        List<Symbol> params = symbolTable.getParameters(jmmNode.get("name"));

        List<JmmNode> paramNodes = jmmNode.getChildren();
        if (isChecking) {
            paramNodes.remove(0);
        }
        if (paramNodes.isEmpty() && params.isEmpty()) {
            return null;
        }

        for (int i = 0; i < params.size(); i++) {
            if (i >= paramNodes.size()) {
                String message = String.format("Incorrect number of parameters passed to method %s", jmmNode.get("name"));
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
                return null;
            }
            Symbol param = params.get(i);
            JmmNode paramNode = paramNodes.get(i);

            if (!param.getType().getName().equals(paramNode.getObject("type", Type.class).getName()) || !arrayCondition(param, paramNode)) {
                String message = String.format("Parameter type mismatch in call to %s", jmmNode.get("name"));
                Report report = NodeUtils.createSemanticError(jmmNode, message);
                addReport(report);
            }
        }

        Symbol lastParam = params.get(params.size()-1);
        if (lastParam.getType().getObject("isVarargs", Boolean.class)
                && !paramNodes.get(params.size()-1).getObject("type", Type.class).isArray()) {
            JmmNode arrayInit = new JmmNodeImpl("ArrayExpr");
            arrayInit.putObject("type", new Type("int", true));

            for (int i = params.size()-1; i < paramNodes.size(); i++) {
                JmmNode paramNode = paramNodes.get(i);
                paramNode.detach();
                arrayInit.add(paramNode);

                if (!lastParam.getType().getName().equals(paramNode.getObject("type", Type.class).getName())
                    || paramNode.getObject("type", Type.class).isArray()) {
                    String message = String.format("Varargs misuse in method %s", jmmNode.get("name"));
                    Report report = NodeUtils.createSemanticError(jmmNode, message);
                    addReport(report);
                }
            }

            jmmNode.add(arrayInit);
        }
        else if (paramNodes.size() != params.size()) {
            String message = String.format("Incorrect number of parameters passed to method %s", jmmNode.get("name"));
            Report report = NodeUtils.createSemanticError(jmmNode, message);
            addReport(report);
        }

        return null;
    }
}
