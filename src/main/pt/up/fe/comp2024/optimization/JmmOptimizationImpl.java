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

    private boolean runRegisterAllocation(OllirResult ollirResult, int colors) {
        LivenessAnalysis analyzer = new LivenessAnalysis();
        analyzer.buildLivenessSets(ollirResult);

        for (Method method : ollirResult.getOllirClass().getMethods()) {
            InterferenceGraph graph = new InterferenceGraph(method.getVarTable().keySet().stream().toList());
            graph.buildEdges(analyzer.getDefs(method.getMethodName()), analyzer.getOuts(method.getMethodName()),
                    analyzer.getIns(method.getMethodName()));

            GraphColorer colorer = new GraphColorer(graph);
            if (!colorer.colorGraph(colors)) return false;

            RegisterAllocator allocator = new RegisterAllocator(method.getVarTable(), graph, colors);
            allocator.allocateRegisters();
        }

        return true;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int regValue = Integer.parseInt(ollirResult.getConfig().getOrDefault("registerAllocation", "-1"));
        if (regValue == -1) return ollirResult;

        ollirResult.getOllirClass().buildCFGs();
        ollirResult.getOllirClass().buildVarTables();

        if (regValue != 0) {
            runRegisterAllocation(ollirResult, regValue);
        }
        else {
            int currRegs = 1;
            boolean success;
            do {
                success = runRegisterAllocation(ollirResult, currRegs);
                currRegs++;
            } while (!success);
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
