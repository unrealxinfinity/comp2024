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
MAIN : 'main';
RETURN : 'return' ;
TRUE : 'true';
FALSE: 'false';
THIS : 'this';
IF: 'if';
ELSE:'else';
WHILE: 'while';
STR: 'String';
INT: 'int';
BOOLEAN:'bool';
NEW: 'new';

INTEGER : [0-9]+ ;
ID : [a-zA-Z]+INTEGER?[a-zA-Z]*INTEGER? ;
STRING : [a-zA-Z]+;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDecl EOF
    ;

importDeclaration : 'import' ID ( '.' ID )* ';' ;

classDecl
    : 'class' ID ('extends' ID)? '{'
            (varDecl)* (methodDecl)*
        '}'
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name=INT '['']'
    | name=INT '...'
    | name= INT
    | name = BOOLEAN
    | name= STR LRECT RRECT
    | name= ID
    | name= STR
    | name= VOID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN param RPAREN
        LCURLY
            varDecl* stmt*
        RCURLY
    | (PUBLIC)? STATIC VOID MAIN LPAREN param RPAREN
        LCURLY
            ( varDecl)* ( stmt )*
        RCURLY
    ;

param
    : (type name=ID(',')?)*
    ;

stmt
    : LCURLY (stmt)* RCURLY
    | IF LPAREN expr RPAREN
        (stmt)*
      ELSE
        (stmt)*
    | WHILE LPAREN expr RPAREN (stmt)*
    | expr SEMI
    | ID '=' expr SEMI
    | ID LRECT expr RRECT '=' expr SEMI
    | RETURN expr SEMI
    ;
expr
    : '(' expr ')' #ParensExpr
    | expr '[' expr ']' #IndexedExpr
    | expr '.' LENGTH #LengthExpr
    | expr '.' ID LPAREN (expr ( ',' expr )*)? RPAREN #Custom3Expr
    | expr (op= MUL | op=DIV)  expr #BinaryExpr //
    | expr (op= ADD | op=SUB) expr #BinaryExpr //
    | NOT expr #LogicalExpr
    | expr (op=LT) expr #LogicalExpr
    | expr (op=AND) expr #LogicalExpr
    //| expr OR expr #LogicalExpr
    | NEW INT LRECT expr RRECT #CustomExpr
    | NEW ID LPAREN RPAREN #Custom2Expr
    | LRECT (expr ( ',' expr)* )? RRECT #Custom3Expr
    | value=INTEGER #IntegerLiteral //
    | value=TRUE #BooleanLiteral
    | value=FALSE #BooleanLiteral
    | name=ID #VarRefExpr //
    | THIS #This
    ;



