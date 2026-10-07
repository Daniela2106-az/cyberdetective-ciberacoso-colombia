# CyberDetective – El Árbol de la Verdad

Juego educativo de investigación en Java/JavaFX sobre **ciberacoso en Colombia**. El jugador asume el papel de un detective digital que analiza evidencias (chats, perfiles falsos, publicaciones, redes de cuentas), identifica al agresor, clasifica el delito según la ley colombiana y reconstruye la línea de tiempo del caso. Sirve además para practicar **estructuras de datos (árbol AVL)**.

## Qué hace el proyecto

- Presenta una investigación por niveles, cada uno con un caso de acoso digital distinto.
- Cada caso incluye evidencias, la ley colombiana aplicable y la pena correspondiente.
- Los casos se organizan en un **árbol AVL ordenado por gravedad del delito**, que se visualiza gráficamente.
- Se puede jugar en solitario o en **modo multijugador cooperativo** (un jugador crea la investigación como host y otros se unen por red).

## Características

- 4 niveles de casos más un nivel final de cronología y reporte:
  1. Injuria en redes sociales
  2. Calumnia – difusión de rumores falsos
  3. Suplantación de identidad digital
  4. Acoso y hostigamiento digital coordinado
- Mapa de investigación interactivo y panel de evidencias con imágenes.
- Visualizador del árbol AVL (inserciones y rotaciones).
- Minijuegos: Escombros, Sopa de letras y Conexión.
- Multijugador por sockets TCP (puerto `12345`) con sincronización de preguntas, puntajes y minijuegos.
- Interfaz en pantalla completa con estilos CSS propios (`styles.css`).

## Estructura de carpetas

```
cyberdetective/
├── pom.xml                         # Configuración Maven (Java 17, JavaFX 21)
└── src/main/
    ├── java/cyberdetective/
    │   ├── Main.java               # Punto de entrada JavaFX
    │   ├── controller/             # Lógica del juego (individual y multijugador)
    │   ├── data/                   # NivelesData: casos, pistas y preguntas
    │   ├── minijuego/              # Escombros, Sopa de letras, Conexión y gestor
    │   ├── model/                  # Caso, ArbolAVL, NodoAVL
    │   ├── network/                # NetworkClient y GameMessage
    │   ├── server/                 # GameServer y ClientHandler
    │   └── view/                   # Pantallas: menú, lobby, juego, mapa, evidencias, árbol
    └── resources/
        ├── styles.css
        └── images/                 # Avatares, íconos de nivel y evidencias
```

## Requisitos y dependencias

- JDK 17 o superior
- Maven 3.6+
- JavaFX 21 (`javafx-controls`, `javafx-fxml`, `javafx-media`), que Maven descarga automáticamente
- Conexión a internet la primera vez (dependencias y la fuente DM Sans/DM Mono de Google Fonts usada por el CSS)

## Cómo usarlo

```bash
git clone https://github.com/Daniela2106-az/cyberdetective.git
cd cyberdetective
mvn clean javafx:run
```

También puedes abrirlo en IntelliJ IDEA como proyecto Maven y ejecutar `cyberdetective.Main`.

### Menú principal

- **Iniciar investigación**: partida individual.
- **Crear Investigación (Host)**: inicia el servidor en segundo plano y abre el lobby.
- **Unirse a Investigación**: introduce la IP del host (por defecto `localhost`) y tu nombre.
- **¿Cómo funciona?**: explicación del juego.

### Multijugador

El host debe permitir conexiones entrantes al puerto `12345`. Los jugadores deben estar en la misma red (o tener acceso a la IP del host). El servidor también puede ejecutarse solo con la clase `cyberdetective.server.GameServer`.
