# Guía de uso y desarrollo de STARBUCKS Pre-nómina

STARBUCKS Pre-nómina es una aplicación de escritorio para concentrar personal, días trabajados e incidencias por quincena. El flujo termina con la revisión, la autorización y los reportes de descuentos preliminares. La entrega incluye una copia de MariaDB con cuentas iniciales y datos académicos ficticios.

## Instalación en Windows

### Preparar el equipo

1. Instala JDK 17 o 21, Apache NetBeans 20 y MariaDB Server 11.8 con sus herramientas de cliente. El respaldo se generó con MariaDB 11.8.
2. Copia la carpeta completa del proyecto, incluido `pom.xml`, `src` y `entrega`. Maven descarga las dependencias la primera vez que compila.
3. Inicia el servicio de MariaDB y confirma el puerto configurado durante su instalación.

### Restaurar la base

Usa el respaldo `starbucks_prenomina_windows.sql` de la carpeta `entrega` en una instalación donde todavía no exista el esquema `starbucks_prenomina`. El archivo conserva claves, datos, usuarios de la aplicación y relaciones. Si ya tienes un esquema con ese nombre, haz una copia de seguridad y restaura este archivo en una instancia aparte para evitar mezclar registros.

En el **Símbolo del sistema**, desde la carpeta raíz del proyecto, ejecuta lo siguiente. Si `mariadb.exe` no está en `PATH`, utiliza la ruta completa al ejecutable entre comillas.

```bat
mariadb.exe --default-character-set=utf8mb4 -u root -p < "entrega\starbucks_prenomina_windows.sql"
mariadb.exe -u root -p -e "SELECT COUNT(*) AS cuentas FROM starbucks_prenomina.usuarios;"
```

MariaDB solicita la contraseña del servidor que definiste al instalarlo. Esa contraseña es distinta de las que se usan para entrar en la aplicación. La segunda consulta debe devolver tres cuentas para el respaldo entregado.

El respaldo incluye las contraseñas iniciales en texto plano por el alcance académico del proyecto. Guárdalo junto con el proyecto en un espacio de acceso restringido. Para regenerar el respaldo de una instalación local más adelante, ejecuta este comando desde la raíz del proyecto.

```bat
mariadb-dump.exe -u root -p --single-transaction --skip-add-drop-table --skip-add-locks --default-character-set=utf8mb4 --databases starbucks_prenomina > "entrega\starbucks_prenomina_respaldo.sql"
```

### Configurar y abrir la aplicación

Define las variables en Windows y vuelve a abrir NetBeans para que lea la configuración. Puedes asignarlas en **Configuración avanzada del sistema → Variables de entorno**.

| Variable | Configuración habitual |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `starbucks_prenomina` |
| `DB_USER` | Cuenta de MariaDB con acceso al esquema |
| `DB_PASSWORD` | Contraseña de esa cuenta de MariaDB |

Si ejecutas desde una consola o una configuración de Maven, también puedes usar las propiedades Java `db.host`, `db.port`, `db.name`, `db.user` y `db.password`. Cuando coinciden ambas configuraciones, tienen prioridad las propiedades Java.

En NetBeans abre la carpeta que contiene `pom.xml`. Selecciona **Run Project** y usa `com.mycompany.starbucks.Starbucks` como clase principal si NetBeans te pide elegirla. El código compila con destino Java 17 y puede ejecutarse con JDK 21.

## Uso por rol

| Cuenta inicial | Rol | Operaciones principales |
|---|---|---|
| `administrador` | Administrador | Gestionar periodos, capturar, registrar reglas aprobadas, calcular, revisar, autorizar y consultar reportes |
| `capturista` | Capturista | Dar de alta y editar colaboradores y capturar días o incidencias en periodos editables |
| `revisor` | Revisor | Consultar, registrar observaciones, devolver a captura, autorizar y generar reportes |

### 1. Preparar una quincena

En **Personal**, guarda el número de empleado, el nombre, el salario diario y el estado. Busca por número o nombre y usa **Editar seleccionado** para cambiar un registro.

En **Periodos**, registra una quincena del día 1 al 15 o del 16 al último día del mes. Selecciona la fila y abre **Días trabajados del periodo seleccionado**. Guarda la cantidad por colaborador. El valor cero es válido y queda registrado.

### 2. Capturar incidencias

Abre **Incidencias**, selecciona colaborador, quincena, tipo y fecha. Completa los campos que aparecen al seleccionar el tipo. Los permisos, vacaciones e incapacidades admiten un rango dentro de la misma quincena. Usa **Ver detalle** para consultar lo guardado y **Editar seleccionada** para modificar una incidencia pendiente de un periodo en captura.

### 3. Calcular, revisar y autorizar

1. Como administrador, abre **Pre-nómina**, selecciona la quincena y entra en **Reglas autorizadas**. Registra el tratamiento de cada tipo presente y la referencia de su aprobación académica. El respaldo no incluye reglas de descuento.
2. Pulsa **Calcular**. Corrige los datos que indique la aplicación si faltan días trabajados, hay incidencias incompletas o falta una regla aplicable.
3. Usa **Ver detalle** sobre una fila para contrastar días trabajados, incidencias y descuento. Selecciona **Enviar a revisión** cuando el resultado esté listo.
4. Como revisor o administrador, registra observaciones con **Revisar**. Usa **Devolver a captura** si hay correcciones. Pulsa **Autorizar** para guardar el cierre y bloquear cambios sobre el periodo.

### 4. Consultar reportes

En **Reportes**, elige personal, incidencias, descuentos preliminares o revisiones y autorizaciones. Aplica los filtros y pulsa **Consultar**. Usa **Exportar CSV** para guardar la tabla en UTF-8. El módulo está disponible para administradores y revisores. Para terminar, usa **Cerrar sesión** en el encabezado.

Las tres cuentas de esta entrega ya están persistidas en MariaDB. En esta revisión, la pantalla **Usuarios** permite consultar las cuentas. Sus acciones de alta y eliminación todavía son simuladas.

## Cómo se desarrolló

El proyecto usa **Java Swing y SwingWorker** para la interfaz, **FlatLaf** para el tema visual, **Maven** para compilar y resolver dependencias, y **JDBC con MariaDB Connector/J** para acceder al esquema. La ventana principal conserva el menú mediante `CardLayout`. El diseño del encabezado, la navegación y el panel toma como referencia la estructura del proyecto académico Refaccionaria, adaptada al tema oscuro y verde de Starbucks.

| Parte | Ubicación | Función |
|---|---|---|
| Arranque y conexión | `Starbucks.java`, `config/` | Instancia única, configuración JDBC y verificación inicial |
| Interfaz | `view/` | Inicio de sesión, navegación, formularios Swing y reportes |
| Dominio | `model/` | Colaboradores, quincenas, roles, incidencias y resultados |
| Reglas de negocio | `service/` | Permisos, validación, cálculo con `BigDecimal` y transiciones del periodo |
| Persistencia | `dao/` | Consultas parametrizadas y operaciones de MariaDB |
| Recursos SQL | `src/main/resources/sql/` | Esquema de instalación y datos ficticios opcionales |
| Respaldo instalable | `entrega/` | Copia del esquema con registros y cuentas iniciales |

El desarrollo comenzó con la recuperación de la compilación y la conexión JDBC. Después se integraron Personal y Periodos, la captura de días e incidencias, el cálculo preliminar, la revisión, la autorización y los reportes. El cálculo guarda los resultados y la bitácora dentro de una transacción, con reversión si falla la escritura. La migración se verificó restaurando el respaldo en un esquema separado de MariaDB y autenticando las tres cuentas con los roles esperados. La ejecución en Windows queda por validar en el equipo de destino.
