grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
PERIOD: '.';
COMMA: ',';
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LRECT = '[';
RRECT = ']';
MUL : '*' ;
ADD : '+' ;
DIV : '/' ;
MINUS: '-';
IF : 'if' ;
ELSE: 'else';
WHILE: 'while';
AND: '&&'
OR: '||'
BoolEQ: '=='
LT: '<'
LE: '<='
GT: '>'
GE:'>='
NOT:'!'


CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
NEW: 'new'
THIS: 'this';
RETURN : 'return' ;

INTEGER : [0-9] ;
BOOLEAN : 'true'|'false' ;
ID : [a-zA-Z]+INTEGER*[a-zA-Z]*INTEGER* ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;
importDecl 
    : ‘import’ ID ( ‘.’ ID )* SEMI

classDecl
    : 'class' name=ID ('extends' ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY 
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= 'int' 
    | name= 'int...'
    | name= 'boolean'
    | name= 'int' LRECT RRECT
    | name= ID
    ;

methodDecl locals[boolean isPublic=false]
    : ('public' {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : (type name=ID(',')?)*
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt
    | IF LPAREN expr RPAREN 
        LCURLY 
            stmt* 
        RCURLY 
      (
      ELSE
        LCURLY
            stmt*
        RCURLY
      )? #IfStmt  
    | WHILE LPAREN expr RPAREN
        LCURLY
            stmt*
        RCURLY #WhileStmt
    | expr SEMI #Just
    | ID LRECT expr RRECT EQUALS expr SEMI 
    | RETURN expr SEMI #ReturnStmt 
    ;
expr
    : op=(NOT) expr #UnaryBoolExpr
    | expr op=(MUL | DIV) expr #BinaryExpr 
    | expr op=(ADD | MINUS) expr #BinaryExpr
    | expr op=(BoolEQ | LE | LT | GE | GT ) expr #BinaryBoolExpr
    | expr op=(AND|OR) expr #BinaryBoolExpr
    | expr LRECT expr RRECT
    | expr PERIOD 'length'
    | expr PERIOD name=ID LPAREN (expr (COMMA expr)*)? RPAREN
    | NEW 'int' LRECT expr RRECT
    | NEW name=ID LPAREN RPAREN
    | LPAREN expr RPAREN
    | LRECT (expr (COMMA ex)*)? RRECT
    | expr PERIOD name=ID LPAREN 
    | value=INTEGER #IntegerLiteral 
    | value=BOOLEAN #BooleanLiteral
    | name=ID #VarRefExpr 
    | THIS
    ;



