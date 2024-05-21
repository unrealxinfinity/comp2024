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

import java.util.*;
import java.util.function.BinaryOperator;
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
    Integer extraRerence=0;
    Integer cmpLabelNumbers=0;
    int stackSize;
    int maxStack;
    private final SymbolTable symbolTable;

    private final FunctionClassMap<TreeNode, String> generators;

    private String translatePackage(String pkg){
        var path= pkg.split("\\.");
        path = Arrays.copyOf(path,path.length-1);
        return String.join("/",path) + "/";
    }

    private String getPackageFromImport(String name){
        var imports = ollirResult.getOllirClass().getImports();
        for (var imp: imports){
            var temp = imp.split("\\.");
            if(temp.length>1){
                var path = Arrays.copyOf(temp,temp.length-1);
                var evaluatedClass = temp[temp.length-1];
                evaluatedClass = evaluatedClass.replaceAll("\\[\\]", "");
                if(name.equals(evaluatedClass)){
                    return String.join("/",path).replaceAll("\\[\\]", "") + "/";
                }
            }
        }
        return "" ;
    }
    private String loadBooleanLiteral(){
        pushToStack();
        StringBuilder code = new StringBuilder();
        code.append("cmp_true_"+this.cmpLabelNumbers).append(NL);
        code.append("iconst_0");
        code.append("cmp_true"+this.cmpLabelNumbers+":").append(NL);
        code.append("iconst_1");
        var branch ="cmp_true_"+this.cmpLabelNumbers;
        this.cmpLabelNumbers++;
        return branch;
    }
    private String negBooleanLiteral(){
        pushToStack();
        StringBuilder code = new StringBuilder();
        code.append("iconst_1").append(NL);
        code.append("ixor");
        return code.toString();
    }
    private String instWithOp(Operation op,Boolean inc){
        var opType = op.getOpType();
        var type = op.getTypeInfo().getTypeOfElement();
        if(!inc){
            switch (type) {
                case INT32:  switch (opType){
                    case ADD: return "iadd";
                    case SUB: return "isub";
                    case MUL: return "imul";
                    case DIV: return "idiv";
                };
                case BOOLEAN: switch (opType){
                    case LTH: return "if_icmplt";
                    case LTE: return "if_icmple";
                    case GTH: return "if_icmpgt";
                    case GTE: return "if_icmpge";
                    case EQ: return "if_icmpeq";
                    case NEQ: return "if_icmpne";
                    case AND:
                    case OR:
                    case NOTB: return negBooleanLiteral();
                    case XOR: break;
                };
                case ARRAYREF:switch(opType){

                };
                case OBJECTREF: switch(opType){
                    case EQ: return "if_acmpeq";
                    case NEQ: return "if_acmpne";
                    default: break;
                }
                case CLASS:
                case THIS:
                case STRING: break;
                default: return "error"; // need to add to the list of reports
            }
        }
        else{
            switch (type) {
                case INT32:  switch (opType){
                    case ADD: return "iinc";
                };

                default: return "error"; // need to add to the list of reports
            }
        }
        return "error";
    }
    private String generateJasminType(Type type){
        var t = type.getTypeOfElement();
        if (t.equals(ElementType.INT32)) {
            return "I";
        } else if (t.equals(ElementType.BOOLEAN)) {
            return "Z";
        } else if (t.equals(ElementType.ARRAYREF)) {
            var temp = ((ArrayType)type).getElementType();
            return "[" + generateJasminType(temp);
        } else if (t.equals(ElementType.OBJECTREF)) {
            var temp = "L"+getPackageFromImport(((ClassType)type).getName()) + ((ClassType)type).getName()+";";
            return temp;
        } else if (t.equals(ElementType.CLASS)) {
            return "L";
        } else if (t.equals(ElementType.THIS)) {
            return this.thisClass;
        } else if (t.equals(ElementType.STRING)) {
            return "Ljava/lang/String;";
        } else if (t.equals(ElementType.VOID)) {
            return "V";
        } else {
            return "error";
        }
    }
    private String generateArrayElementType(Type type){
        ElementType elementstype = type.getTypeOfElement();
        switch(elementstype){
            case INT32: return "int";
            case BOOLEAN: return "boolean";
            default: return "";
        }
    }
    private String storeLoadInstWithType(Type type,Boolean isStore,Boolean isArrayOperand){
        ElementType elementType = type.getTypeOfElement();
        if (isStore){
            return switch (elementType) {
                case INT32,BOOLEAN-> "istore";
                case OBJECTREF,CLASS,THIS,STRING-> "astore";
                case ARRAYREF ->{
                    if(isArrayOperand){
                        var elementstype = ((ArrayType) type).getElementType();
                        switch(elementstype.getTypeOfElement()){
                            case INT32: yield "iastore";
                            case OBJECTREF:yield "aastore";
                            default: yield "error";
                        }
                    }
                    else{
                        yield "astore";
                    }

                }
                default -> "error"; // need to add to the list of reports
            };
        }
        else{
            return switch (elementType) {
                case INT32,BOOLEAN -> "iload";
                case OBJECTREF,CLASS,THIS,STRING-> "aload";
                case ARRAYREF -> {
                    if(isArrayOperand){
                        var elementstype = ((ArrayType) type).getElementType();
                        switch(elementstype.getTypeOfElement()){
                            case INT32: yield "iaload";
                            case OBJECTREF:yield "aaload";
                            default: yield "error";
                        }
                    }
                    else{
                        yield "aload";
                    }

                }
                default -> "error"; // need to add to the list of reports
            };
        }

    }
    private String returnInstWithType(Type type){
        return switch (type.getTypeOfElement()) {
            case INT32,BOOLEAN -> "ireturn";
            case ARRAYREF,OBJECTREF,CLASS,THIS,STRING-> "areturn";
            default -> "error"; // need to add to the list of reports
        };
    }
    private String distinguishLiteral(String literal){
        boolean isNumber = literal.matches("\\d+");
        if(isNumber) {
            if(Integer.parseInt(literal)>=-1 && Integer.parseInt(literal)<=5){
                return "iconst_";
            }
            else if(Integer.parseInt(literal)>=-128 && Integer.parseInt(literal)<=127){
                return "bipush ";
            }
            else if(Integer.parseInt(literal)>=-32768 && Integer.parseInt(literal)<=32767){
                return "sipush ";
            }
        }
        return "ldc ";
    }
    private String findLabel(Instruction inst){
        for(var entry :this.currentMethod.getLabels().entrySet()){
            var k = entry.getKey();
            var ins = entry.getValue();
            if(ins.equals(inst)){
                return k+":"+NL;
            }
        }
        return "";
    }
    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.symbolTable = ollirResult.getSymbolTable();
        this.thisClass = ollirResult.getOllirClass().getClassName();
        this.superClass = ollirResult.getOllirClass().getSuperClass();

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
        generators.put(UnaryOpInstruction.class,this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class,this::generateCall);
        generators.put(FieldInstruction.class,this::generateFieldInst);
        generators.put(CondBranchInstruction.class,this::generateConditional);
        generators.put(GotoInstruction.class,this::generateGoTo);
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
    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = this.thisClass;
        var signatureTemp = ollirResult.getOllirClass().getClassAccessModifier().name().toLowerCase();
        var signature = signatureTemp.equals("default") ? "" : signatureTemp + " ";
        var pkg = "";
        var fields = ollirResult.getOllirClass().getFields();
        code.append(".class ").append(signature).append(pkg).append(className).append(NL);
        // TODO: Hardcoded to Object, needs to be expanded
        //Appends the class and superclass according to their package
        var superclass = this.superClass;
        if(superclass != null){
            if(superClass.equals("Object")){
                code.append(".super java/lang/Object").append(NL);
            }
            else{
                pkg = getPackageFromImport(superclass);
                code.append(".super "+ pkg + superclass).append(NL);
            }
        }
        else{
            superclass="Object";
            pkg="java/lang/";
            code.append(".super java/lang/Object").append(NL);
        }

        //Appends the class fields to the jasmin code
        for(var field: fields){
            var accessModifier = field.getFieldAccessModifier().toString().toLowerCase()+" ";
            if(field.getFieldAccessModifier().toString().toLowerCase().equals("default")) accessModifier="";
            var fieldName = field.getFieldName();
            var isFinal = field.isFinalField()?"final ":"";
            var isStatic = field.isStaticField()?"static ":"";
            var type = generateJasminType(field.getFieldType());
            var isInitialized = field.isInitialized()? " = " + field.getInitialValue():"";
            code.append(".field "+ accessModifier + isStatic +  isFinal+ fieldName + " " + type + isInitialized).append(NL);
        }
        // generate a single constructor method
        code.append(";default constructor").append(NL).append(".method public <init>()V").append(NL).append("   aload_0").append(NL).append("   invokespecial ").append(pkg).append(superclass).append("/").append("<init>()V").append(NL).append("   return\n"+".end method\n" );
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


    private void pushToStack() {
        stackSize++;
        if (stackSize > maxStack) maxStack = stackSize;
    }

    private void popFromStack() {
        stackSize--;
        if (stackSize < 0) throw new RuntimeException("Stack went negative");
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;
        stackSize = 0;
        maxStack = 0;
        extraRerence = 0;

        var code = new StringBuilder();
        var tempCode = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";
        var st = method.isStaticMethod()? "static " : "";
        var f = method.isFinalMethod()? "final ":"";
        var va = method.isVarargs()? "varargs ":"";

        var methodName = method.getMethodName();
        var methodParams = method.getParams();
        var returnElem = method.getReturnType();
        var returnType = generateJasminType(returnElem);


        // TODO: Hardcoded param types and return type, needs to be expanded
        //code.append("\n.method ").append(modifier).append(methodName).append("(I)I").append(NL);
        //not hardcoded anymore i think?
        code.append("\n.method ").append(modifier).append(st).append(f).append(va).append(methodName).append("(");
        for(var param:methodParams){
            code.append(generateJasminType(param.getType()));
        }
        code.append(")").append(returnType).append(NL);
        // Add limits
        //code.append(TAB).append(".limit stack 99").append(NL);
        //code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            for(String label : method.getLabels(inst)){
                tempCode.append(label+":").append(NL);
            }
          tempCode.append(instCode);
            if(inst instanceof CallInstruction && ((CallInstruction)inst).getReturnType().getTypeOfElement() != ElementType.VOID){
                tempCode.append(TAB).append("pop").append(NL);
                popFromStack();
            }
            else if (inst instanceof CallInstruction && this.extraRerence != 0 ){
                tempCode.append(TAB).append("pop").append(NL);
                popFromStack();
                this.extraRerence-=1;
            }
            if (stackSize != this.extraRerence) throw new RuntimeException("Stack was not 0 after end of statement");
        }
        tempCode.append(".end method\n");

        code.append(TAB).append(".limit stack ").append(maxStack).append(NL);
        code.append(TAB).append(".limit locals ").append(Collections.max(method.getVarTable().values().stream()
                .map(Descriptor::getVirtualReg).toList())+1).append(NL);
        code.append(tempCode);
        // unset method
        currentMethod = null;

        return code.toString();

    }
    private String generateCall(CallInstruction call){
        var code = new StringBuilder();
        boolean popCaller = false;
        //Does the register operations to get the callee reference
        if(!call.getInvocationType().equals(CallType.NEW)  && !call.getInvocationType().equals(CallType.invokestatic) && !call.getInvocationType().equals(CallType.ldc)){
            var get_caller_reference = generators.apply(call.getCaller());
            code.append(get_caller_reference);
            popCaller = true;
        }
        //Does the register operations to load the argument values to the stack
        for (var arg:call.getArguments()){
            code.append(generators.apply(arg));
        }
        //generates the type of invokation instruction
        var func="";
        var funcToCall="";
        var path="";
        var caller = ((Operand)call.getCaller());
        var mapEntry = currentMethod.getVarTable().get(caller.getName());

        if(!call.getInvocationType().equals(CallType.arraylength)) {
            //gets the package for the caller
            if (mapEntry != null) {

                path = getPackageFromImport(((ClassType) mapEntry.getVarType()).getName()) + ((ClassType) mapEntry.getVarType()).getName();
            } else {
                path = getPackageFromImport(caller.getName()) + caller.getName();
            }
        }

        if(call.getInvocationType().equals(CallType.NEW)) {
            var callRet = call.getReturnType();
            var objRef = generateJasminType(callRet);

            if (callRet instanceof ArrayType) {
                //Case call is for an array type
                var arrayType = (ArrayType) callRet;
                var elementsType = arrayType.getElementType();
                //Case the type of the array is a reference of object or another array object (multiarray)
                if (elementsType.equals(ElementType.OBJECTREF) || elementsType.equals(ElementType.ARRAYREF)) {
                    if (arrayType.getNumDimensions() > 1) {
                        funcToCall += "multianewarray " + generateJasminType(elementsType) + " " + arrayType.getNumDimensions();
                    } else {
                        funcToCall += "anewarray " + path;
                    }
                } else {
                    funcToCall += "newarray " + generateArrayElementType(arrayType.getElementType());
                }
            } else {
                code.append(call.getInvocationType().toString().toLowerCase());
                code.append(" ");
                funcToCall += objRef.substring(1, objRef.length() - 1);
            }
            funcToCall = funcToCall.replace("\"", "");
            code.append(funcToCall).append(NL);
            code.append("dup").append(NL);
            pushToStack();
            //has to be popped later since i will only use one of the duplicated references
            this.extraRerence += 1;
        }
        else if (call.getInvocationType().equals(CallType.arraylength)){
            code.append(call.getInvocationType().toString().toLowerCase()).append(NL);
        }
        else{
            code.append(call.getInvocationType().toString().toLowerCase());
            code.append(" ");
            //Appends the function spec / name and package
            func = ((LiteralElement) call.getMethodName()).getLiteral();
            func = func.replaceAll("\\\"", ""); // Assigning the result back to func
            func = func.equals("") ? "<init>" : func;
            funcToCall+= path + "/" + func;
            funcToCall = funcToCall.replace("\"", "");

            //Appends the list of args
            code.append(funcToCall).append("(");
            var arguments = "";
            for (var arg : call.getArguments()) {
                arguments += generateJasminType(arg.getType()).equals("V") ? "" : generateJasminType(arg.getType());
            }
            code.append(arguments).append(")");

            //Generates the return type for the calL
            code.append(generateJasminType(call.getReturnType()));
            //Generates the number of arguments at the end if its invokeinterface
            if(call.getInvocationType().equals(CallType.invokeinterface)){
                code.append(" ").append(call.getArguments().size());
            }
            code.append(NL);
        }

        for (int i = 0; i < call.getArguments().size(); i++) {
            popFromStack();
        }
        if (popCaller) popFromStack();
        if (!call.getReturnType().getTypeOfElement().equals(ElementType.VOID)) pushToStack();

        return code.toString();

    }
    private String generateFieldInst(FieldInstruction fieldInst){
        var code = new StringBuilder();
        var instType = fieldInst.getInstType();
        var getFieldVal = instType.equals(InstructionType.PUTFIELD) ? generators.apply(fieldInst.getOperands().get(2)) :  null;
        var getObjRef = generators.apply(fieldInst.getObject());

        code.append(getObjRef);
        if(getFieldVal!=null){
            code.append(getFieldVal);
            popFromStack();
            popFromStack();
        }
        var callObjName = ((ClassType)fieldInst.getObject().getType()).getName();
        var fieldName = fieldInst.getField().getName();
        var pkg = getPackageFromImport(callObjName);

        var callObjRef = pkg + callObjName+"/"+fieldName;
        var fieldType = generateJasminType(fieldInst.getField().getType());

        code.append(instType.toString().toLowerCase()).append(" "+ callObjRef).append(" "+fieldType).append(NL);
        return code.toString();
    }
    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();
        var value = new StringBuilder();
        var lhs = assign.getDest();
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }
        if(checkInc(lhs,assign.getRhs())){
            Element left = ((BinaryOpInstruction)assign.getRhs()).getLeftOperand();
            Element right = ((BinaryOpInstruction)assign.getRhs()).getRightOperand();
            int reg = 0;
            String incVal = "";
            if(left instanceof LiteralElement){
                reg = currentMethod.getVarTable().get(((Operand)right).getName()).getVirtualReg();
                incVal = ((LiteralElement) left).getLiteral();
            }
            else{
                reg = currentMethod.getVarTable().get(((Operand)left).getName()).getVirtualReg();
                incVal = ((LiteralElement) right).getLiteral();
            }
            var op = instWithOp(((BinaryOpInstruction)assign.getRhs()).getOperation(),true);
            code.append(op).append(" ").append(reg).append(" ").append(incVal).append(NL);
            popFromStack();
            popFromStack();
        }
        else{
            var rhs = generators.apply(assign.getRhs());
            // generate code for loading what's on the right
            value.append(rhs);
            var assignType = assign.getTypeOfAssign().getTypeOfElement();
            if(assignType.equals(ElementType.BOOLEAN)){
                if(assign.getRhs() instanceof BinaryOpInstruction) {
                    value.append(loadBooleanLiteral()).append(NL);
                    System.out.println(this.stackSize);
                }
                popFromStack();
            }
            // store value in the stack in destination
            var operand = (Operand) lhs;
            code.append(generateLhsOperand(operand,value));

        }


        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        pushToStack();
        var lit= literal.getLiteral().equals("-1")? "m1" : literal.getLiteral();
        return distinguishLiteral(literal.getLiteral()) + lit + NL;
    }

    private String generateOperand(Operand operand) {
        StringBuilder code  = new StringBuilder();

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        //changed the hardcoded version with integer
        var type = currentMethod.getVarTable().get(operand.getName()).getVarType();

        //index of the array access
        String index = "";
        String inst = "";
        if(operand instanceof ArrayOperand){
            inst = storeLoadInstWithType(type,false,true);
            if(reg > 3){
                code.append("aload " + reg).append(NL);
            }
            else {
                code.append("aload_" + reg).append(NL);
            }
            pushToStack(); //push the aload

            for(var op:((ArrayOperand) operand).getIndexOperands()){
                index += generators.apply(op);
            }
            code.append(index);
            code.append(inst).append(NL);
            pushToStack();//push the loaded value content

            popFromStack(); // pop the aload
            for(int i = 0;i<((ArrayOperand) operand).getIndexOperands().size();i++){
                popFromStack(); // pop the indexes
            }

        }
        else{
            inst = storeLoadInstWithType(type,false,false);
            pushToStack();
            if(reg>3){
                code.append(inst + " " + reg + NL);
            }
            else{
                code.append(inst + "_" + reg + NL);
            }
        }
       return code.toString();
    }
    private String generateLhsOperand(Operand lhs,StringBuilder value){
        StringBuilder code = new StringBuilder();
        // get register
        var reg = currentMethod.getVarTable().get(lhs.getName()).getVirtualReg();
        // get type of the content in the register
        var type = currentMethod.getVarTable().get(lhs.getName()).getVarType();

        //index of the array access
        String index = "";
        var inst = "";
        if(lhs instanceof ArrayOperand){
            inst = storeLoadInstWithType(type,true,true);


            if(reg>3){
                code.append("aload "+ reg).append(NL);

            }
            else{
                code.append("aload_" + reg).append(NL);
            }
            pushToStack(); //push the aload

            for(var op:((ArrayOperand) lhs).getIndexOperands()){
                index += generators.apply(op);
            }

            code.append(index);

            code.append(value.toString());
            code.append(inst).append(NL);

            popFromStack(); // pop the aload
            for(int i = 0;i<((ArrayOperand) lhs).getIndexOperands().size();i++){
                popFromStack(); // pop the indexes
            }
            popFromStack(); //pop the value

        }
        else{
            inst = storeLoadInstWithType(type,true,false);
            code.append(value.toString());
            popFromStack(); // pop the value
            if(reg<=3){
                code.append(inst+"_").append(reg).append(NL);
            }
            else{
                code.append(inst+" ").append(reg).append(NL);
            }
        }
        return code.toString();
    }
    private String generateUnaryOp(UnaryOpInstruction unaryOp){
        var code  = new StringBuilder();
        var op = unaryOp.getOperation();
        var inst = instWithOp(op,false);
        code.append(generators.apply(unaryOp.getOperand())); // appends the operand for the unary operation
        code.append(inst).append(NL); //appends the instruction generated according to operation
        popFromStack();
        return code.toString();

    }
    private Boolean checkInc(Element lhs,Instruction binaryOp){

        if(binaryOp instanceof  BinaryOpInstruction){
            Element left  = ((BinaryOpInstruction)binaryOp).getLeftOperand();
            Element right = ((BinaryOpInstruction)binaryOp).getRightOperand();
            if((left instanceof  LiteralElement && right instanceof Operand) || (right instanceof LiteralElement && left instanceof Operand)){
                if(left instanceof LiteralElement){
                    if(left.getType().getTypeOfElement().equals(ElementType.INT32) && ((Operand) right).getName().equals(((Operand)lhs).getName())){
                        return true;
                    }
                }
                else if (right instanceof LiteralElement){
                    if(right.getType().getTypeOfElement().equals(ElementType.INT32) && ((Operand) left).getName().equals(((Operand)lhs).getName())){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        // load values on the left and on the right
        var left = binaryOp.getLeftOperand();
        var right = binaryOp.getRightOperand();

        var type1 = binaryOp.getLeftOperand().getType().getTypeOfElement();
        var type2 = binaryOp.getRightOperand().getType().getTypeOfElement();
        if (!type1.equals(type2)){
            //Add error report here
        }
        // apply operation
        var op = instWithOp(binaryOp.getOperation(),false);
        code.append(generators.apply(left));
        code.append(generators.apply(right));
        if (op.equals("error")) throw new NotImplementedException(binaryOp.getOperation().getOpType());

        if(binaryOp.getOperation().getTypeInfo().getTypeOfElement().equals(ElementType.BOOLEAN)){
            code.append(op).append(" ");
        }
        else{
            code.append(op).append(NL);
        }
        popFromStack();

        return code.toString();
    }
    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        //not hardcoded anymore
        if(returnInst.getReturnType().getTypeOfElement().equals(ElementType.VOID)){
            code.append("return").append(NL);
            return code.toString();
        }

        code.append(generators.apply(returnInst.getOperand()));
        code.append(returnInstWithType(returnInst.getReturnType())).append(NL);
        popFromStack();

        return code.toString();
    }
    private String generateConditional(CondBranchInstruction branchInst){
        StringBuilder code = new StringBuilder();
        var condition = branchInst.getCondition();
        if (branchInst.getCondition() instanceof SingleOpInstruction){
            code.append(generators.apply(branchInst.getCondition())).append("ifne ").append(branchInst.getLabel()).append(NL);
        }
        else{
            code.append(generators.apply(branchInst.getCondition())).append(branchInst.getLabel()).append(NL);
        }
        //
        popFromStack();
        return code.toString();
    }
    private String generateGoTo(GotoInstruction goToInst){
        StringBuilder code = new StringBuilder();
        code.append("goto").append(" ").append(goToInst.getLabel()).append(NL);
        return code.toString();
    }
}
