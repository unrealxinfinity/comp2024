package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::visitVarDecl);
        setDefaultVisit(this::defaultVisit);
    }
    private String visitVarDecl(JmmNode node, Void unused) {
        //System.out.println("Entered Visit VarDecl");
        StringBuilder codeBuilder = new StringBuilder();
        //if (METHOD_DECL.check(node.getParent()) ){return "";}
        if (CLASS_DECL.check(node.getParent())) {
            codeBuilder.append(" public ");
        }
        String varName = node.get("name");
        String ollirType = OptUtils.toOllirType(node.getJmmChild(0));
        codeBuilder.append(varName).append(ollirType).append(END_STMT);

        return codeBuilder.toString();
    }
    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        System.out.println("Entered Visit Return");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }
        if (retType.getName()=="void"){
            code.append("ret.V;");
        }
        else {
            code.append(expr.getComputation());
            code.append("ret");
            code.append(OptUtils.toOllirType(retType));
            code.append(SPACE);

            code.append(expr.getCode());

            code.append(END_STMT);
        }
        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");
        String code="";
        if (node.getJmmChild(0).getObject("isArray", Boolean.class)) {
             code = id + ".array"+ typeCode;
        }
        else{
            code = id + typeCode;
        }
        return code;
    }



    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        boolean hasreturn = false;
        if (isPublic) {
            code.append("public ");
        }
        var childCode="";
        // Method name
        var name = node.get("name");
        code.append(name);

        // Parameters
        StringBuilder paramCode = new StringBuilder();
        var paramNodes = node.getChildren("Param");
        for (int i = 0; i < paramNodes.size(); i++) {
            var paramNode = paramNodes.get(i);
            var paramCodeStr = visit(paramNode);
            paramCode.append(paramCodeStr);
            if (i < paramNodes.size() - 1) {
                paramCode.append(",");
            }
        }

        // Return type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append("(").append(paramCode).append(")").append(retType).append(L_BRACKET);
        int i=0;
        // Method body
        for (var child: node.getChildren()) {
            if (PARAM.check(child)){continue;}
            //if (VAR_DECL.check(child)){continue;}
            System.out.println(child.getKind());
            if( i== node.getNumChildren()-1){
                JmmNode childofchild= child.getJmmChild(0);
                childCode= visitReturn(childofchild,unused);
            }
            else {
                childCode = visit(child);
            }
            code.append(childCode);
            i++;
        }
        if(!hasreturn){code.append("ret.V;");}
        code.append(R_BRACKET).append(NL);

        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        code.append(L_BRACKET);

        code.append(NL);


        for (var child : node.getChildren()) {
            var result = visit(child);
            var needNl = true;
            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }
            if (VAR_DECL.check(child) && needNl){
                code.append(".field ");


            }
            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
