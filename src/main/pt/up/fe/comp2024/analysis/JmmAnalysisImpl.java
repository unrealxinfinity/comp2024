package pt.up.fe.comp2024.analysis;

import jas.Var;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.passes.*;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import java.util.ArrayList;
import java.util.List;

public class JmmAnalysisImpl implements JmmAnalysis {


    private final List<AnalysisPass> analysisPasses;
    private final List<AnalysisPass> initialPasses;

    public JmmAnalysisImpl() {

        this.initialPasses = List.of(new UndeclaredVariable());
        this.analysisPasses = List.of(new TypePass(), new VarargPass(), new StaticPass(), new OperandsMismatch(),
            new MethodCalls(), new ConditionTypes(), new ArrayExpressions(), new Assignments(), new MethodReturns(), new DupPass(),
                new MainPass());

    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        JmmNode rootNode = parserResult.getRootNode();

        SymbolTable table = JmmSymbolTableBuilder.build(rootNode);

        List<Report> reports = new ArrayList<>();
        int counter = 0;

        for (var analysisPass : initialPasses) {
            try {
                var passReports = analysisPass.analyze(rootNode, table);
                counter += passReports.size();
                reports.addAll(passReports);
            } catch (Exception e) {
                reports.add(Report.newError(Stage.SEMANTIC,
                        -1,
                        -1,
                        "Problem while executing analysis pass '" + analysisPass.getClass() + "'",
                        e)
                );
            }
        }

        if (counter != 0) return new JmmSemanticsResult(parserResult, table, reports);

        // Visit all nodes in the AST
        for (var analysisPass : analysisPasses) {
            try {
                var passReports = analysisPass.analyze(rootNode, table);
                reports.addAll(passReports);
            } catch (Exception e) {
                reports.add(Report.newError(Stage.SEMANTIC,
                        -1,
                        -1,
                        "Problem while executing analysis pass '" + analysisPass.getClass() + "'",
                        e)
                );
            }

        }

        return new JmmSemanticsResult(parserResult, table, reports);
    }
}
