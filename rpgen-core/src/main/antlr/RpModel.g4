grammar RpModel;
import RpBase;

model_root:
  ( extern_decl
  | id_decl
  | enum_decl
  | struct_decl
  | opaque_decl
  )*;

extern_decl: EXTERN name=identifier;

id_decl: ID name=identifier;

enum_decl: ENUM name=identifier LC
  enum_value
  (COMMA enum_value)*
  COMMA?
  RC;

struct_decl: mutation* STRUCT name=identifier (base=type_ref)? struct_body?;

mutation: AT id=identifier LP (expr (COMMA expr)*)? RP;

enum_value: name=identifier EQ value=INT_LITERAL;

opaque_decl: OPAQUE name=identifier EQ primitive_type;

identifier: base_identifier | keyword | primitive_type;
keyword: ID | ENUM | OPAQUE | STRUCT | EXTERN;
primitive_type: BOOL | STR | I8 | I16 | I32 | I64 | U8 | U16 | U32 | U64 | F32 | F64;

EXTERN: 'extern';
ID: 'id';
ENUM: 'enum';
OPAQUE: 'opaque';
STRUCT: 'struct';

BOOL: 'bool';
STR: 'str';
I8: 'i8';
I16: 'i16';
I32: 'i32';
I64: 'i64';
U8: 'u8';
U16: 'u16';
U32: 'u32';
U64: 'u64';
F32: 'f32';
F64: 'f64';
