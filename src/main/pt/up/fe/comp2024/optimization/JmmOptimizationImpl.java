package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.OllirErrorException;
import org.specs.comp.ollir.VarScope;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmVisitor;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.optimization.graph.GraphColorer;
import pt.up.fe.comp2024.optimization.graph.InterferenceGraph;
import pt.up.fe.comp2024.optimization.passes.ConstantFoldingVisitor;
import pt.up.fe.comp2024.optimization.passes.ConstantPropagationVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            InterferenceGraph graph = new InterferenceGraph(method.getVarTable().entrySet()
                    .stream().filter(descriptor -> descriptor.getValue().getScope().equals(VarScope.LOCAL))
                    .map(Map.Entry::getKey).toList());
            graph.buildEdges(analyzer.getDefs(method.getMethodName()), analyzer.getOuts(method.getMethodName()),
                    analyzer.getIns(method.getMethodName()));

            GraphColorer colorer = new GraphColorer(graph);
            if (!colorer.colorGraph(colors)) return false;

            RegisterAllocator allocator = new RegisterAllocator(method.getVarTable(), graph, colors, method.isStaticMethod());
            allocator.allocateRegisters();
        }

        for (Method method : ollirResult.getOllirClass().getMethods()) {
            for (Map.Entry<String, Descriptor> descriptor : method.getVarTable().entrySet().stream()
                    .filter(descriptor -> !descriptor.getValue().getScope().equals(VarScope.FIELD)).toList()) {
                Report report = Report.newLog(Stage.OPTIMIZATION, 0, 0, descriptor.getKey() + ": " + descriptor.getValue().getVirtualReg()
                , null);
                ollirResult.getReports().add(report);
                System.out.println(report);
            }
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
            if (!runRegisterAllocation(ollirResult, regValue)) {
                Report report = Report.newError(Stage.OPTIMIZATION, 0,0,
                        "Couldn't allocate with the specified number of registers!", null);
                ollirResult.getReports().add(report);
            }
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
            List<Report> reports;
            do {
                reports = new ArrayList<>();
                AnalysisVisitor visitor = new ConstantFoldingVisitor();
                ConstantPropagationVisitor visitor2 = new ConstantPropagationVisitor();
                reports.addAll(visitor.analyze(semanticsResult.getRootNode(), semanticsResult.getSymbolTable()));
                reports.addAll(visitor2.analyze(semanticsResult.getRootNode()));
            } while (!reports.isEmpty());
        }

        return semanticsResult;
    }
}
