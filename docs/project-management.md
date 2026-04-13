# Gestion del Proyecto

## Branches requeridas

- `master`
- `develop`

Las dos ya existen localmente en este repositorio.

## Milestones sugeridos

### Beta

- Login y control de sesion
- Carga de Excel a MySQL
- Consulta por codigo postal

### RC

- Alta manual de registros
- Pruebas JUnit y ajuste final de CI
- Documentacion y arquitectura

## Issues sugeridos

### 1. Login y control de sesion

**Titulo**

`[REQ] Implementar login y control de sesion`

**Descripcion**

Permitir que el usuario acceda al sistema SVCL con credenciales y mantener una sesion valida para proteger las operaciones administrativas.

**Analisis**

El sistema expone operaciones sensibles como importar bases y agregar registros manuales. Era necesario protegerlas con autenticacion y sesiones HTTP.

**Solucion**

Se implementaron endpoints de login, logout y consulta de sesion, junto con cookies de sesion en el backend Java.

**Milestone**

`Beta`

### 2. Importacion de Excel a MySQL

**Titulo**

`[REQ] Importar base de cobertura desde Excel a MySQL`

**Descripcion**

Cargar un archivo `.xlsx` con cobertura y distancias para poblar la base de datos del sistema.

**Analisis**

La informacion operativa se entrega en Excel, por lo que el backend debia procesar hojas, validar columnas y guardar la informacion en MySQL.

**Solucion**

Se implemento `WorkbookImporter` para leer el Excel y `CoverageRepository` para insertar los datos en MySQL y actualizar metadatos.

**Milestone**

`Beta`

### 3. Consulta por codigo postal

**Titulo**

`[REQ] Consultar cobertura por sucursal y codigo postal`

**Descripcion**

Permitir al usuario seleccionar una sucursal origen y un codigo postal para obtener cobertura, sucursal destino y validacion de distancia.

**Analisis**

El valor principal del sistema es decidir si un codigo postal tiene cobertura valida desde una sucursal base, incluyendo la regla de distancia maxima.

**Solucion**

Se implemento la busqueda en MySQL, el cruce con rutas y la validacion de distancia menor o igual a 1600 km.

**Milestone**

`Beta`

### 4. Alta manual de registros

**Titulo**

`[REQ] Agregar registros manuales a la base`

**Descripcion**

Permitir capturar manualmente un registro de cobertura sin reemplazar toda la base.

**Analisis**

En pruebas y correcciones puntuales no siempre conviene importar un Excel completo. Se requeria una opcion de captura manual con validaciones.

**Solucion**

Se agrego el endpoint `/api/manual-entry` y la interfaz para capturar codigo postal, plaza, sucursal y datos de cobertura.

**Milestone**

`RC`

### 5. Integracion continua con Maven y JUnit

**Titulo**

`[REQ] Configurar integracion continua con pruebas JUnit`

**Descripcion**

Asegurar que el repositorio ejecute pruebas automaticamente al integrarse con Travis CI.

**Analisis**

La rubrica exige una prueba automatica corriendo en CI. Se necesitaba una estructura Maven y al menos una prueba funcional.

**Solucion**

Se definio `pom.xml`, se agrego `CoberturaTest` en `src/test/java` y se configuro `.travis.yml` para ejecutar `mvn test`.

**Milestone**

`RC`

### 6. Documentacion de arquitectura

**Titulo**

`[REQ] Documentar la arquitectura cliente-servidor-base de datos`

**Descripcion**

Presentar de forma clara la arquitectura de la solucion para sustentar el diseno del sistema.

**Analisis**

La evaluacion requiere evidenciar la separacion entre cliente, servidor de aplicaciones y base de datos.

**Solucion**

Se documento la arquitectura en `docs/architecture.md` con descripcion de capas y flujo principal.

**Milestone**

`RC`

## Pasos para subir esta parte a GitHub

1. Crear el repositorio en GitHub.
2. Agregar el remoto:

```bash
git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
```

3. Subir ramas:

```bash
git push -u origin master
git push -u origin develop
```

4. Crear milestones:
   - `Beta`
   - `RC`

5. Crear los issues usando el contenido de este archivo o la plantilla en `.github/ISSUE_TEMPLATE/requisito.md`.
