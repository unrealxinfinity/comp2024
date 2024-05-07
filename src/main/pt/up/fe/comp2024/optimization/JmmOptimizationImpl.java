package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.optimization.passes.ConstantFoldingVisitor;
import pt.up.fe.comp2024.optimization.passes.ConstantPropagationVisitor;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        AnalysisVisitor visitor = new ConstantFoldingVisitor();
        AnalysisVisitor visitor2 = new ConstantPropagationVisitor();
        visitor.analyze(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
        visitor2.analyze(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
        return semanticsResult;
    }
}
