# SVCL - Sistema de Validacion de Cobertura Logistica

## Resumen Ejecutivo

El **Sistema de Validacion de Cobertura Logistica (SVCL)** es una solucion desarrollada para **ARM Acabados** con el objetivo de automatizar la validacion de cobertura por codigo postal y apoyar la toma de decisiones operativas en procesos de distribucion y atencion al cliente. El sistema integra una interfaz web, un backend en **Java 17 (Temurin)**, persistencia en **MySQL**, pruebas automatizadas con **JUnit** y ejecucion de integracion continua mediante **GitHub Actions**.

## Problema

Antes de la implementacion del SVCL, la validacion de cobertura se realizaba de forma manual mediante archivos de Excel. Este enfoque provocaba:

- tiempos elevados de consulta
- dependencia de capturas manuales
- errores humanos en la interpretacion de la informacion
- baja trazabilidad del proceso de validacion

## Solucion

El SVCL automatiza el proceso de validacion de codigos postales mediante una plataforma construida con **Java y MySQL**, permitiendo consultas instantaneas, importacion controlada de datos y administracion centralizada de la base de cobertura. Con ello se reduce el tiempo de respuesta, se mejora la exactitud de la informacion y se fortalece la operacion logistica.

## Arquitectura de la Solucion

La aplicacion se implementa bajo una **arquitectura de N-Capas**, organizada academicamente en **3 capas principales**:

- **Presentacion**: interfaz web para consulta y administracion.
- **Negocio**: backend Java encargado de autenticacion, validaciones y procesamiento de reglas de cobertura.
- **Datos**: base de datos MySQL para almacenar coberturas, rutas y metadatos.

Adicionalmente, el proyecto incorpora un flujo de **CI/CD automatizado** mediante **GitHub Actions**, con construccion y validacion automatica del proyecto usando Maven.

## Tabla de Contenidos

- [Requerimientos Tecnicos](#requerimientos-tecnicos)
- [Instalacion y Pruebas](#instalacion-y-pruebas)
- [Configuracion y Uso](#configuracion-y-uso)
- [Gobernanza del Proyecto](#gobernanza-del-proyecto)
- [Roadmap](#roadmap)
- [Entregables Finales](#entregables-finales)

## Requerimientos Tecnicos

Para ejecutar el proyecto se requiere el siguiente entorno:

- **Java 17 (Temurin)**
- **Maven 3.8 o superior**
- **MySQL 8.0**
- **IDE recomendado**: IntelliJ IDEA o Visual Studio Code

## Instalacion y Pruebas

### Clonar el repositorio

```bash
git clone https://github.com/RicardoSantos55/svcl-java-mysql.git
cd svcl-java-mysql
```

### Ejecutar pruebas

```bash
mvn test
```

### Compilar y empaquetar el proyecto

```bash
mvn package
```

### Ejecutar el servidor

```bash
mvn exec:java
```

## Configuracion y Uso

### Configuracion

Para fines de entrega academica, la conexion a la base de datos debe documentarse en un archivo `application.properties`, incorporando las credenciales de acceso a MySQL. Una configuracion de referencia seria:

```properties
app.host=127.0.0.1
app.port=8010
db.host=127.0.0.1
db.port=3306
db.name=svcl
db.user=root
db.password=tu_password
```

Nota tecnica: la implementacion actual tambien admite configuracion equivalente mediante variables de entorno para ejecucion por terminal.

### Manual de Uso para Usuario Final

El usuario final interactua con el sistema para consultar cobertura por codigo postal.

1. Inicia sesion en la interfaz web.
2. Selecciona la sucursal origen.
3. Captura el codigo postal a consultar.
4. Revisa el resultado de cobertura, sucursal destino y validacion de distancia.

### Manual de Uso para Administrador

El administrador tiene permisos para gestionar la base de datos del sistema.

1. Inicia sesion con credenciales administrativas.
2. Importa una base nueva desde archivo Excel.
3. Agrega registros manuales cuando se requieran ajustes puntuales.
4. Verifica el estado de la base, rutas y codigos postales repetidos.

## Gobernanza del Proyecto

### Guia de Contribucion

La colaboracion sobre el proyecto debe seguir el siguiente flujo de trabajo:

1. Clonar el repositorio.
2. Crear una rama de trabajo con prefijo `feat/`.
3. Desarrollar y validar los cambios localmente.
4. Enviar un **Pull Request** dirigido a la rama `develop`.
5. Revisar, aprobar y fusionar los cambios antes de promoverlos a la rama principal.

Ejemplo:

```bash
git checkout develop
git pull
git checkout -b feat/nueva-funcionalidad
git add .
git commit -m "feat: descripcion del cambio"
git push -u origin feat/nueva-funcionalidad
```

## Roadmap

Como mejoras futuras del sistema, se proponen las siguientes lineas de evolucion:

1. **Integracion con Google Maps API** para enriquecer el calculo y visualizacion de rutas logisticas.
2. **Panel de analitica operativa** para medir frecuencia de consultas, zonas de cobertura y tiempos de respuesta.
3. **Control de usuarios y roles** para segmentar permisos entre administradores, operadores y consulta ejecutiva.

## Entregables Finales

### Video de demostracion

Colocar aqui el enlace al video final de presentacion del proyecto:

```text
Pendiente de agregar enlace de demostracion
```

### Archivo ejecutable `.jar`

Despues de ejecutar `mvn package`, el entregable compilado se genera en:

```text
target/svcl-java-mysql-1.0.0.jar
```

### Documentacion de apoyo

La documentacion tecnica complementaria se encuentra en:

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/project-management.md`](docs/project-management.md)
