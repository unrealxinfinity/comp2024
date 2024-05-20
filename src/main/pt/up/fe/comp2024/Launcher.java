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
               "ArrayAccess {\n" +
               "\n" +
               "    .construct ArrayAccess().V {\n" +
               "        invokespecial(this, \"<init>\").V;\n" +
               "    }\n" +
               "\n" +
               "    .method public static main(args.array.String).V {\n" +
               "temp0.i32 :=.i32 5.i32;\n" +
               "a.array.i32 :=.array.i32 new(array, temp0.i32).array.i32;\n" +
               "temp1.i32 :=.i32 0.i32;\n" +
               "a[temp1.i32].i32 :=.i32 1.i32;\n" +
               "temp2.i32 :=.i32 1.i32;\n" +
               "a[temp2.i32].i32 :=.i32 2.i32;\n" +
               "temp3.i32 :=.i32 2.i32;\n" +
               "a[temp3.i32].i32 :=.i32 3.i32;\n" +
               "temp4.i32 :=.i32 3.i32;\n" +
               "a[temp4.i32].i32 :=.i32 4.i32;\n" +
               "temp5.i32 :=.i32 4.i32;\n" +
               "a[temp5.i32].i32 :=.i32 5.i32;\n" +
               "temp8.i32 :=.i32 0.i32;\n" +
               "temp7.i32 :=.i32 a[temp8.i32].i32;\n" +
               "invokestatic(ioPlus, \"printResult\", temp7.i32).V;\n" +
               "temp11.i32 :=.i32 1.i32;\n" +
               "temp10.i32 :=.i32 a[temp11.i32].i32;\n" +
               "invokestatic(ioPlus, \"printResult\", temp10.i32).V;\n" +
               "temp14.i32 :=.i32 2.i32;\n" +
               "temp13.i32 :=.i32 a[temp14.i32].i32;\n" +
               "invokestatic(ioPlus, \"printResult\", temp13.i32).V;\n" +
               "temp17.i32 :=.i32 3.i32;\n" +
               "temp16.i32 :=.i32 a[temp17.i32].i32;\n" +
               "invokestatic(ioPlus, \"printResult\", temp16.i32).V;\n" +
               "temp20.i32 :=.i32 4.i32;\n" +
               "temp19.i32 :=.i32 a[temp20.i32].i32;\n" +
               "invokestatic(ioPlus, \"printResult\", temp19.i32).V;\n" +
               "\n" +
               "ret.V;\n" +
               "    }\n" +
               "\n" +
               "}";
        OllirResult ollirResult = new OllirResult(semanticsResult,ollirCode,Collections.emptyList());
        // Print OLLIR code
        System.out.println(ollirResult.getOllirCode());


        // Code generation stage

        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
       // TestUtils.noErrors(jasminResult.getReports());
        jasminResult.compile();
        // Print Jasmin code
        System.out.println(jasminResult.getJasminCode());
       // System.out.println("HERE");
       jasminResult.run();


    }

}
