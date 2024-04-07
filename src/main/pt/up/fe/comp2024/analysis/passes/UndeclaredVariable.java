package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_LITERAL, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        Symbol checkField = table.getFields().stream()
                .filter(param -> param.getName().equals(varRefName)).findFirst().orElse(null);

        // Var is a field, return
        if (checkField != null) {
            varRefExpr.put("type", checkField.getType().getName());
            return null;
        }

        Symbol checkParam = table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(varRefName)).findFirst().orElse(null);

        // Var is a parameter, return
        if (checkParam != null) {
            varRefExpr.put("type", checkParam.getType().getName());
            return null;
        }

        Symbol checkLocal = table.getLocalVariables(currentMethod).stream()
                .filter(param -> param.getName().equals(varRefName)).findFirst().orElse(null);

        // Var is a declared variable, return
        if (checkLocal != null) {
            varRefExpr.put("type", checkLocal.getType().getName());
            return null;
        }


        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }


}
