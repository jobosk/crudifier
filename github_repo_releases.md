
# Como crear un repositorio para las releases de la libreria

## Clonar el repo original de la libreria en una nueva carpeta (si no existe)

`git clone https://github.com/jobosk/crudifier.git crudifier-repository`

## Acceder a la carpeta

`cd crudifier-repository`

## Crear una nueva rama para sacar versiones en el repositorio

`git branch repository`

## Cambiar a dicha rama

`git checkout repository`

## Borrar ficheros del proyecto

Borramos todos los ficheros menos la carpeta `.git`

## Compilar e instalar el JAR de la 'release' en la carpeta

```bash
mvn install:install-file -DgroupId=com.github.jobosk -DartifactId=crudifier -Dversion=<version> -Dfile=<ruta al JAR compilado de la version> -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=.  -DcreateChecksum=true
```

## Añadir, 'comitear' y 'pushear' el nuevo compilado de la 'release'

`git add -A . && git commit -m "released version <version>"`

`git push origin repository`

## Referenciar el JAR como un repositorio desde otros proyectos

La URL para acceder al JAR es esta:

https://github.com/jobosk/crudifier/blob/repository/com/github/jobosk/crudifier/<version>/crudifier-<version>.jar?raw=true

Y es accesible añadiendo este repositorio en Maven:

<repository>
	<id>...</id>
	<name>...</name>
	<url>https://github.com/jobosk/crudifier/blob/repository/com/github/jobosk/crudifier/<version>/crudifier-<version>.jar?raw=true</url>
</repository>