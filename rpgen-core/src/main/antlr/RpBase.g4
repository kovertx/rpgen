grammar RpBase;

expr
  : func=identifier LP (expr (COMMA expr)*)? RP #expr_call
  | identifier                                  #expr_id
  | type_ref                                    #expr_ref
  | value=INT_LITERAL                           #expr_int
  | value=STR_LITERAL                           #expr_str
  ;

method_args: LP field_list RP;
struct_body: LC field_list RC;

field_list: (field (COMMA field)* COMMA?)?;

field: name=identifier (isOptional=QMARK)? COLON type=type_ref;

type_ref: type=identifier (LT
  type_ref?
  (COMMA type_ref)*
  COMMA?
  GT)?
  (isNullable=QMARK)?;

identifier: base_identifier;

// Common lexical elements

INT_LITERAL: DIGIT+;
STR_LITERAL: '"' STR_CHARS* '"';
fragment STR_CHARS
  : ~["\\\r\n]
  | '\\' ESCAPE_CHAR
  ;
fragment ESCAPE_CHAR: ['"\\bfnrtv];

base_identifier: IDENTIFIER | boolean_literal;
IDENTIFIER: LETTER ( LETTER | DIGIT )*;

fragment LETTER: [A-Za-z];
fragment DIGIT: [0-9];

boolean_literal: TRUE | FALSE;

COLON: ':';
COMMA: ',';
SEMICOLON: ';';
DOT: '.';
LP: '(';
RP: ')';
LC: '{';
RC: '}';
LT: '<';
GT: '>';
EQ: '=';
QMARK: '?';
FSLASH: '/';
AT: '@';

TRUE: 'true';
FALSE: 'false';

WS: [ \t\r\n\u000C]+ -> channel(HIDDEN);
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
COMMENT: '/*' .*? '*/' -> channel(HIDDEN);