# Quick setup using generic CRUDs

This library will greatly simplify the process of creating
a CRUD interface to the entities of model, allowing to focus
in the design itself and removing the need of a graphical
user interface or making queries the database to manipulate
data, in early development stages.

## API

### Create test entity

Request:

POST /testentity
```
{
	"name": "new present test entity"
}
```

Response:

200:
```
{
    "id": <UUID>
    , "name": "new present test entity"
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
