package pt.up.fe.comp2024;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2024.analysis.JmmAnalysisImpl;
import pt.up.fe.comp2024.backend.JasminBackendImpl;
import pt.up.fe.comp2024.optimization.JmmOptimizationImpl;
import pt.up.fe.comp2024.parser.JmmParserImpl;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

import java.util.Collections;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        Map<String, String> config = CompilerConfig.parseArgs(args);

        var inputFile = CompilerConfig.getInputFile(config).orElseThrow();
        if (!inputFile.isFile()) {
            throw new RuntimeException("Option '-i' expects a path to an existing input file, got '" + args[0] + "'.");
        }
        String code = SpecsIo.read(inputFile);

        // Parsing stage
        JmmParserImpl parser = new JmmParserImpl();
        JmmParserResult parserResult = parser.parse(code, config);
        //TestUtils.noErrors(parserResult.getReports());

        // Print AST
        System.out.println(parserResult.getRootNode().toTree());


        // Semantic Analysis stage
        JmmAnalysisImpl sema = new JmmAnalysisImpl();
        JmmSemanticsResult semanticsResult = sema.semanticAnalysis(parserResult);
        TestUtils.noErrors(semanticsResult.getReports());


        // Optimization stage
      //  JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
      //  OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
        //TestUtils.noErrors(ollirResult.getReports());
       String ollirCode  = "import ioPlus;\n" +
               "SimpleControlFlow {\n" +
               "\t.construct SimpleControlFlow().V {\n" +
               "\t\tinvokespecial(this, \"<init>\").V;\n" +
               "\t}\n" +
               "\n" +
               "\t.method public static main(args.array.String).V {\n" +
               "\n" +
               "\t\ta.i32 :=.i32 2.i32;\n" +
               "\t\tb.i32 :=.i32 3.i32;\n" +
               "\t\tif (b.i32 >=.bool a.i32) goto ELSE_0;\n" +
               "\t\tinvokestatic(ioPlus, \"printResult\", a.i32).V;\n" +
               "\t\tgoto ENDIF_1;\n" +
               "\t\tELSE_0:\n" +
               "\t\tinvokestatic(ioPlus, \"printResult\", b.i32).V;\n" +
               "\t\tENDIF_1:\n" +
               "\t\tret.V;\n" +
               "\t}\n" +
               "\n" +
               "}";
        OllirResult ollirResult = new OllirResult(semanticsResult,ollirCode,Collections.emptyList());
        // Print OLLIR code
        System.out.println(ollirResult.getOllirCode());


        // Code generation stage

        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
       // TestUtils.noErrors(jasminResult.getReports());
        //jasminResult.compile();
        // Print Jasmin code
        System.out.println(jasminResult.getJasminCode());
       // System.out.println("HERE");
       // jasminResult.run();


    }

}
