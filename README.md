# Quick setup using generic CRUDs

This library will greatly simplify the process of creating
a CRUD interface to the entities of model, allowing to focus
in the design itself and removing the need of a graphical
user interface or making queries the database to manipulate
data, in early development stages.

## How to use

- Define your model entities
- Define ID resolvers extending: GenericIdResolver<YOUR_ENTITY>
- Indicate ID resolver for each entity with:
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class
        , property = "id"
        , scope = YOUR_ENTITY.class
        , resolver = YOUR_ENTITY_ID_RESOLVER.class
)
- Define repository interfaces extending: JpaRepository<YOUR_ENTITY, UUID>
- Define service interfaces extending: ICrudService<YOUR_ENTITY>
- Define service implementations implementing them, and extending: CrudService<YOUR_ENTITY>
- Define controllers extending: CrudController<TestEntity>

## API

### Create test entity

Request:

POST /testentity
```
{
	"name": "new test entity"
}
```

Response:

200:
```
{
    "id": <UUID>
    , "name": "new test entity"
}
```

### Update test entity

Request:

PUT /testentity/<UUID>
```
{
    "name": "edited name of present test entity"
}
```

Response:

200:
```
{
    "id": <UUID>
    , "name": "edited name of present test entity"
}
```

### Delete test entity

Request:

DELETE /testentity/<UUID>

Response:

200

### Find all test entity

Request:

GET /testentity[?\<filter>][&\<filter>...]

filter=
- \<entity attribute>=\<value>
- page=<0..N>
- size=<1..N>
- order=[-]\<entity attribute>

**Note**
Filtering entity attributes by String values aplies a 'contains'
match, whilst numeric values apply an exact match. Also, filters
'page' and 'size' must be both present if one is.

Response:

200:
```
[
    {
        "id": <UUID>
        , "name": "first present test entity"
    }
    , {
        "id": <UUID>
        , "name": "second present test entity"
    }
]
```

### Find one test entity

Request:

GET /testentity/<UUID>

Response:

200:
```
{
    "id": <UUID>
    , "name": "present test entity"
}
```
