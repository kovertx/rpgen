grammar RpGen;

blocks: header_block=any_content (config_block | model_block | rpc_block | http_block)*;

config_block: BLOCK_HEADER WS CONFIG
  WS LANG EQ lang=identifier
  (WS GEN EQ gen=identifier)?
  (WS SCOPE EQ scope=identifier)? WS? ENDL any_content;

model_block: BLOCK_HEADER WS MODEL WS NAME EQ name=identifier WS? ENDL any_content;
rpc_block: BLOCK_HEADER WS RPC WS NAME EQ name=identifier WS? ENDL any_content;
http_block: BLOCK_HEADER WS HTTP WS NAME EQ name=identifier WS? ENDL any_content;

any_content: (ANY | identifier | EQ | ANY | ENDL | WS)*;

identifier: MODEL | RPC | HTTP | CONFIG | LANG | GEN | SCOPE | NAME | IDENT;

MODEL: 'model';
HTTP: 'http';
RPC: 'rpc';

CONFIG: 'config';
LANG: 'lang' 'uage'?;
GEN: 'gen' 'erator'?;
SCOPE: 'scope';
NAME: 'name';
EQ: '=';
IDENT: [a-zA-Z][a-zA-Z0-9\-]*;
BLOCK_HEADER: {this.getCharPositionInLine() == 0}? '---';

ENDL: [\r\n]+;
WS: [ \t\u000C]+;
ANY: (.)+?;
