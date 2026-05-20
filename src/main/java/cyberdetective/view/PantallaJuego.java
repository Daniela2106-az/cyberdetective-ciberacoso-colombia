package cyberdetective.view;

import cyberdetective.controller.JuegoController;
import cyberdetective.controller.JuegoController.FaseNivel;
import cyberdetective.model.Caso;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Pantalla principal del juego.
 * Maneja tres vistas: Investigación, Árbol AVL y Nivel Final.
 * El flujo por nivel es: recolectar evidencias → interrogatorio
 * → tarjeta del nodo → árbol actualizado → continuar.
 */
public class PantallaJuego implements JuegoController.JuegoListener {

    private Stage stage;
    private JuegoController controller;

    // ── Panel lateral ──────────────────────────────
    private Label labelPuntaje;
    private Label labelPuntajeOponente;
    private Label labelNivelBadge;
    private Label labelMensajeDetective;
    private TextArea logArea;
    private Button btnVerArbol;
    private Button btnVerInvestigacion;

    // ── Panel central ──────────────────────────────
    private StackPane contenedorCentral;
    private VBox vistaInvestigacion;
    private VBox vistaArbol;
    private VBox vistaNivelFinal;

    private Label labelTituloNivel;
    private Label labelDescripcionNivel;
    private VBox panelFase;
    private Label labelFaseActual;
    private ProgressBar barraEvidencias;
    private Label labelProgresoEvidencias;
    private Button btnAccionPrincipal;

    private VisualizadorArbol visualizadorArbol;

    // Vista activa
    private enum Vista { INVESTIGACION, ARBOL, NIVEL_FINAL }
    private Vista vistaActiva = Vista.INVESTIGACION;

    private MapaInvestigacion mapa;
    private VBox opcionesBox;

    public PantallaJuego(Stage stage, JuegoController controller) {
        this.stage = stage;
        this.controller = controller;
        this.controller.agregarListener(this);
        this.mapa = null;
    }

    public PantallaJuego(Stage stage, JuegoController controller,
                         MapaInvestigacion mapa) {
        this.stage = stage;
        this.controller = controller;
        this.controller.agregarListener(this);
        this.mapa = mapa;
    }

    public void mostrar() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0a0f;");

        root.setLeft(construirPanelLateral());
        root.setCenter(construirContenedorCentral());

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );
        scene.setFill(Color.web("#0a0a0f"));
        stage.setScene(scene);

        controller.iniciarInvestigacion();
    }

    // ══════════════════════════════════════════════
    //  PANEL LATERAL
    // ══════════════════════════════════════════════

    private ScrollPane construirPanelLateral() {
        VBox panel = new VBox();
        panel.getStyleClass().add("panel-lateral");
        panel.setMinWidth(340);
        panel.setMaxWidth(340);
        panel.setPrefWidth(340);
        panel.setSpacing(0);

        // Cabecera
        Label appLabel = new Label("CYBERDETECTIVE");
        appLabel.getStyleClass().add("titulo-app");

        Circle avatar = new Circle(24);
        avatar.setFill(Color.web("#00d4ff15"));
        avatar.setStroke(Color.web("#00d4ff40"));
        avatar.setStrokeWidth(1);
        Label avatarLetra = new Label("A");
        avatarLetra.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #00d4ff;"
        );
        StackPane avatarBox = new StackPane(avatar, avatarLetra);
        avatarBox.setPrefSize(48, 48);
        VBox.setMargin(avatarBox, new Insets(20, 0, 8, 0));

        Label alexLabel = new Label("Det. Alex");
        alexLabel.setStyle(
                "-fx-font-size: 19px; -fx-font-weight: 600; -fx-text-fill: #f0f0f8;"
        );
        Label rolLabel = new Label("Especialista en crímenes digitales");
        rolLabel.setStyle("-fx-font-size: 19px; -fx-text-fill: #3a3a5c;");
        VBox.setMargin(rolLabel, new Insets(2, 0, 0, 0));

        panel.getChildren().addAll(appLabel, avatarBox, alexLabel, rolLabel);
        panel.getChildren().add(separador(20));

        // Puntaje
        Label puntajeEtq = new Label("PUNTAJES");
        puntajeEtq.getStyleClass().add("etiqueta-seccion");
        
        HBox scoreBox = new HBox(12);
        scoreBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox myScore = new VBox(2);
        Label myLabel = new Label("TÚ");
        myLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #00d4ff; -fx-font-weight: bold;");
        labelPuntaje = new Label("0");
        labelPuntaje.getStyleClass().add("puntaje-label");
        labelPuntaje.setStyle("-fx-font-size: 29px;");
        myScore.getChildren().addAll(myLabel, labelPuntaje);
        
        VBox opponentScore = new VBox(2);
        Label opLabel = new Label("OPONENTE");
        opLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #ff9f1c; -fx-font-weight: bold;");
        labelPuntajeOponente = new Label("0");
        labelPuntajeOponente.getStyleClass().add("puntaje-label");
        labelPuntajeOponente.setStyle("-fx-font-size: 29px; -fx-text-fill: #ff9f1c;");
        opponentScore.getChildren().addAll(opLabel, labelPuntajeOponente);
        
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setStyle("-fx-background-color: #2a2a3c;");
        
        scoreBox.getChildren().addAll(myScore, sep, opponentScore);
        
        VBox.setMargin(scoreBox, new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(puntajeEtq, scoreBox);
        panel.getChildren().add(separador(20));

        // Nivel activo
        Label nivelEtq = new Label("INVESTIGACIÓN ACTIVA");
        nivelEtq.getStyleClass().add("etiqueta-seccion");
        labelNivelBadge = new Label();
        labelNivelBadge.getStyleClass().add("nivel-badge");
        labelNivelBadge.setWrapText(true);
        VBox.setMargin(labelNivelBadge, new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(nivelEtq, labelNivelBadge);
        panel.getChildren().add(separador(20));

        // Navegación entre vistas
        Label navEtq = new Label("VISTAS");
        navEtq.getStyleClass().add("etiqueta-seccion");

        btnVerInvestigacion = new Button("📋  Investigación");
        btnVerInvestigacion.setMaxWidth(Double.MAX_VALUE);
        btnVerInvestigacion.setStyle(estiloNavActivo());
        btnVerInvestigacion.setOnAction(e -> cambiarVista(Vista.INVESTIGACION));

        btnVerArbol = new Button("🌳  Árbol AVL");
        btnVerArbol.setMaxWidth(Double.MAX_VALUE);
        btnVerArbol.setStyle(estiloNavInactivo());
        btnVerArbol.setOnAction(e -> cambiarVista(Vista.ARBOL));

        VBox.setMargin(btnVerInvestigacion, new Insets(8, 0, 6, 0));
        panel.getChildren().addAll(navEtq, btnVerInvestigacion, btnVerArbol);
        panel.getChildren().add(separador(20));

        // Alex dice
        Label alexEtq = new Label("ALEX DICE");
        alexEtq.getStyleClass().add("etiqueta-seccion");

        labelMensajeDetective = new Label(
                "Analiza cada evidencia con cuidado. El árbol guarda la verdad."
        );
        labelMensajeDetective.setWrapText(true);
        labelMensajeDetective.getStyleClass().add("mensaje-detective");
        labelMensajeDetective.setPadding(new Insets(12, 14, 12, 14));

        VBox cajaMensaje = new VBox(labelMensajeDetective);
        cajaMensaje.setStyle(
                "-fx-background-color: #00d4ff08;" +
                        "-fx-border-color: #00d4ff20;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );
        VBox.setMargin(cajaMensaje, new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(alexEtq, cajaMensaje);
        panel.getChildren().add(separador(20));

        // Registro de eventos
        Label logEtq = new Label("REGISTRO DE EVENTOS");
        logEtq.getStyleClass().add("etiqueta-seccion");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(260);
        logArea.setMinHeight(260);
        logArea.setStyle(
                "-fx-background-color: #0d0d16;" +
                        "-fx-control-inner-background: #0d0d16;" +
                        "-fx-text-fill: #5a5a7a;" +
                        "-fx-font-family: 'DM Mono';" +
                        "-fx-font-size: 19px;" +
                        "-fx-border-color: #1a1a28;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10;"
        );
        VBox.setMargin(logArea, new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(logEtq, logArea);
        panel.getChildren().add(separador(16));

        // Botón menú
        Button btnMenu = new Button("← Menú principal");
        btnMenu.getStyleClass().add("btn-secundario");
        btnMenu.setMaxWidth(Double.MAX_VALUE);
        btnMenu.setOnAction(e -> new MenuPrincipal(stage).mostrar());
        panel.getChildren().add(btnMenu);

        // ScrollPane solo vertical
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background-color: #0f0f18;" +
                        "-fx-background: #0f0f18;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 0 1 0 0;"
        );
        scroll.setMinWidth(340);
        scroll.setMaxWidth(340);
        scroll.setPrefWidth(340);
        return scroll;
    }

    // ══════════════════════════════════════════════
    //  CONTENEDOR CENTRAL (VISTAS)
    // ══════════════════════════════════════════════

    private StackPane construirContenedorCentral() {
        contenedorCentral = new StackPane();
        contenedorCentral.setStyle("-fx-background-color: #0a0a0f;");

        vistaInvestigacion = construirVistaInvestigacion();
        vistaArbol         = construirVistaArbol();
        vistaNivelFinal    = new VBox();

        contenedorCentral.getChildren().addAll(
                vistaArbol, vistaNivelFinal, vistaInvestigacion
        );

        return contenedorCentral;
    }

    private void cambiarVista(Vista vista) {
        vistaActiva = vista;

        vistaInvestigacion.setVisible(vista == Vista.INVESTIGACION);
        vistaArbol.setVisible(vista == Vista.ARBOL);
        vistaNivelFinal.setVisible(vista == Vista.NIVEL_FINAL);

        btnVerInvestigacion.setStyle(
                vista == Vista.INVESTIGACION ? estiloNavActivo() : estiloNavInactivo()
        );
        btnVerArbol.setStyle(
                vista == Vista.ARBOL ? estiloNavActivo() : estiloNavInactivo()
        );

        if (vista == Vista.ARBOL) {
            visualizadorArbol.actualizar(controller.getArbol());
        }
    }

    // ── Vista: Investigación ───────────────────────

    private VBox construirVistaInvestigacion() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-central");

        labelTituloNivel = new Label();
        labelTituloNivel.getStyleClass().add("titulo-nivel");

        labelDescripcionNivel = new Label();
        labelDescripcionNivel.getStyleClass().add("subtitulo");
        labelDescripcionNivel.setWrapText(true);
        VBox.setMargin(labelDescripcionNivel, new Insets(6, 0, 0, 0));

        Label progresoEtq = new Label("EVIDENCIAS");
        progresoEtq.getStyleClass().add("etiqueta-seccion");
        VBox.setMargin(progresoEtq, new Insets(24, 0, 8, 0));

        labelProgresoEvidencias = new Label("0 / 0 recolectadas");
        labelProgresoEvidencias.setStyle(
                "-fx-font-size: 19px; -fx-text-fill: #3a3a5c;"
        );

        barraEvidencias = new ProgressBar(0);
        barraEvidencias.getStyleClass().add("progress-bar");
        barraEvidencias.setMaxWidth(Double.MAX_VALUE);
        barraEvidencias.setPrefHeight(4);

        HBox progresoFila = new HBox(12, barraEvidencias, labelProgresoEvidencias);
        progresoFila.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(barraEvidencias, Priority.ALWAYS);
        VBox.setMargin(progresoFila, new Insets(6, 0, 0, 0));

        labelFaseActual = new Label("FASE: INVESTIGANDO");
        labelFaseActual.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #00d4ff; -fx-letter-spacing: 0.1em;"
        );
        VBox.setMargin(labelFaseActual, new Insets(20, 0, 12, 0));

        panelFase = new VBox(12);
        VBox.setVgrow(panelFase, Priority.ALWAYS);

        btnAccionPrincipal = new Button("Examinar siguiente evidencia");
        btnAccionPrincipal.getStyleClass().add("btn-primario");
        btnAccionPrincipal.setOnAction(e -> manejarAccionPrincipal());

        HBox btnBox = new HBox(btnAccionPrincipal);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(btnBox, new Insets(20, 0, 0, 0));

        ScrollPane scroll = new ScrollPane();
        scroll.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent;"
        );
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);

        VBox contenido = new VBox(0,
                labelTituloNivel, labelDescripcionNivel,
                progresoEtq, progresoFila,
                labelFaseActual, panelFase, btnBox
        );
        scroll.setContent(contenido);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        panel.getChildren().add(scroll);
        return panel;
    }

    // ── Vista: Árbol AVL ───────────────────────────

    private VBox construirVistaArbol() {
        VBox panel = new VBox(16);
        panel.setStyle(
                "-fx-background-color: #0a0a0f; -fx-padding: 40 48 40 48;"
        );
        panel.setVisible(false);

        Label titulo = new Label("Árbol AVL de Investigación");
        titulo.getStyleClass().add("titulo-nivel");

        Label subtitulo = new Label(
                "Cada nodo representa un caso. Haz clic sobre un nodo " +
                        "para ver la información completa del incidente."
        );
        subtitulo.getStyleClass().add("subtitulo");
        subtitulo.setWrapText(true);

        Label instruccion = new Label(
                "Los nodos se ordenan por gravedad del delito (1 = leve → 10 = grave). " +
                        "El árbol se auto-balancea con rotaciones AVL al insertar cada caso."
        );
        instruccion.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #2a2a4a; -fx-wrap-text: true;"
        );
        instruccion.setWrapText(true);

        visualizadorArbol = new VisualizadorArbol();

        // Contenedor del canvas con fondo y borde
        VBox canvasBox = new VBox(visualizadorArbol.getCanvas());
        canvasBox.setStyle(
                "-fx-background-color: #0d0d16;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;"
        );

        panel.getChildren().addAll(titulo, subtitulo, instruccion, canvasBox);
        VBox.setVgrow(canvasBox, Priority.ALWAYS);
        return panel;
    }

    // ══════════════════════════════════════════════
    //  LÓGICA DE FASES
    // ══════════════════════════════════════════════

    private void manejarAccionPrincipal() {
        if (controller.getFaseActual() == FaseNivel.INVESTIGANDO) {
            if (controller.todasLasEvidenciasReveladas()) {
                if (controller.pasarAInterrogatorio()) {
                    renderizarFase(FaseNivel.INTERROGATORIO);
                }
            } else {
                controller.revelarSiguienteEvidencia();
                refrescarListaEvidencias();
                actualizarBarraEvidencias();
                if (controller.todasLasEvidenciasReveladas()) {
                    btnAccionPrincipal.setText("Iniciar interrogatorio →");
                }
            }
        }
    }

    private void renderizarFase(FaseNivel fase) {
        panelFase.getChildren().clear();
        labelNivelBadge.setText(controller.getTituloNivelActual());
        labelTituloNivel.setText(controller.getTituloNivelActual());
        labelDescripcionNivel.setText(controller.getDescripcionNivelActual());

        switch (fase) {
            case INVESTIGANDO   -> renderizarInvestigando();
            case INTERROGATORIO -> renderizarInterrogatorio();
            case CASO_CERRADO   -> {}
        }

        animarEntrada(panelFase);
    }

    // ── Fase 1: Investigando ───────────────────────

    private void renderizarInvestigando() {
        labelFaseActual.setText("FASE 1 – RECOLECCIÓN DE EVIDENCIAS");
        btnAccionPrincipal.setText("Examinar siguiente evidencia");
        btnAccionPrincipal.setDisable(false);
        btnAccionPrincipal.setOnAction(e -> manejarAccionPrincipal());

        Label instruccion = new Label(
                "Examina cada evidencia antes de pasar al interrogatorio. " +
                        "Solo podrás responder las preguntas cuando las hayas visto todas."
        );
        instruccion.setWrapText(true);
        instruccion.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #9090b0; -fx-line-spacing: 4;" +
                        "-fx-wrap-text: true;"
        );

        VBox listaEvidencias = new VBox(8);
        listaEvidencias.setId("listaEvidencias");

        for (int i = 0; i < controller.getEvidenciasReveladas().size(); i++) {
            listaEvidencias.getChildren().add(
                    tarjetaEvidencia(controller.getEvidenciasReveladas().get(i), i + 1)
            );
        }

        panelFase.getChildren().addAll(instruccion, listaEvidencias);
        actualizarBarraEvidencias();

        if (controller.todasLasEvidenciasReveladas()) {
            btnAccionPrincipal.setText("Iniciar interrogatorio →");
            btnAccionPrincipal.setOnAction(e -> {
                if (controller.pasarAInterrogatorio()) {
                    renderizarFase(FaseNivel.INTERROGATORIO);
                }
            });
        }
    }

    private void refrescarListaEvidencias() {
        for (Node n : panelFase.getChildren()) {
            if (n instanceof VBox v && "listaEvidencias".equals(v.getId())) {
                List<String> reveladas = controller.getEvidenciasReveladas();
                int idx = reveladas.size() - 1;
                if (idx >= 0) {
                    VBox tarjeta = tarjetaEvidencia(reveladas.get(idx), idx + 1);
                    tarjeta.setOpacity(0);
                    v.getChildren().add(tarjeta);
                    FadeTransition ft = new FadeTransition(Duration.millis(400), tarjeta);
                    ft.setToValue(1);
                    ft.play();
                }
                break;
            }
        }
    }

    private VBox tarjetaEvidencia(String evidencia, int numero) {
        Label numLabel = new Label(String.format("%02d", numero));
        numLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 19px;" +
                        "-fx-font-weight: 500; -fx-text-fill: #00d4ff; -fx-min-width: 24px;"
        );
        Label evLabel = new Label(evidencia);
        evLabel.setWrapText(true);
        evLabel.setStyle(
                "-fx-font-size: 19px; -fx-text-fill: #c0c0d8; -fx-line-spacing: 2;"
        );
        HBox.setHgrow(evLabel, Priority.ALWAYS);

        HBox fila = new HBox(14, numLabel, evLabel);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(fila);
        tarjeta.setStyle(
                "-fx-background-color: #0f0f1a;" +
                        "-fx-border-color: #00d4ff22;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 16 12 16;"
        );
        return tarjeta;
    }

    private void actualizarBarraEvidencias() {
        int total    = controller.getTotalEvidencias();
        int reveladas = controller.getEvidenciasReveladas().size();
        if (total > 0) barraEvidencias.setProgress((double) reveladas / total);
        labelProgresoEvidencias.setText(reveladas + " / " + total + " recolectadas");
    }

    // ── Fase 2: Interrogatorio ─────────────────────

    private void renderizarInterrogatorio() {
        labelFaseActual.setText("FASE 2 – INTERROGATORIO");
        btnAccionPrincipal.setDisable(true);
        btnAccionPrincipal.setText("Responde las preguntas primero");

        Label resumen = new Label(
                "Has recolectado " + controller.getTotalEvidencias() +
                        " evidencias. Demuestra que identificaste el delito correctamente."
        );
        resumen.setWrapText(true);
        resumen.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #9090b0; -fx-line-spacing: 4;" +
                        "-fx-wrap-text: true;"
        );
        panelFase.getChildren().add(resumen);
        cargarPregunta();
    }

    private void cargarPregunta() {
        if (panelFase.getChildren().size() > 1) {
            panelFase.getChildren().remove(1, panelFase.getChildren().size());
        }

        if (!controller.hayMasPreguntas()) {
            mostrarVeredictoYCierre();
            return;
        }

        String[][] preguntas  = controller.getPreguntasNivelActual();
        int idx               = controller.getPreguntaActual();
        String[] pregunta     = preguntas[idx];
        int totalOpciones     = pregunta.length - 2;

        Label numPregunta = new Label(
                "PREGUNTA " + (idx + 1) + " DE " + preguntas.length
        );
        numPregunta.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );
        VBox.setMargin(numPregunta, new Insets(16, 0, 0, 0));

        Label enunciado = new Label(pregunta[0]);
        enunciado.setWrapText(true);
        enunciado.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 500;" +
                        "-fx-text-fill: #e0e0f0; -fx-line-spacing: 4;"
        );
        VBox.setMargin(enunciado, new Insets(8, 0, 12, 0));

        opcionesBox = new VBox(8);
        for (int i = 1; i <= totalOpciones; i++) {
            final int opcionIdx = i - 1;
            Button btn = new Button(pregunta[i]);
            btn.getStyleClass().add("btn-opcion");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setWrapText(true);
            btn.setOnAction(e -> evaluarRespuesta(opcionIdx, opcionesBox, pregunta));
            opcionesBox.getChildren().add(btn);
        }

        panelFase.getChildren().addAll(numPregunta, enunciado, opcionesBox);
        animarEntrada(opcionesBox);
    }

    private void evaluarRespuesta(int opcionElegida, VBox opcionesBox,
                                  String[] pregunta) {
        // En multijugador, el controlador enviará el mensaje y devolverá false.
        // En solitario, devolverá el resultado pero ahora lo manejaremos en onRespuestaRecibida.
        controller.responderPregunta(opcionElegida);
        
        // Bloqueamos todo inmediatamente para evitar clics múltiples
        for (Node n : opcionesBox.getChildren()) {
            if (n instanceof Button btn) {
                btn.setDisable(true);
            }
        }
        
        actualizarLog();
    }

    // ── Veredicto y tarjeta del nodo ──────────────

    /**
     * Muestra el veredicto legal y la tarjeta completa del nodo
     * ANTES de insertarlo en el árbol. El jugador confirma con un botón.
     */
    private void mostrarVeredictoYCierre() {
        Caso caso = controller.getCasoActualFijo();
        if (caso == null) return;

        // Tarjeta completa del nodo
        VBox tarjetaNodo = new VBox(12);
        tarjetaNodo.setStyle(
                "-fx-background-color: #12121c;" +
                        "-fx-border-color: #00d4ff40;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 20 24 20 24;"
        );
        VBox.setMargin(tarjetaNodo, new Insets(16, 0, 0, 0));

        Label nodoEtq = new Label("NODO A INSERTAR EN EL ÁRBOL AVL");
        nodoEtq.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #00d4ff; -fx-letter-spacing: 0.1em;"
        );

        Label idLabel = new Label("Caso #" + caso.getId() + "  ·  " +
                caso.getTipoAcoso());
        idLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #f0f0f8;"
        );
        idLabel.setWrapText(true);

        // Gravedad con barra de color
        Label gravEtq = new Label("GRAVEDAD DEL DELITO");
        gravEtq.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );
        VBox.setMargin(gravEtq, new Insets(8, 0, 4, 0));

        HBox segmentos = new HBox(3);
        for (int i = 1; i <= 10; i++) {
            VBox seg = new VBox();
            seg.setPrefWidth(22);
            seg.setPrefHeight(6);
            String color;
            if (i <= 3)      color = i <= caso.getGravedad() ? "#ffd166" : "#1e1e2e";
            else if (i <= 6) color = i <= caso.getGravedad() ? "#ff9f1c" : "#1e1e2e";
            else if (i <= 8) color = i <= caso.getGravedad() ? "#ff6b6b" : "#1e1e2e";
            else              color = i <= caso.getGravedad() ? "#ff0044" : "#1e1e2e";
            seg.setStyle("-fx-background-color:" + color + "; -fx-background-radius:2;");
            segmentos.getChildren().add(seg);
        }
        Label gravVal = new Label(caso.getGravedad() + "/10");
        gravVal.setStyle(
                "-fx-font-family:'DM Mono'; -fx-font-size:11px; -fx-text-fill:#6b6b8a;"
        );
        HBox gravFila = new HBox(10, segmentos, gravVal);
        gravFila.setAlignment(Pos.CENTER_LEFT);

        // Evidencias
        Label evEtq = new Label("EVIDENCIAS RECOLECTADAS");
        evEtq.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );
        VBox.setMargin(evEtq, new Insets(8, 0, 4, 0));

        VBox evBox = new VBox(4);
        for (String ev : caso.getEvidencias()) {
            Label evLabel = new Label("◆  " + ev);
            evLabel.setWrapText(true);
            evLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #8080a0;");
            evBox.getChildren().add(evLabel);
        }

        // Ley y pena
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #1e1e2e;");
        VBox.setMargin(sep, new Insets(8, 0, 8, 0));

        Label leyEtq = new Label("MARCO LEGAL");
        leyEtq.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );
        Label leyLabel = new Label(caso.getLeyColombia());
        leyLabel.setWrapText(true);
        leyLabel.setStyle(
                "-fx-font-size: 19px; -fx-font-weight: 500; -fx-text-fill: #00d4ff;"
        );
        VBox.setMargin(leyLabel, new Insets(4, 0, 0, 0));

        Label penaLabel = new Label("Pena: " + caso.getPenaAplicable());
        penaLabel.setWrapText(true);
        penaLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #5a5a7a;");
        VBox.setMargin(penaLabel, new Insets(4, 0, 0, 0));

        tarjetaNodo.getChildren().addAll(
                nodoEtq, idLabel,
                gravEtq, gravFila,
                evEtq, evBox,
                sep, leyEtq, leyLabel, penaLabel
        );

        // Botón de inserción
        Button btnInsertar = new Button("Insertar nodo en el árbol AVL →");
        btnInsertar.getStyleClass().add("btn-primario");
        btnInsertar.setOnAction(e -> insertarEnArbolYContinuar());

        HBox btnBox = new HBox(btnInsertar);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(btnBox, new Insets(12, 0, 0, 0));

        panelFase.getChildren().addAll(tarjetaNodo, btnBox);
        animarEntrada(tarjetaNodo);

        btnAccionPrincipal.setDisable(true);
        btnAccionPrincipal.setText("Revisa el nodo y confirma la inserción");
    }

    /**
     * Inserta el caso en el árbol, muestra el árbol actualizado
     * y espera que el jugador confirme antes de continuar.
     */
    private void insertarEnArbolYContinuar() {
        boolean cerrado = controller.cerrarCaso();
        if (!cerrado) return;

        actualizarLog();
        visualizadorArbol.actualizar(controller.getArbol());

        if (controller.isJuegoTerminado()) {
            // Nivel final — mostrar árbol primero, luego botón para el reporte
            mostrarVistaArbolConBotonFinal();
        } else {
            // Mostrar árbol con botón para continuar al siguiente nivel
            mostrarVistaArbolConBotonContinuar();
        }
    }

    private void mostrarVistaArbolConBotonContinuar() {
        cambiarVista(Vista.ARBOL);

        // Agregar botón temporal de continuar en la vista árbol
        Button btnContinuar = new Button("Continuar investigación →");
        btnContinuar.getStyleClass().add("btn-primario");
        btnContinuar.setOnAction(e -> {
            // Quitar el botón temporal
            vistaArbol.getChildren().removeIf(
                    n -> n instanceof HBox hb && hb.getId() != null &&
                            hb.getId().equals("btnContinuarBox")
            );
            if (mapa != null) {
                mapa.regresarAlMapa();
            } else {
                controller.iniciarInvestigacion();
                renderizarFase(FaseNivel.INVESTIGANDO);
                cambiarVista(Vista.INVESTIGACION);
            }
        });

        HBox btnBox = new HBox(btnContinuar);
        btnBox.setId("btnContinuarBox");
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(btnBox, new Insets(16, 0, 0, 0));

        vistaArbol.getChildren().add(btnBox);
    }

    private void mostrarVistaArbolConBotonFinal() {
        cambiarVista(Vista.ARBOL);

        Button btnReporte = new Button("Ver reporte final del caso →");
        btnReporte.getStyleClass().add("btn-primario");
        btnReporte.setOnAction(e -> {
            vistaArbol.getChildren().removeIf(
                    n -> n instanceof HBox hb && hb.getId() != null &&
                            hb.getId().equals("btnReporteBox")
            );
            mostrarNivelFinal();
        });

        HBox btnBox = new HBox(btnReporte);
        btnBox.setId("btnReporteBox");
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(btnBox, new Insets(16, 0, 0, 0));

        vistaArbol.getChildren().add(btnBox);
    }

    // ══════════════════════════════════════════════
    //  NIVEL FINAL
    // ══════════════════════════════════════════════

    private List<Caso> ordenCorrecto;
    private List<Caso> casosDesordenados;
    private int siguienteCorrecto = 0;
    private int intentosFallidos  = 0;

    private void mostrarNivelFinal() {
        vistaNivelFinal.getChildren().clear();
        vistaNivelFinal.setStyle(
                "-fx-background-color: #0a0a0f; -fx-padding: 40 48 40 48;"
        );
        vistaNivelFinal.setSpacing(20);

        // El recorrido inorden da el orden correcto por gravedad
        ordenCorrecto     = controller.getArbol().recorridoInorden();
        casosDesordenados = new java.util.ArrayList<>(ordenCorrecto);
        java.util.Collections.shuffle(casosDesordenados);
        siguienteCorrecto = 0;
        intentosFallidos  = 0;

        Label titulo = new Label("Nivel Final – La verdad detrás del acoso");
        titulo.getStyleClass().add("titulo-nivel");

        Label subtitulo = new Label(
                "El detective Alex reúne todas las evidencias. " +
                        "Para presentar el caso ante el juez, los delitos deben " +
                        "organizarse de menor a mayor gravedad."
        );
        subtitulo.getStyleClass().add("subtitulo");
        subtitulo.setWrapText(true);

        // Instrucción de Alex
        VBox cajaMision = new VBox(8);
        cajaMision.setStyle(
                "-fx-background-color: #00d4ff08;" +
                        "-fx-border-color: #00d4ff25;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 16 20 16 20;"
        );
        Label misionEtq = new Label("ALEX DICE");
        misionEtq.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #00d4ff; -fx-letter-spacing: 0.1em;"
        );
        Label misionTexto = new Label(
                "Para presentar el caso ante el juez debemos organizar los delitos " +
                        "de menor a mayor gravedad, tal como los registra el árbol AVL. " +
                        "Haz clic en los casos en el orden correcto — empieza por el menos grave."
        );
        misionTexto.setWrapText(true);
        misionTexto.setStyle(
                "-fx-font-size: 19px; -fx-font-style: italic; -fx-text-fill: #00d4ffcc;"
        );
        cajaMision.getChildren().addAll(misionEtq, misionTexto);

        // Línea de progreso del recorrido
        Label progresoEtq = new Label("RECORRIDO INORDEN — PROGRESO");
        progresoEtq.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );

        HBox lineaRecorrido = new HBox(8);
        lineaRecorrido.setAlignment(Pos.CENTER_LEFT);
        lineaRecorrido.setId("lineaRecorrido");

        for (int i = 0; i < ordenCorrecto.size(); i++) {
            VBox slot = crearSlotRecorrido(i);
            lineaRecorrido.getChildren().add(slot);
        }

        // Feedback de Alex
        Label feedbackLabel = new Label("Selecciona el caso con menor gravedad para comenzar.");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setId("feedbackLabel");
        feedbackLabel.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #9090b0; -fx-line-spacing: 4;" +
                        "-fx-wrap-text: true;"
        );

        // Nodos desordenados para que el jugador elija
        Label seleccionEtq = new Label("CASOS DISPONIBLES — HAZ CLIC EN EL ORDEN CORRECTO");
        seleccionEtq.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );

        VBox casosBox = new VBox(8);
        casosBox.setId("casosBox");

        for (Caso c : casosDesordenados) {
            VBox tarjeta = crearTarjetaSeleccionable(c, casosBox, lineaRecorrido, feedbackLabel);
            casosBox.getChildren().add(tarjeta);
        }

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox contenido = new VBox(16,
                titulo, subtitulo, cajaMision,
                progresoEtq, lineaRecorrido,
                feedbackLabel,
                seleccionEtq, casosBox
        );
        scroll.setContent(contenido);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        vistaNivelFinal.getChildren().add(scroll);
        cambiarVista(Vista.NIVEL_FINAL);
        labelNivelBadge.setText("Nivel Final – La verdad");
    }

    private VBox crearSlotRecorrido(int posicion) {
        VBox slot = new VBox();
        slot.setAlignment(Pos.CENTER);
        slot.setPrefWidth(140);
        slot.setPrefHeight(56);
        slot.setStyle(
                "-fx-background-color: #0f0f1a;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8;"
        );

        Label numLabel = new Label(String.valueOf(posicion + 1));
        numLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 17px; -fx-text-fill: #2a2a3c;"
        );

        Label vaciLabel = new Label("— pendiente —");
        vaciLabel.setStyle("-fx-font-size: 19px; -fx-text-fill: #2a2a3c;");

        slot.getChildren().addAll(numLabel, vaciLabel);
        slot.setId("slot_" + posicion);
        return slot;
    }

    private VBox crearTarjetaSeleccionable(Caso caso, VBox casosBox,
                                           HBox lineaRecorrido,
                                           Label feedbackLabel) {
        VBox tarjeta = new VBox(4);
        tarjeta.setId("caso_" + caso.getId());
        tarjeta.setStyle(
                "-fx-background-color: #12121c;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 18 14 18;" +
                        "-fx-cursor: hand;"
        );

        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Caso #" + caso.getId());
        idLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 19px; -fx-text-fill: #3a3a5c;"
        );
        Label tipoLabel = new Label(caso.getTipoAcoso());
        tipoLabel.setStyle(
                "-fx-font-size: 19px; -fx-font-weight: 600; -fx-text-fill: #e0e0f0;"
        );
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label gravLabel = new Label("Gravedad: " + caso.getGravedad() + "/10");
        gravLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 19px; -fx-text-fill: #4a4a6a;"
        );
        cabecera.getChildren().addAll(idLabel, tipoLabel, spacer, gravLabel);

        Label leyLabel = new Label(caso.getLeyColombia());
        leyLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #00d4ffaa;");

        tarjeta.getChildren().addAll(cabecera, leyLabel);

        // Hover
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(
                "-fx-background-color: #00d4ff08;" +
                        "-fx-border-color: #00d4ff40;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 18 14 18;" +
                        "-fx-cursor: hand;"
        ));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(
                "-fx-background-color: #12121c;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 18 14 18;" +
                        "-fx-cursor: hand;"
        ));

        tarjeta.setOnMouseClicked(e ->
                evaluarSeleccionRecorrido(caso, tarjeta, casosBox, lineaRecorrido, feedbackLabel)
        );

        return tarjeta;
    }

    private void evaluarSeleccionRecorrido(Caso casoElegido, VBox tarjeta,
                                           VBox casosBox, HBox lineaRecorrido,
                                           Label feedbackLabel) {
        if (siguienteCorrecto >= ordenCorrecto.size()) return;

        Caso esperado = ordenCorrecto.get(siguienteCorrecto);

        if (casoElegido.getId() == esperado.getId()) {
            // Correcto — marcar la tarjeta y llenar el slot
            tarjeta.setStyle(
                    "-fx-background-color: #00ff8808;" +
                            "-fx-border-color: #00ff8855;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 14 18 14 18;" +
                            "-fx-opacity: 0.6;"
            );
            tarjeta.setDisable(true);

            // Llenar slot en la línea de recorrido
            VBox slot = (VBox) lineaRecorrido.lookup("#slot_" + siguienteCorrecto);
            if (slot != null) {
                slot.getChildren().clear();
                slot.setStyle(
                        "-fx-background-color: #00ff8810;" +
                                "-fx-border-color: #00ff8855;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 8;" +
                                "-fx-background-radius: 8;" +
                                "-fx-padding: 8;"
                );
                Label casoLabel = new Label("C" + casoElegido.getId());
                casoLabel.setStyle(
                        "-fx-font-family: 'DM Mono'; -fx-font-size: 17px;" +
                                "-fx-font-weight: 600; -fx-text-fill: #00ff88;"
                );
                Label gravSlot = new Label("G:" + casoElegido.getGravedad());
                gravSlot.setStyle(
                        "-fx-font-family: 'DM Mono'; -fx-font-size: 17px; -fx-text-fill: #00ff8888;"
                );
                slot.getChildren().addAll(casoLabel, gravSlot);
                animarEntrada(slot);
            }

            feedbackLabel.setStyle(
                    "-fx-font-size: 19px; -fx-text-fill: #00ff88; -fx-line-spacing: 2;"
            );

            siguienteCorrecto++;

            if (siguienteCorrecto == ordenCorrecto.size()) {
                // Todos correctos — pasar a identificar al agresor
                feedbackLabel.setText(
                        "✓ Perfecto. Recorrido inorden completado. " +
                                "El árbol AVL organizó los delitos de menor a mayor gravedad automáticamente."
                );
                PauseTransition pausa = new PauseTransition(Duration.millis(1200));
                pausa.setOnFinished(ev -> mostrarPreguntaAgresor());
                pausa.play();
            } else {
                Caso siguiente = ordenCorrecto.get(siguienteCorrecto);
                feedbackLabel.setText(
                        "✓ Correcto. " + casoElegido.getTipoAcoso() +
                                " (G:" + casoElegido.getGravedad() + ") registrado. " +
                                "Ahora selecciona el siguiente delito más grave."
                );
            }

        } else {
            // Incorrecto
            intentosFallidos++;
            feedbackLabel.setStyle(
                    "-fx-font-size: 19px; -fx-text-fill: #ff4466; -fx-line-spacing: 2;"
            );
            feedbackLabel.setText(
                    "✗ Ese delito tiene gravedad " + casoElegido.getGravedad() +
                            " — no es el siguiente en el recorrido. " +
                            "Recuerda: el árbol AVL organiza de menor a mayor gravedad. " +
                            "Busca el caso menos grave disponible."
            );

            // Efecto de sacudida visual
            tarjeta.setStyle(
                    "-fx-background-color: #ff004408;" +
                            "-fx-border-color: #ff004455;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 14 18 14 18;" +
                            "-fx-cursor: hand;"
            );
            PauseTransition reset = new PauseTransition(Duration.millis(600));
            reset.setOnFinished(ev -> tarjeta.setStyle(
                    "-fx-background-color: #12121c;" +
                            "-fx-border-color: #1e1e2e;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 14 18 14 18;" +
                            "-fx-cursor: hand;"
            ));
            reset.play();
        }
    }

    private void mostrarPreguntaAgresor() {
        vistaNivelFinal.getChildren().clear();
        vistaNivelFinal.setSpacing(20);
        vistaNivelFinal.setStyle(
                "-fx-background-color: #0a0a0f; -fx-padding: 40 48 40 48;"
        );

        Label titulo = new Label("Identificar al agresor");
        titulo.getStyleClass().add("titulo-nivel");

        Label subtitulo = new Label(
                "Basado en todas las evidencias recolectadas — " +
                        "IPs rastreadas, metadatos, patrones de escritura y horarios — " +
                        "¿quién está detrás de todos los ataques contra Valeria?"
        );
        subtitulo.getStyleClass().add("subtitulo");
        subtitulo.setWrapText(true);

        // Resumen de evidencias clave del árbol
        VBox resumenEvidencias = new VBox(6);
        resumenEvidencias.setStyle(
                "-fx-background-color: #0f0f1a;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 16 20 16 20;"
        );
        Label evEtq = new Label("EVIDENCIAS CLAVE DEL ÁRBOL");
        evEtq.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );
        resumenEvidencias.getChildren().add(evEtq);

        for (Caso c : ordenCorrecto) {
            String ev = c.getEvidencias()[0];
            Label evLabel = new Label("◆  [Caso #" + c.getId() + "] " + ev);
            evLabel.setWrapText(true);
            evLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #6060808;");
            resumenEvidencias.getChildren().add(evLabel);
        }

        // Sospechosos — uno de ellos es el correcto
        Label sospEtq = new Label("SELECCIONA AL AGRESOR");
        sospEtq.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );
        VBox.setMargin(sospEtq, new Insets(8, 0, 0, 0));

        String[] todosSospechosos = {
                "Mateo R. – compañero de clase de Valeria",
                "Sebastián L. – exnovio de Valeria",
                "Camila V. – rival académica de Valeria",
                "Usuario anónimo conocido como 'ShadowNet_21'"
        };

        VBox sospBox = new VBox(8);
        for (String s : todosSospechosos) {
            Button btnS = new Button(s);
            btnS.getStyleClass().add("btn-opcion");
            btnS.setMaxWidth(Double.MAX_VALUE);
            btnS.setOnAction(e -> evaluarSospechoso(s, btnS, sospBox));
            sospBox.getChildren().add(btnS);
        }

        vistaNivelFinal.getChildren().addAll(
                titulo, subtitulo, resumenEvidencias, sospEtq, sospBox
        );
        animarEntrada(vistaNivelFinal);
    }

    private void evaluarSospechoso(String elegido, Button btnElegido, VBox sospBox) {
        String correcto = controller.getSospechoso();
        boolean acerto = elegido.equals(correcto);

        // Deshabilitar todos los botones y colorear
        for (Node n : sospBox.getChildren()) {
            if (n instanceof Button btn) {
                btn.setDisable(true);
                if (btn.getText().equals(correcto)) {
                    btn.getStyleClass().add("btn-opcion-correcto");
                } else if (btn == btnElegido && !acerto) {
                    btn.getStyleClass().add("btn-opcion-incorrecto");
                }
            }
        }

        PauseTransition pausa = new PauseTransition(Duration.millis(1000));
        pausa.setOnFinished(e -> {
            if (acerto) {
                controller.getLogEventos().add("★ Agresor identificado correctamente.");
                mostrarCierreFinal(true);
            } else {
                controller.getLogEventos().add(
                        "✗ Agresor incorrecto. El culpable era: " + correcto
                );
                mostrarCierreFinal(false);
            }
            actualizarLog();
        });
        pausa.play();
    }

    private void mostrarCierreFinal(boolean acerto) {
        vistaNivelFinal.getChildren().clear();
        vistaNivelFinal.setSpacing(24);

        Label titulo = new Label(acerto ? "Caso cerrado." : "Caso cerrado — con una advertencia.");
        titulo.getStyleClass().add("menu-titulo");

        Label mensaje = new Label(acerto
                ? "Identificaste correctamente al agresor. " +
                "Las evidencias del árbol AVL fueron suficientes para cerrar el caso."
                : "No identificaste al agresor correcto, pero el árbol de evidencias " +
                "fue suficiente para cerrar el caso. El culpable era: " +
                controller.getSospechoso() + "."
        );
        mensaje.setWrapText(true);
        mensaje.getStyleClass().add("subtitulo");

        // Árbol final iluminado
        visualizadorArbol.actualizar(controller.getArbol());
        VBox canvasFinal = new VBox(visualizadorArbol.getCanvas());
        canvasFinal.setStyle(
                "-fx-background-color: #0d0d16;" +
                        (acerto
                                ? "-fx-border-color: #00d4ff50;"
                                : "-fx-border-color: #1e1e2e;") +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 16;"
        );

        // Reporte
        String reporte = controller.getArbol().generarReporte()
                + "\n\nAgresor identificado: " + controller.getSospechoso()
                + "\nPuntaje final: " + controller.getPuntaje() + " puntos."
                + "\nFallos en el recorrido: " + intentosFallidos;

        TextArea reporteArea = new TextArea(reporte);
        reporteArea.setEditable(false);
        reporteArea.setWrapText(true);
        reporteArea.setPrefHeight(240);
        reporteArea.setStyle(
                "-fx-background-color: #0d0d16;" +
                        "-fx-control-inner-background: #0d0d16;" +
                        "-fx-text-fill: #9090b0;" +
                        "-fx-font-family: 'DM Mono';" +
                        "-fx-font-size: 17px;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 16;"
        );

        Button btnNueva = new Button("Nueva investigación");
        btnNueva.getStyleClass().add("btn-primario");
        btnNueva.setOnAction(e -> new PantallaInicio(stage).mostrar());

        Button btnMenu = new Button("Volver al menú");
        btnMenu.getStyleClass().add("btn-secundario");
        btnMenu.setOnAction(e -> new MenuPrincipal(stage).mostrar());

        HBox botones = new HBox(14, btnNueva, btnMenu);
        botones.setAlignment(Pos.CENTER_LEFT);

        vistaNivelFinal.getChildren().addAll(
                titulo, mensaje, canvasFinal, reporteArea, botones
        );

        animarEntrada(vistaNivelFinal);
        cambiarVista(Vista.NIVEL_FINAL);
    }

    private VBox construirTarjetaResumenCaso(Caso c, int orden) {
        VBox tarjeta = new VBox(6);
        tarjeta.setStyle(
                "-fx-background-color: #0f0f1a;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 18 14 18;"
        );

        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label ordenLabel = new Label("#" + orden);
        ordenLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 19px;" +
                        "-fx-text-fill: #3a3a5c;"
        );

        Label tipoLabel = new Label(c.getTipoAcoso());
        tipoLabel.setStyle(
                "-fx-font-size: 19px; -fx-font-weight: 600; -fx-text-fill: #e0e0f0;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label gravLabel = new Label("Gravedad " + c.getGravedad() + "/10");
        gravLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 19px; -fx-text-fill: #00d4ff;"
        );

        cabecera.getChildren().addAll(ordenLabel, tipoLabel, spacer, gravLabel);

        Label leyLabel = new Label(c.getLeyColombia());
        leyLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #00d4ff88;");

        Label penaLabel = new Label("Pena: " + c.getPenaAplicable());
        penaLabel.setWrapText(true);
        penaLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #8080a0;");

        tarjeta.getChildren().addAll(cabecera, leyLabel, penaLabel);
        return tarjeta;
    }

    private void mostrarReporteCompleto() {
        String reporte = controller.getArbol().generarReporte()
                + "\n\nAgresor identificado: " + controller.getSospechoso()
                + "\nPuntaje final: " + controller.getPuntaje() + " puntos.";

        TextArea reporteArea = new TextArea(reporte);
        reporteArea.setEditable(false);
        reporteArea.setWrapText(true);
        reporteArea.setStyle(
                "-fx-background-color: #0d0d16;" +
                        "-fx-control-inner-background: #0d0d16;" +
                        "-fx-text-fill: #9090b0;" +
                        "-fx-font-family: 'DM Mono';" +
                        "-fx-font-size: 17px;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 20;"
        );

        vistaNivelFinal.getChildren().add(reporteArea);
        VBox.setVgrow(reporteArea, Priority.ALWAYS);
        animarEntrada(reporteArea);
    }

    // ══════════════════════════════════════════════
    //  UTILIDADES
    // ══════════════════════════════════════════════

    private Separator separador(int margen) {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1a1a28;");
        VBox.setMargin(sep, new Insets(margen, 0, margen, 0));
        return sep;
    }

    private void animarEntrada(Node nodo) {
        nodo.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), nodo);
        ft.setToValue(1);
        ft.play();
    }

    private void actualizarLog() {
        StringBuilder sb = new StringBuilder();
        for (String evento : controller.getLogEventos()) {
            sb.append(evento).append("\n");
        }
        logArea.setText(sb.toString());
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    // ══════════════════════════════════════════════
    //  LISTENERS
    // ══════════════════════════════════════════════

    @Override
    public void onFaseCambiada(FaseNivel nuevaFase) {
        Platform.runLater(() -> renderizarFase(nuevaFase));
    }

    @Override
    public void onEvidenciaRevelada(String ev, int reveladas, int total) {
        Platform.runLater(() -> {
            barraEvidencias.setProgress((double) reveladas / total);
            labelProgresoEvidencias.setText(reveladas + " / " + total + " recolectadas");
        });
    }

    @Override public void onNivelCompletado(int nivel, Caso caso) {}

    @Override
    public void onArbolActualizado() {
        Platform.runLater(() -> visualizadorArbol.actualizar(controller.getArbol()));
    }

    @Override
    public void onJuegoTerminado(String reporte) {
        // Se maneja desde insertarEnArbolYContinuar()
    }

    @Override
    public void onPuntajeActualizado(int p) {
        Platform.runLater(() -> labelPuntaje.setText(String.valueOf(p)));
    }

    @Override
    public void onMensajeDetective(String msg) {
        Platform.runLater(() -> labelMensajeDetective.setText(msg));
    }

    @Override
    public void onRespuestaRecibida(int opcion, boolean correcta, String jugador) {
        Platform.runLater(() -> {
            if (opcionesBox != null) {
                String[][] preguntas = controller.getPreguntasNivelActual();
                int qIdx = controller.getPreguntaActual();
                
                // Si ya avanzó el índice (en modo solitario), retrocedemos uno para buscar la correcta
                // Pero como onRespuestaRecibida se llama ANTES de avanzar en solitario ahora, 
                // o el índice ya es el correcto según la lógica.
                // Buscamos la respuesta correcta en la data actual.
                String[] dataPregunta = preguntas[Math.min(qIdx, preguntas.length - 1)];
                int correctaIdx = Integer.parseInt(dataPregunta[dataPregunta.length - 1]);

                for (int i = 0; i < opcionesBox.getChildren().size(); i++) {
                    Node n = opcionesBox.getChildren().get(i);
                    if (n instanceof Button btn) {
                        btn.setDisable(true);
                        if (i == correctaIdx) {
                            btn.getStyleClass().add("btn-opcion-correcto");
                        } else if (i == opcion && !correcta) {
                            btn.getStyleClass().add("btn-opcion-incorrecto");
                        }
                    }
                }

                PauseTransition pausa = new PauseTransition(Duration.millis(1200));
                pausa.setOnFinished(e -> cargarPregunta());
                pausa.play();
            }
        });
    }

    @Override
    public void onRespuestaOponente(String oponente, int opcion, boolean correcta) {
        Platform.runLater(() -> {
            if (opcionesBox != null) {
                for (Node n : opcionesBox.getChildren()) {
                    if (n instanceof Button b) {
                        b.setDisable(true);
                        b.setOpacity(0.5);
                    }
                }
                Label aviso = new Label("¡EL DETECTIVE " + oponente.toUpperCase() + " SE ADELANTÓ!");
                aviso.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 17px;");
                panelFase.getChildren().add(aviso);
                
                // Esperar un momento y pasar a la siguiente
                PauseTransition pausa = new PauseTransition(Duration.millis(1500));
                pausa.setOnFinished(e -> cargarPregunta());
                pausa.play();
            }
        });
    }

    @Override
    public void onRespuestaIncorrecta(String jugador, int opcion, boolean soyYo) {
        Platform.runLater(() -> {
            if (opcionesBox != null) {
                if (soyYo) {
                    // Marcar en rojo mi respuesta incorrecta
                    if (opcion >= 0 && opcion < opcionesBox.getChildren().size()) {
                        Node n = opcionesBox.getChildren().get(opcion);
                        if (n instanceof Button btn) {
                            btn.getStyleClass().add("btn-opcion-incorrecto");
                        }
                    }
                    Label aviso = new Label("¡Respuesta incorrecta! Esperando al otro detective...");
                    aviso.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 17px;");
                    panelFase.getChildren().add(aviso);
                } else {
                    // El otro falló, avisamos
                    Label aviso = new Label("¡EL DETECTIVE " + jugador.toUpperCase() + " FALLÓ! TE TOCA.");
                    aviso.setStyle("-fx-text-fill: #ff9f1c; -fx-font-weight: bold; -fx-font-size: 17px;");
                    panelFase.getChildren().add(aviso);
                }
            }
        });
    }

    @Override
    public void onAmbosFallaron(int correcta) {
        Platform.runLater(() -> {
            if (opcionesBox != null) {
                // Deshabilitar todo y resaltar correcta
                for (int i = 0; i < opcionesBox.getChildren().size(); i++) {
                    Node n = opcionesBox.getChildren().get(i);
                    if (n instanceof Button btn) {
                        btn.setDisable(true);
                        if (i == correcta) {
                            btn.getStyleClass().add("btn-opcion-correcto");
                        }
                    }
                }
                Label aviso = new Label("¡AMBOS FALLARON! LA RESPUESTA ERA LA RESALTADA.");
                aviso.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold; -fx-font-size: 17px;");
                panelFase.getChildren().add(aviso);
                
                PauseTransition pausa = new PauseTransition(Duration.millis(3000));
                pausa.setOnFinished(e -> cargarPregunta());
                pausa.play();
            }
        });
    }

    @Override
    public void onPuntajeOponenteActualizado(int puntaje) {
        Platform.runLater(() -> {
            if (labelPuntajeOponente != null) {
                labelPuntajeOponente.setText(String.valueOf(puntaje));
            }
        });
    }

    @Override
    public void onOponenteDesconectado(String nombre) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conexión perdida");
            alert.setHeaderText("El detective " + nombre + " se ha desconectado.");
            alert.setContentText("La investigación ya no puede continuar de forma sincronizada.");
            alert.showAndWait();
        });
    }


    // ── Estilos de navegación ──────────────────────

    private String estiloNavActivo() {
        return "-fx-background-color: #00d4ff15;" +
                "-fx-border-color: #00d4ff40;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-text-fill: #00d4ff;" +
                "-fx-font-size: 19px;" +
                "-fx-padding: 10 16 10 16;" +
                "-fx-cursor: hand;";
    }

    private String estiloNavInactivo() {
        return "-fx-background-color: transparent;" +
                "-fx-border-color: #1e1e2e;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-text-fill: #4a4a6a;" +
                "-fx-font-size: 19px;" +
                "-fx-padding: 10 16 10 16;" +
                "-fx-cursor: hand;";
    }
}