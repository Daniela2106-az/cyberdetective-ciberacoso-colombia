package cyberdetective.view;

import cyberdetective.controller.JuegoController;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MapaInvestigacion implements JuegoController.JuegoListener {

        private Stage stage;
        private JuegoController controller;
        private Label labelEsperando;
        private int nivelPendiente = -1;

        private static final double ANCHO = 1280;
        private static final double ALTO = 800;

        private static final double[][] POSICIONES = {
                        { 200, 480 },
                        { 420, 300 },
                        { 660, 420 },
                        { 880, 260 },
                        { 1080, 400 }
        };

        private static final String[] TITULOS_NIVEL = {
                        "Red Social", "Foro de Rumores",
                        "Cuenta Fantasma", "Ataque Coordinado", "La Verdad"
        };

        private static final String[] ICONOS = {
                        "/images/nivel1_icono.png", "/images/nivel2_icono.png",
                        "/images/nivel3_icono2.png", "/images/nivel4_icono.png",
                        "/images/nivelfinal_icono.png"
        };

        private static final String[] DESCRIPCIONES = {
                        "Mensajes ofensivos a Valeria",
                        "Rumores falsos se vuelven virales",
                        "Perfil falso con su identidad",
                        "Múltiples cuentas la atacan",
                        "Revelar la verdad completa"
        };

        public MapaInvestigacion(Stage stage, JuegoController controller) {
                this.stage = stage;
                this.controller = controller;
        }

        public void mostrar() {
                StackPane raiz = new StackPane();
                // Eliminamos setPrefSize para evitar que la ventana se reduzca

                // Fondo
                raiz.getChildren().add(construirFondo());

                // Overlay oscuro
                Region overlay = new Region();
                overlay.setStyle("-fx-background-color: rgba(8,8,14,0.58);");
                overlay.setMouseTransparent(true);
                raiz.getChildren().add(overlay);

                // Pane del mapa
                Pane mapaPane = new Pane();
                mapaPane.setStyle("-fx-background-color: transparent;");

                int nivelDesbloqueado = controller.isJuegoTerminado()
                                ? 5
                                : controller.getNivelActual();

                dibujarLineas(mapaPane, nivelDesbloqueado);
                dibujarPuntos(mapaPane, nivelDesbloqueado);

                raiz.getChildren().add(mapaPane);

                // Etiqueta de espera
                labelEsperando = new Label("ESPERANDO AL OTRO DETECTIVE...");
                labelEsperando.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #00d4ff; " +
                        "-fx-padding: 20 40; -fx-font-weight: bold; -fx-border-color: #00d4ff; -fx-border-radius: 10; -fx-background-radius: 10;");
                labelEsperando.setVisible(false);
                StackPane.setAlignment(labelEsperando, Pos.CENTER);
                raiz.getChildren().add(labelEsperando);
                
                controller.removerListener(this);
                controller.agregarListener(this);

                // UI flotante encima — mouse transparent para no bloquear clics del mapa
                HBox barraTop = construirBarraTop();
                StackPane.setAlignment(barraTop, Pos.TOP_LEFT);
                raiz.getChildren().add(barraTop);

                // Reutilizar la escena si ya existe
                Scene scene = stage.getScene();
                if (scene == null) {
                        scene = new Scene(raiz);
                        scene.getStylesheets().add(
                                        getClass().getResource("/styles.css").toExternalForm());
                        scene.setFill(Color.web("#08080e"));
                        stage.setScene(scene);
                } else {
                        scene.setRoot(raiz);
                }
                
                // Forzar pantalla completa con un pequeño retraso para asegurar que JavaFX ya procesó el cambio
                javafx.application.Platform.runLater(() -> {
                    stage.setFullScreen(true);
                });

                mapaPane.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(700), mapaPane);
                ft.setToValue(1);
                ft.play();
        }

        private ImageView construirFondo() {
                try {
                        Image img = new Image(
                                        getClass().getResourceAsStream("/images/mapa_fondo.png"));
                        ImageView iv = new ImageView(img);
                        // Hacemos que el fondo se adapte dinámicamente al tamaño de la ventana
                        iv.fitWidthProperty().bind(stage.widthProperty());
                        iv.fitHeightProperty().bind(stage.heightProperty());
                        iv.setPreserveRatio(false);
                        iv.setMouseTransparent(true);
                        return iv;
                } catch (Exception e) {
                        ImageView iv = new ImageView();
                        iv.fitWidthProperty().bind(stage.widthProperty());
                        iv.fitHeightProperty().bind(stage.heightProperty());
                        iv.setMouseTransparent(true);
                        return iv;
                }
        }

        private void dibujarLineas(Pane pane, int nivelDesbloqueado) {
                for (int i = 0; i < POSICIONES.length - 1; i++) {
                        Line linea = new Line(
                                        POSICIONES[i][0], POSICIONES[i][1],
                                        POSICIONES[i + 1][0], POSICIONES[i + 1][1]);
                        boolean activa = (i + 1) < nivelDesbloqueado;
                        linea.setStrokeWidth(activa ? 2.5 : 1.5);
                        linea.setStroke(activa
                                        ? Color.web("#00d4ff50")
                                        : Color.web("#ffffff15"));
                        if (!activa)
                                linea.getStrokeDashArray().addAll(10.0, 7.0);
                        linea.setMouseTransparent(true);
                        pane.getChildren().add(linea);
                }
        }

        private void dibujarPuntos(Pane pane, int nivelDesbloqueado) {
                for (int i = 0; i < POSICIONES.length; i++) {
                        int nivelNum = i + 1;
                        double cx = POSICIONES[i][0];
                        double cy = POSICIONES[i][1];
                        boolean completado = nivelNum < nivelDesbloqueado;
                        boolean disponible = nivelNum == nivelDesbloqueado;
                        boolean bloqueado = nivelNum > nivelDesbloqueado;

                        // Halo pulsante — mouse transparent para no bloquear
                        if (disponible) {
                                Circle halo = new Circle(cx, cy, 60);
                                halo.setFill(Color.TRANSPARENT);
                                halo.setStroke(Color.web("#00d4ff"));
                                halo.setStrokeWidth(1.5);
                                halo.setOpacity(0);
                                halo.setMouseTransparent(true);

                                ScaleTransition sc = new ScaleTransition(
                                                Duration.millis(1600), halo);
                                sc.setFromX(0.9);
                                sc.setFromY(0.9);
                                sc.setToX(1.7);
                                sc.setToY(1.7);
                                sc.setCycleCount(Animation.INDEFINITE);

                                FadeTransition ft = new FadeTransition(
                                                Duration.millis(1600), halo);
                                ft.setFromValue(0.6);
                                ft.setToValue(0.0);
                                ft.setCycleCount(Animation.INDEFINITE);

                                sc.play();
                                ft.play();
                                pane.getChildren().add(halo);
                        }

                        // Círculo de fondo
                        Circle fondoCirc = new Circle(cx, cy, 50);
                        fondoCirc.setFill(bloqueado
                                        ? Color.web("#10101a")
                                        : Color.web("#14141f"));
                        fondoCirc.setStroke(
                                        completado ? Color.web("#00ff8870")
                                                        : disponible ? Color.web("#00d4ff90") : Color.web("#2a2a3c"));
                        fondoCirc.setStrokeWidth(2);
                        if (disponible) {
                                fondoCirc.setEffect(new javafx.scene.effect.DropShadow(
                                                30, Color.web("#00d4ff60")));
                        }
                        fondoCirc.setMouseTransparent(true);
                        pane.getChildren().add(fondoCirc);

                        // Ícono PNG
                        try {
                                Image img = new Image(
                                                getClass().getResourceAsStream(ICONOS[i]));
                                ImageView imgView = new ImageView(img);
                                imgView.setFitWidth(90);
                                imgView.setFitHeight(90);
                                imgView.setPreserveRatio(true);
                                imgView.setX(cx - 45);
                                imgView.setY(cy - 45);
                                if (bloqueado)
                                        imgView.setOpacity(0.2);
                                imgView.setMouseTransparent(true);
                                pane.getChildren().add(imgView);
                        } catch (Exception e) {
                                Label fallback = new Label(String.valueOf(nivelNum));
                                fallback.setStyle(
                                                "-fx-font-size: 29px; -fx-font-weight: 700;" +
                                                                "-fx-text-fill: " + (bloqueado ? "#3a3a5c" : "#00d4ff")
                                                                + ";");
                                fallback.setLayoutX(cx - 12);
                                fallback.setLayoutY(cy - 16);
                                fallback.setMouseTransparent(true);
                                pane.getChildren().add(fallback);
                        }

                        // Check de completado
                        if (completado) {
                                Circle checkBg = new Circle(cx + 34, cy - 34, 13);
                                checkBg.setFill(Color.web("#00ff88"));
                                checkBg.setStroke(Color.web("#0a0a0f"));
                                checkBg.setStrokeWidth(2);
                                checkBg.setMouseTransparent(true);

                                Label checkLbl = new Label("✓");
                                checkLbl.setStyle(
                                                "-fx-font-size: 19px; -fx-font-weight: 700;" +
                                                                "-fx-text-fill: #0a0a0f;");
                                checkLbl.setLayoutX(cx + 34 - 7);
                                checkLbl.setLayoutY(cy - 34 - 9);
                                checkLbl.setMouseTransparent(true);
                                pane.getChildren().addAll(checkBg, checkLbl);
                        }

                        // Etiqueta título
                        Label titulo = new Label(TITULOS_NIVEL[i]);
                        titulo.setStyle(
                                        "-fx-font-size: 17px; -fx-font-weight: 700;" +
                                                        "-fx-text-fill: "
                                                        + (bloqueado ? "#4a4a6a" : completado ? "#00ff88cc" : "#ffffff")
                                                        + ";" +
                                                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.95),8,0,0,2);");
                        titulo.setMaxWidth(140);
                        titulo.setWrapText(true);
                        titulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                        titulo.setLayoutX(cx - 70);
                        titulo.setLayoutY(cy + 56);
                        titulo.setMouseTransparent(true);
                        pane.getChildren().add(titulo);

                        // Etiqueta descripción
                        Label desc = new Label(DESCRIPCIONES[i]);
                        desc.setStyle(
                                        "-fx-font-size: 19px;" +
                                                        "-fx-text-fill: " + (bloqueado ? "#3a3a5c" : "#a0a0c0") + ";" +
                                                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.95),6,0,0,2);");
                        desc.setMaxWidth(140);
                        desc.setWrapText(true);
                        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                        desc.setLayoutX(cx - 70);
                        desc.setLayoutY(cy + 76);
                        desc.setMouseTransparent(true);
                        pane.getChildren().add(desc);

                        // Área de clic — Rectangle transparente encima de todo
                        if (disponible) {
                                Rectangle areaClick = new Rectangle(
                                                cx - 54, cy - 54, 108, 108);
                                areaClick.setFill(Color.web("#ffffff", 0.001));
                                areaClick.setOnMouseEntered(e -> {
                                        fondoCirc.setScaleX(1.12);
                                        fondoCirc.setScaleY(1.12);
                                });
                                areaClick.setOnMouseExited(e -> {
                                        fondoCirc.setScaleX(1.0);
                                        fondoCirc.setScaleY(1.0);
                                });
                                areaClick.setOnMouseClicked(e -> {
                                        e.consume();
                                        abrirNivel(nivelNum);
                                });
                                // El área de clic va de ÚLTIMO para estar encima
                                pane.getChildren().add(areaClick);
                        }
                }
        }

        // UI flotante — info del jugador arriba, botón menú arriba derecha
        private HBox construirBarraTop() {
                HBox barra = new HBox();
                barra.setPadding(new Insets(24, 28, 0, 32));
                barra.setAlignment(Pos.CENTER_LEFT);
                barra.setStyle("-fx-background-color: transparent;");
                barra.setMaxHeight(Region.USE_PREF_SIZE);

                // Info jugador — solo visual, no bloquea clics
                VBox infoJugador = new VBox(2);
                infoJugador.setMouseTransparent(true);

                Label appLabel = new Label("CYBERDETECTIVE");
                appLabel.setStyle(
                                "-fx-font-size: 19px; -fx-font-weight: 700;" +
                                                "-fx-text-fill: #00d4ff; -fx-letter-spacing: 0.15em;" +
                                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.9),8,0,0,2);");

                Label nombreLabel = new Label("Det. " + controller.getNombreJugador());
                nombreLabel.setStyle(
                                "-fx-font-size: 29px; -fx-font-weight: 700; -fx-text-fill: #ffffff;" +
                                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.95),10,0,0,3);");

                Label puntajeLabel = new Label(
                                "Puntaje: " + controller.getPuntaje() + " pts");
                puntajeLabel.setStyle(
                                "-fx-font-size: 19px; -fx-text-fill: #00d4ff;" +
                                                "-fx-font-family: 'DM Mono';" +
                                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.9),8,0,0,2);");

                Label instruccion = new Label(
                                controller.isJuegoTerminado()
                                                ? "✓  Investigación completada"
                                                : "Selecciona el punto activo para continuar");
                instruccion.setStyle(
                                "-fx-font-size: 17px; -fx-text-fill: #808090;" +
                                                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.9),8,0,0,2);");

                // Barra de progreso global
                int nivelActual = controller.isJuegoTerminado() ? 5 : controller.getNivelActual();
                ProgressBar barraGlobal = new ProgressBar((double) nivelActual / 5.0);
                barraGlobal.setPrefWidth(220);
                barraGlobal.setPrefHeight(6);
                barraGlobal.setStyle("-fx-accent:#00ff88;-fx-background-color:#1a1a28;");
                VBox.setMargin(barraGlobal, new Insets(8, 0, 4, 0));

                Label tProg = new Label("Investigación: " + (nivelActual * 20) + "% completada");
                tProg.setStyle("-fx-font-size:10px; -fx-text-fill:#5a5a7a; -fx-font-family:'DM Mono';");

                infoJugador.getChildren().addAll(
                                appLabel, nombreLabel, puntajeLabel, barraGlobal, tProg, instruccion);

                // Spacer transparente
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                spacer.setMouseTransparent(true);

                // Botón menú
                Button btnMenu = new Button("← Menú");
                btnMenu.setStyle(
                                "-fx-background-color: rgba(12,12,22,0.80);" +
                                                "-fx-border-color: #2a2a3c;" +
                                                "-fx-border-width: 1;" +
                                                "-fx-border-radius: 8;" +
                                                "-fx-background-radius: 8;" +
                                                "-fx-text-fill: #9090b0;" +
                                                "-fx-font-size: 19px;" +
                                                "-fx-padding: 10 20 10 20;" +
                                                "-fx-cursor: hand;");
                btnMenu.setOnAction(e -> new MenuPrincipal(stage).mostrar());

                barra.getChildren().addAll(infoJugador, spacer, btnMenu);

                // La barra solo ocupa su altura real — no bloquea el resto de la pantalla
                barra.setPickOnBounds(false);

                return barra;
        }

        private void abrirNivel(int nivel) {
                if (controller instanceof cyberdetective.controller.MultiplayerJuegoController multi) {
                        labelEsperando.setVisible(true);
                        nivelPendiente = nivel;
                        multi.notificarListoParaNivel(nivel);
                } else {
                        AccionesNivel acciones = new AccionesNivel(stage, controller, this);
                        acciones.mostrar(nivel);
                }
        }

        @Override
        public void onNivelIniciado() {
                javafx.application.Platform.runLater(() -> {
                        labelEsperando.setVisible(false);
                        if (nivelPendiente != -1) {
                                controller.removerListener(this); // Evitamos duplicados al volver
                                AccionesNivel acciones = new AccionesNivel(stage, controller, this);
                                acciones.mostrar(nivelPendiente);
                                nivelPendiente = -1;
                        }
                });
        }

        // Métodos obligatorios de la interfaz JuegoListener (vacíos ya que no se usan en el mapa)
        @Override public void onNivelCompletado(int nivel, cyberdetective.model.Caso caso) {}
        @Override public void onArbolActualizado() {}
        @Override public void onJuegoTerminado(String reporte) {}
        @Override public void onPuntajeActualizado(int nuevoPuntaje) {}
        @Override public void onMensajeDetective(String mensaje) {}
        @Override public void onFaseCambiada(cyberdetective.controller.JuegoController.FaseNivel nuevaFase) {}
        @Override public void onEvidenciaRevelada(String evidencia, int totalReveladas, int total) {}

        public void regresarAlMapa() {
                mostrar();
        }
}