grammar RpRpc;
import RpBase;

rpc_api: ( rpc_method )*;

rpc_method:
  ( rpc_command_decl
  | rpc_query_decl
  | rpc_notice_decl
  | rpc_group
  );

rpc_group: COMMENT? GROUP name=identifier method_args? LC ( rpc_method )* RC;

rpc_command_decl: COMMENT? COMMAND name=identifier method_args;
rpc_query_decl: COMMENT? QUERY name=identifier method_args COLON type_ref;
rpc_notice_decl: COMMENT? NOTICE name=identifier method_args;

// rpc lexing

identifier: IDENTIFIER | keywords;
keywords: GROUP | COMMAND | QUERY | NOTICE;

GROUP: 'group';
COMMAND: 'command';
QUERY: 'query';
NOTICE: 'notice';