package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class OperandsMismatch extends AnalysisVisitor {

    public void buildVisitor(){
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }
}
