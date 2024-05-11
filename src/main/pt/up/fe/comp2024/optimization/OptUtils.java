package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    private static int ifStmtNumber = -1;

    private static int elseStmtNumber =-1;

    private static int whileCondNumber =-1;

    private static int whileLoopNumber =-1;

    private static int whileEndNumber=-1;
    public static String getWhileCond(){
        whileCondNumber++;
        return "whileCond"+whileCondNumber;
    }
    public static String getWhileLoop(){
        whileLoopNumber++;
        return "whileLoop"+whileLoopNumber;
    }

    public static String getWhileEnd(){
        whileEndNumber++;
        return "whileEnd"+whileEndNumber;
    }
    public static String getif(){
        ifStmtNumber++;
        return "if"+ifStmtNumber;
    }
    public static String getendif(){
        elseStmtNumber++;
        return "endif"+elseStmtNumber;
    }
    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");
        boolean isArray= typeNode.getJmmChild(0).getObject("isArray", Boolean.class);
        return toOllirType(typeName, isArray);
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    private static String toOllirType(String typeName, boolean isArray) {

        String type = (isArray? ".array." : ".") + switch (typeName) {
            case "int" -> "i32";
            case "boolean"-> "bool";
            case "String" -> "String";
            case "void" -> "V";
            default -> typeName;
        };

        return type;
    }


}
