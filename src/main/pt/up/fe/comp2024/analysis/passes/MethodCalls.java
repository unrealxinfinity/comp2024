package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

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
            return symbol.getType().isArray() == paramNode.getJmmChild(0).getObject("isArray", Boolean.class);
        }
    }

    private Void checkSameClassTypes(JmmNode jmmNode, SymbolTable symbolTable) {
        Optional<List<Symbol>> maybeParams = symbolTable.getParametersTry("nonexistent");
        if (maybeParams.isEmpty()) {
            if (symbolTable.getSuper() == null) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Method does not exist");
                addReport(report);
            }

            return null;
        }
        List<Symbol> params = maybeParams.get();

        List<JmmNode> paramNodes = jmmNode.getChildren();
        if (isChecking) {
            paramNodes.remove(0);
        }
        if (paramNodes.isEmpty() && params.isEmpty()) {
            return null;
        }

        for (int i = 0; i < params.size(); i++) {
            Symbol param = params.get(i);
            JmmNode paramNode = paramNodes.get(i);

            if (!param.getType().getName().equals(paramNode.getObject("type", Type.class).getName()) || !arrayCondition(param, paramNode)) {
                Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Parameter type mismatch");
                addReport(report);
            }
        }

        Symbol lastParam = params.get(params.size()-1);
        if (lastParam.getType().getObject("isVarargs", Boolean.class)
                && !paramNodes.get(params.size()-1).getObject("type", Type.class).isArray()) {
            for (int i = params.size(); i < paramNodes.size(); i++) {
                JmmNode paramNode = paramNodes.get(i);
                if (!lastParam.getType().getName().equals(paramNode.getObject("type", Type.class).getName())
                    || paramNode.getObject("type", Type.class).isArray()) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Varargs misuse");
                    addReport(report);
                }
            }
        }

        return null;
    }
}
