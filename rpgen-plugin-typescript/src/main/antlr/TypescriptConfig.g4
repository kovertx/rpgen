grammar TypescriptConfig;

typescript_config:
  typescript_import*
  typescript_alias*
  ;
typescript_import: IMPORT LC typescript_imported_type (COMMA typescript_imported_type)* COMMA? RC FROM source=STR_LITERAL;
typescript_alias: TYPE identifier EQ type_ref;

typescript_imported_type: type_name=identifier (AS alias=identifier)?;

type_ref: identifier (LT type_ref (COMMA type_ref)* GT)? QMARK?;

///
/// Lexer and some basic primitives
///

identifier: IDENTIFIER | any_keyword;
any_keyword: AS | FROM | IMPORT | TYPE;

AS: 'as';
FROM: 'from';
IMPORT: 'import';
TYPE: 'type';

STR_LITERAL: '"' STR_CHARS* '"';
fragment STR_CHARS
  : ~["\\\r\n]
  | '\\' ESCAPE_CHAR
  ;
fragment ESCAPE_CHAR: ['"\\bfnrtv];

IDENTIFIER: LETTER ( LETTER | DIGIT )*;

fragment LETTER: [A-Za-z];
fragment DIGIT: [0-9];

COMMA: ',';
DOT: '.';
LC: '{';
RC: '}';
LT: '<';
GT: '>';
EQ: '=';
QMARK: '?';

WS: [ \t\r\n\u000C]+ -> channel(HIDDEN);
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
COMMENT: '/*' .*? '*/' -> channel(HIDDEN);