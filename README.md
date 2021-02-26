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
    <version>1.3</version>
</dependency>
```
- Define your model entities
- Define ID resolvers extending: GenericIdResolver<YOUR_ENTITY, YOUR_ENTITYS_ID> (no extra logic required)
- Indicate ID resolver for each entity with:
```
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class
        , property = "id"
        , scope = YOUR_ENTITY.class
        , resolver = YOUR_ENTITY_ID_RESOLVER.class
)
```
- Define repository interfaces extending: JpaRepository<YOUR_ENTITY, YOUR_ENTITYS_ID>
- Define service interfaces extending: ICrudService<YOUR_ENTITY, YOUR_ENTITYS_ID>
- Define service implementations implementing them, and extending: CrudService<YOUR_ENTITY, YOUR_ENTITYS_ID>
- Declare public constructor for those services:
```
@Autowired
public YOUR_ENTITY_SERVICE(final YOUR_ENTITY_REPOSITORY repository) {
	super(repository);
}
```
- Define controllers with a particular mapping, extending: CrudController<YOUR_ENTITY, YOUR_ENTITYS_ID> (no extra logic required)

- Add the following bean in any configuration class (to avoid Jackson bug describen below):
```
@Bean
public Jackson2ObjectMapperBuilderCustomizer addCustomSerializationFeatures() {
	return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.featuresToEnable(
		SerializationFeature.HANDLE_CIRCULAR_REFERENCE_INDIVIDUALLY_FOR_COLLECTIONS
	);
}
```
**Note:** If the previous feature does not compile, it means that some other dependency
(probably a Spring one, since it comes by default with it) has injected 'jackson-databind'
as a transitive dependency (and it does not have the fix for the bug). This can be solved
by adding the following dependency before any other that might add the one without the fix:
```
<dependency>
    <groupId>com.github.jobosk</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.11-fix_2791</version>
</dependency>
```

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

- Due to the way Jackson handles circular dependencies, serialization
of two or more different entities poiting to the same sub-entity will
result in the serialization of the sub-entity as an object in the first
occurrence and as an ID the following ones.

### Known *fixed* bugs

- There is a bug in the Jackson serializer mentioned in the following issue:

https://github.com/FasterXML/jackson-databind/issues/2791

The current fix for this bug relies in the presence of the following
dependency (within the 'crudifier' library itself, **no need to add it
again, unless some other dependency includes the one without the fix
as a transitive dependency**):
```
<dependency>
    <groupId>com.github.jobosk</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.11-fix_2791</version>
</dependency>
```

To make use of the bug-fixing "functionality", a Jackson configuration
bean (indicated in the 'How to' section) is required.
