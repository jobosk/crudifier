# Quick setup using generic CRUDs

This library will greatly simplify the process of creating
a CRUD interface to the entities of model, allowing to focus
in the design itself and removing the need of a graphical
user interface or making queries the database to manipulate
data, in early development stages.

## Requirements

This library is intended to be used using Spring Boot 2.3
and JPA repositories.

## How to use

- Add to POM:
```
<dependency>
    <groupId>com.github.jobosk</groupId>
    <artifactId>crudifier</artifactId>
    <version>1.2</version>
</dependency>
```
- Define your model entities
- Define ID resolvers extending: GenericIdResolver<YOUR_ENTITY> (no extra logic required)
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
- Declare public constructor for those services:
```
@Autowired
public YOUR_ENTITY_SERVICE(final YOUR_ENTITY_REPOSITORY repository) {
	super(repository);
}
```
- Define controllers with a particular mapping, extending: CrudController<YOUR_ENTITY> (no extra logic required)

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

## Known bugs

- Filtering by ID in the find all could result in a parsing exception.

- Due to the way Jackson handles circular dependencies, serialization
of two or more different entities poiting to the same sub-entity will
result in the serialization of the sub-entity as an object in the first
occurrence and as an ID the following ones.
