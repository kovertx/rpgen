grammar KotlinConfig;

kotlin_config:
  kotlin_package?
  kotlin_import*
  ;

kotlin_package: PACKAGE scoped_identifier;
kotlin_import: IMPORT scoped_identifier (AS identifier)?;
scoped_identifier: identifier (DOT identifier)*;

///
/// Lexer and some basic primitives
///

identifier: IDENTIFIER | PACKAGE | IMPORT | AS;

AS: 'as';
IMPORT: 'import';
PACKAGE: 'package';
IDENTIFIER: LETTER ( LETTER | DIGIT )*;

fragment LETTER: [A-Za-z];
fragment DIGIT: [0-9];

DOT: '.';

WS: [ \t\r\n\u000C]+ -> channel(HIDDEN);
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
COMMENT: '/*' .*? '*/' -> channel(HIDDEN);
