grammar RpHttp;
import RpBase;

http_api:
  http_auth_scheme*
  http_route*
  ;

http_auth_scheme
  : BASIC  AUTH name=identifier                      #http_auth_scheme_basic
  | BEARER AUTH name=identifier EQ http_bearer_token #http_auth_scheme_bearer
  ;

http_bearer_token
  : JWT (token_type=type_ref)? (EQ payload_type=type_ref)? #http_bearer_token_jwt
  ;

http_route
  : detail=http_route_group_detail # http_route_group
  | detail=http_endpoint_detail    # http_endpoint
  ;

http_route_group_detail: COMMENT? http_path
  LC
  http_auth_requirement?
  http_route*
  RC;

http_endpoint_detail: COMMENT? name=identifier EQ http_verb http_path? LC
  http_auth_requirement?
  http_query_params?
  ( http_response_def
  | http_request_type
  )*
  RC;

http_auth_requirement: (REQUIRE | ACCEPT) AUTH auth_name=identifier;

http_request_type: REQUEST BODY TYPE EQ type_ref;

http_response_def: RESPONSE (http_response_status (COMMA http_response_status)*)? (EQ body_type=type_ref)?;
http_response_status: INT_LITERAL | identifier LP INT_LITERAL RP;

http_query_params: QUERY PARAMS struct_body;

http_path: FSLASH | ((FSLASH http_path_part)+ FSLASH?);
http_path_part
  : identifier                                   #path_literal_simple
  | STR_LITERAL                                  #path_literal_quoted
  | (LP param=identifier COLON type=type_ref RP) #path_param
  ;

// http lexical rules

identifier: IDENTIFIER | keyword;

keyword: http_verb | ACCEPT | AUTH | BASIC | BEARER | BODY | JWT | RESPONSE | REQUEST | REQUIRE | TYPE | QUERY | PARAMS;

ACCEPT: 'accept';
AUTH: 'auth';
BASIC: 'basic';
BEARER: 'bearer';
BODY: 'body';
JWT: 'jwt';
PARAMS: 'params';
QUERY: 'query';
RESPONSE: 'response';
REQUEST: 'request';
REQUIRE: 'require';
TYPE: 'type';

http_verb: DELETE | GET | PATCH | POST | PUT;
DELETE: [dD] [eE] [lL] ([eE] [tT] [eE])?;
GET: [gG] [eE] [tT];
PATCH: [pP] [aA] [tT] [cC] [hH];
POST: [pP] [oO] [sS] [tT];
PUT: [pP] [uU] [tT];