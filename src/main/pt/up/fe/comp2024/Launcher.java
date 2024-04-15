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
        //TestUtils.noErrors(semanticsResult.getReports());
        /*

        // Optimization stage
        JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
        OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
        TestUtils.noErrors(ollirResult.getReports());
        */
        // Print OLLIR code
        //System.out.println(ollirResult.getOllirCode());


        // Code generation stage
        String ollircode = """
                import ola.io;
                Simple extends Object {
                                
                                
                .method public add(a.i32, b.i32).i32 {
                tmp0.i32 :=.i32 invokevirtual(this.Simple, "constInstr").i32;
                tmp1.i32 :=.i32 a.i32 +.i32 tmp0.i32;
                c.i32 :=.i32 tmp1.i32;
                ret.i32 c.i32;
                }
                                
                .method public static main(args.array.String).V {
                a.i32 :=.i32 20.i32;
                b.i32 :=.i32 10.i32;
                tmp2.Simple :=.Simple new(Simple).Simple;
                invokespecial(tmp2.Simple, "").V;
                s.Simple :=.Simple tmp2.Simple;
                tmp3.i32 :=.i32 invokevirtual(s.Simple, "add", a.i32, b.i32).i32;
                c.i32 :=.i32 tmp3.i32;
                invokestatic(io, "println", c.i32).V;
                ret.V ;
                }
                                
                .method public constInstr().i32 {
                c.i32 :=.i32 0.i32;
                c.i32 :=.i32 4.i32;
                c.i32 :=.i32 8.i32;
                c.i32 :=.i32 14.i32;
                c.i32 :=.i32 250.i32;
                c.i32 :=.i32 400.i32;
                c.i32 :=.i32 1000.i32;
                c.i32 :=.i32 100474650.i32;
                c.i32 :=.i32 10.i32;
                ret.i32 c.i32;
                }
                                
                .construct Simple().V {
                invokespecial(this, "").V;
                }
                }             
                """;
        OllirResult ollirResult = new OllirResult(semanticsResult,ollircode,Collections.emptyList());
        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
       // TestUtils.noErrors(jasminResult.getReports());
        jasminResult.compile();
        // Print Jasmin code
        System.out.println(jasminResult.getJasminCode());

    }

}
