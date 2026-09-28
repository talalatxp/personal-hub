# Personal Hub

Aplicación Android personal con Kotlin y Jetpack Compose. Los datos se guardan en el dispositivo, sin cuentas, sincronización ni permisos de red.

Actualmente incluye la base de la aplicación: pantalla inicial, almacenamiento privado con Room, preferencias con DataStore y pruebas. Las funciones de entrenamiento todavía están en desarrollo.

## Requisitos

- JDK 17.
- Android SDK Platform 36 y Build Tools 36.0.0, con las licencias aceptadas.
- Android 8.0/API 26 o superior para instalar la aplicación.

El wrapper de Gradle y las versiones de las dependencias están incluidos en el repositorio. La primera compilación necesita conexión para descargarlas.

## Compilar y validar

Desde la carpeta del proyecto, en PowerShell:

```powershell
$env:JAVA_HOME = 'C:/ruta/al/jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA/Android/Sdk"
.\scripts\validate.ps1 -JdkHome $env:JAVA_HOME -SdkRoot $env:ANDROID_HOME
```

Añadir `-Connected` para ejecutar las pruebas instrumentadas con un dispositivo o emulador conectado. Para aplicar el formato Kotlin:

```powershell
.\gradlew.bat :app:ktlintFormat --no-daemon
```

El APK de prueba se genera en `app/build/outputs/apk/debug/app-debug.apk`. Usa firma debug; no incluye la firma de distribución.

## Privacidad

La aplicación desactiva el backup automático y declara exclusiones de copia en la nube y transferencia del dispositivo. El script de validación comprueba el manifest release y los permisos del APK.

Git excluye compilaciones, configuración local, claves de firma, bases de datos, copias, medios privados y documentación interna. No guardar datos personales ni credenciales entre los archivos del código fuente.
