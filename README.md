# SVCL Java MySQL

Entrega limpia del proyecto **Sistema de Validacion de Cobertura Logistica (SVCL)** con:

- frontend web en `src/main/resources/static`
- backend en Java 11
- conexion MySQL por JDBC
- importacion de Excel `.xlsx`
- prueba automatica con JUnit
- integracion continua con Travis CI

## Estructura

```text
SVCL_entrega_java_mysql/
├── .gitignore
├── .travis.yml
├── README.md
├── docs/
│   └── architecture.md
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/svcl/app/
    │   └── resources/static/
    └── test/java/com/svcl/app/
```

## Variables de entorno

El backend toma la configuracion desde variables de entorno:

```bash
export APP_HOST=127.0.0.1
export APP_PORT=8010
export APP_USERNAME=admin
export APP_PASSWORD=admin

export SVCL_DB_HOST=127.0.0.1
export SVCL_DB_PORT=3306
export SVCL_DB_NAME=svcl
export SVCL_DB_USER=root
export SVCL_DB_PASSWORD=tu_password
```

Opcionalmente puedes usar una URL JDBC completa:

```bash
export SVCL_DB_URL="jdbc:mysql://127.0.0.1:3306/svcl?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
```

## Comandos de terminal

Compilar:

```bash
mvn clean compile
```

Ejecutar pruebas:

```bash
mvn test
```

Empaquetar:

```bash
mvn clean package
```

Levantar el servidor:

```bash
mvn exec:java
```

Luego abre:

```text
http://127.0.0.1:8010
```

## Base de datos

El backend crea automaticamente las tablas MySQL necesarias:

- `coverage`
- `distances`
- `metadata`

Puedes iniciar vacio y cargar un Excel desde la interfaz web, o dejar un archivo semilla en:

```text
data/current_database.xlsx
```

## Git y entrega final

Inicializacion local:

```bash
git init -b master
git add .
git commit -m "feat: migrate SVCL backend to Java and MySQL"
git checkout -b develop
git checkout master
```

Publicacion:

```bash
git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
git push -u origin master
git push -u origin develop
```


