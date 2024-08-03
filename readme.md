# RpGen

RpGen is a schema language and code generation tool for defining models and APIs.

RpGen schemas are broken into:

- `model` sections define data types that need to be worked with (structs, enums, etc.)
- `http` sections define HTTP APIs
- `rpc` sections define RPC APIs
- `config` sections provide language or generator-specific configuration

## Features

- Schema First: Write your models and APIs, then generate implementation code from them.
- Escape Hatches: RpGen tries to be flexible enough to handle common cases, but also tries to make
  it less painful if you need to write something custom.
- Pluggable: RpGen can be extended to target new languages, libraries, and frameworks.

## What it isn't

RpGen isn't meant as a tool to document existing models or APIs. It might be possible, but you may
find yourself working against the tool.

## Builtin Languages and Generators:

Out of the box, RpGen just comes with the generators I find useful, but it's open to contributions.

- kotlin@
  - model: data classes, enum classes, etc. Defaults to using kotlinx.serialization.
  - kovertx-*: type-safe routing for endpoints defined in http schemas using [Kovertx](#TODO)
  - rpc-model: data classes and sealed interfaces for RPC APIs
- typescript@
  - model: interfaces, enums, branded types, etc.
  - json-codec: defines [json-codec](#TODO) codecs for models
  - fetch-json-codec: defines a client using the Fetch API and generated `json-codec` bindings
- openapi@
  - specification: generate an OpenAPI specification from model and http schemas?
  - generator (TODO): run [OpenAPI Generator](#TODO) using the implicitly defined spec, get all their
    generators for free.

RpGen is designed to be extensible, it provides a [plugin interface](docs/plugin-interface.md) for
defining your own languages and/or generators.

## A Sample

We define a schema. In this case, 

````
--- model name=SampleModel

// id types are named UUIDs. Where possible, code generators can create
// opaque type wrappers for extra safety
id UserId

// structs are your basic composite types. They might become typescript
// interfaces, or kotlin data classes
struct Profile {
    id: UserId,
    name: str,
    age: i32
}

// opaque types can be used to create names for other types. Where possible,
// code generators can create type wrappers for extra safety
opaque UserToken = str

struct UserTokenPayload {
  sub: UserId
}

struct LoginInfo {
    username: str,
    password: str
}

struct SignupForm {
    username: str,
    password: str,
    email: str
}

--- http name=SampleApi

// We define an auth scheme that can be used in our API. Here, we're saying the "User" auth scheme
// consists of a JWT (sent via bearer auth), and that our earlier "UserToken" type is meant to be
// used as the token representation (rather than passing around plain strings), and that the token
// payload should contain fields specified in "UserTokenPayload".
bearer auth User = jwt UserToken -> UserTokenPayload

// routes can be nested, everything inside this block will have /api as a prefix of the full path
/api {
    // endpoints are defined by a method name (what to call the corresponding function on generated
    // clients or server stubs), an HTTP verb, and a path
    createUser = POST /signup {
        // we specify the type of the request and response body (which will default to assuming JSON
        // for most generators)
        request body type = SignupInfo
        response body type = UserToken
    }

    authenticate = POST /login {
        request body type = LoginInfo
        response body type = UserToken
    }
    
    /users {
        // here we're requiring that all endpoints under /api/users will require the "User"
        // authentication as defined earlier
        require auth User
        
        findUsers = GET / {
            // URL query parameters can be optional or required
            query params {
                name?: str
                minAge?: i32
                maxAge?: i32
            }
            response body type = List<Profile>
        }
        
        // path parameters can be typed for your convenience
        /(userId: UserId) {
            getUserProfile = GET /profile {
                response body type = Profile
            }
        }
    }
}
````

With a schema in hand, we need to generate code. The RpGen compiler uses a JSON config file to tell
it what schemas to parse, and what code generator pipelines to run.

````json
{
  "schemas": ["sample.rpg"],
  "pipelines": [
    {
      "outDir": "frontend/src/generated",
      "generators": [
        "typescript@model",
        "typescript@json-codec",
        "typescript@fetch-json-codec"
      ]
    },
    {
      "outDir": "backend/src/main/kotlin",
      "generators": [
        "kotlin@model",
        "kotlin@vertx-web"
      ]
    }
  ]
}
````

...a pipeline minimally consists of an output directory and a list of generators.

Paths referenced in the config file (e.g. schemas and output directories) are relative to the
config file by default. So our pre-codegen project might look roughly like:

- sample-project/
  - sample.rpg
  - rpgen.config.json
  - frontend/
    - src/
      - ...implementation code
  - backend/
    - src/main/kotlin/
      - ...implementation code