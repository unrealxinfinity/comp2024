package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.optimization.graph.GraphColorer;
import pt.up.fe.comp2024.optimization.graph.InterferenceGraph;
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

        ollirResult.getOllirClass().buildCFGs();
        ollirResult.getOllirClass().buildVarTables();
        LivenessAnalysis analyzer = new LivenessAnalysis();
        analyzer.buildLivenessSets(ollirResult);

        for (Method method : ollirResult.getOllirClass().getMethods()) {
            InterferenceGraph graph = new InterferenceGraph(method.getVarTable().keySet().stream().toList());
            graph.buildEdges(analyzer.getDefs(method.getMethodName()), analyzer.getOuts(method.getMethodName()),
                    analyzer.getIns(method.getMethodName()));

            GraphColorer colorer = new GraphColorer(graph);
            colorer.colorGraph(5);

            RegisterAllocator allocator = new RegisterAllocator(method.getVarTable(), graph, 5);
            allocator.allocateRegisters();
        }

        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (Boolean.parseBoolean(semanticsResult.getConfig().getOrDefault("optimize", "false"))) {
            AnalysisVisitor visitor = new ConstantFoldingVisitor();
            AnalysisVisitor visitor2 = new ConstantPropagationVisitor();
            visitor.analyze(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
            visitor2.analyze(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
        }

        return semanticsResult;
    }
}
