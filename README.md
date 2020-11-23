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
```
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class
        , property = "id"
        , scope = YOUR_ENTITY.class
        , resolver = YOUR_ENTITY_ID_RESOLVER.class
)
```
- Define repository interfaces extending: JpaRepository<YOUR_ENTITY, UUID>
- Define service interfaces extending: ICrudService<YOUR_ENTITY>
- Define service implementations implementing them, and extending: CrudService<YOUR_ENTITY>
- Define controllers with a particular mapping, extending: CrudController<YOUR_ENTITY>

## API

This will enable the following API to interact with the model:

### Create entity

Request:

POST /<entity_mapping>
```
{
	"name": "new entity"
}
```

Response:

200:
```
{
    "id": <UUID>
    , "name": "new entity"
}
```

### Update entity

Request:

PUT /<entity_mapping>/<UUID>
```
{
    "name": "edited name of present entity"
}
```

Response:

200:
```
{
    "id": <UUID>
    , "name": "edited name of present entity"
}
```

### Delete entity

Request:

DELETE /<entity_mapping>/<UUID>

Response:

200

### Find all entity

Request:

GET /<entity_mapping>[?\<filter>][&\<filter>...]

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
        , "name": "first present entity"
    }
    , {
        "id": <UUID>
        , "name": "second present entity"
    }
]
```

### Find one entity

Request:

GET /<entity_mapping>/<UUID>

Response:

200:
```
{
    "id": <UUID>
    , "name": "present entity"
}
```
