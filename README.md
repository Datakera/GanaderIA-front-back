# GanadeIA - Aplicación Android

GanadeIA es una aplicación Android moderna desarrollada en **Kotlin** utilizando **Jetpack Compose** para la construcción de interfaces gráficas de forma declarativa y reactiva. Este proyecto ha sido estructurado para facilitar el crecimiento de la aplicación, implementando una base sólida lista para separar la lógica de negocio (Backend y APIs) de la capa visual (Frontend).

Actualmente cuenta con las interfaces principales de **Login** y **Dashboard** para gestionar el registro de animales, analizar datos mediante inteligencia artificial y ver recomendaciones en tiempo real. 

---

## 🛠 Entorno de Desarrollo y Cómo Ejecutar el Proyecto
Para que cualquiera de los desarrolladores pueda correr este proyecto, sigue estos pasos:
1. Asegúrate de tener instalado [Android Studio](https://developer.android.com/studio) (Versión Iguana o superior es recomendada).
2. Clona el repositorio y ábrelo en Android Studio seleccionando `File > Open`.
3. Deja que Android Studio sincronice el proyecto y descargue las dependencias Gradle de manera automática. (Esto puede tomar un par de minutos).
4. Crea o inicia un dispositivo virtual (Emulador) a través del "Device Manager" o conecta un dispositivo físico mediante "Depuración USB".
5. Presiona el botón verde de **Run (Play)** en la barra superior de Android Studio para compilar y ejecutar la app.

---

## 🏗 Arquitectura del Proyecto

El proyecto se dividió bajo principios de **Clean Architecture** (Arquitectura Limpia) y el patrón MVVM (que se puede implementar más a detalle próximamente). Esto significa que el código está segmentado por responsabilidades en el paquete `app/src/main/java/com/ganadeia/app/`:

- `ui/`: Todo lo referente a la interfaz de usuario.
- `domain/`: La lógica de negocio pura y los modelos.
- `data/`: Acceso a datos, conexión con APIs y bases de datos locales.

### 🎨 Para Desarrolladores de Frontend (UI)
Todo tu trabajo ocurrirá en la carpeta **`ui/`**. Aquí no hay lógica de red ni bases de datos.
- **`screens/`**: Para crear nuevas pantallas, agrégalas como funciones `@Composable` en este paquete.
- **`theme/`**: Los colores, tipografías y el tema base se encuentran definidos aquí (`Color.kt`, `Type.kt`, `Theme.kt`). Si los diseños en Figma cambian, edita estos archivos para que el cambio aplique en toda la app.
- **Navegación**: Revisa el archivo `AppNavigation.kt`. Usa la clase `NavHost` de Jetpack Navigation Compose para registrar tus nuevas pantallas (rutas) y conectar su navegación.

### ⚙️ Para Desarrolladores de Backend / Integración de Datos
Todo el trabajo crítico con información real, consumo de APIs (por ejemplo usando Retrofit, Ktor), e implementación de bases de datos locales (Room) ocurrirá en las carpetas **`data/`** y **`domain/`**.

1. **`domain/`**: Crea en esta carpeta tus *Modelos de datos puros* de Kotlin (data classes) y los *Casos de Uso* (Use Cases). Estos archivos no deben depender de librerías de Android ni de las APIs. Son las reglas del negocio.
2. **`data/`**: Define aquí tus servicios web (`ApiServices`), repositorios e implementaciones concretas que devuelvan los modelos del `domain`. Puedes agregar librerías como Retrofit o Ktor en el `app/build.gradle.kts`.
3. **VMs / State Holders**: Para conectar los datos con la interfaz (`ui`), se sugiere la creación de **ViewModels**. Los ViewModels serán los encargados de comunicarse con los Repositorios definidos en `data/` y exponer *States* (`StateFlows` o `LiveDatas`) hacia los composables en la carpeta `ui/`.

--

