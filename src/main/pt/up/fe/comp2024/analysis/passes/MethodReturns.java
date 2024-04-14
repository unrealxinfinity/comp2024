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

public class MethodReturns extends AnalysisVisitor {

    protected void buildVisitor() {
        addVisit("ReturnStmt", this::visitReturn);
    }

    private Void visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        JmmNode methodDecl = jmmNode.getAncestor(Kind.METHOD_DECL).get();
        Type returnType = symbolTable.getReturnType(methodDecl.get("name"));

        if (TypeUtils.areTypesAssignable(jmmNode.getJmmChild(0).getObject("type", Type.class), returnType, false, symbolTable)) {
            return null;
        }
        if (jmmNode.getJmmChild(0).getObject("type", Type.class).getOptionalObject("assumedType").isPresent()) {
            return null;
        }

        String message = String.format("Incompatible return types in method %s", methodDecl.get("name"));
        Report report = NodeUtils.createSemanticError(jmmNode, message);
        addReport(report);
        return null;
    }
}
