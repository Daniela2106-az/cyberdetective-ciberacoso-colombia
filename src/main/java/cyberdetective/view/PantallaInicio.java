package cyberdetective.view;

import cyberdetective.controller.JuegoController;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Pantalla de ingreso del nombre del jugador.
 * Aparece después del menú principal y antes del mapa.
 */
public class PantallaInicio {

    private Stage stage;

    public PantallaInicio(Stage stage) {
        this.stage = stage;
    }

    public void mostrar() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0a0f;");

        VBox contenido = construirContenido();
        root.setCenter(contenido);

        // Reutilizar la escena si ya existe
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
            );
            scene.setFill(Color.web("#0a0a0f"));
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        
        stage.setFullScreen(true);
        animarEntrada(contenido);
    }

    private VBox construirContenido() {
        VBox contenido = new VBox(0);
        contenido.setAlignment(Pos.CENTER);
        contenido.setMaxWidth(520);

        // Avatares de Alex y Valeria
        HBox avatares = construirAvatares();
        VBox.setMargin(avatares, new Insets(0, 0, 36, 0));

        // Título
        Label titulo = new Label("Bienvenido, Detective");
        titulo.setStyle(
                "-fx-font-size: 38px; -fx-font-weight: 600; -fx-text-fill: #f0f0f8;"
        );

        Label subtitulo = new Label(
                "El caso de Valeria necesita tu ayuda. " +
                        "Junto al Det. Alex reconstruirás el árbol de la verdad."
        );
        subtitulo.getStyleClass().add("subtitulo");
        subtitulo.setWrapText(true);
        subtitulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subtitulo.setMaxWidth(440);
        VBox.setMargin(subtitulo, new Insets(10, 0, 40, 0));

        // Campo de nombre
        Label nombreEtq = new Label("¿CUÁL ES TU NOMBRE, DETECTIVE?");
        nombreEtq.getStyleClass().add("etiqueta-seccion");

        TextField campoNombre = new TextField();
        campoNombre.getStyleClass().add("input-nombre");
        campoNombre.setPromptText("Escribe tu nombre...");
        campoNombre.setMaxWidth(380);
        VBox.setMargin(campoNombre, new Insets(10, 0, 0, 0));

        // Mensaje de Valeria
        VBox burbujaValeria = construirBurbujaValeria();
        VBox.setMargin(burbujaValeria, new Insets(28, 0, 32, 0));

        // Botón de inicio
        Button btnIniciar = new Button("Iniciar investigación →");
        btnIniciar.getStyleClass().add("btn-primario");
        btnIniciar.setPrefWidth(280);
        btnIniciar.setDisable(true);

        // Habilitar botón solo si hay nombre
        campoNombre.textProperty().addListener((obs, old, nuevo) ->
                btnIniciar.setDisable(nuevo.trim().isEmpty())
        );

        // Enter también inicia
        campoNombre.setOnAction(e -> {
            if (!campoNombre.getText().trim().isEmpty()) {
                iniciarJuego(campoNombre.getText().trim());
            }
        });

        btnIniciar.setOnAction(e ->
                iniciarJuego(campoNombre.getText().trim())
        );

        Button btnVolver = new Button("← Volver al menú");
        btnVolver.getStyleClass().add("btn-secundario");
        btnVolver.setOnAction(e -> new MenuPrincipal(stage).mostrar());
        VBox.setMargin(btnVolver, new Insets(12, 0, 0, 0));

        contenido.getChildren().addAll(
                avatares, titulo, subtitulo,
                nombreEtq, campoNombre,
                burbujaValeria,
                btnIniciar, btnVolver
        );

        return contenido;
    }

    private HBox construirAvatares() {
        HBox box = new HBox(24);
        box.setAlignment(Pos.CENTER);

        // Avatar Valeria
        VBox valeriaBox = construirAvatar(
                "/images/valeria_avatar.png",
                "Valeria",
                "#a080ff"
        );

        // Separador visual
        Label vs = new Label("×");
        vs.setStyle(
                "-fx-font-size: 29px; -fx-text-fill: #2a2a3c; -fx-font-weight: 300;"
        );
        vs.setTranslateY(8);

        // Avatar Alex
        VBox alexBox = construirAvatar(
                "/images/detective_avatar.png",
                "Det. Alex",
                "#00d4ff"
        );

        box.getChildren().addAll(valeriaBox, vs, alexBox);
        return box;
    }

    private VBox construirAvatar(String rutaImagen, String nombre, String color) {
        StackPane avatarStack = new StackPane();
        avatarStack.setPrefSize(80, 80);

        Circle circulo = new Circle(40);
        circulo.setFill(Color.web(color + "10"));
        circulo.setStroke(Color.web(color + "30"));
        circulo.setStrokeWidth(1.5);

        try {
            ImageView img = new ImageView(
                    new Image(getClass().getResourceAsStream(rutaImagen))
            );
            img.setFitWidth(72);
            img.setFitHeight(72);
            img.setPreserveRatio(true);
            img.setStyle("-fx-background-radius: 40;");
            avatarStack.getChildren().addAll(circulo, img);
        } catch (Exception e) {
            Label inicial = new Label(nombre.substring(0, 1));
            inicial.setStyle(
                    "-fx-font-size: 34px; -fx-font-weight: 600; " +
                            "-fx-text-fill: " + color + ";"
            );
            avatarStack.getChildren().addAll(circulo, inicial);
        }

        Label nombreLabel = new Label(nombre);
        nombreLabel.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 500; " +
                        "-fx-text-fill: " + color + "80;"
        );

        VBox box = new VBox(8, avatarStack, nombreLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox construirBurbujaValeria() {
        VBox burbuja = new VBox(6);
        burbuja.getStyleClass().add("burbuja-valeria");
        burbuja.setMaxWidth(440);

        Label quien = new Label("VALERIA DICE");
        quien.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #8080ff; -fx-letter-spacing: 0.1em;"
        );

        Label texto = new Label(
                "\"Necesito ayuda. Los mensajes no paran y no sé quién está " +
                        "detrás de todo esto. Por favor, encuentra las evidencias " +
                        "y descubre la verdad antes de que sea demasiado tarde.\""
        );
        texto.getStyleClass().add("texto-valeria");
        texto.setWrapText(true);

        burbuja.getChildren().addAll(quien, texto);
        return burbuja;
    }

    private void iniciarJuego(String nombreJugador) {
        JuegoController controller = new JuegoController(nombreJugador);
        // Elegir sospechoso al azar para modo solitario
        int randomIdx = (int)(Math.random() * 4);
        controller.setSospechoso(randomIdx);
        
        MapaInvestigacion mapa = new MapaInvestigacion(stage, controller);
        mapa.mostrar();
    }

    private void animarEntrada(VBox contenido) {
        contenido.setOpacity(0);
        contenido.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(600), contenido);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(Duration.millis(600), contenido);
        move.setToY(0);

        fade.play();
        move.play();
    }
}