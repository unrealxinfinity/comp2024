grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

LENGTH: 'length';
EQUALS: '=';
SEMI : ';' ;
LRECT: '[';
RRECT: ']';
LCURLY: '{';
RCURLY: '}';
LPAREN: '(';
RPAREN: ')';
MUL: '*' ;
ADD: '+' ;
DIV: '/' ;
SUB: '-' ;
NOT: '!';
AND: '&&';
LT: '<';

PUBLIC : 'public';
STATIC: 'static';
CLASS: 'class';
VOID: 'void';
MAIN: 'main';
RETURN: 'return';
TRUE: 'true';
FALSE: 'false';
THIS : 'this';
IF: 'if';
ELSE:'else';
WHILE: 'while';
STR: 'String';
INT: 'int';
INTARRAY: 'int[]';
STRINGARRAY:'String[]';
INTVARARG:'int...';
BOOLEAN:'boolean';
NEW: 'new';

INTEGER : [0-9]+ ;
ID : [a-zA-Z]+INTEGER?[a-zA-Z]*INTEGER? ;
STRING : [a-zA-Z]+;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl : 'import' ( ID '.')* name=ID ';' ;

classDecl
    : 'class' name=ID ('extends' superclass=ID)? '{'
            (varDecl)* (methodDecl)*
        '}'
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name=INTARRAY
    | name=INTVARARG
    | name= INT
    | name = BOOLEAN
    | name= STRINGARRAY
    | name= ID
    | name= STR
    | name= VOID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN (param)? (',' param)* RPAREN
        LCURLY
            varDecl* stmt*
        RCURLY
    | (PUBLIC)? STATIC type name=MAIN LPAREN (param)? (',' param)* RPAREN
        LCURLY
            ( varDecl)* ( stmt )*
        RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY (stmt)* RCURLY #EncloseStatement
    | IF LPAREN expr RPAREN
        (stmt)*
      ELSE
        (stmt)* #IfStatement
    | WHILE LPAREN expr RPAREN (stmt)* #WhileStatement
    | expr SEMI #SimpleStatement
    | ID '=' expr SEMI #AssignmentStatement
    | ID LRECT expr RRECT '=' expr SEMI #ArrayAlterIndexStatement
    | RETURN expr SEMI #ReturnStatemnt
    ;
expr
    : '(' expr ')' #ParensExpr
    | expr '[' expr ']' #IndexedExpr
    | expr '.' LENGTH #LengthFunctionExpr
    | expr '.' ID LPAREN (expr ( ',' expr )*)? RPAREN #ClassFunctionCallExpr
    | expr (op= MUL | op=DIV)  expr #BinaryExpr //
    | expr (op= ADD | op=SUB) expr #BinaryExpr //
    | NOT expr #LogicalExpr
    | expr (op=LT) expr #BinaryExpr
    | expr (op=AND) expr #BinaryExpr
    //| expr OR expr #LogicalExpr
    | NEW INT LRECT expr RRECT #NewArrayExpr
    | NEW ID LPAREN RPAREN #NewClassExpr
    | LRECT (expr ( ',' expr)* )? RRECT #ArrayExpr
    | value=INTEGER #IntegerLiteral
    | value=TRUE #BooleanLiteral
    | value=FALSE #BooleanLiteral
    | name=ID #VarRefLiteral //
    | THIS #This
    ;



