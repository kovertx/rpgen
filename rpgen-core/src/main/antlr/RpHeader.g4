grammar RpHeader;

header: include_stmt*;

include_stmt: INCLUDE path=STR_LITERAL;

INCLUDE: 'include';
STR_LITERAL: '"' STR_CHARS* '"';
fragment STR_CHARS
  : ~["\\\r\n]
  | '\\' ESCAPE_CHAR
  ;
fragment ESCAPE_CHAR: ['"\\bfnrtv];

WS: [ \t\r\n\u000C]+ -> channel(HIDDEN);
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
COMMENT: '/*' .*? '*/' -> channel(HIDDEN);