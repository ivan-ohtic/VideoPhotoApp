# VideoPhotoApp

## Descripción
Esta aplicación Android se enfoca en la reproducción y visualización de videos y fotos desde archivos locales. Utiliza una arquitectura eficiente para garantizar una experiencia de usuario fluida y un manejo óptimo de recursos.

## Comenzando

Estas instrucciones te permitirán obtener una copia del proyecto en funcionamiento en tu máquina local para propósitos de desarrollo y pruebas.

### Prerrequisitos

Para instalar y ejecutar este proyecto, necesitas:

- Java
- Android Studio (versión 7.0 o superior)
- SDK de Android (API Level 24 o superior)

### Instalación

Sigue estos pasos para configurar el entorno de desarrollo:

1. **Clonar el Repositorio.**
   ```bash
   git clone https://github.com/ivan-ohtic/VideoPhotoApp.git

2. **Abrir el Proyecto en Android Studio.**

3. **Sincronizar el Proyecto con los Archivos Gradle.**

4. **Ejecutar el Proyecto en un Emulador o Dispositivo Físico.**

### Uso

La aplicación VideoPhotoApp está diseñada para leer y reproducir medios (imágenes y videos) desde un archivo ZIP proporcionado. Aquí te explicamos cómo usarla:

   **Cargar Archivos**: La aplicación accede al contenido de un archivo ZIP en la ruta /src/main/assets. Este archivo debe llamarse NSIGN_Prueba_Android y contener recursos (imagenes o vídeos mp4) y un archivo `events.json`.

   **Configuración del Contenido**: El archivo `events.json` incluido en el ZIP define las playlist, zonas de pantalla y recursos, el orden en el que deben ser mostrados y el tiempo de reproducción de cada uno.

   - **Zona en Pantalla**: Se refiere a la ubicación y dimensiones en pantalla donde se mostrará cada recurso.
   - **Orden y Tiempo de Reproducción**: Define el orden en que se mostrarán las imágenes y videos, así como la duración de cada uno en pantalla.

   **Interfaz de Usuario (UI)**: Al abrir la aplicación, comienza automáticamente a reproducir los medios según el orden definido en `events.json`. 

   - **Imágenes**: Se muestran durante el tiempo especificado en `events.json`. Después de este tiempo, la aplicación continúa con el siguiente recurso.
   - **Videos**: Se reproducen completamente antes de pasar al siguiente recurso en la lista.

   **Reproducción Asíncrona**: La lectura y reproducción de recursos se realiza de manera asíncrona para evitar bloqueos en la interfaz de usuario.

   **Indicador de Proceso**: Al inicio mientras se cargan los medios, se muestra un indicador de progreso para informar al usuario que la carga está en curso.

   **Manejo de Errores**: La aplicación incluye un manejo de errores para posibles problemas relacionados con la lectura de recursos y del archivo `events.json`.


### Este proyecto ha sido desarrollado utilizando las siguientes herramientas y tecnologías:
   
   Android Studio - IDE utilizado para el desarrollo
   
   ExoPlayer - Librería para la reproducción de videos
   
   GSON - Utilizado para la conversión de objetos Java a JSON y viceversa
