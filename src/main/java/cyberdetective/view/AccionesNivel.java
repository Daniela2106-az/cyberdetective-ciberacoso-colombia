package cyberdetective.view;

import cyberdetective.controller.JuegoController;
import cyberdetective.minijuego.GestorMinijuegos;
import cyberdetective.minijuego.Minijuego;
import cyberdetective.model.Caso;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Pantalla unificada de un nivel.
 *
 * Flujo niveles 1-4:
 *   1. Acciones de investigación (evidencias integradas)
 *   2. Preguntas sobre el delito y la sanción
 *   3. Reporte del caso
 *   4. Insertar nodo en el árbol AVL → regreso al mapa
 *
 * Flujo nivel final (5):
 *   1. Recorrer el árbol (inorden interactivo)
 *   2. Analizar nodos / reconstruir la línea de hechos
 *   3. Reporte global del caso → regreso al mapa
 */
public class AccionesNivel implements JuegoController.JuegoListener {

    private final Stage stage;
    private final JuegoController controller;
    private final MapaInvestigacion mapa;

    private boolean[] accionesCompletadas;
    private int nivel;
    private static final int TOTAL_NIVELES = 5;

    // UI global — se construye una sola vez
    private Label labelMensajeAlex;
    private Button btnSiguiente;
    private VBox panelAcciones;
    private BorderPane root;
    private VBox contenidoScroll;
    private ScrollPane scrollCentral;
    private ProgressBar barraProgreso;
    private Label labelProgreso;
    private VisualizadorArbol visualizadorArbol;
    private List<javafx.scene.Node> nodosAccionesCache;
    private StackPane stackCentral;
    private Button btnEvBtnCache;
    private Button btnInvCache;
    private Button btnArbolBtnCache;
    private VBox[] tarjetasAcciones = new VBox[3];
    private VBox opcionesBoxInterrogatorio;
    private List<Caso> ordenCorrecto;
    private List<Caso> casosDesordenados;
    private int siguienteCorrecto = 0;
    private int intentosFallidos  = 0;
    
    private Label labelPuntaje;
    private Label labelPuntajeOponente;

    public AccionesNivel(Stage stage, JuegoController controller,
                         MapaInvestigacion mapa) {
        this.stage = stage;
        this.controller = controller;
        this.mapa = mapa;
    }

    // ════════════════════════════════════════════════
    //  PUNTO DE ENTRADA
    // ════════════════════════════════════════════════

    public void mostrar(int nivel) {
        this.nivel = nivel;
        this.accionesCompletadas = new boolean[3];
        this.nodosAccionesCache = null;
        controller.agregarListener(this);
        controller.iniciarInvestigacion();

        // Root fijo — la Scene no se recrea, evita cambios de tamaño
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0a0f;");
        root.setMinWidth(1280);
        root.setMinHeight(800);

        contenidoScroll = new VBox(20);
        contenidoScroll.setStyle("-fx-padding: 36 44 36 44;");

        scrollCentral = new ScrollPane(contenidoScroll);
        scrollCentral.setFitToWidth(true);
        scrollCentral.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollCentral.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        stackCentral = new StackPane(scrollCentral);

        root.setLeft(construirPanelLateral());
        root.setCenter(stackCentral);

        // Reutilizar la Scene existente, solo cambiar el root
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

        if (nivel == 5) {
            mostrarNivelFinal();
        } else {
            mostrarFaseAcciones();
        }
    }

    // ════════════════════════════════════════════════
    //  PANEL LATERAL
    // ════════════════════════════════════════════════

    private VBox construirPanelLateral() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel-lateral");
        panel.setMinWidth(300);
        panel.setMaxWidth(300);

        Label appLabel = new Label("CYBERDETECTIVE");
        appLabel.getStyleClass().add("titulo-app");

        Circle avatar = new Circle(24);
        avatar.setFill(Color.web("#00d4ff15"));
        avatar.setStroke(Color.web("#00d4ff40"));
        avatar.setStrokeWidth(1);
        Label avatarLetra = new Label(
                controller.getNombreJugador().substring(0, 1).toUpperCase()
        );
        avatarLetra.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:#00d4ff;");
        StackPane avatarBox = new StackPane(avatar, avatarLetra);
        avatarBox.setPrefSize(48, 48);
        VBox.setMargin(avatarBox, new Insets(20, 0, 8, 0));

        Label nombreLabel = new Label("Det. " + controller.getNombreJugador());
        nombreLabel.setStyle("-fx-font-size:16px;-fx-font-weight:600;-fx-text-fill:#f0f0f8;");
        Label rolLabel = new Label("Investigador en campo");
        rolLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#5a5a7a;");

        panel.getChildren().addAll(appLabel, avatarBox, nombreLabel, rolLabel);
        panel.getChildren().add(separador(16));

        // Puntajes
        Label puntajeEtq = new Label("PUNTAJES");
        puntajeEtq.getStyleClass().add("etiqueta-seccion");
        
        HBox scoreBox = new HBox(15);
        scoreBox.setAlignment(Pos.CENTER_LEFT);
        
        // Tú
        VBox myBox = new VBox(2);
        Label myTitle = new Label("TÚ");
        myTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #00d4ff; -fx-font-weight: bold;");
        labelPuntaje = new Label(String.valueOf(controller.getPuntaje()));
        labelPuntaje.getStyleClass().add("puntaje-label");
        labelPuntaje.setStyle("-fx-font-size: 29px;");
        myBox.getChildren().addAll(myTitle, labelPuntaje);
        
        // Oponente
        VBox opBox = new VBox(2);
        Label opTitle = new Label("OPONENTE");
        opTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff9f1c; -fx-font-weight: bold;");
        
        int puntosOp = 0;
        if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
            puntosOp = mc.getOponentePuntaje();
        }
        
        labelPuntajeOponente = new Label(String.valueOf(puntosOp));
        labelPuntajeOponente.getStyleClass().add("puntaje-label");
        labelPuntajeOponente.setStyle("-fx-font-size: 29px; -fx-text-fill: #ff9f1c;");
        opBox.getChildren().addAll(opTitle, labelPuntajeOponente);
        
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setStyle("-fx-background-color: #2a2a3c;");
        
        scoreBox.getChildren().addAll(myBox, sep, opBox);
        VBox.setMargin(scoreBox, new Insets(8, 0, 0, 0));
        
        panel.getChildren().addAll(puntajeEtq, scoreBox);
        panel.getChildren().add(separador(14));

        // Progreso del detective
        Label progresoEtq = new Label("PROGRESO DEL DETECTIVE");
        progresoEtq.getStyleClass().add("etiqueta-seccion");

        int nivelActual = nivel;
        double progreso = (double)nivelActual / TOTAL_NIVELES;
        barraProgreso = new ProgressBar(progreso);
        barraProgreso.setMaxWidth(Double.MAX_VALUE);
        barraProgreso.setPrefHeight(6);
        barraProgreso.setStyle(
                "-fx-accent:#00d4ff;-fx-background-color:#1a1a28;" +
                        "-fx-background-radius:4;-fx-border-radius:4;"
        );
        VBox.setMargin(barraProgreso, new Insets(6, 0, 4, 0));

        labelProgreso = new Label("Nivel " + nivelActual + " de " + TOTAL_NIVELES);
        labelProgreso.setStyle("-fx-font-size:11px;-fx-text-fill:#5a5a7a;");

        panel.getChildren().addAll(progresoEtq, barraProgreso, labelProgreso);
        panel.getChildren().add(separador(14));

        // Caso activo
        Label nivelEtq = new Label("CASO ACTIVO");
        nivelEtq.getStyleClass().add("etiqueta-seccion");
        Label nivelVal = new Label(cyberdetective.data.NivelesData.getTituloNivel(nivel));
        nivelVal.getStyleClass().add("nivel-badge");
        nivelVal.setWrapText(true);
        VBox.setMargin(nivelVal, new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(nivelEtq, nivelVal);
        panel.getChildren().add(separador(14));

        // Botones de vista (solo niveles 1-4)
        if (nivel <= 4) {
            Label vistasEtq = new Label("VISTAS");
            vistasEtq.getStyleClass().add("etiqueta-seccion");

            btnInvCache = new Button("📋  Investigación");
            btnInvCache.setMaxWidth(Double.MAX_VALUE);
            btnInvCache.setStyle(estiloNavActivo());

            btnArbolBtnCache = new Button("🌳  Árbol AVL");
            btnArbolBtnCache.setMaxWidth(Double.MAX_VALUE);
            btnArbolBtnCache.setStyle(estiloNavInactivo());

            btnEvBtnCache = new Button("🔍  Evidencias");
            btnEvBtnCache.setMaxWidth(Double.MAX_VALUE);
            btnEvBtnCache.setStyle(estiloNavInactivo());

            btnInvCache.setOnAction(e -> {
                btnInvCache.setStyle(estiloNavActivo());
                btnArbolBtnCache.setStyle(estiloNavInactivo());
                btnEvBtnCache.setStyle(estiloNavInactivo());
                mostrarFaseAcciones();
            });
            btnArbolBtnCache.setOnAction(e -> {
                btnArbolBtnCache.setStyle(estiloNavActivo());
                btnInvCache.setStyle(estiloNavInactivo());
                btnEvBtnCache.setStyle(estiloNavInactivo());
                mostrarVistaArbol();
            });
            btnEvBtnCache.setOnAction(e -> {
                btnEvBtnCache.setStyle(estiloNavActivo());
                btnInvCache.setStyle(estiloNavInactivo());
                btnArbolBtnCache.setStyle(estiloNavInactivo());
                mostrarVistaEvidencias(-1);
            });

            VBox.setMargin(btnInvCache, new Insets(8, 0, 4, 0));
            VBox.setMargin(btnArbolBtnCache, new Insets(0, 0, 4, 0));
            panel.getChildren().addAll(vistasEtq, btnInvCache, btnArbolBtnCache, btnEvBtnCache);
            panel.getChildren().add(separador(14));
        }

        // Objetivos del nivel (solo 1-4)
        if (nivel <= 4) {
            Label checkEtq = new Label("OBJETIVOS DEL NIVEL");
            checkEtq.getStyleClass().add("etiqueta-seccion");
            VBox.setMargin(checkEtq, new Insets(0, 0, 8, 0));
            panel.getChildren().add(checkEtq);

            panelAcciones = new VBox(8);
            for (String obj : getObjetivosNivel(nivel)) {
                HBox fila = new HBox(10);
                fila.setAlignment(Pos.CENTER_LEFT);
                Circle dot = new Circle(5);
                dot.setFill(Color.web("#2a2a3c"));
                dot.setStroke(Color.web("#3a3a5c"));
                dot.setStrokeWidth(1);
                Label objLabel = new Label(obj);
                objLabel.setWrapText(true);
                objLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#707090;");
                HBox.setHgrow(objLabel, Priority.ALWAYS);
                fila.getChildren().addAll(dot, objLabel);
                panelAcciones.getChildren().add(fila);
            }
            panel.getChildren().add(panelAcciones);
            panel.getChildren().add(separador(14));
        }

        // Alex dice
        Label alexEtq = new Label("ALEX DICE");
        alexEtq.getStyleClass().add("etiqueta-seccion");
        labelMensajeAlex = new Label("Analiza cada pista con cuidado.");
        labelMensajeAlex.setWrapText(true);
        labelMensajeAlex.getStyleClass().add("mensaje-detective");
        labelMensajeAlex.setPadding(new Insets(12, 14, 12, 14));
        VBox cajaMensaje = new VBox(labelMensajeAlex);
        cajaMensaje.setStyle(
                "-fx-background-color:#00d4ff08;-fx-border-color:#00d4ff20;" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;"
        );
        VBox.setMargin(cajaMensaje, new Insets(8, 0, 0, 0));
        panel.getChildren().addAll(alexEtq, cajaMensaje);
        panel.getChildren().add(separador(14));

        Button btnMapa = new Button("← Volver al mapa");
        btnMapa.getStyleClass().add("btn-secundario");
        btnMapa.setMaxWidth(Double.MAX_VALUE);
        btnMapa.setOnAction(e -> mapa.regresarAlMapa());
        panel.getChildren().add(btnMapa);

        return panel;
    }

    // ════════════════════════════════════════════════
    //  INTERCAMBIO DE CONTENIDO CENTRAL
    // ════════════════════════════════════════════════

    /** Reemplaza los hijos del scroll sin recrear la Scene. */
    private void setCentral(javafx.scene.Node... nodos) {
        contenidoScroll.getChildren().setAll(nodos);
        scrollCentral.setVvalue(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), contenidoScroll);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ════════════════════════════════════════════════
    //  VISTA: ÁRBOL AVL
    // ════════════════════════════════════════════════

    private void mostrarVistaArbol() {
        visualizadorArbol = obtenerVisualizadorArbol();
        visualizadorArbol.actualizar(controller.getArbol());

        Label titulo = new Label("Árbol AVL de Investigación");
        titulo.getStyleClass().add("titulo-nivel");
        Label sub = new Label(
                "Cada nodo es un caso. Haz clic sobre un nodo para ver " +
                        "la información completa del incidente."
        );
        sub.getStyleClass().add("subtitulo"); sub.setWrapText(true);
        Label instruccion = new Label(
                "Nodos ordenados por gravedad del delito (1 = leve → 10 = grave). " +
                        "El árbol se auto-balancea con rotaciones AVL al insertar cada caso."
        );
        instruccion.setStyle("-fx-font-size:12px;-fx-text-fill:#2a2a4a;");
        instruccion.setWrapText(true);
        VBox canvasBox = new VBox(visualizadorArbol.getCanvas());
        canvasBox.setStyle(
                "-fx-background-color:#0d0d16;-fx-border-color:#1e1e2e;" +
                        "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;" +
                        "-fx-padding:16;"
        );
        setCentral(titulo, sub, instruccion, canvasBox);
    }

    // ════════════════════════════════════════════════
    //  VISTA: PANEL DE EVIDENCIAS
    // ════════════════════════════════════════════════

    private void mostrarVistaEvidencias(int casoIdParaPopup) {
        Label titulo = new Label("Panel de Evidencias");
        titulo.getStyleClass().add("titulo-nivel");
        Label sub = new Label(
                "Evidencias recolectadas hasta el momento. " +
                        "Haz clic en cualquier ficha para ver los datos técnicos forenses."
        );
        sub.getStyleClass().add("subtitulo"); sub.setWrapText(true);

        VBox fichas = new VBox(14);
        List<Caso> casosEnArbol = controller.getArbol().recorridoInorden();
        Caso casoActual = controller.getCasoActualFijo();

        if (casosEnArbol.isEmpty() && casoActual == null) {
            Label vacio = new Label("Aún no se han recolectado evidencias.");
            vacio.setStyle("-fx-font-size:13px;-fx-text-fill:#3a3a5c;");
            fichas.getChildren().add(vacio);
        } else {
            for (Caso c : casosEnArbol) fichas.getChildren().add(construirFichaEvidencia(c, true));
            if (casoActual != null &&
                    casosEnArbol.stream().noneMatch(c -> c.getId() == casoActual.getId())) {
                fichas.getChildren().add(construirFichaEvidencia(casoActual, false));
            }
        }
        setCentral(titulo, sub, fichas);

        if (casoIdParaPopup != -1) {
            // Find the instance and call the popup
            Caso objetivo = casosEnArbol.stream().filter(c -> c.getId() == casoIdParaPopup).findFirst().orElse(
                    (casoActual != null && casoActual.getId() == casoIdParaPopup) ? casoActual : null
            );
            if (objetivo != null) mostrarPopupEvidencia(objetivo);
        }
    }

    private VBox construirFichaEvidencia(Caso caso, boolean cerrado) {
        VBox ficha = new VBox(10);
        ficha.setStyle(
                "-fx-background-color:#0f0f1a;" +
                        "-fx-border-color:" + (cerrado ? "#00d4ff30" : "#2a2a3c") + ";" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                        "-fx-padding:16 20 16 20;"
        );
        HBox cab = new HBox(10); cab.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("Caso #" + caso.getId() + "  ·  " + caso.getTipoAcoso());
        idLabel.setStyle("-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:#e0e0f0;");
        idLabel.setWrapText(true);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label estadoLabel = new Label(cerrado ? "✓ Cerrado" : "⏳ En curso");
        estadoLabel.setStyle(
                "-fx-font-size:11px;-fx-font-weight:600;" +
                        "-fx-text-fill:" + (cerrado ? "#00ff88" : "#ff9f1c") + ";"
        );
        cab.getChildren().addAll(idLabel, spacer, estadoLabel);

        Label descLabel = new Label(caso.getDescripcion());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#8080a0;");

        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#1e1e2e;");

        ficha.getChildren().addAll(cab, descLabel, sep);

        if (cerrado) {
            Label leyLabel = new Label(caso.getLeyColombia());
            leyLabel.setWrapText(true);
            leyLabel.setStyle("-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:#00d4ff;");
            Label penaLabel = new Label("Pena: " + caso.getPenaAplicable());
            penaLabel.setWrapText(true);
            penaLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6b6b8a;");
            ficha.getChildren().addAll(leyLabel, penaLabel);
        }
        
        Label lblClick = new Label("Haz clic para ver evidencia técnica");
        lblClick.setStyle("-fx-font-size: 19px; -fx-text-fill: #00d4ff90; -fx-font-weight: 600;");
        ficha.getChildren().add(lblClick);
        
        ficha.setStyle("-fx-background-color:#0f0f1a;-fx-border-color:" + (cerrado ? "#00d4ff30" : "#2a2a3c") + ";-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:16 20 16 20;-fx-cursor: hand;");
        ficha.setOnMouseClicked(e -> mostrarPopupEvidencia(caso));

        return ficha;
    }

    private void mostrarPopupEvidencia(Caso caso) {
        VBox overlayBg = new VBox();
        overlayBg.setAlignment(Pos.CENTER);
        overlayBg.setStyle("-fx-background-color: rgba(10, 10, 15, 0.95);");

        HBox forensicCard = construirEvidenciaEspecifica(caso.getId());

        VBox infoTarjeta = new VBox(8);
        infoTarjeta.setStyle("-fx-background-color:#0f0f1a;-fx-border-color:#2a2a3c;-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:16 20;");

        HBox cabecera = new HBox(12); cabecera.setAlignment(Pos.CENTER_LEFT);
        Circle ind = new Circle(8);
        ind.setFill(Color.web(caso.getGravedad() >= 7 ? "#ff0044" : (caso.getGravedad() >= 4 ? "#ff9f1c" : "#1a1a2a")));
        ind.setStroke(Color.web("#3a3a5c")); ind.setStrokeWidth(1.5);
        Label tit = new Label(caso.getTipoAcoso()); tit.getStyleClass().add("accion-titulo"); tit.setWrapText(true);
        cabecera.getChildren().addAll(ind, tit);

        Label desc = new Label(caso.getDescripcion());
        desc.setStyle("-fx-font-size:13px;-fx-text-fill:#8080a8;-fx-wrap-text:true;");

        infoTarjeta.getChildren().addAll(cabecera, desc);

        boolean cerrado = controller.getArbol().recorridoInorden().stream().anyMatch(c -> c.getId() == caso.getId());
        if (cerrado || (controller.getCasoActualFijo() != null && caso.getId() == controller.getCasoActualFijo().getId() && controller.todasLasEvidenciasReveladas())) {
            Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color:#1e1e2e;");
            Label ley = new Label(caso.getLeyColombia());
            ley.setStyle("-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:#00d4ff;"); ley.setWrapText(true);
            Label pena = new Label("Pena: " + caso.getPenaAplicable());
            pena.setStyle("-fx-font-size:12px;-fx-text-fill:#6b6b8a;"); pena.setWrapText(true);
            infoTarjeta.getChildren().addAll(sep, ley, pena);
        }

        Button btnCerrar = new Button("✕");
        btnCerrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4466; -fx-font-size: 26px; -fx-cursor: hand; -fx-padding: 0;");
        btnCerrar.setOnAction(e -> stackCentral.getChildren().remove(overlayBg));

        HBox topBar = new HBox(btnCerrar);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(0, 0, 8, 0));

        VBox content = new VBox(topBar, forensicCard, infoTarjeta);
        VBox.setMargin(infoTarjeta, new Insets(14, 0, 0, 0));
        content.setMaxWidth(680);
        content.setAlignment(Pos.CENTER);

        overlayBg.getChildren().add(content);
        stackCentral.getChildren().add(overlayBg);

        overlayBg.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), overlayBg);
        ft.setToValue(1); ft.play();
    }

    private HBox construirEvidenciaEspecifica(int casoId) {
        String usuario = obtenerUsuarioSospechoso();
        String dispositivo = obtenerDispositivoSospechoso();
        String ip = obtenerIPSospechoso();

        switch (casoId) {
            case 1:
                return construirEvidenciaConDatos("/images/evidencia_chat.png", "DATOS DEL CHAT CAPTURADO",
                        new String[][]{{"Usuario",usuario},{"Plataforma","Red social NetCity"},
                        {"Fecha","Hace 3 días, 10:47pm"},{"Dispositivo",dispositivo},
                        {"Mensajes","14 mensajes ofensivos"},{"Estado","Cuenta activa"}},
                        "Contenido censurado — solo datos técnicos visibles");
            case 2:
                return construirEvidenciaConDatos("/images/evidencia_post_falso.png", "METADATOS DE LA PUBLICACIÓN",
                        new String[][]{{"Publicado por",usuario},{"Fecha","Martes, 10:03pm"},
                        {"Plataforma","Foro NetCity"},{"Contenido","Información falsa [CENSURADO]"},
                        {"Alcance","847 vistas, 134 compartidos"},{"Estado","Reportado 3 veces"}},
                        "Contenido censurado por contener información falsa");
            case 3:
                return construirEvidenciaConDatos("/images/evidencia_perfil_falso.png", "DATOS FORENSES DEL PERFIL FALSO",
                        new String[][]{{"IP creación",ip},{"Dispositivo",dispositivo},
                        {"Fecha","Hace 5 días, 11:22pm"},{"Foto","Robada del perfil de Valeria"},
                        {"Publicaciones","8 mensajes ofensivos en 5 días"},{"Estado","Suspendida por reporte"}},
                        "Perfil suspendido — datos preservados para investigación");
            case 4:
                return construirEvidenciaConDatos("/images/evidencia_red_cuentas.png", "DASHBOARD FORENSE — ANÁLISIS DE CUENTAS",
                        new String[][]{{"IP común",ip},{"Dispositivo",dispositivo},
                        {"Cuentas","4 perfiles diferentes"},{"Horario","Ataques entre 9pm y 11pm"},
                        {"Patrón","Mismo estilo de escritura"},{"Última acción","Hace 2 horas"}},
                        "Datos extraídos del servidor de la plataforma");
            default:
                HBox h = new HBox(); h.getChildren().add(new Label("Evidencia para el caso " + casoId));
                return h;
        }
    }

    // ════════════════════════════════════════════════
    //  FASE 1 — ACCIONES (niveles 1-4)
    // ════════════════════════════════════════════════

    private void mostrarFaseAcciones() {
        if (nodosAccionesCache != null) {
            setCentral(nodosAccionesCache.toArray(new javafx.scene.Node[0]));
            return;
        }

        Label titulo = new Label(cyberdetective.data.NivelesData.getTituloNivel(nivel));
        titulo.getStyleClass().add("titulo-nivel");
        Label descripcion = new Label(cyberdetective.data.NivelesData.getDescripcionNivel(nivel));
        descripcion.getStyleClass().add("subtitulo"); descripcion.setWrapText(true);

        VBox dialogoInicial = construirDialogoInicial();
        VBox accionesVisuales = construirAccionesVisuales();

        btnSiguiente = new Button("Continuar al interrogatorio →");
        btnSiguiente.getStyleClass().add("btn-primario");
        btnSiguiente.setDisable(true);
        btnSiguiente.setOnAction(e -> {
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                btnSiguiente.setText("⏳ Esperando al oponente...");
                btnSiguiente.setDisable(true);
                mc.enviarListoParaInterrogatorio();
            } else {
                mostrarFasePreguntas();
            }
        });
        HBox btnBox = new HBox(btnSiguiente);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        nodosAccionesCache = java.util.Arrays.asList(titulo, descripcion, dialogoInicial, accionesVisuales, btnBox);
        setCentral(nodosAccionesCache.toArray(new javafx.scene.Node[0]));
    }

    private VBox construirDialogoInicial() {
        VBox dialogo = new VBox(12);
        dialogo.setStyle(
                "-fx-background-color:#0f0f1a;-fx-border-color:#1e1e2e;" +
                        "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;" +
                        "-fx-padding:20 24 20 24;"
        );
        VBox burbujaValeria = new VBox(4);
        burbujaValeria.getStyleClass().add("burbuja-valeria");
        Label quienV = new Label("VALERIA");
        quienV.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#9090d0;-fx-letter-spacing:0.1em;");
        Label textoV = new Label(getMensajeValeria(nivel));
        textoV.getStyleClass().add("texto-valeria"); textoV.setWrapText(true);
        burbujaValeria.getChildren().addAll(quienV, textoV);

        VBox burbujaAlex = new VBox(4);
        burbujaAlex.getStyleClass().add("burbuja-alex");
        VBox.setMargin(burbujaAlex, new Insets(8, 0, 0, 0));
        Label quienA = new Label("DET. ALEX");
        quienA.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#00d4ff;-fx-letter-spacing:0.1em;");
        Label textoA = new Label(getMensajeAlex(nivel));
        textoA.getStyleClass().add("texto-alex"); textoA.setWrapText(true);
        burbujaAlex.getChildren().addAll(quienA, textoA);

        dialogo.getChildren().addAll(burbujaValeria, burbujaAlex);
        return dialogo;
    }

    private VBox construirAccionesVisuales() {
        VBox contenedor = new VBox(16);
        Label etq = new Label("ACCIONES DE INVESTIGACIÓN");
        etq.getStyleClass().add("etiqueta-seccion");
        contenedor.getChildren().add(etq);
        
        tarjetasAcciones = new VBox[3];
        switch (nivel) {
            case 1 -> contenedor.getChildren().addAll(construirAccionesNivel1());
            case 2 -> contenedor.getChildren().addAll(construirAccionesNivel2());
            case 3 -> contenedor.getChildren().addAll(construirAccionesNivel3());
            case 4 -> contenedor.getChildren().addAll(construirAccionesNivel4());
        }
        return contenedor;
    }

    // ── Nivel 1 ─────────────────────────────────────

    private javafx.collections.ObservableList<javafx.scene.Node> construirAccionesNivel1() {
        var nodos = javafx.collections.FXCollections.<javafx.scene.Node>observableArrayList();
        String usuario = obtenerUsuarioSospechoso();
        String dispositivo = obtenerDispositivoSospechoso();

        VBox accion1 = crearTarjetaAccion("1. Recolectar captura de pantalla",
                "Debes resolver el desafío para recolectar esta evidencia.", 0);
        tarjetasAcciones[0] = accion1;
        HBox ev1 = construirEvidenciaConDatos("/images/evidencia_chat.png", "DATOS DEL CHAT CAPTURADO",
                new String[][]{{"Usuario",usuario},{"Plataforma","Red social NetCity"},
                        {"Fecha","Hace 3 días, 10:47pm"},{"Dispositivo",dispositivo},
                        {"Mensajes","14 mensajes ofensivos"},{"Estado","Cuenta activa"}},
                "Contenido censurado — solo datos técnicos visibles");
        
        Button btnDesafio = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio.getStyleClass().add("btn-primario");
        btnDesafio.setMaxWidth(Double.MAX_VALUE);
        btnDesafio.setOnAction(e -> {
            btnDesafio.setDisable(true);
            btnDesafio.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(0);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(0);
                mostrarMinijuegoPopupParaAccion(gameId, 0, ev1, accion1, btnDesafio, "Captura recolectada. Usuario: " + usuario, "Bien. La captura confirma al usuario.", false);
            }
        });
        
        // Guardar referencias para el modo multijugador
        mapaEvidencias.put(0, ev1);
        mapaAccionesVBox.put(0, accion1);
        mapaBtnDesafio.put(0, btnDesafio);
        mapaTextosCompletar.put(0, new String[]{"Captura recolectada. Usuario: " + usuario, "Bien. La captura confirma al usuario."});

        accion1.getChildren().add(btnDesafio); nodos.add(accion1);

        VBox accion2 = crearTarjetaAccion("2. Identificar el usuario agresor",
                "Según la tarjeta de datos, ¿cuál es el usuario agresor?", 1);
        tarjetasAcciones[1] = accion2;

        Button btnDesafio2 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio2.getStyleClass().add("btn-primario");
        btnDesafio2.setMaxWidth(Double.MAX_VALUE);
        btnDesafio2.setOnAction(e -> {
            btnDesafio2.setDisable(true);
            btnDesafio2.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(1);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(1);
                mostrarMinijuegoPopupParaAccion(gameId, 1, null, accion2, btnDesafio2, "Usuario confirmado: " + usuario, "Correcto. Ese usuario coincide con los datos de la captura.", false);
            }
        });

        // Guardar referencias para el modo multijugador
        mapaDatosExtra.put(1, usuario);
        mapaAccionesVBox.put(1, accion2);
        mapaBtnDesafio.put(1, btnDesafio2);
        mapaTextosCompletar.put(1, new String[]{"Usuario confirmado: " + usuario, "Correcto. Ese usuario coincide con los datos de la captura."});

        accion2.getChildren().add(btnDesafio2); nodos.add(accion2);

        VBox accion3 = crearTarjetaAccion("3. Clasificar el tipo de agresión",
                "Según los mensajes y la ley colombiana, ¿qué tipo de agresión es esta?", 2);
        tarjetasAcciones[2] = accion3;

        Button btnDesafio3 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio3.getStyleClass().add("btn-primario");
        btnDesafio3.setMaxWidth(Double.MAX_VALUE);
        btnDesafio3.setOnAction(e -> {
            btnDesafio3.setDisable(true);
            btnDesafio3.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(2);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(2);
                mostrarMinijuegoPopupParaAccion(gameId, 2, null, accion3, btnDesafio3, "Agresión: Injuria — Art. 220 C.P.", "Correcto. Los mensajes ofensivos reiterados son Injuria.", false);
            }
        });

        String datosExtra = "Injuria (Art. 220 C.P.):Mensajes que ofenden el honor y afectan el buen nombre|" +
                            "Amenaza (Art. 347 C.P.):Mensajes que generan miedo o anuncian un mal futuro|" +
                            "Extorsión (Art. 244 C.P.):Exigencia de algo a cambio de no causar daño|" +
                            "Acoso laboral (Ley 1010):Conducta persistente en entorno de trabajo";

        mapaDatosExtra.put(2, datosExtra);
        mapaAccionesVBox.put(2, accion3);
        mapaBtnDesafio.put(2, btnDesafio3);
        mapaTextosCompletar.put(2, new String[]{"Agresión: Injuria — Art. 220 C.P.", "Correcto. Los mensajes ofensivos reiterados son Injuria."});

        accion3.getChildren().add(btnDesafio3); nodos.add(accion3);
        return nodos;
    }

    // ── Nivel 2 ─────────────────────────────────────

    private javafx.collections.ObservableList<javafx.scene.Node> construirAccionesNivel2() {
        var nodos = javafx.collections.FXCollections.<javafx.scene.Node>observableArrayList();
        String usuario = obtenerUsuarioSospechoso();

        VBox accion1 = crearTarjetaAccion("1. Identificar la publicación original",
                "Debes resolver el desafío para identificar la publicación.", 0);
        tarjetasAcciones[0] = accion1;
        HBox evPost = construirEvidenciaConDatos("/images/evidencia_post_falso.png",
                "METADATOS DE LA PUBLICACIÓN",
                new String[][]{{"Publicado por",usuario},{"Fecha","Martes, 10:03pm"},
                        {"Plataforma","Foro NetCity"},{"Contenido","Información falsa [CENSURADO]"},
                        {"Alcance","847 vistas, 134 compartidos"},{"Estado","Reportado 3 veces"}},
                "Contenido censurado por contener información falsa");

        Button btnDesafio = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio.getStyleClass().add("btn-primario");
        btnDesafio.setMaxWidth(Double.MAX_VALUE);
        btnDesafio.setOnAction(e -> {
            btnDesafio.setDisable(true);
            btnDesafio.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(0);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(0);
                mostrarMinijuegoPopupParaAccion(gameId, 0, evPost, accion1, btnDesafio, "Publicación identificada. Autor: " + usuario, "Esa es la publicación que inició todo. Fíjate en quién la publicó.", false);
            }
        });

        // Guardar referencias para el modo multijugador
        mapaEvidencias.put(0, evPost);
        mapaAccionesVBox.put(0, accion1);
        mapaBtnDesafio.put(0, btnDesafio);
        mapaTextosCompletar.put(0, new String[]{"Publicación identificada. Autor: " + usuario, "Esa es la publicación que inició todo. Fíjate en quién la publicó."});

        accion1.getChildren().add(btnDesafio); nodos.add(accion1);

        VBox accion2 = crearTarjetaAccion("2. Rastrear quién inició el rumor",
                "Debes resolver el desafío para confirmar el autor del rumor.", 1);
        tarjetasAcciones[1] = accion2;

        Button btnDesafio2 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio2.getStyleClass().add("btn-primario");
        btnDesafio2.setMaxWidth(Double.MAX_VALUE);
        btnDesafio2.setOnAction(e -> {
            btnDesafio2.setDisable(true);
            btnDesafio2.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(1);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(1);
                mostrarMinijuegoPopupParaAccion(gameId, 1, null, accion2, btnDesafio2, "Origen rastreado: " + usuario, "Confirmado. Los metadatos apuntan a esa cuenta.", false);
            }
        });

        // Guardar referencias para el modo multijugador
        mapaDatosExtra.put(1, usuario);
        mapaAccionesVBox.put(1, accion2);
        mapaBtnDesafio.put(1, btnDesafio2);
        mapaTextosCompletar.put(1, new String[]{"Origen rastreado: " + usuario, "Confirmado. Los metadatos apuntan a esa cuenta."});

        accion2.getChildren().add(btnDesafio2); nodos.add(accion2);

        VBox accion3 = crearTarjetaAccion("3. Clasificar el tipo de agresión",
                "Según los mensajes y la ley colombiana, ¿qué tipo de agresión es esta?", 2);
        tarjetasAcciones[2] = accion3;

        Button btnDesafio3 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio3.getStyleClass().add("btn-primario");
        btnDesafio3.setMaxWidth(Double.MAX_VALUE);
        btnDesafio3.setOnAction(e -> {
            btnDesafio3.setDisable(true);
            btnDesafio3.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(2);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(2);
                mostrarMinijuegoPopupParaAccion(gameId, 2, null, accion3, btnDesafio3, "Agresión: Calumnia — Art. 221 C.P.", "Correcto. Imputar un delito falso a alguien es Calumnia.", false);
            }
        });

        String datosExtra3 = "Calumnia (Art. 221 C.P.):Imputar falsamente a otro la comisión de un delito|" +
                            "Injuria (Art. 220 C.P.):Mensajes que ofenden el honor y afectan el buen nombre|" +
                            "Amenaza (Art. 347 C.P.):Mensajes que generan miedo o anuncian un mal futuro|" +
                            "Acoso (Ley 1010):Conducta persistente de hostigamiento";

        mapaDatosExtra.put(2, datosExtra3);
        mapaAccionesVBox.put(2, accion3);
        mapaBtnDesafio.put(2, btnDesafio3);
        mapaTextosCompletar.put(2, new String[]{"Agresión: Calumnia — Art. 221 C.P.", "Correcto. Imputar un delito falso a alguien es Calumnia."});

        accion3.getChildren().add(btnDesafio3); nodos.add(accion3);
        return nodos;
    }

    // ── Nivel 3 ─────────────────────────────────────

    private javafx.collections.ObservableList<javafx.scene.Node> construirAccionesNivel3() {
        var nodos = javafx.collections.FXCollections.<javafx.scene.Node>observableArrayList();
        String ip = obtenerIPSospechoso();
        String dispositivo = obtenerDispositivoSospechoso();

        // 1. Analizar la información del perfil falso (Escombros)
        VBox accion1 = crearTarjetaAccion("1. Analizar la información del perfil falso",
                "Debes resolver el desafío para analizar los datos del perfil.", 0);
        tarjetasAcciones[0] = accion1;
        HBox evPerfil = construirEvidenciaConDatos("/images/evidencia_perfil_falso.png",
                "DATOS FORENSES DEL PERFIL FALSO",
                new String[][]{{"IP creación",ip},{"Dispositivo",dispositivo},
                        {"Fecha","Hace 5 días, 11:22pm"},{"Foto","Robada del perfil de Valeria"},
                        {"Publicaciones","8 mensajes ofensivos en 5 días"},{"Estado","Suspendida por reporte"}},
                "Perfil suspendido — datos preservados para investigación");
        
        Button btnDesafio1 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio1.getStyleClass().add("btn-primario");
        btnDesafio1.setMaxWidth(Double.MAX_VALUE);
        btnDesafio1.setOnAction(e -> {
            btnDesafio1.setDisable(true);
            btnDesafio1.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(0);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(0);
                mostrarMinijuegoPopupParaAccion(gameId, 0, evPerfil, accion1, btnDesafio1, "Perfil analizado. IP: " + ip, "Todos los indicadores apuntan a suplantación. La IP es la clave.", false);
            }
        });
        
        mapaEvidencias.put(0, evPerfil);
        mapaAccionesVBox.put(0, accion1);
        mapaBtnDesafio.put(0, btnDesafio1);
        mapaTextosCompletar.put(0, new String[]{"Perfil analizado. IP: " + ip, "Todos los indicadores apuntan a suplantación. La IP es la clave."});
        accion1.getChildren().add(btnDesafio1); nodos.add(accion1);

        // 2. Rastrear la dirección IP de creación (Sopa de letras)
        VBox accion2 = crearTarjetaAccion("2. Rastrear la dirección IP de creación",
                "Debes resolver el desafío para confirmar la IP de origen.", 1);
        tarjetasAcciones[1] = accion2;

        Button btnDesafio2 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio2.getStyleClass().add("btn-primario");
        btnDesafio2.setMaxWidth(Double.MAX_VALUE);
        btnDesafio2.setOnAction(e -> {
            btnDesafio2.setDisable(true);
            btnDesafio2.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(1);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(1);
                mostrarMinijuegoPopupParaAccion(gameId, 1, null, accion2, btnDesafio2, "IP rastreada: " + ip, "Esa IP ya la vimos antes. Confirma quién creó la cuenta.", false);
            }
        });

        mapaDatosExtra.put(1, ip);
        mapaAccionesVBox.put(1, accion2);
        mapaBtnDesafio.put(1, btnDesafio2);
        mapaTextosCompletar.put(1, new String[]{"IP rastreada: " + ip, "Esa IP ya la vimos antes. Confirma quién creó la cuenta."});
        accion2.getChildren().add(btnDesafio2); nodos.add(accion2);

        // 3. Identificar quién está detrás de la suplantación (Conexiones)
        VBox accion3 = crearTarjetaAccion("3. Identificar quién está detrás de la suplantación",
                "Conecta la IP con el sospechoso correspondiente.", 2);
        tarjetasAcciones[2] = accion3;

        Button btnDesafio3 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio3.getStyleClass().add("btn-primario");
        btnDesafio3.setMaxWidth(Double.MAX_VALUE);
        btnDesafio3.setOnAction(e -> {
            btnDesafio3.setDisable(true);
            btnDesafio3.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(2);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(2);
                mostrarMinijuegoPopupParaAccion(gameId, 2, null, accion3, btnDesafio3, "Sospechoso: " + controller.getSospechoso(), "Confirmado. IP y dispositivo apuntan a esta persona.", false);
            }
        });

        String sospReal = controller.getSospechoso();
        String[] sospechosos = getSospechososOrdenados();
        int realIdx = obtenerIndiceSospechoso();
        String[] ips = {"192.168.100.22", ip, "10.10.10.1", "172.20.0.55"};

        String dataConexion = String.format("%s:%s|*%s:%s|%s:%s|%s:%s",
                ips[0], sospechosos[(realIdx+1)%4],
                ip, sospReal,
                ips[2], sospechosos[(realIdx+2)%4],
                ips[3], sospechosos[(realIdx+3)%4]);

        mapaDatosExtra.put(2, dataConexion);
        mapaAccionesVBox.put(2, accion3);
        mapaBtnDesafio.put(2, btnDesafio3);
        mapaTextosCompletar.put(2, new String[]{"Sospechoso: " + sospReal, "Confirmado. IP y dispositivo apuntan a esta persona."});

        accion3.getChildren().add(btnDesafio3); nodos.add(accion3);
        return nodos;
    }

    // ── Nivel 4 ─────────────────────────────────────

    private javafx.collections.ObservableList<javafx.scene.Node> construirAccionesNivel4() {
        var nodos = javafx.collections.FXCollections.<javafx.scene.Node>observableArrayList();
        String ip = obtenerIPSospechoso();
        String dispositivo = obtenerDispositivoSospechoso();

        // 1. Identificar cuentas de la misma persona (Escombros)
        VBox accion1 = crearTarjetaAccion("1. Identificar cuentas de la misma persona",
                "Debes resolver el desafío para analizar el dashboard forense.", 0);
        tarjetasAcciones[0] = accion1;
        HBox evRed = construirEvidenciaConDatos("/images/evidencia_red_cuentas.png",
                "DASHBOARD FORENSE — ANÁLISIS DE CUENTAS",
                new String[][]{{"IP común",ip},{"Dispositivo",dispositivo},
                        {"Cuentas","4 perfiles diferentes"},{"Horario","Ataques entre 9pm y 11pm"},
                        {"Patrón","Mismo estilo de escritura"},{"Última acción","Hace 2 horas"}},
                "Datos extraídos del servidor de la plataforma");
        
        Button btnDesafio1 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio1.getStyleClass().add("btn-primario");
        btnDesafio1.setMaxWidth(Double.MAX_VALUE);
        btnDesafio1.setOnAction(e -> {
            btnDesafio1.setDisable(true);
            btnDesafio1.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(0);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(0);
                mostrarMinijuegoPopupParaAccion(gameId, 0, evRed, accion1, btnDesafio1, "4 cuentas vinculadas a la IP: " + ip, "Una sola IP detrás de 4 cuentas. Misma persona.", false);
            }
        });

        mapaEvidencias.put(0, evRed);
        mapaAccionesVBox.put(0, accion1);
        mapaBtnDesafio.put(0, btnDesafio1);
        mapaTextosCompletar.put(0, new String[]{"4 cuentas vinculadas a la IP: " + ip, "Una sola IP detrás de 4 cuentas. Misma persona."});
        accion1.getChildren().add(btnDesafio1); nodos.add(accion1);

        // 2. Encontrar patrones de comportamiento (Sincronización multijugador)
        VBox accion2 = crearTarjetaAccion("2. Encontrar patrones de comportamiento",
                "¿Cuál de estos patrones confirma que las 4 cuentas son de la misma persona?", 1);
        tarjetasAcciones[1] = accion2;
        String[] patrones = {
                "Misma IP, mismo dispositivo y horarios idénticos de actividad",
                "Todas las cuentas tienen el mismo número de seguidores",
                "Las cuentas publican el mismo tipo de contenido",
                "Las cuentas fueron creadas en el mismo año"
        };
        VBox opcionesP = new VBox(8);
        mapaOpcionesVBox.put(1, opcionesP); // Para sincronizar multijugador
        
        for (int i = 0; i < patrones.length; i++) {
            final int finalI = i;
            final boolean ok = i == 0;
            Button btn = new Button(patrones[i]);
            btn.getStyleClass().add("btn-opcion"); btn.setMaxWidth(Double.MAX_VALUE); btn.setWrapText(true);
            btn.setOnAction(e -> {
                procesarAccionBoton(1, ok, opcionesP, btn,
                    () -> {
                        completarAccion(1, accion2, "Patrón: misma IP, dispositivo y horarios.",
                                "Exacto. Ese patrón forense es la prueba más sólida.", true);
                        opcionesP.getChildren().forEach(n -> { if (n instanceof Button b) b.setDisable(true); });
                        btn.getStyleClass().add("btn-opcion-correcto");
                    },
                    (tipo) -> {
                        completarAccion(1, accion2, "Patrón: misma IP, dispositivo y horarios.",
                                "El otro detective identificó el patrón primero.", false);
                        opcionesP.getChildren().forEach(n -> { if (n instanceof Button b) b.setDisable(true); });
                    },
                    "Ese patrón no es evidencia forense suficiente."
                );
            });
            opcionesP.getChildren().add(btn);
        }
        accion2.getChildren().add(opcionesP); nodos.add(accion2);

        // 3. Identificar al responsable principal (Conexiones)
        VBox accion3 = crearTarjetaAccion("3. Identificar al responsable principal",
                "Conecta la IP con el sospechoso correspondiente.", 2);
        tarjetasAcciones[2] = accion3;

        Button btnDesafio3 = new Button("⚔  INICIAR DESAFÍO");
        btnDesafio3.getStyleClass().add("btn-primario");
        btnDesafio3.setMaxWidth(Double.MAX_VALUE);
        btnDesafio3.setOnAction(e -> {
            btnDesafio3.setDisable(true);
            btnDesafio3.setText("⏳ Esperando al oponente...");
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.enviarListoParaMinijuego(2);
            } else {
                int gameId = cyberdetective.minijuego.GestorMinijuegos.seleccionarIdAleatorio(2);
                mostrarMinijuegoPopupParaAccion(gameId, 2, null, accion3, btnDesafio3, "Responsable: " + controller.getSospechoso(), "Tenemos evidencias para presentar todos los cargos.", false);
            }
        });

        String sospReal = controller.getSospechoso();
        String[] sospechosos = getSospechososOrdenados();
        int realIdx = obtenerIndiceSospechoso();
        String[] ips = {"192.168.100.22", ip, "10.10.10.1", "172.20.0.55"};

        String dataConexion = String.format("%s:%s|*%s:%s|%s:%s|%s:%s",
                ips[0], sospechosos[(realIdx+1)%4],
                ip, sospReal,
                ips[2], sospechosos[(realIdx+2)%4],
                ips[3], sospechosos[(realIdx+3)%4]);

        mapaDatosExtra.put(2, dataConexion);
        mapaAccionesVBox.put(2, accion3);
        mapaBtnDesafio.put(2, btnDesafio3);
        mapaTextosCompletar.put(2, new String[]{"Responsable: " + sospReal, "Tenemos evidencias para presentar todos los cargos."});

        accion3.getChildren().add(btnDesafio3); nodos.add(accion3);
        return nodos;
    }

    // ════════════════════════════════════════════════
    //  COMPLETAR ACCIÓN
    // ════════════════════════════════════════════════

    private void completarAccion(int indice, VBox tarjeta, String logMsg, String mensajeAlex, boolean notifyServer) {
        completarAccionConTipo(indice, tarjeta, logMsg, mensajeAlex, 0, notifyServer);
    }

    private void completarAccionConTipo(int indice, VBox tarjeta, String logMsg, String mensajeAlex, int tipoResultado, boolean notifyServer) {
        if (accionesCompletadas[indice]) return;
        accionesCompletadas[indice] = true;

        tarjeta.getChildren().forEach(n -> {
            if (n instanceof HBox hb) hb.getChildren().forEach(c -> {
                if (c instanceof Circle circ) {
                    if (tipoResultado == 2) {
                        circ.setFill(Color.web("#ff4466")); circ.setStroke(Color.web("#ff446660"));
                    } else {
                        circ.setFill(Color.web("#00ff88")); circ.setStroke(Color.web("#00ff8860"));
                    }
                }
            });
            // Deshabilitar botones o cualquier nodo interactivo
            n.setDisable(true);
            if (tipoResultado != 0) {
                n.setOpacity(0.5);
            }
        });
        tarjeta.setMouseTransparent(true);
        tarjeta.getStyleClass().remove("accion-card");
        
        if (tipoResultado == 0) {
            Label completadoLabel = new Label("✓  Completado");
            tarjeta.getStyleClass().add("accion-card-completada");
            completadoLabel.getStyleClass().add("accion-completada-label");
            tarjeta.getChildren().add(completadoLabel);
        } else if (tipoResultado == 1) {
            tarjeta.setOpacity(0.5);
            tarjeta.setStyle("-fx-border-color: #3a3a5c; -fx-background-color: #0d0d16; " +
                            "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
            
            Label lbl = new Label("OBJETIVO COMPLETADO POR TU COMPAÑERO");
            lbl.setStyle("-fx-text-fill: #00d4ff; -fx-font-family: 'DM Mono'; -fx-font-size: 17px; -fx-font-weight: bold;");
            
            HBox box = new HBox(8, new Label("👤"), lbl);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(0, 212, 255, 0.1); -fx-background-radius: 6;");
            
            tarjeta.getChildren().add(box);
        } else if (tipoResultado == 2) {
            tarjeta.setOpacity(0.5);
            tarjeta.setStyle("-fx-border-color: #3a3a5c; -fx-background-color: #0d0d16; " +
                            "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
            
            Label lbl = new Label("NINGUNO RESPONDIÓ CORRECTAMENTE");
            lbl.setStyle("-fx-text-fill: #ff4466; -fx-font-family: 'DM Mono'; -fx-font-size: 17px; -fx-font-weight: bold;");
            
            HBox box = new HBox(8, new Label("✗"), lbl);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(255, 68, 102, 0.1); -fx-background-radius: 6;");
            
            tarjeta.getChildren().add(box);
        }

        actualizarChecklist(indice);
        labelMensajeAlex.setText(mensajeAlex);
        controller.revelarSiguienteEvidencia();

        javafx.scene.Node lastAdded = tarjeta.getChildren().get(tarjeta.getChildren().size() - 1);
        FadeTransition ft = new FadeTransition(Duration.millis(300), lastAdded);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        boolean todasCompletas = true;
        for (boolean b : accionesCompletadas) if (!b) { todasCompletas = false; break; }

        if (todasCompletas) {
            PauseTransition pausa = new PauseTransition(Duration.millis(500));
            pausa.setOnFinished(e -> {
                btnSiguiente.setDisable(false);
                labelMensajeAlex.setText("Excelente. Fase de campo completa. Continúa al interrogatorio.");
                ScaleTransition st = new ScaleTransition(Duration.millis(200), btnSiguiente);
                st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.04); st.setToY(1.04);
                st.setAutoReverse(true); st.setCycleCount(4); st.play();
            });
            pausa.play();
        }

        // RED: Notificar al servidor si somos nosotros quienes completamos la acción
        if (notifyServer && controller instanceof cyberdetective.controller.MultiplayerJuegoController multi) {
            multi.notificarAccionInvestigacion(indice, logMsg);
        }
    }

    @Override
    public void onAccionSincronizada(int accionIdx, String data) {
        javafx.application.Platform.runLater(() -> {
            if (accionIdx >= 0 && accionIdx < tarjetasAcciones.length) {
                VBox tarjeta = tarjetasAcciones[accionIdx];
                if (tarjeta != null) {
                    marcarAccionOponente(accionIdx, tarjeta);
                }
            }
        });
    }

    private void marcarAccionOponente(int index, VBox tarjeta) {
        if (accionesCompletadas[index]) return;
        accionesCompletadas[index] = true;

        // Estilo bloqueado/opaco pero preservando contenido
        tarjeta.getStyleClass().remove("accion-completada");
        tarjeta.setMouseTransparent(true);
        tarjeta.setOpacity(0.5);
        tarjeta.setStyle("-fx-border-color: #3a3a5c; -fx-background-color: #0d0d16; " +
                        "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        // NO quitamos botones ni HBox, solo los deshabilitamos todos
        tarjeta.getChildren().forEach(n -> n.setDisable(true));

        Label lbl = new Label("OBJETIVO COMPLETADO POR TU COMPAÑERO");
        lbl.setStyle("-fx-text-fill: #00d4ff; -fx-font-family: 'DM Mono'; -fx-font-size: 17px; -fx-font-weight: bold;");
        
        HBox box = new HBox(8, new Label("👤"), lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 8 12; -fx-background-color: rgba(0, 212, 255, 0.1); -fx-background-radius: 6;");
        
        // Lo añadimos al final sin borrar lo anterior
        tarjeta.getChildren().add(box);
        actualizarProgreso();
    }

    private void actualizarProgreso() {
        int completadas = 0;
        if (accionesCompletadas != null) {
            for (boolean b : accionesCompletadas) if (b) completadas++;
            double porcentaje = (double) completadas / accionesCompletadas.length;
            if (barraProgreso != null) barraProgreso.setProgress(porcentaje);
            if (labelProgreso != null) labelProgreso.setText("Objetivos: " + completadas + " / " + accionesCompletadas.length);
            
            if (completadas == accionesCompletadas.length && btnSiguiente != null) {
                btnSiguiente.setDisable(false);
            }
        }
    }

    private void actualizarChecklist(int indice) {
        if (panelAcciones != null && indice >= 0 && indice < panelAcciones.getChildren().size()) {
            javafx.scene.Node fila = panelAcciones.getChildren().get(indice);
            if (fila instanceof HBox hb) {
                hb.getChildren().forEach(n -> {
                    if (n instanceof javafx.scene.shape.Circle c) {
                        c.setFill(javafx.scene.paint.Color.web("#00ff88"));
                    }
                    if (n instanceof Label l) {
                        l.setStyle("-fx-font-size:12px;-fx-text-fill:#00ff88; -fx-strikethrough: true;");
                    }
                });
            }
        }
    }

    @Override public void onNivelCompletado(int n, cyberdetective.model.Caso c) {}
    @Override public void onArbolActualizado() {
        if (visualizadorArbol != null) visualizadorArbol.actualizar(controller.getArbol());
    }
    @Override public void onJuegoTerminado(String r) {}
    @Override public void onPuntajeActualizado(int p) {
        javafx.application.Platform.runLater(() -> {
            if (labelPuntaje != null) labelPuntaje.setText(String.valueOf(p));
        });
    }
    @Override public void onPuntajeOponenteActualizado(int p) {
        javafx.application.Platform.runLater(() -> {
            if (labelPuntajeOponente != null) labelPuntajeOponente.setText(String.valueOf(p));
        });
    }
    @Override public void onInterrogatorioListo() {
        javafx.application.Platform.runLater(() -> mostrarFasePreguntas());
    }
    @Override public void onNivel5CronologiaListo() {
        javafx.application.Platform.runLater(() -> mostrarAnalisisNodos());
    }
    @Override public void onNivel5ReporteFinalListo() {
        javafx.application.Platform.runLater(() -> mostrarReporteFinal());
    }
    @Override public void onMensajeDetective(String m) {
        javafx.application.Platform.runLater(() -> {
            if (labelMensajeAlex != null) labelMensajeAlex.setText(m);
        });
    }
    @Override public void onFaseCambiada(cyberdetective.controller.JuegoController.FaseNivel f) {}
    @Override public void onEvidenciaRevelada(String e, int tr, int t) {
        javafx.application.Platform.runLater(() -> {
            barraProgreso.setProgress((double)tr / t);
            labelProgreso.setText("Evidencias: " + tr + " / " + t);
        });
    }
    @Override
    public void onRespuestaRecibida(int opcion, boolean correcta, String jugador) {
        // Capturamos los datos de la pregunta AQUÍ (síncronamente), ANTES del runLater.
        // En este punto preguntaActual aún NO ha sido incrementado.
        String[][] preguntas = controller.getPreguntasNivelActual();
        int qIdx = controller.getPreguntaActual();
        // Puede que ya se incrementó si el listener se llamó después del ++; tomamos el anterior
        int safeIdx = (qIdx > 0 && qIdx >= preguntas.length) ? preguntas.length - 1 : qIdx;
        String[] dataPregunta = preguntas[safeIdx];
        int correctaIdx = Integer.parseInt(dataPregunta[dataPregunta.length - 1]);
        String textoCorrecta = dataPregunta[correctaIdx + 1];

        javafx.application.Platform.runLater(() -> {
            if (opcionesBoxInterrogatorio == null) return;

            // Deshabilitar todos los botones y marcar resultado
            for (int i = 0; i < opcionesBoxInterrogatorio.getChildren().size(); i++) {
                javafx.scene.Node n = opcionesBoxInterrogatorio.getChildren().get(i);
                if (n instanceof Button btn) {
                    btn.setDisable(true);
                    if (i == correctaIdx) {
                        btn.getStyleClass().removeAll("btn-opcion-incorrecto");
                        btn.getStyleClass().add("btn-opcion-correcto");
                    } else if (i == opcion && !correcta) {
                        btn.getStyleClass().add("btn-opcion-incorrecto");
                    }
                }
            }

            if (correcta) {
                labelMensajeAlex.setText("✓ ¡Correcto! +50 puntos.");
                labelMensajeAlex.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
            } else {
                labelMensajeAlex.setText("✗ Incorrecto. −25 puntos. La respuesta era: " + textoCorrecta);
                labelMensajeAlex.setStyle("-fx-text-fill: #ff4466; -fx-font-weight: bold;");
            }

            PauseTransition pausa = new PauseTransition(Duration.millis(2500));
            pausa.setOnFinished(e -> {
                labelMensajeAlex.setStyle("");
                mostrarSiguientePregunta();
            });
            pausa.play();
        });
    }

    @Override
    public void onRespuestaIncorrecta(String jugador, int opcion, boolean soyYo) {
        javafx.application.Platform.runLater(() -> {
            if (opcionesBoxInterrogatorio != null) {
                if (soyYo) {
                    if (opcion >= 0 && opcion < opcionesBoxInterrogatorio.getChildren().size()) {
                        javafx.scene.Node n = opcionesBoxInterrogatorio.getChildren().get(opcion);
                        if (n instanceof Button btn) {
                            btn.getStyleClass().add("btn-opcion-incorrecto");
                        }
                    }
                    labelMensajeAlex.setText("✗ Respuesta incorrecta. Esperando al oponente...");
                    labelMensajeAlex.setStyle("-fx-text-fill: #ff4466;");
                } else {
                    labelMensajeAlex.setText("⚡ ¡EL DETECTIVE " + jugador.toUpperCase() + " FALLÓ! TE TOCA.");
                    labelMensajeAlex.setStyle("-fx-text-fill: #00d4ff; -fx-font-weight: bold;");
                }
            }
        });
    }

    @Override
    public void onAmbosFallaron(int correcta) {
        javafx.application.Platform.runLater(() -> {
            if (opcionesBoxInterrogatorio != null) {
                for (int i = 0; i < opcionesBoxInterrogatorio.getChildren().size(); i++) {
                    javafx.scene.Node n = opcionesBoxInterrogatorio.getChildren().get(i);
                    if (n instanceof Button btn) {
                        btn.setDisable(true);
                        if (i == correcta) btn.getStyleClass().add("btn-opcion-correcto");
                    }
                }
                labelMensajeAlex.setText("✗ Ambos fallaron. La respuesta correcta era la resaltada.");
                labelMensajeAlex.setStyle("-fx-text-fill: #ff4466; -fx-font-weight: bold;");
                
                PauseTransition pausa = new PauseTransition(Duration.millis(3000));
                pausa.setOnFinished(e -> {
                    labelMensajeAlex.setStyle(""); 
                    mostrarSiguientePregunta();
                });
                pausa.play();
            }
        });
    }

    @Override
    public void onRespuestaOponente(String oponente, int opcion, boolean correcta) {
        javafx.application.Platform.runLater(() -> {
            if (opcionesBoxInterrogatorio != null) {
                if (correcta) {
                    // Si el oponente acertó, bloqueamos todo, mostramos mensaje y avanzamos
                    for (javafx.scene.Node n : opcionesBoxInterrogatorio.getChildren()) {
                        if (n instanceof Button b) {
                            b.setDisable(true);
                            b.setOpacity(0.3);
                        }
                    }
                    labelMensajeAlex.setText("⚡ ¡EL DETECTIVE " + oponente.toUpperCase() + " SE ADELANTÓ Y ACERTÓ!");
                    labelMensajeAlex.setStyle("-fx-text-fill: #ff9f1c; -fx-font-weight: bold;");
                    
                    PauseTransition pausa = new PauseTransition(Duration.millis(1500));
                    pausa.setOnFinished(e -> {
                        labelMensajeAlex.setStyle(""); 
                        mostrarSiguientePregunta();
                    });
                    pausa.play();
                } else {
                    // Rebote: El oponente falló, avisamos al jugador local
                    labelMensajeAlex.setText("⚡ ¡" + oponente.toUpperCase() + " FALLÓ! ES TU TURNO");
                    labelMensajeAlex.setStyle("-fx-text-fill: #00d4ff; -fx-font-weight: bold;");
                    
                    // Aseguramos que nuestros botones (los que no hemos pulsado) estén activos
                    for (javafx.scene.Node n : opcionesBoxInterrogatorio.getChildren()) {
                        if (n instanceof Button b && !b.getStyleClass().contains("btn-opcion-incorrecto")) {
                            b.setDisable(false);
                            b.setOpacity(1.0);
                        }
                    }
                }
            }
        });
    }

    @Override public void onNivelIniciado() {}


    // ════════════════════════════════════════════════
    //  FASE 2 — PREGUNTAS (niveles 1-4)
    // ════════════════════════════════════════════════

    private void mostrarFasePreguntas() {
        if (!controller.pasarAInterrogatorio()) return;
        mostrarSiguientePregunta();
    }

    private void mostrarSiguientePregunta() {
        if (!controller.hayMasPreguntas()) { mostrarFaseReporte(); return; }

        String[][] preguntas = controller.getPreguntasNivelActual();
        int idx = controller.getPreguntaActual();
        String[] pregunta = preguntas[idx];
        int totalOpciones = pregunta.length - 2;

        Label etqFase = new Label("INTERROGATORIO — FASE 2 DE 3");
        etqFase.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#00d4ff;-fx-letter-spacing:0.12em;");

        Label numP = new Label("PREGUNTA " + (idx+1) + " DE " + preguntas.length);
        numP.setStyle("-fx-font-size:10px;-fx-font-weight:600;-fx-text-fill:#3a3a5c;-fx-letter-spacing:0.1em;");

        Label enunciado = new Label(pregunta[0]); enunciado.setWrapText(true);
        enunciado.setStyle("-fx-font-size:15px;-fx-font-weight:500;-fx-text-fill:#e0e0f0;-fx-line-spacing:4;");
        VBox.setMargin(enunciado, new Insets(8, 0, 12, 0));

        opcionesBoxInterrogatorio = new VBox(10);
        for (int i = 1; i <= totalOpciones; i++) {
            final int oi = i - 1;
            Button btn = new Button(pregunta[i]);
            btn.getStyleClass().add("btn-opcion"); btn.setMaxWidth(Double.MAX_VALUE); btn.setWrapText(true);
            btn.setOnAction(e -> evaluarRespuesta(oi, opcionesBoxInterrogatorio, pregunta));
            opcionesBoxInterrogatorio.getChildren().add(btn);
        }

        VBox contenedor = new VBox(12);
        contenedor.setStyle("-fx-background-color:#0f0f1a;-fx-border-color:#1e1e2e;" +
                "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:24 28 24 28;");
        contenedor.getChildren().addAll(numP, enunciado, opcionesBoxInterrogatorio);
        setCentral(etqFase, contenedor);
    }

    private java.util.Map<Integer, javafx.scene.Node> mapaEvidencias = new java.util.HashMap<>();
    private java.util.Map<Integer, VBox> mapaAccionesVBox = new java.util.HashMap<>();
    private java.util.Map<Integer, VBox> mapaOpcionesVBox = new java.util.HashMap<>();
    private java.util.Map<Integer, Button> mapaBtnDesafio = new java.util.HashMap<>();
    private java.util.Map<Integer, String[]> mapaTextosCompletar = new java.util.HashMap<>();
    private java.util.Map<Integer, String> mapaDatosExtra = new java.util.HashMap<>();
    private StackPane minijuegoOverlayActivo = null;

    private String nombreDesafio(int id) {
        switch (id) {
            case 1: return "Escombros";
            case 2: return "Escombros II";
            case 3: return "Escombros III";
            case 4: case 5: case 6: return "Sopa de Letras";
            case 7: case 8: case 9: return "Conexión";
            default: return "Desafío " + id;
        }
    }

    private String instruccionDesafio(int id) {
        switch (id) {
            case 1: case 2: case 3:
                return "Arrastra los escombros fuera del centro para descubrir la evidencia oculta. " +
                       "¡Haz clic en la evidencia cuando quede despejada!";
            case 4: case 5: case 6:
                return "Encuentra la palabra oculta seleccionando las letras correctas.";
            case 7: case 8: case 9:
                return "Conecta cada concepto con su definición correcta haciendo clic en ambos.";
            default:
                return "Resuelve el desafío para responder la pregunta.";
        }
    }

    /** Abre el popup del minijuego para la fase de acciones. */
    private void mostrarMinijuegoPopupParaAccion(int gameId, int accionIdx, javafx.scene.Node evidenciaVirtual, VBox accionCard, Button btnDesafio, String txtTitulo, String txtSub, boolean multiplayer) {
        cyberdetective.minijuego.Minijuego juego = cyberdetective.minijuego.GestorMinijuegos.crearMinijuego(gameId);
        juego.setEvidenciaNode(evidenciaVirtual);
        if (mapaDatosExtra.containsKey(accionIdx)) {
            juego.setDatosExtra(mapaDatosExtra.get(accionIdx));
        }

        if (nivel == 3 && juego instanceof cyberdetective.minijuego.SopaLetrasMinijuego sopa) {
            sopa.setModoIP(true);
        }

        juego.setOnVerEvidenciaRequest(() -> {
            Caso c = controller.getCasoActualFijo();
            if (c != null) {
                mostrarPopupEvidencia(c);
            }
        });

        // — Título —
        Label titulo = new Label("🎮  DESAFÍo: " + nombreDesafio(gameId));
        titulo.setStyle("-fx-text-fill:#00d4ff;-fx-font-size:14px;-fx-font-weight:700;");

        Label instruccion = new Label(instruccionDesafio(gameId));
        instruccion.setStyle("-fx-text-fill:#9090b0;-fx-font-size:12px;");
        instruccion.setWrapText(true);

        VBox header = new VBox(4, titulo, instruccion);
        header.setStyle("-fx-padding:16 20 0 20;");

        VBox popupContent = new VBox(12, header, juego);
        popupContent.setAlignment(Pos.TOP_CENTER);
        popupContent.setStyle("-fx-background-color:#0d0d1a;-fx-border-color:#00d4ff40;" +
                "-fx-border-width:2;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:0 0 20 0;");
        popupContent.setMaxWidth(750);
        popupContent.setMaxHeight(750);

        // — Overlay semitransparente —
        StackPane overlay = new StackPane(popupContent);
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.78);");
        overlay.setPrefSize(stage.getWidth(), stage.getHeight());

        minijuegoOverlayActivo = overlay;

        // Insertar overlay sobre el stackCentral o el root
        if (stackCentral != null) {
            stackCentral.getChildren().add(overlay);
        } else {
            ((StackPane) root.getCenter()).getChildren().add(overlay);
        }

        cyberdetective.controller.MultiplayerJuegoController mc =
            (multiplayer && controller instanceof cyberdetective.controller.MultiplayerJuegoController)
                ? (cyberdetective.controller.MultiplayerJuegoController) controller : null;

        juego.setOnJuegoTerminado(tiempoMs -> {
            javafx.application.Platform.runLater(() -> {
                // Quitar overlay
                if (minijuegoOverlayActivo != null) {
                    if (stackCentral != null) stackCentral.getChildren().remove(minijuegoOverlayActivo);
                    else ((StackPane) root.getCenter()).getChildren().remove(minijuegoOverlayActivo);
                    minijuegoOverlayActivo = null;
                }

                if (mc != null) {
                    // Multijugador: notificar al servidor y esperar MINIGAME_RESOLVED
                    mc.enviarMinijuegoCompletado(accionIdx, tiempoMs);
                    labelMensajeAlex.setText("⏳ Desafío completado! Esperando resultado...");
                } else {
                    // Solitario: aplicar puntaje y revelar
                    completarAccion(accionIdx, accionCard, txtTitulo, txtSub, true);
                    accionCard.getChildren().remove(btnDesafio);
                    evidenciaVirtual.setStyle("-fx-cursor:default;");
                    accionCard.getChildren().add(evidenciaVirtual);
                }
            });
        });

        juego.iniciar();
    }

    @Override
    public void onMinijuegoIniciado(int gameId, int accionIdx) {
        javafx.application.Platform.runLater(() -> {
            labelMensajeAlex.setText("⚔ ¡Desafío iniciado! ¡Sé el primero en resolver!");
            if (mapaAccionesVBox.containsKey(accionIdx)) {
                mostrarMinijuegoPopupParaAccion(gameId, accionIdx, mapaEvidencias.get(accionIdx), mapaAccionesVBox.get(accionIdx), mapaBtnDesafio.get(accionIdx), mapaTextosCompletar.get(accionIdx)[0], mapaTextosCompletar.get(accionIdx)[1], true);
            }
        });
    }

    @Override
    public void onMinijuegoResuelto(String ganador, int accionIdx, boolean soyYo) {
        javafx.application.Platform.runLater(() -> {
            // Cerrar el popup para ambos jugadores
            if (minijuegoOverlayActivo != null) {
                if (stackCentral != null) stackCentral.getChildren().remove(minijuegoOverlayActivo);
                else ((StackPane) root.getCenter()).getChildren().remove(minijuegoOverlayActivo);
                minijuegoOverlayActivo = null;
            }

            if (soyYo) {
                labelMensajeAlex.setText("★ ¡GANASTE el desafío! +50 puntos.");
                labelMensajeAlex.setStyle("-fx-text-fill:#00ff88;-fx-font-weight:bold;");
            } else {
                labelMensajeAlex.setText("✖ " + ganador.toUpperCase() + " ganó el desafío. +0 puntos.");
                labelMensajeAlex.setStyle("-fx-text-fill:#ff9f1c;-fx-font-weight:bold;");
            }
            
            PauseTransition pausa = new PauseTransition(Duration.millis(2500));
            pausa.setOnFinished(e -> {
                labelMensajeAlex.setStyle("");
                if (mapaAccionesVBox.containsKey(accionIdx)) {
                    VBox card = mapaAccionesVBox.get(accionIdx);
                    Button btn = mapaBtnDesafio.get(accionIdx);
                    
                    card.getChildren().remove(btn);
                    
                    if (mapaEvidencias.containsKey(accionIdx)) {
                        javafx.scene.Node ev = mapaEvidencias.get(accionIdx);
                        ev.setStyle("-fx-cursor:default;");
                        if (!card.getChildren().contains(ev)) {
                            card.getChildren().add(ev);
                        }
                    }
                    
                    if (soyYo) {
                        String[] txts = mapaTextosCompletar.get(accionIdx);
                        completarAccion(accionIdx, card, txts[0], txts[1], false);
                    } else {
                        marcarAccionOponente(accionIdx, card);
                    }
                }
            });
            pausa.play();
        });
    }

    @Override
    public void onAccionResuelta(int accionIdx, String jugador, boolean soyYo) {
        javafx.application.Platform.runLater(() -> {
            if (mapaOnCorrecto.containsKey(accionIdx)) {
                if (soyYo) {
                    mapaOnCorrecto.get(accionIdx).run();
                } else {
                    // Si fue el oponente, bloqueamos y marcamos la correcta
                    if (mapaOpcionesBox.containsKey(accionIdx)) {
                        for (javafx.scene.Node n : mapaOpcionesBox.get(accionIdx).getChildren()) {
                            if (n instanceof Button b) {
                                b.setDisable(true);
                                b.setOpacity(0.5);
                            }
                        }
                    }
                    if (mapaOnSecundario.containsKey(accionIdx)) {
                        mapaOnSecundario.get(accionIdx).accept(1); // 1 = Compañero
                    }
                    labelMensajeAlex.setText("⚡ ¡EL DETECTIVE " + jugador.toUpperCase() + " RESOLVIÓ LA ACCIÓN " + (accionIdx+1) + "!");
                    labelMensajeAlex.setStyle("-fx-text-fill: #ff9f1c; -fx-font-weight: bold;");
                }
            }
        });
    }

    @Override
    public void onAccionFallida(int accionIdx, String jugador, boolean soyYo) {
        javafx.application.Platform.runLater(() -> {
            if (soyYo) {
                if (mapaBtnElegido.containsKey(accionIdx)) {
                    Button btn = mapaBtnElegido.get(accionIdx);
                    btn.setStyle(btn.getStyle() + "-fx-border-color:#ff004460;-fx-text-fill:#ff4466;");
                }
                labelMensajeAlex.setText("✗ Acción incorrecta. Esperando al oponente...");
                labelMensajeAlex.setStyle("-fx-text-fill: #ff4466;");
            } else {
                labelMensajeAlex.setText("⚡ ¡EL DETECTIVE " + jugador.toUpperCase() + " FALLÓ LA ACCIÓN! TE TOCA.");
                labelMensajeAlex.setStyle("-fx-text-fill: #00d4ff; -fx-font-weight: bold;");
            }
        });
    }

    @Override
    public void onAccionAmbosFallaron(int accionIdx) {
        javafx.application.Platform.runLater(() -> {
            if (mapaOpcionesBox.containsKey(accionIdx)) {
                for (javafx.scene.Node n : mapaOpcionesBox.get(accionIdx).getChildren()) {
                    if (n instanceof Button b) b.setDisable(true);
                }
            }
            if (mapaOnSecundario.containsKey(accionIdx)) {
                mapaOnSecundario.get(accionIdx).accept(2); // 2 = Ambos fallaron
            }
            labelMensajeAlex.setText("✗ Ambos fallaron la acción " + (accionIdx+1) + ". Se revela la correcta.");
            labelMensajeAlex.setStyle("-fx-text-fill: #ff4466; -fx-font-weight: bold;");
        });
    }



    @Override
    public void onOponenteDesconectado(String nombre) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conexión perdida");
            alert.setHeaderText("El detective " + nombre + " se ha desconectado.");
            alert.setContentText("La investigación ya no puede continuar de forma sincronizada.");
            alert.showAndWait();
        });
    }

    @Override public void onArbolListoParaInsertar() {
        System.out.println("Activando mostrarFaseInsercion en UI...");
        javafx.application.Platform.runLater(() -> mostrarFaseInsercion());
    }

    private java.util.Map<Integer, Runnable> mapaOnCorrecto = new java.util.HashMap<>();
    private java.util.Map<Integer, java.util.function.IntConsumer> mapaOnSecundario = new java.util.HashMap<>();
    private java.util.Map<Integer, javafx.scene.layout.Pane> mapaOpcionesBox = new java.util.HashMap<>();
    private java.util.Map<Integer, Button> mapaBtnElegido = new java.util.HashMap<>();

    private void procesarAccionBoton(int accionIdx, boolean esCorrecta, javafx.scene.layout.Pane opcionesBox, Button btnElegido, Runnable onCorrecto, java.util.function.IntConsumer onSecundario, String msgError) {
        if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
            mapaOnCorrecto.put(accionIdx, onCorrecto);
            mapaOnSecundario.put(accionIdx, onSecundario);
            mapaOpcionesBox.put(accionIdx, opcionesBox);
            
            // Bloquear mis opciones
            for (javafx.scene.Node n : opcionesBox.getChildren()) {
                if (n instanceof Button b) b.setDisable(true);
            }
            mapaBtnElegido.put(accionIdx, btnElegido);
            
            labelMensajeAlex.setText("Validando acción con el cuartel...");
            mc.intentarAccionInvestigacion(accionIdx, esCorrecta);
        } else {
            if (esCorrecta) {
                onCorrecto.run();
            } else {
                btnElegido.setStyle(btnElegido.getStyle() + "-fx-border-color:#ff004460;-fx-text-fill:#ff4466;");
                labelMensajeAlex.setText(msgError);
                PauseTransition p = new PauseTransition(Duration.millis(900));
                p.setOnFinished(ig -> {
                    btnElegido.setStyle(btnElegido.getStyle().replace("-fx-border-color:#ff004460;-fx-text-fill:#ff4466;", ""));
                });
                p.play();
            }
        }
    }

    private void evaluarRespuesta(int opcionElegida, VBox opcionesBox, String[] pregunta) {
        // En modo multijugador Y solitario, la respuesta se aplica de forma local.
        // En multijugador: responderPregunta() ya no envía al servidor, aplica +50/-25 localmente.
        // En solitario: idem, lógica base de JuegoController.
        boolean correcto = controller.responderPregunta(opcionElegida);
        // La UI se actualiza a través del listener onRespuestaRecibida disparado por JuegoController
    }

    // ════════════════════════════════════════════════
    //  FASE 3 — REPORTE DEL CASO (niveles 1-4)
    // ════════════════════════════════════════════════

    private void mostrarFaseReporte() {
        Caso caso = controller.getCasoActualFijo();
        if (caso == null) { mostrarFaseInsercion(); return; }

        Label etqFase = new Label("REPORTE DEL CASO — FASE 3 DE 3");
        etqFase.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#00d4ff;-fx-letter-spacing:0.12em;");
        Label titulo = new Label("Caso #" + caso.getId() + " — " + caso.getTipoAcoso());
        titulo.getStyleClass().add("titulo-nivel"); titulo.setWrapText(true);

        VBox tarjetaCaso = new VBox(14);
        tarjetaCaso.setStyle("-fx-background-color:#12121c;-fx-border-color:#00d4ff40;" +
                "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:24 28 24 28;");

        Label descEtq = etq("DESCRIPCIÓN DEL INCIDENTE");
        Label descLabel = new Label(caso.getDescripcion()); descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#9090b0;-fx-line-spacing:3;");
        VBox.setMargin(descLabel, new Insets(4,0,12,0));

        Label evEtq = etq("EVIDENCIAS RECOLECTADAS");
        VBox evBox = new VBox(6); VBox.setMargin(evBox, new Insets(4,0,12,0));
        for (String ev : caso.getEvidencias()) {
            Label evLabel = new Label("◆  " + ev); evLabel.setWrapText(true);
            evLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#c0c0d8;");
            evBox.getChildren().add(evLabel);
        }

        Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color:#1e1e2e;");
        VBox.setMargin(sep, new Insets(4,0,12,0));

        Label leyEtq = etq("MARCO LEGAL APLICABLE");
        Label leyLabel = new Label(caso.getLeyColombia()); leyLabel.setWrapText(true);
        leyLabel.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#00d4ff;");
        VBox.setMargin(leyLabel, new Insets(4,0,8,0));
        Label penaLabel = new Label("Pena posible: " + caso.getPenaAplicable()); penaLabel.setWrapText(true);
        penaLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6b6b8a;"); VBox.setMargin(penaLabel, new Insets(0,0,12,0));

        Label gravEtq = etq("GRAVEDAD DEL DELITO");
        HBox barraGrav = construirBarraGravedad(caso.getGravedad());

        tarjetaCaso.getChildren().addAll(descEtq, descLabel, evEtq, evBox, sep, leyEtq, leyLabel, penaLabel, gravEtq, barraGrav);

        Button btnInsertar = new Button("Insertar nodo en el árbol AVL →");
        btnInsertar.getStyleClass().add("btn-primario");
        if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
            btnInsertar.setOnAction(e -> {
                btnInsertar.setDisable(true);
                btnInsertar.setText("⏳ Esperando al otro detective...");
                labelMensajeAlex.setText("Esperando a que el otro detective termine el interrogatorio...");
                mc.enviarListoParaArbol();
            });
        } else {
            btnInsertar.setOnAction(e -> mostrarFaseInsercion());
        }
        HBox btnBox = new HBox(btnInsertar); btnBox.setAlignment(Pos.CENTER_RIGHT);

        labelMensajeAlex.setText("Resumen completo. Cuando estés listo, inserta el nodo en el árbol AVL.");
        setCentral(etqFase, titulo, tarjetaCaso, btnBox);
    }

    // ════════════════════════════════════════════════
    //  FASE 4 — INSERCIÓN EN EL ÁRBOL (niveles 1-4)
    // ════════════════════════════════════════════════

    private void mostrarFaseInsercion() {
        boolean cerrado = controller.cerrarCaso();
        if (!cerrado) return;

        if (controller.isJuegoTerminado()) {
            labelMensajeAlex.setText("¡Investigación completada! El árbol revela la verdad.");
            controller.removerListener(this);
            mapa.regresarAlMapa();
            return;
        }

        visualizadorArbol = obtenerVisualizadorArbol();
        visualizadorArbol.actualizar(controller.getArbol());

        Label etqFase = new Label("✓  NODO INSERTADO EN EL ÁRBOL AVL");
        etqFase.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#00ff88;-fx-letter-spacing:0.12em;");
        Label confirmacion = new Label("El caso fue registrado correctamente en el árbol.");
        confirmacion.setStyle("-fx-font-size:15px;-fx-font-weight:600;-fx-text-fill:#00ff88;");

        VBox canvasBox = new VBox(visualizadorArbol.getCanvas());
        canvasBox.setStyle("-fx-background-color:#0d0d16;-fx-border-color:#1e1e2e;" +
                "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:16;");

        Label instruccionArbol = new Label(
                "Nodos ordenados por gravedad (1 = leve → 10 = grave). " +
                        "Haz clic en un nodo para ver los detalles del caso.");
        instruccionArbol.setWrapText(true);
        instruccionArbol.setStyle("-fx-font-size:12px;-fx-text-fill:#3a3a5c;");

        Button btnContinuar = new Button("Continuar investigación →");
        btnContinuar.getStyleClass().add("btn-primario");
        btnContinuar.setOnAction(e -> {
            controller.removerListener(this);
            mapa.regresarAlMapa();
        });
        HBox btnBox = new HBox(btnContinuar); btnBox.setAlignment(Pos.CENTER_RIGHT);

        labelMensajeAlex.setText("Nodo insertado. El árbol AVL se reorganizó. Regresa al mapa para continuar.");
        setCentral(etqFase, confirmacion, canvasBox, instruccionArbol, btnBox);
    }

    // ════════════════════════════════════════════════
    //  NIVEL FINAL — flujo especial (nivel 5)
    // ════════════════════════════════════════════════

    private void mostrarNivelFinal() {
        // 1. Obtener los casos del árbol (ÚNICOS por ID)
        java.util.Set<Integer> idsVistos = new java.util.HashSet<>();
        List<Caso> todosLosCasos = new java.util.ArrayList<>();
        for (Caso c : controller.getArbol().recorridoInorden()) {
            if (idsVistos.add(c.getId())) {
                todosLosCasos.add(c);
            }
        }
        this.ordenCorrecto = todosLosCasos;
        
        // 2. Desordenar para el puzzle
        casosDesordenados = new java.util.ArrayList<>(ordenCorrecto);
        java.util.Collections.shuffle(casosDesordenados);
        
        siguienteCorrecto = 0; intentosFallidos = 0;
        mostrarRecorridoArbol();
    }

    /** Paso 1: recorrer el árbol en inorden de forma interactiva. */
    private void mostrarRecorridoArbol() {
        visualizadorArbol = obtenerVisualizadorArbol();
        visualizadorArbol.actualizar(controller.getArbol());

        Label titulo = new Label("Nivel Final – La verdad detrás del acoso");
        titulo.getStyleClass().add("titulo-nivel");
        Label sub = new Label(
                "El detective Alex reúne todas las evidencias. " +
                        "Para presentar el caso ante el juez, los delitos deben " +
                        "organizarse de menor a mayor gravedad.");
        sub.getStyleClass().add("subtitulo"); sub.setWrapText(true);

        VBox canvasBox = new VBox(visualizadorArbol.getCanvas());
        canvasBox.setStyle("-fx-background-color:#0d0d16;-fx-border-color:#00d4ff25;" +
                "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:16;");

        VBox cajaMision = new VBox(8);
        cajaMision.setStyle("-fx-background-color:#00d4ff08;-fx-border-color:#00d4ff25;" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:16 20 16 20;");
        Label alexEtq2 = new Label("ALEX DICE");
        alexEtq2.setStyle("-fx-font-size:10px;-fx-font-weight:600;-fx-text-fill:#00d4ff;-fx-letter-spacing:0.1em;");
        Label alexTxt = new Label(
                "Recorre el árbol inorden — de menor a mayor gravedad — " +
                        "haciendo clic en los casos en el orden correcto. " +
                        "Empieza por el delito menos grave.");
        alexTxt.setWrapText(true);
        alexTxt.setStyle("-fx-font-size:13px;-fx-font-style:italic;-fx-text-fill:#00d4ffcc;");
        cajaMision.getChildren().addAll(alexEtq2, alexTxt);

        Label progresoEtq2 = etq("RECORRIDO INORDEN — PROGRESO");
        HBox lineaRecorrido = new HBox(8); lineaRecorrido.setAlignment(Pos.CENTER_LEFT);
        lineaRecorrido.setId("lineaRecorrido");
        for (int i = 0; i < ordenCorrecto.size(); i++) lineaRecorrido.getChildren().add(crearSlotRecorrido(i));

        Label feedbackLabel = new Label("Selecciona el caso con menor gravedad para comenzar.");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-font-size:14px;-fx-text-fill:#9090b0;-fx-line-spacing:4;");

        Label seleccionEtq = etq("CASOS DISPONIBLES — HAZ CLIC EN EL ORDEN CORRECTO");
        VBox casosBox = new VBox(8);
        for (Caso c : casosDesordenados)
            casosBox.getChildren().add(crearTarjetaSeleccionable(c, casosBox, lineaRecorrido, feedbackLabel));

        labelMensajeAlex.setText("Analiza el árbol y haz clic de menor a mayor gravedad.");
        setCentral(titulo, sub, canvasBox, cajaMision, progresoEtq2, lineaRecorrido, feedbackLabel, seleccionEtq, casosBox);
    }

    private VBox crearSlotRecorrido(int posicion) {
        VBox slot = new VBox(); slot.setAlignment(Pos.CENTER);
        slot.setPrefWidth(130); slot.setPrefHeight(52);
        slot.setStyle("-fx-background-color:#0f0f1a;-fx-border-color:#1e1e2e;" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8;");
        Label numLabel = new Label(String.valueOf(posicion + 1));
        numLabel.setStyle("-fx-font-family:'DM Mono';-fx-font-size:10px;-fx-text-fill:#2a2a3c;");
        Label vacioLabel = new Label("— pendiente —");
        vacioLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#2a2a3c;");
        slot.getChildren().addAll(numLabel, vacioLabel); slot.setId("slot_" + posicion);
        return slot;
    }

    private VBox crearTarjetaSeleccionable(Caso caso, VBox casosBox,
                                           HBox lineaRecorrido, Label feedbackLabel) {
        VBox tarjeta = new VBox(4); tarjeta.setId("caso_" + caso.getId());
        tarjeta.setStyle("-fx-background-color:#12121c;-fx-border-color:#1e1e2e;" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                "-fx-padding:14 18 14 18;-fx-cursor:hand;");
        HBox cab = new HBox(10); cab.setAlignment(Pos.CENTER_LEFT);
        Label idL = new Label("Caso #" + caso.getId());
        idL.setStyle("-fx-font-family:'DM Mono';-fx-font-size:11px;-fx-text-fill:#3a3a5c;");
        Label tipoL = new Label(caso.getTipoAcoso());
        tipoL.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#e0e0f0;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label gravL = new Label("Gravedad: " + caso.getGravedad() + "/10");
        gravL.setStyle("-fx-font-family:'DM Mono';-fx-font-size:11px;-fx-text-fill:#4a4a6a;");
        cab.getChildren().addAll(idL, tipoL, sp, gravL);
        Label leyL = new Label(caso.getLeyColombia());
        leyL.setStyle("-fx-font-size:12px;-fx-text-fill:#00d4ffaa;");
        tarjeta.getChildren().addAll(cab, leyL);
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(
                "-fx-background-color:#00d4ff08;-fx-border-color:#00d4ff40;" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                        "-fx-padding:14 18 14 18;-fx-cursor:hand;"));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(
                "-fx-background-color:#12121c;-fx-border-color:#1e1e2e;" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                        "-fx-padding:14 18 14 18;-fx-cursor:hand;"));
        tarjeta.setOnMouseClicked(e ->
                evaluarSeleccionRecorrido(caso, tarjeta, lineaRecorrido, feedbackLabel));
        return tarjeta;
    }

    private void evaluarSeleccionRecorrido(Caso casoElegido, VBox tarjeta,
                                           HBox lineaRecorrido, Label feedbackLabel) {
        if (siguienteCorrecto >= ordenCorrecto.size()) return;
        Caso esperado = ordenCorrecto.get(siguienteCorrecto);

        if (casoElegido.getId() == esperado.getId()) {
            tarjeta.setDisable(true);
            tarjeta.setStyle("-fx-background-color:#00ff8812;-fx-border-color:#00ff8850;" +
                    "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-padding:14 18 14 18;-fx-opacity:0.6;");

            javafx.scene.Node slotNode = lineaRecorrido.lookup("#slot_" + siguienteCorrecto);
            if (slotNode instanceof VBox slot) {
                slot.getChildren().clear();
                slot.setStyle("-fx-background-color:#00ff8815;-fx-border-color:#00ff8850;" +
                        "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8;");
                Label casoLabel = new Label("C#" + casoElegido.getId());
                casoLabel.setStyle("-fx-font-family:'DM Mono';-fx-font-size:11px;-fx-text-fill:#00ff88;");
                Label gravSlot = new Label("G:" + casoElegido.getGravedad());
                gravSlot.setStyle("-fx-font-size:10px;-fx-text-fill:#00ff8880;");
                slot.getChildren().addAll(casoLabel, gravSlot);
                FadeTransition ft = new FadeTransition(Duration.millis(300), slot);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }

            feedbackLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#00ff88;-fx-line-spacing:2;");
            siguienteCorrecto++;

            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.intentarAccionInvestigacion(100, true);
            }

            if (siguienteCorrecto == ordenCorrecto.size()) {
                feedbackLabel.setText("✓ Perfecto. Recorrido inorden completado. El árbol AVL organizó los delitos de menor a mayor gravedad.");
                PauseTransition pausa = new PauseTransition(Duration.millis(1200));
                pausa.setOnFinished(ev -> {
                    if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                        feedbackLabel.setText("✓ Esperando a que el oponente termine de organizar por gravedad...");
                        mc.enviarListoCronologiaNivel5();
                    } else {
                        mostrarAnalisisNodos();
                    }
                });
                pausa.play();
            } else {
                feedbackLabel.setText("✓ Correcto — " + casoElegido.getTipoAcoso() +
                        " (G:" + casoElegido.getGravedad() + "). Selecciona el siguiente delito más grave.");
            }
        } else {
            intentosFallidos++;
            if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                mc.intentarAccionInvestigacion(100, false);
            }
            feedbackLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#ff4466;-fx-line-spacing:2;");
            feedbackLabel.setText("✗ Ese delito tiene gravedad " + casoElegido.getGravedad() +
                    " — no es el siguiente. Busca el caso menos grave disponible.");
            tarjeta.setStyle("-fx-background-color:#ff004408;-fx-border-color:#ff004455;" +
                    "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                    "-fx-padding:14 18 14 18;-fx-cursor:hand;");
            PauseTransition reset = new PauseTransition(Duration.millis(600));
            reset.setOnFinished(ev -> tarjeta.setStyle(
                    "-fx-background-color:#12121c;-fx-border-color:#1e1e2e;" +
                            "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                            "-fx-padding:14 18 14 18;-fx-cursor:hand;")); reset.play();
        }
    }

    /** Paso 2: analizar los nodos — reconstruir la línea de hechos. */
    private void mostrarAnalisisNodos() {
        // 1. Obtener los casos del árbol y ordenarlos por orden cronológico (antiguo a reciente) - ÚNICOS
        java.util.Set<Integer> idsVistos = new java.util.HashSet<>();
        List<Caso> todosLosCasos = new java.util.ArrayList<>();
        for (Caso c : controller.getArbol().recorridoInorden()) {
            if (idsVistos.add(c.getId())) {
                todosLosCasos.add(c);
            }
        }
        this.ordenCorrecto = todosLosCasos;
        this.ordenCorrecto.sort((a, b) -> Integer.compare(a.getOrdenCronologico(), b.getOrdenCronologico()));
        
        // 2. Desordenamos los casos para que el usuario tenga que ordenarlos
        List<Caso> casosDesordenadosPuzzle = new java.util.ArrayList<>(ordenCorrecto);
        java.util.Collections.shuffle(casosDesordenadosPuzzle);

        List<Caso> seleccionUsuario = new java.util.ArrayList<>();

        Label titulo = new Label("Laboratorio Forense: Reconstrucción Temporal");
        titulo.getStyleClass().add("titulo-nivel");
        Label sub = new Label("Selecciona los incidentes en orden, desde el más antiguo (hace meses) hasta el más reciente (hoy).");
        sub.getStyleClass().add("subtitulo");

        VBox contenedorContenido = new VBox(20);
        contenedorContenido.setStyle("-fx-padding: 0 20 0 0;");

        FlowPane panelSeleccion = new FlowPane(15, 15);
        panelSeleccion.setAlignment(Pos.TOP_LEFT);

        VBox lineaTiempo = new VBox(10);
        lineaTiempo.setStyle("-fx-background-color: #080810; -fx-padding: 20; -fx-border-color: #1e1e2e; -fx-border-radius: 12;");

        actualizarPanelSeleccion(panelSeleccion, casosDesordenadosPuzzle, seleccionUsuario, lineaTiempo);

        contenedorContenido.getChildren().addAll(
                etq("PASO 1: ANALIZA LAS FECHAS Y SELECCIONA"),
                panelSeleccion,
                etq("PASO 2: LÍNEA DE TIEMPO RECONSTRUIDA"),
                lineaTiempo
        );

        scrollCentral.setFitToWidth(true);
        setCentral(titulo, sub, contenedorContenido);
        labelMensajeAlex.setText("Analiza las fechas en las descripciones de las evidencias para determinar qué ocurrió primero.");
    }

    private void actualizarPanelSeleccion(FlowPane panel, List<Caso> disponibles, List<Caso> seleccionados, VBox linea) {
        panel.getChildren().clear();
        linea.getChildren().clear();

        // 1. Mostrar la Línea de Tiempo con NUMERACIÓN
        for (int i = 0; i < seleccionados.size(); i++) {
            Caso c = seleccionados.get(i);
            HBox h = new HBox(15);
            h.setAlignment(Pos.CENTER_LEFT);
            h.setStyle("-fx-background-color: #0f0f1a; -fx-padding: 12; -fx-border-color: #00ff8840; -fx-border-radius: 8; -fx-margin: 5;");

            Label num = new Label("#" + (i + 1));
            num.setStyle("-fx-font-family: 'DM Mono'; -fx-text-fill: #00ff88; -fx-font-weight: bold;");

            VBox info = new VBox(2);
            Label t = new Label(c.getTipoAcoso());
            t.setStyle("-fx-text-fill: #f0f0f8; -fx-font-weight: bold;");
            
            // Pistas temporales (usamos la primera evidencia que suele tener la fecha)
            Label d = new Label(c.getEvidencias()[0]);
            d.setWrapText(true);
            d.setStyle("-fx-text-fill: #00ff8880; -fx-font-size: 19px;");

            info.getChildren().addAll(t, d);
            h.getChildren().addAll(num, info);
            linea.getChildren().add(h);
        }

        // 2. Mostrar "Tarjetas de Evidencia" para elegir
        for (Caso c : disponibles) {
            VBox tarjetaEvidencia = new VBox(8);
            tarjetaEvidencia.setStyle("-fx-background-color: #161625; -fx-padding: 15; -fx-border-color: #2a2a3c; -fx-border-radius: 10; -fx-pref-width: 280;");

            Label etiqueta = new Label("EVIDENCIA SIN CLASIFICAR");
            etiqueta.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a5a7a; -fx-letter-spacing: 0.1em;");

            VBox pistas = new VBox(4);
            for(String evidencia : c.getEvidencias()) {
                Label p = new Label("• " + evidencia);
                p.setStyle("-fx-text-fill: #9090b0; -fx-font-size: 17px; -fx-wrap-text: true;");
                p.setWrapText(true);
                pistas.getChildren().add(p);
            }

            Button btnSeleccionar = new Button("Seleccionar");
            btnSeleccionar.setStyle(estiloBotonMonospace());
            btnSeleccionar.setMaxWidth(Double.MAX_VALUE);

            btnSeleccionar.setOnAction(e -> {
                int siguienteCronologico = seleccionados.size() + 1;
                if (c.getOrdenCronologico() == siguienteCronologico) {
                    if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                        mc.intentarAccionInvestigacion(101, true);
                    }
                    disponibles.remove(c);
                    seleccionados.add(c);
                    labelMensajeAlex.setText("¡Excelente! La marca de tiempo coincide con la progresión del caso.");
                    actualizarPanelSeleccion(panel, disponibles, seleccionados, linea);
                } else {
                    if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                        mc.intentarAccionInvestigacion(101, false);
                    }
                    labelMensajeAlex.setText("✗ Ese incidente no ocurrió en este momento. Revisa bien las fechas.");
                }
            });

            tarjetaEvidencia.getChildren().addAll(etiqueta, pistas, btnSeleccionar);
            panel.getChildren().add(tarjetaEvidencia);
        }

        if (disponibles.isEmpty()) {
            labelMensajeAlex.setText("Cronología completa. El historial de agresiones está listo para el reporte.");
            Button btnFin = new Button("Generar Reporte Final →");
            btnFin.getStyleClass().add("btn-primario");
            btnFin.setPadding(new Insets(15, 30, 15, 30));
            btnFin.setOnAction(ev -> {
                if (controller instanceof cyberdetective.controller.MultiplayerJuegoController mc) {
                    btnFin.setDisable(true);
                    btnFin.setText("⏳ Esperando al oponente...");
                    mc.enviarListoReporteFinalNivel5();
                } else {
                    mostrarReporteFinal();
                }
            });
            panel.getChildren().add(btnFin);
        }
    }

    /** Paso 3: reporte global del caso. */
    private void mostrarReporteFinal() {
        Label titulo = new Label("Reporte Final — Caso Valeria");
        titulo.getStyleClass().add("titulo-nivel");
        Label sub = new Label(
                "El detective Alex presenta el caso completo. " +
                        "Todos los delitos quedan documentados con sus leyes y sanciones.");
        sub.getStyleClass().add("subtitulo"); sub.setWrapText(true);

        VBox cajaAgresor = new VBox(8);
        cajaAgresor.setStyle("-fx-background-color:#00ff8808;-fx-border-color:#00ff8830;" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:16 20 16 20;");
        Label agresorEtq = new Label("AGRESOR IDENTIFICADO");
        agresorEtq.setStyle("-fx-font-size:10px;-fx-font-weight:600;-fx-text-fill:#00ff88;-fx-letter-spacing:0.1em;");
        Label agresorLabel = new Label(controller.getSospechoso()); agresorLabel.setWrapText(true);
        agresorLabel.setStyle("-fx-font-size:15px;-fx-font-weight:600;-fx-text-fill:#f0f0f8;");
        Label intentosLabel = new Label("Errores en el recorrido: " + intentosFallidos +
                "  ·  Puntaje final: " + controller.getPuntaje() + " pts");
        intentosLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#5a5a7a;");
        cajaAgresor.getChildren().addAll(agresorEtq, agresorLabel, intentosLabel);

        Label delitosEtq = etq("DELITOS DOCUMENTADOS");
        VBox delitosBox = new VBox(10);
        for (Caso c : ordenCorrecto) {
            VBox card = new VBox(4);
            card.setStyle("-fx-background-color:#0f0f1a;-fx-border-color:#1e1e2e;" +
                    "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12 16 12 16;");
            HBox cabCard = new HBox(10); cabCard.setAlignment(Pos.CENTER_LEFT);
            Label idL = new Label("Caso #" + c.getId());
            idL.setStyle("-fx-font-family:'DM Mono';-fx-font-size:11px;-fx-text-fill:#3a3a5c;");
            Label tipoL = new Label(c.getTipoAcoso());
            tipoL.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#e0e0f0;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label gL = new Label("G:" + c.getGravedad() + "/10");
            gL.setStyle("-fx-font-family:'DM Mono';-fx-font-size:11px;-fx-text-fill:#00d4ff;");
            cabCard.getChildren().addAll(idL, tipoL, sp, gL);
            Label leyL = new Label(c.getLeyColombia()); leyL.setStyle("-fx-font-size:12px;-fx-text-fill:#00d4ff88;");
            Label penaL = new Label("Pena: " + c.getPenaAplicable()); penaL.setWrapText(true);
            penaL.setStyle("-fx-font-size:12px;-fx-text-fill:#6b6b8a;");
            card.getChildren().addAll(cabCard, leyL, penaL); delitosBox.getChildren().add(card);
        }

        Label reporteEtq = etq("REPORTE COMPLETO");
        String reporteTexto = controller.getArbol().generarReporte()
                + "\n\nAgresor identificado: " + controller.getSospechoso()
                + "\nErrores en el recorrido: " + intentosFallidos
                + "\nPuntaje final: " + controller.getPuntaje() + " puntos.";
        TextArea reporteArea = new TextArea(reporteTexto);
        reporteArea.setEditable(false); reporteArea.setWrapText(true); reporteArea.setPrefHeight(200);
        reporteArea.setStyle("-fx-background-color:#0d0d16;-fx-control-inner-background:#0d0d16;" +
                "-fx-text-fill:#9090b0;-fx-font-family:'DM Mono';-fx-font-size:11px;" +
                "-fx-border-color:#1e1e2e;-fx-border-width:1;-fx-border-radius:8;" +
                "-fx-background-radius:8;-fx-padding:12;");

        Button btnNueva = new Button("Nueva investigación");
        btnNueva.getStyleClass().add("btn-primario");
        btnNueva.setOnAction(e -> new PantallaInicio(stage).mostrar());
        Button btnMenu = new Button("Volver al menú");
        btnMenu.getStyleClass().add("btn-secundario");
        btnMenu.setOnAction(e -> new MenuPrincipal(stage).mostrar());
        HBox botonesBox = new HBox(14, btnNueva, btnMenu); botonesBox.setAlignment(Pos.CENTER_LEFT);

        labelMensajeAlex.setText("Caso cerrado. Lo que empezó como una broma se convirtió en " +
                ordenCorrecto.size() + " delitos reales. Buen trabajo, detective.");
        setCentral(titulo, sub, cajaAgresor, delitosEtq, delitosBox, reporteEtq, reporteArea, botonesBox);
    }

    // ════════════════════════════════════════════════
    //  UI REUTILIZABLE
    // ════════════════════════════════════════════════

    private VBox crearTarjetaAccion(String titulo, String descripcion, int idx) {
        VBox tarjeta = new VBox(12); tarjeta.getStyleClass().add("accion-card");
        HBox cabecera = new HBox(10); cabecera.setAlignment(Pos.CENTER_LEFT);
        Circle indicador = new Circle(8);
        indicador.setFill(Color.web("#1a1a2a")); indicador.setStroke(Color.web("#3a3a5c")); indicador.setStrokeWidth(1.5);
        Label tituloLabel = new Label(titulo); tituloLabel.getStyleClass().add("accion-titulo"); tituloLabel.setWrapText(true);
        cabecera.getChildren().addAll(indicador, tituloLabel);
        Label descLabel = new Label(descripcion);
        descLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#8080a8;-fx-wrap-text:true;"); descLabel.setWrapText(true);
        tarjeta.getChildren().addAll(cabecera, descLabel);
        return tarjeta;
    }

    private HBox construirEvidenciaConDatos(String rutaImagen, String tituloTarjeta,
                                            String[][] datos, String notaCensura) {
        HBox contenedor = new HBox(20); contenedor.setAlignment(Pos.TOP_LEFT);
        VBox imgBox = new VBox(8); imgBox.setAlignment(Pos.TOP_CENTER);
        ImageView img = cargarImagen(rutaImagen, 320, 200);
        Label censura = new Label("⚠  " + notaCensura);
        censura.setStyle("-fx-font-size:10px;-fx-font-weight:600;-fx-text-fill:#ff9f1c;" +
                "-fx-background-color:rgba(255,159,28,0.10);-fx-border-color:#ff9f1c40;" +
                "-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:4 10 4 10;");
        censura.setWrapText(true); censura.setMaxWidth(320);
        imgBox.getChildren().addAll(img, censura);

        VBox tarjeta = new VBox(0);
        tarjeta.setStyle("-fx-background-color:#0d0d18;-fx-border-color:#00d4ff25;" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:16 18 16 18;-fx-min-width:260px;");
        Label tarjetaTitulo = new Label(tituloTarjeta);
        tarjetaTitulo.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#00d4ff;-fx-letter-spacing:0.12em;");
        VBox.setMargin(tarjetaTitulo, new Insets(0, 0, 12, 0)); tarjeta.getChildren().add(tarjetaTitulo);
        for (String[] dato : datos) {
            HBox fila = new HBox(8); fila.setAlignment(Pos.TOP_LEFT); VBox.setMargin(fila, new Insets(0,0,6,0));
            Label clave = new Label(dato[0]);
            clave.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:#5a5a7a;-fx-min-width:100px;-fx-font-family:'DM Mono';");
            Label valor = new Label(dato[1]);
            valor.setStyle("-fx-font-size:12px;-fx-text-fill:#d8d8f0;-fx-font-family:'DM Mono';-fx-wrap-text:true;"); valor.setWrapText(true);
            HBox.setHgrow(valor, Priority.ALWAYS); fila.getChildren().addAll(clave, valor); tarjeta.getChildren().add(fila);
            Region linea = new Region(); linea.setPrefHeight(1); linea.setStyle("-fx-background-color:#1a1a28;");
            VBox.setMargin(linea, new Insets(0,0,6,0)); tarjeta.getChildren().add(linea);
        }
        Label nota = new Label("ℹ  Usa estos datos para responder las preguntas.");
        nota.setStyle("-fx-font-size:10px;-fx-text-fill:#3a3a5c;-fx-font-style:italic;-fx-wrap-text:true;"); nota.setWrapText(true);
        tarjeta.getChildren().add(nota);
        contenedor.getChildren().addAll(imgBox, tarjeta);
        return contenedor;
    }

    private HBox construirBarraGravedad(int gravedad) {
        HBox segmentos = new HBox(3); segmentos.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 10; i++) {
            VBox seg = new VBox(); seg.setPrefWidth(22); seg.setPrefHeight(6);
            String color;
            if (i <= 3) color = i <= gravedad ? "#ffd166" : "#1e1e2e";
            else if (i <= 6) color = i <= gravedad ? "#ff9f1c" : "#1e1e2e";
            else if (i <= 8) color = i <= gravedad ? "#ff6b6b" : "#1e1e2e";
            else color = i <= gravedad ? "#ff0044" : "#1e1e2e";
            seg.setStyle("-fx-background-color:" + color + ";-fx-background-radius:2;");
            segmentos.getChildren().add(seg);
        }
        Label val = new Label(gravedad + "/10");
        val.setStyle("-fx-font-family:'DM Mono';-fx-font-size:11px;-fx-text-fill:#6b6b8a;");
        HBox fila = new HBox(10, segmentos, val); fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /** Etiqueta de sección reutilizable. */
    private Label etq(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:10px;-fx-font-weight:600;-fx-text-fill:#3a3a5c;-fx-letter-spacing:0.1em;");
        return l;
    }

    // ════════════════════════════════════════════════
    //  UTILIDADES
    // ════════════════════════════════════════════════

    private VisualizadorArbol obtenerVisualizadorArbol() {
        if (visualizadorArbol == null) {
            visualizadorArbol = new VisualizadorArbol();
            visualizadorArbol.setOnIrEvidencia(caso -> {
                if (btnEvBtnCache != null) btnEvBtnCache.setStyle(estiloNavActivo());
                if (btnInvCache != null) btnInvCache.setStyle(estiloNavInactivo());
                if (btnArbolBtnCache != null) btnArbolBtnCache.setStyle(estiloNavInactivo());
                mostrarVistaEvidencias(caso.getId());
            });
        }
        return visualizadorArbol;
    }

    private ImageView cargarImagen(String ruta, double ancho, double alto) {
        try {
            var stream = getClass().getResourceAsStream(ruta);
            if (stream == null) return new ImageView();
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitWidth(ancho); iv.setFitHeight(alto); iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) { return new ImageView(); }
    }

    private String obtenerUsuarioSospechoso() {
        int i = obtenerIndiceSospechoso();
        return i >= 0 ? cyberdetective.data.NivelesData.PERFILES_SOSPECHOSOS[i][1] : "@usuario_sospechoso";
    }
    private String obtenerIPSospechoso() {
        int i = obtenerIndiceSospechoso();
        return i >= 0 ? cyberdetective.data.NivelesData.PERFILES_SOSPECHOSOS[i][2] : "192.168.0.1";
    }
    private String obtenerDispositivoSospechoso() {
        int i = obtenerIndiceSospechoso();
        return i >= 0 ? cyberdetective.data.NivelesData.PERFILES_SOSPECHOSOS[i][3] : "Dispositivo desconocido";
    }
    private int obtenerIndiceSospechoso() {
        String sospReal = controller.getSospechoso();
        for (int i = 0; i < cyberdetective.data.NivelesData.PERFILES_SOSPECHOSOS.length; i++)
            if (cyberdetective.data.NivelesData.PERFILES_SOSPECHOSOS[i][0].equals(sospReal)) return i;
        return 0;
    }
    private String[] getSospechososOrdenados() {
        return new String[]{
                "Mateo R. – compañero de clase de Valeria",
                "Sebastián L. – exnovio de Valeria",
                "Camila V. – rival académica de Valeria",
                "Usuario anónimo conocido como 'ShadowNet_21'"
        };
    }
    private void mezclar(String[] arr) {
        java.util.Random rnd = new java.util.Random();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1); String tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
    }
    private Separator separador(int margen) {
        Separator sep = new Separator(); sep.setStyle("-fx-background-color:#1a1a28;");
        VBox.setMargin(sep, new Insets(margen, 0, margen, 0)); return sep;
    }
    private String estiloNavActivo() {
        return "-fx-background-color:#00d4ff15;-fx-border-color:#00d4ff40;" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-text-fill:#00d4ff;-fx-font-size:13px;-fx-padding:10 16 10 16;-fx-cursor:hand;";
    }
    private String estiloNavInactivo() {
        return "-fx-background-color:transparent;-fx-border-color:#1e1e2e;" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-text-fill:#4a4a6a;-fx-font-size:13px;-fx-padding:10 16 10 16;-fx-cursor:hand;";
    }
    private String estiloBotonMonospace() {
        return "-fx-background-color:#0d0d16;-fx-border-color:#2a2a3c;" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-text-fill:#00d4ff;-fx-font-family:'DM Mono';" +
                "-fx-font-size:13px;-fx-padding:10 16 10 16;-fx-cursor:hand;";
    }
    private String estiloBotonIP() {
        return "-fx-background-color:#0d0d16;-fx-border-color:#2a2a3c;" +
                "-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;" +
                "-fx-text-fill:#00d4ff;-fx-font-family:'DM Mono';" +
                "-fx-font-size:13px;-fx-padding:10 16 10 16;-fx-cursor:hand;";
    }

    private String[] getObjetivosNivel(int nivel) {
        return switch (nivel) {
            case 1 -> new String[]{"Recolectar capturas de pantalla","Identificar el usuario agresor","Clasificar el tipo de agresión"};
            case 2 -> new String[]{"Identificar la publicación original","Rastrear quién inició el rumor","Determinar si es información falsa"};
            case 3 -> new String[]{"Analizar el perfil falso","Rastrear la IP de creación","Identificar al suplantador"};
            case 4 -> new String[]{"Identificar cuentas vinculadas","Encontrar patrones de comportamiento","Identificar al responsable principal"};
            default -> new String[]{};
        };
    }
    private String getMensajeValeria(int nivel) {
        return switch (nivel) {
            case 1 -> "\"Empecé a recibir mensajes muy ofensivos. No sé quién es pero no para. Tengo todas las capturas guardadas.\"";
            case 2 -> "\"Alguien publicó cosas terribles sobre mí y se está volviendo viral. No es verdad nada de eso.\"";
            case 3 -> "\"Hay un perfil falso con mi foto. Está hablando mal de mis compañeros como si fuera yo.\"";
            case 4 -> "\"Ahora son varias cuentas al mismo tiempo. Es como si hubiera un grupo coordinado atacándome.\"";
            default -> "\"Gracias por investigar mi caso.\"";
        };
    }
    private String getMensajeAlex(int nivel) {
        return switch (nivel) {
            case 1 -> "\"Vamos a analizar cada captura y encontrar al responsable. Empieza por recolectar la evidencia visual.\"";
            case 2 -> "\"Quien publica primero siempre deja rastros en los metadatos. Identifica el origen del rumor.\"";
            case 3 -> "\"Un perfil falso siempre deja huella digital. La IP de creación nos dirá exactamente quién fue.\"";
            case 4 -> "\"Un ataque coordinado desde varias cuentas pero con la misma IP. Identifiquemos al responsable principal.\"";
            default -> "\"Buen trabajo, detective.\"";
        };
    }
}