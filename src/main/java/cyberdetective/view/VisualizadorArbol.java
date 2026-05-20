package cyberdetective.view;

import cyberdetective.model.ArbolAVL;
import cyberdetective.model.NodoAVL;
import cyberdetective.model.Caso;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Clase que dibuja el árbol AVL de forma estética usando JavaFX Canvas.
 */
public class VisualizadorArbol {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private Consumer<Caso> onIrEvidencia;
    private Popup popupNodo;

    // Configuración estética
    private static final double RADIO_NODO         = 24;
    private static final double ESPACIADO_V        = 60;
    private static final double ANCHO_CANVAS        = 760;
    private static final double ALTO_CANVAS          = 240;

    private static final Color COLOR_FONDO      = Color.web("#0d0d16");
    private static final Color COLOR_ARISTA      = Color.web("#1e1e2e");
    private static final Color COLOR_NODO_FONDO  = Color.web("#12121c");
    private static final Color COLOR_NODO_BORDE  = Color.web("#00d4ff");
    private static final Color COLOR_TEXTO_ID    = Color.web("#f0f0f8");
    private static final Color COLOR_TEXTO_GRAV  = Color.web("#00d4ff");
    private static final Color COLOR_VACIO       = Color.web("#2a2a3c");

    // Guarda la posición de cada nodo para detectar el clic
    private static class NodoPosicion {
        double x, y;
        Caso caso;
        NodoPosicion(double x, double y, Caso caso) {
            this.x = x; this.y = y; this.caso = caso;
        }
    }

    private final List<NodoPosicion> posicionesNodos = new ArrayList<>();

    public VisualizadorArbol() {
        canvas = new Canvas(ANCHO_CANVAS, ALTO_CANVAS);
        gc = canvas.getGraphicsContext2D();
        dibujarVacio();
        configurarClickEnNodos();
    }

    public void setOnIrEvidencia(Consumer<Caso> callback) {
        this.onIrEvidencia = callback;
    }

    public Canvas getCanvas() { return canvas; }

    /**
     * Detecta si el clic cayó dentro del radio de algún nodo
     * y muestra la tarjeta de información del caso correspondiente.
     */
    private void configurarClickEnNodos() {
        canvas.setOnMouseClicked(e -> {
            double mx = e.getX();
            double my = e.getY();

            for (NodoPosicion np : posicionesNodos) {
                double dist = Math.sqrt(Math.pow(mx - np.x, 2) + Math.pow(my - np.y, 2));
                if (dist <= RADIO_NODO) {
                    mostrarTarjetaNodo(np.caso, e.getScreenX(), e.getScreenY());
                    return;
                }
            }

            // Clic fuera de cualquier nodo — cerrar popup si está abierto
            if (popupNodo != null && popupNodo.isShowing()) {
                popupNodo.hide();
            }
        });

        // Cambiar cursor al pasar sobre un nodo
        canvas.setOnMouseMoved(e -> {
            boolean sobreNodo = false;
            for (NodoPosicion np : posicionesNodos) {
                double dist = Math.sqrt(Math.pow(e.getX() - np.x, 2) +
                        Math.pow(e.getY() - np.y, 2));
                if (dist <= RADIO_NODO) {
                    sobreNodo = true;
                    break;
                }
            }
            canvas.setCursor(sobreNodo
                    ? javafx.scene.Cursor.HAND
                    : javafx.scene.Cursor.DEFAULT
            );
        });
    }

    /**
     * Muestra un popup con toda la información del caso al hacer clic en un nodo.
     */
    private void mostrarTarjetaNodo(Caso caso, double screenX, double screenY) {
        if (popupNodo != null && popupNodo.isShowing()) {
            popupNodo.hide();
        }

        popupNodo = new Popup();
        popupNodo.setAutoHide(true);

        VBox tarjeta = new VBox(12);
        tarjeta.setStyle(
                "-fx-background-color: #13131f;" +
                        "-fx-border-color: #00d4ff40;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 20 24 20 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 24, 0, 0, 8);"
        );
        tarjeta.setMaxWidth(340);

        // Cabecera
        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Caso #" + caso.getId());
        idLabel.setStyle(
                "-fx-font-family: 'DM Mono'; -fx-font-size: 19px;" +
                        "-fx-font-weight: 500; -fx-text-fill: #00d4ff;" +
                        "-fx-background-color: #00d4ff15;" +
                        "-fx-border-color: #00d4ff30; -fx-border-width: 1;" +
                        "-fx-border-radius: 20; -fx-background-radius: 20;" +
                        "-fx-padding: 3 10 3 10;"
        );

        Label gravedadLabel = new Label("Gravedad: " + caso.getGravedad() + "/10");
        gravedadLabel.setStyle(
                "-fx-font-size: 19px; -fx-text-fill: #3a3a5c;" +
                        "-fx-font-family: 'DM Mono';"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        cabecera.getChildren().addAll(idLabel, spacer, gravedadLabel);

        // Tipo de acoso
        Label tipoLabel = new Label(caso.getTipoAcoso());
        tipoLabel.setWrapText(true);
        tipoLabel.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600; -fx-text-fill: #f0f0f8;"
        );

        // Separador visual
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #1e1e2e;");

        // Evidencias
        Label evEtiqueta = new Label("EVIDENCIAS");
        evEtiqueta.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );

        VBox evBox = new VBox(4);
        for (String ev : caso.getEvidencias()) {
            Label evLabel = new Label("◆  " + ev);
            evLabel.setWrapText(true);
            evLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #8080a0;");
            evBox.getChildren().add(evLabel);
        }

        // Ley y pena
        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color: #1e1e2e;");

        Label leyEtiqueta = new Label("MARCO LEGAL");
        leyEtiqueta.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 600;" +
                        "-fx-text-fill: #3a3a5c; -fx-letter-spacing: 0.1em;"
        );

        Label leyLabel = new Label(caso.getLeyColombia());
        leyLabel.setWrapText(true);
        leyLabel.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: 500; -fx-text-fill: #00d4ff;"
        );

        Label penaLabel = new Label("Pena: " + caso.getPenaAplicable());
        penaLabel.setWrapText(true);
        penaLabel.setStyle("-fx-font-size: 19px; -fx-text-fill: #5a5a7a;");

        Label hinLabel = new Label("Clic fuera para cerrar");
        hinLabel.setStyle(
                "-fx-font-size: 17px; -fx-text-fill: #2a2a3c; -fx-font-style: italic;"
        );

        javafx.scene.control.Button btnEvidencia = new javafx.scene.control.Button("Ir a la evidencia →");
        btnEvidencia.setStyle("-fx-background-color: #00d4ff20; -fx-text-fill: #00d4ff; -fx-font-size: 19px; -fx-font-weight: 700; -fx-padding: 6 12; -fx-cursor: hand; -fx-border-color: #00d4ff60; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnEvidencia.setMaxWidth(Double.MAX_VALUE);
        btnEvidencia.setOnAction(e -> {
            if (popupNodo != null && popupNodo.isShowing()) popupNodo.hide();
            if (onIrEvidencia != null) {
                Platform.runLater(() -> onIrEvidencia.accept(caso));
            }
        });

        tarjeta.getChildren().addAll(
                cabecera, tipoLabel, sep,
                evEtiqueta, evBox, sep2,
                leyEtiqueta, leyLabel, penaLabel,
                btnEvidencia, hinLabel
        );

        // Animación de entrada
        tarjeta.setOpacity(0);
        popupNodo.getContent().add(tarjeta);
        popupNodo.show(canvas, screenX + 12, screenY - 80);

        FadeTransition ft = new FadeTransition(Duration.millis(200), tarjeta);
        ft.setToValue(1);
        ft.play();
    }
    
    // ── Dibujo del árbol ───────────────────────────────────────────────

    /**
     * Redibuja el árbol completo y anima el canvas cuando entra un nodo nuevo.
     */
    public void actualizar(ArbolAVL arbol) {
        posicionesNodos.clear();
        gc.clearRect(0, 0, ANCHO_CANVAS, ALTO_CANVAS);

        if (arbol == null || arbol.estaVacio()) {
            dibujarVacio();
            return;
        }

        dibujarArbol(arbol.getRaiz(), ANCHO_CANVAS / 2, 36, ANCHO_CANVAS / 4);
        animarNuevoNodo();
    }

    private void dibujarArbol(NodoAVL nodo, double x, double y, double offset) {
        if (nodo == null) return;

        if (nodo.getIzquierdo() != null) {
            double xH = x - offset;
            double yH = y + ESPACIADO_V;
            dibujarArista(x, y, xH, yH);
            dibujarArbol(nodo.getIzquierdo(), xH, yH, offset / 2);
        }

        if (nodo.getDerecho() != null) {
            double xH = x + offset;
            double yH = y + ESPACIADO_V;
            dibujarArista(x, y, xH, yH);
            dibujarArbol(nodo.getDerecho(), xH, yH, offset / 2);
        }

        dibujarNodo(nodo, x, y);
        posicionesNodos.add(new NodoPosicion(x, y, nodo.getCaso()));
    }

    private void dibujarArista(double x1, double y1, double x2, double y2) {
        gc.setStroke(COLOR_ARISTA);
        gc.setLineWidth(1.5);
        gc.strokeLine(x1, y1, x2, y2);
    }

    private void dibujarNodo(NodoAVL nodo, double x, double y) {
        // Halo exterior
        gc.setFill(Color.web("#00d4ff", 0.05));
        gc.fillOval(x - RADIO_NODO - 4, y - RADIO_NODO - 4,
                (RADIO_NODO + 4) * 2, (RADIO_NODO + 4) * 2);

        // Fondo del nodo
        gc.setFill(COLOR_NODO_FONDO);
        gc.fillOval(x - RADIO_NODO, y - RADIO_NODO,
                RADIO_NODO * 2, RADIO_NODO * 2);

        // Borde
        gc.setStroke(COLOR_NODO_BORDE);
        gc.setLineWidth(1.5);
        gc.strokeOval(x - RADIO_NODO, y - RADIO_NODO,
                RADIO_NODO * 2, RADIO_NODO * 2);

        // ID del caso
        gc.setFill(COLOR_TEXTO_ID);
        gc.setFont(Font.font("DM Sans", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("C" + nodo.getCaso().getId(), x, y + 2);

        // Gravedad
        gc.setFill(COLOR_TEXTO_GRAV);
        gc.setFont(Font.font("DM Mono", FontWeight.NORMAL, 9));
        gc.fillText("G:" + nodo.getCaso().getGravedad(), x, y + 14);

        // Factor de equilibrio
        int altIzq = nodo.getIzquierdo() != null ? nodo.getIzquierdo().getAltura() : 0;
        int altDer = nodo.getDerecho()   != null ? nodo.getDerecho().getAltura()   : 0;
        int fe = altIzq - altDer;

        gc.setFill(fe == 0 ? Color.web("#2a2a4a") : Color.web("#ff4466"));
        gc.setFont(Font.font("DM Mono", 8));
        gc.fillText("fe:" + fe, x + RADIO_NODO + 4, y - RADIO_NODO + 8);

        // Indicador de que es clickeable
        gc.setFill(Color.web("#00d4ff", 0.3));
        gc.setFont(Font.font("DM Sans", 7));
        gc.fillText("▲ info", x, y - RADIO_NODO - 4);
    }

    private void dibujarVacio() {
        gc.setFill(COLOR_FONDO);
        gc.fillRect(0, 0, ANCHO_CANVAS, ALTO_CANVAS);

        gc.setFill(COLOR_VACIO);
        gc.setFont(Font.font("DM Sans", FontWeight.NORMAL, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(
                "El árbol de investigación está vacío — " +
                        "completa el primer nivel para insertar el primer caso.",
                ANCHO_CANVAS / 2, ALTO_CANVAS / 2 - 8
        );
        gc.setFont(Font.font("DM Sans", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#1e1e2e"));
        gc.fillText(
                "Los nodos se organizan por gravedad del delito (1 = leve → 10 = grave)",
                ANCHO_CANVAS / 2, ALTO_CANVAS / 2 + 14
        );
    }

    /**
     * Pulso visual breve en el canvas cuando se inserta un nuevo nodo.
     */
    private void animarNuevoNodo() {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), canvas);
        st.setFromX(0.97);
        st.setFromY(0.97);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
}