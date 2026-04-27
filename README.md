# mcell
Me cago en La Liga

> ¿Bloqueos de La Liga? Compruébalo en tu tele.

Una aplicación para Android TV y móviles que te dice si los bloqueos a nivel de ISP ordenados por La Liga están afectando ahora mismo a las URLs que te importan.

[**Sitio web**](https://mattogodoy.github.io/mcell/) · [**Descargar APK**](https://github.com/mattogodoy/mcell/releases/latest/download/mcell.apk)

<p align="center">
  <img src="docs/assets/icon.png" alt="mcell" width="160" />
</p>

## Por qué

Cuando hay partido, los principales ISPs españoles (Movistar, MasOrange, Vodafone, DIGI) están obligados por orden judicial a bloquear rangos enteros de IPs para combatir la piratería. Los bloqueos son demasiado amplios y rompen sistemáticamente el acceso a CDNs legítimos que alojan tiendas, medios, equipos de fútbol, patrocinadores e incluso la RAE. Si tu Smart TV deja de cargar contenido justo cuando empieza el partido, probablemente no es tu tele.

mcell te muestra, en dos segundos, si una URL que te importa es accesible desde tu red *ahora mismo*.

## Funciones

- Un único APK funciona en **Android TV** (leanback) y **móviles Android** (táctil). `minSdk` 26, `targetSdk` 34.
- Hasta **6 URLs configurables**, con estado en tres niveles (accesible / error HTTP / error de red) y un motivo localizado al enfocar la fila.
- Un **banner de estado global** en la parte superior, alimentado por los datos públicos de [hayahora.futbol](https://hayahora.futbol) y replicando la lógica de clasificación de su página principal.
- **Detección de VPN** mediante `ConnectivityManager`.
- **Sin caché.** Cada comprobación es una petición de red nueva — esa es la idea.
- **Sin telemetría, sin cuentas, sin analíticas.** El único permiso que pide es el acceso a internet.
- Interfaz solo en español (castellano).

## Instalación

Descarga el APK firmado más reciente desde [Releases](https://github.com/mattogodoy/mcell/releases/latest) e instálalo manualmente en tu tele o móvil (tendrás que permitir la instalación de orígenes desconocidos).

## Compilación

Requisitos: JDK 17 y el SDK de Android con soporte para Compose.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

La batería de tests unitarios cubre el comprobador de URLs, el parser de hayahora.futbol (con su control de antigüedad de los datos), el repositorio de la lista de URLs, la derivación del banner y la máquina de estados de `HomeViewModel`.

## Stack

- Kotlin + Jetpack Compose, con `androidx.tv.material3` para el estilo de foco en TV.
- OkHttp para las comprobaciones de URL (HEAD con fallback a GET y caché desactivada en todos los niveles).
- AndroidX DataStore Preferences para guardar la lista de URLs del usuario.
- `kotlinx.serialization` para parsear el JSON de hayahora.futbol.
- MVVM con un único módulo `:app` e inyección de dependencias manual mediante un `AppContainer`.

## Cómo se calcula el estado global

El banner replica la regla que usa la propia página de `hayahora.futbol`: los bloqueos se consideran activos cuando **o bien** hay más de 10 IPs distintas de Cloudflare bloqueadas por más de 2 ISPs cada una, **o bien** tanto `188.114.96.5` como `188.114.97.5` están bloqueadas por cualquier ISP. Si el `lastUpdate` de hayahora tiene más de 6 horas, mostramos "desconocido" antes que datos potencialmente desactualizados.

## Atribución

Los datos sobre bloqueos son generosamente publicados por **[hayahora.futbol](https://hayahora.futbol)**, un proyecto independiente que monitoriza los bloqueos de La Liga en todos los principales ISPs españoles en tiempo real. mcell se limita a consumir su API pública, sin scraping ni APIs ocultas. Si esta app te resulta útil, considera ayudar a documentar bloqueos desde tu propia conexión con [OONI Probe](https://ooni.org/install/).

## Licencia

[Licencia Pública General GNU v3.0](LICENSE).
