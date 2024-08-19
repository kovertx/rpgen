grammar KotlinConfig;

kotlin_config:
  kotlin_package?
  kotlin_import*
  kotlin_typealias*
  ;

kotlin_package: PACKAGE scoped_identifier;
kotlin_import: IMPORT scoped_identifier (AS identifier)?;
kotlin_typealias: TYPEALIAS identifier EQ scoped_identifier;
scoped_identifier: identifier (DOT identifier)*;

///
/// Lexer and some basic primitives
///

identifier: IDENTIFIER | PACKAGE | IMPORT | TYPEALIAS | AS;

AS: 'as';
IMPORT: 'import';
TYPEALIAS: 'typealias';
PACKAGE: 'package';
IDENTIFIER: LETTER ( LETTER | DIGIT )*;

fragment LETTER: [A-Za-z];
fragment DIGIT: [0-9];

EQ: '=';
DOT: '.';

WS: [ \t\r\n\u000C]+ -> channel(HIDDEN);
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
COMMENT: '/*' .*? '*/' -> channel(HIDDEN);
