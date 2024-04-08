package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.JmmAnalysisImpl;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//import static jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle.returnType;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    String superClass;
    String thisClass;
    private final SymbolTable symbolTable;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.symbolTable = ollirResult.getSymbolTable();
        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class,this::generateCall);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }
    private String getImportedPackage(String p){
        return "";
    }

    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        var signatureTemp = ollirResult.getOllirClass().getClassAccessModifier().name().toLowerCase();
        var signature = signatureTemp.equals("default") ? "" : signatureTemp + " ";
        var packageName = ollirResult.getOllirClass().getPackage();
        var pkg = (packageName == null) ? "" : packageName+"/";
        var imports = ollirResult.getOllirClass().getImports();
        var fields = ollirResult.getOllirClass().getFields();
        System.out.println(signature);
        code.append(".class ").append(signature).append(pkg).append(className).append(NL);
        this.thisClass = pkg + className;
        // TODO: Hardcoded to Object, needs to be expanded
        //Appends the class and superclass according to their package
        var superclass = ollirResult.getOllirClass().getSuperClass();
        if(superclass != null){
            code.append(".super "+ pkg + superclass).append(NL);
            superClass=pkg + superclass;
        }
        else{
            code.append(".super java/lang/Object").append(NL);
        }
        //Appends the class fields to the jasmin code
        for(var field: fields){
            var accessModifier = field.getFieldAccessModifier().toString().toLowerCase();
            var isFinal = field.isFinalField()?"final":"";
            var isStatic = field.isStaticField()?"static":"";
            var type = generateJasminType(field.getFieldType().getTypeOfElement());
            var isInitialized = field.isInitialized()? " = " + field.getInitialValue():"";
            code.append(".field "+ accessModifier + " " + isStatic + " " + isFinal + " " + type + isInitialized).append(NL);
        }
        System.out.println(code.toString());
        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);
        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }
            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String storeLoadInstWithType(ElementType type,Boolean isStore){
        if (isStore){
            return switch (type) {
                case INT32,BOOLEAN -> "istore";
                case ARRAYREF,OBJECTREF,CLASS,THIS,STRING-> "astore";
                default -> "error"; // need to add to the list of reports
            };
        }
        else{
            return switch (type) {
                case INT32,BOOLEAN -> "iload";
                case ARRAYREF,OBJECTREF,CLASS,THIS,STRING-> "aload";
                default -> "error"; // need to add to the list of reports
            };
        }

    }
    private String instWithType(ElementType type,OperationType opType){
         switch (type) {
            case INT32:  switch (opType){
                case ADD: return "iadd";
                case SUB: return "isub";
                case MUL: return "imul";
                case DIV: return "idiv" ;
            };
            case BOOLEAN: switch (opType){
                case EQ:
                case SUB:
                case AND:
                case OR:
                case XOR: break;
            };
            case ARRAYREF:
            case OBJECTREF:
            case CLASS:
            case THIS:
            case STRING: break;
            default: return "error"; // need to add to the list of reports
        };
         return "error";
    }
    private String generateJasminType(ElementType type){
        return switch (type) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> "[";
            case OBJECTREF -> "Ljava/lang/Object";
            case CLASS -> "L";
            case THIS -> "L";
            case STRING -> "Ljava/lang/String";
            case VOID -> "V";
            default -> "error";
        };
    }
    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();
        var methodParams = method.getParams();
        var returnElem = method.getReturnType().getTypeOfElement();
        var returnType = generateJasminType(returnElem);


        // TODO: Hardcoded param types and return type, needs to be expanded
        //code.append("\n.method ").append(modifier).append(methodName).append("(I)I").append(NL);
        //not hardcoded anymore i think?
        code.append("\n.method ").append(modifier).append(methodName).append("(");
        for(var param:methodParams){
            code.append(generateJasminType(param.getType().getTypeOfElement()));
        }
        code.append(")").append(returnType).append(NL);
        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            System.out.println(inst);
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }
    private String generateCall(CallInstruction call){
        var code = new StringBuilder();
        //Gets the call instruction
        //generates the type of invokation instruction
        code.append(call.getInvocationType().toString().toLowerCase()).append(" ");
        System.out.println(call.getMethodName().equals(null));
        var func = (LiteralElement) call.getMethodName();
        var funcToCall="";
        var caller = call.getCaller().getType().getTypeOfElement();
        if(caller.equals(ElementType.THIS)){
            if(func != null){
                funcToCall = this.thisClass+ "/" + func.getLiteral();
            }
            else{
                funcToCall = this.thisClass+ "/" + "<init>";
            }
        }
        else{
            if(func != null){
                funcToCall  = this.superClass + "/" + func.getLiteral();
            }
            else{
                funcToCall = this.superClass+ "/" + "<init>";
            }
        }
        funcToCall = funcToCall.replace("\"", "");
        System.out.println(funcToCall);
        code.append(funcToCall).append("(");
        //translates the list of args
        var arguments = "";
        for(var arg : call.getArguments()){
            arguments += generateJasminType(arg.getType().getTypeOfElement());
        }
        code.append(arguments).append(")").append(generateJasminType(call.getReturnType().getTypeOfElement()));
        System.out.println(code.toString());
        return code.toString();
    }
    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        var type = currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement();
        // not hardcoded anymore
        var inst = storeLoadInstWithType(type,true);
        code.append(inst+"_").append(reg).append(NL);
        System.out.println(reg);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        //changed the hardcoded version with integer
        var type = currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement();
        var inst = storeLoadInstWithType(type,false);
        return inst + "_" + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));
        var type1 = binaryOp.getLeftOperand().getType().getTypeOfElement();
        var type2 = binaryOp.getRightOperand().getType().getTypeOfElement();

        if (!type1.equals(type2)){
            //Add error report here
        }

        // apply operation
        var op = instWithType(type1,binaryOp.getOperation().getOpType());
        if (op.equals("error")) throw new NotImplementedException(binaryOp.getOperation().getOpType());
        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        //not hardcoded anymore
        code.append(generators.apply(returnInst.getOperand()));
        code.append("ireturn").append(NL);

        return code.toString();
    }

}
