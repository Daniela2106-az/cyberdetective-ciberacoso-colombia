package cyberdetective.minijuego;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Minijuego "Sopa de Letras":
 * Cuadrícula de 8x8 con caracteres aleatorios.
 * Una palabra oculta de forma horizontal o vertical.
 * El jugador debe hacer clic en todas las letras correctas.
 */
public class SopaLetrasMinijuego extends Minijuego {

    private static final int FILAS = 12;
    private static final int COLS = 12;
    private static final double W = 650;
    private static final double H = 650;

    private GridPane grid;
    private List<LetraCasilla> letrasObjetivo = new ArrayList<>();
    private int letrasEncontradas = 0;
    private boolean completado = false;
    private boolean modoIP = false;

    public void setModoIP(boolean modoIP) {
        this.modoIP = modoIP;
    }

    public SopaLetrasMinijuego() {
        setPrefSize(W, H);
        setMaxSize(W, H);
        setStyle("-fx-background-color: #0d0d1a; -fx-border-color: #1e1e3f; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    @Override
    public void iniciar() {
        getChildren().clear();
        letrasObjetivo.clear();
        letrasEncontradas = 0;
        completado = false;

        String palabra = datosExtra != null && !datosExtra.isEmpty() ? datosExtra.toUpperCase() : "@AGRESOR";
        if (palabra.length() > 12) palabra = palabra.substring(0, 12); // Max 12 chars para 12x12

        String textInstr = modoIP ? "🔍 Se requiere identificar la dirección IP del servidor." : "🔍 Se requiere identificar el nombre de usuario del agresor.";
        Label instruccion = new Label(textInstr);
        instruccion.setStyle("-fx-text-fill:#00d4ff;-fx-font-size:13px;-fx-font-weight:600;");

        javafx.scene.control.Button btnVerEvidencia = new javafx.scene.control.Button("🔍 Revisar Evidencia");
        btnVerEvidencia.getStyleClass().add("btn-secundario");
        btnVerEvidencia.setOnAction(e -> {
            if (onVerEvidenciaRequest != null) onVerEvidenciaRequest.run();
        });

        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(4);
        grid.setVgap(4);

        char[][] tablero = new char[FILAS][COLS];
        boolean[][] esObjetivo = new boolean[FILAS][COLS];
        Random rnd = new Random();

        // 1. Decidir orientación y posición
        boolean horizontal = rnd.nextBoolean();
        int len = palabra.length();
        int rFila = horizontal ? rnd.nextInt(FILAS) : rnd.nextInt(FILAS - len + 1);
        int rCol = horizontal ? rnd.nextInt(COLS - len + 1) : rnd.nextInt(COLS);

        // 2. Colocar palabra
        for (int i = 0; i < len; i++) {
            int f = horizontal ? rFila : rFila + i;
            int c = horizontal ? rCol + i : rCol;
            tablero[f][c] = palabra.charAt(i);
            esObjetivo[f][c] = true;
        }

        // 3. Rellenar y crear nodos
        for (int i = 0; i < FILAS; i++) {
            for (int j = 0; j < COLS; j++) {
                char c = tablero[i][j];
                if (!esObjetivo[i][j]) {
                    if (modoIP) {
                        if (rnd.nextInt(5) == 0) c = '.';
                        else c = (char) (rnd.nextInt(10) + '0');
                    } else {
                        if (rnd.nextInt(5) == 0) c = '@';
                        else c = (char) (rnd.nextInt(26) + 'A');
                    }
                }
                
                LetraCasilla casilla = new LetraCasilla(c, esObjetivo[i][j]);
                grid.add(casilla, j, i);
                
                if (esObjetivo[i][j]) {
                    letrasObjetivo.add(casilla);
                }
            }
        }

        VBox vbox = new VBox(20, instruccion, btnVerEvidencia, grid);
        vbox.setAlignment(Pos.CENTER);
        vbox.prefWidthProperty().bind(this.widthProperty());
        vbox.prefHeightProperty().bind(this.heightProperty());

        getChildren().add(vbox);
        tiempoInicio = System.currentTimeMillis();
    }

    @Override
    public void finalizar() {
        completado = true;
    }

    private void verificarEstado() {
        if (completado) return;
        
        boolean win = letrasObjetivo.stream().allMatch(LetraCasilla::isSeleccionada);
        if (win) {
            completado = true;
            // Destacar todas
            for (LetraCasilla c : letrasObjetivo) {
                c.bg.setFill(Color.web("#00ff88"));
                c.lbl.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 29px;");
            }
            completar();
        }
    }

    private class LetraCasilla extends StackPane {
        Rectangle bg;
        Label lbl;
        boolean objetivo;
        boolean seleccionada = false;

        public LetraCasilla(char c, boolean esObjetivo) {
            this.objetivo = esObjetivo;
            bg = new Rectangle(40, 40);
            bg.setFill(Color.web("#1a1a2a"));
            bg.setStroke(Color.web("#2a2a3c"));
            bg.setArcWidth(8);
            bg.setArcHeight(8);

            lbl = new Label(String.valueOf(c));
            lbl.setStyle("-fx-text-fill: #8080a0; -fx-font-family: 'DM Mono'; -fx-font-size: 18px; -fx-font-weight: bold;");

            getChildren().addAll(bg, lbl);

            setOnMouseClicked(e -> {
                if (completado) return;
                
                if (seleccionada) {
                    // Desmarcar
                    seleccionada = false;
                    bg.setFill(Color.web("#1a1a2a"));
                    lbl.setStyle("-fx-text-fill: #8080a0; -fx-font-family: 'DM Mono'; -fx-font-size: 18px; -fx-font-weight: bold;");
                } else {
                    // Marcar
                    seleccionada = true;
                    if (objetivo) {
                        bg.setFill(Color.web("#00d4ff50"));
                        bg.setStroke(Color.web("#00d4ff"));
                        lbl.setStyle("-fx-text-fill: #00d4ff; -fx-font-family: 'DM Mono'; -fx-font-size: 18px; -fx-font-weight: bold;");
                        verificarEstado();
                    } else {
                        // Error visual temporal
                        bg.setFill(Color.web("#ff446650"));
                        bg.setStroke(Color.web("#ff4466"));
                        lbl.setStyle("-fx-text-fill: #ff4466; -fx-font-family: 'DM Mono'; -fx-font-size: 18px; -fx-font-weight: bold;");
                        javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                        p.setOnFinished(ev -> {
                            if (!completado) {
                                seleccionada = false;
                                bg.setFill(Color.web("#1a1a2a"));
                                bg.setStroke(Color.web("#2a2a3c"));
                                lbl.setStyle("-fx-text-fill: #8080a0; -fx-font-family: 'DM Mono'; -fx-font-size: 18px; -fx-font-weight: bold;");
                            }
                        });
                        p.play();
                    }
                }
            });
            setStyle("-fx-cursor: hand;");
        }

        public boolean isSeleccionada() {
            return seleccionada;
        }
    }
}
