package cyberdetective.view;

import cyberdetective.model.Caso;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PanelEvidencias extends VBox {

    public PanelEvidencias() {
        super(16);
        setStyle("-fx-background-color: transparent;");
    }

    /**
     * Carga y muestra toda la información legal y de evidencias
     * del caso recibido. Cada evidencia aparece con una animación
     * de entrada escalonada.
     */
    public void cargarCaso(Caso caso) {
        getChildren().clear();

        if (caso == null) return;

        // Cabecera del caso
        Label tipoCasoLabel = new Label(caso.getTipoAcoso().toUpperCase());
        tipoCasoLabel.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-text-fill: #00d4ff;" +
                        "-fx-letter-spacing: 0.12em;"
        );

        Label descripcionLabel = new Label(caso.getDescripcion());
        descripcionLabel.setWrapText(true);
        descripcionLabel.setStyle(
                "-fx-font-size: 19px;" +
                        "-fx-text-fill: #9090b0;" +
                        "-fx-line-spacing: 3;"
        );

        VBox cabecera = new VBox(6, tipoCasoLabel, descripcionLabel);
        cabecera.setStyle(
                "-fx-background-color: #12121c;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 16 18 16 18;"
        );

        getChildren().add(cabecera);

        // Sección de evidencias
        Label secEvidencias = new Label("EVIDENCIAS");
        secEvidencias.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-text-fill: #3a3a5c;" +
                        "-fx-letter-spacing: 0.12em;"
        );
        getChildren().add(secEvidencias);

        // Cada evidencia con animación escalonada
        String[] evidencias = caso.getEvidencias();
        for (int i = 0; i < evidencias.length; i++) {
            VBox tarjeta = construirTarjetaEvidencia(evidencias[i], i + 1);
            getChildren().add(tarjeta);
            animarEntradaEscalonada(tarjeta, i * 80);
        }

        // Sección legal
        Label secLegal = new Label("MARCO LEGAL");
        secLegal.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-text-fill: #3a3a5c;" +
                        "-fx-letter-spacing: 0.12em;"
        );
        VBox.setMargin(secLegal, new Insets(8, 0, 0, 0));

        VBox panelLegal = construirPanelLegal(caso);

        // Gravedad visual
        VBox panelGravedad = construirBarraGravedad(caso.getGravedad());

        getChildren().addAll(secLegal, panelLegal, panelGravedad);
    }

    private VBox construirTarjetaEvidencia(String evidencia, int numero) {
        Label numLabel = new Label(String.format("%02d", numero));
        numLabel.setStyle(
                "-fx-font-family: 'DM Mono';" +
                        "-fx-font-size: 19px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-text-fill: #00d4ff;" +
                        "-fx-min-width: 24px;"
        );

        Label evLabel = new Label(evidencia);
        evLabel.setWrapText(true);
        evLabel.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-text-fill: #c0c0d8;" +
                        "-fx-line-spacing: 2;"
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
                        "-fx-padding: 12 16 12 16;" +
                        "-fx-cursor: hand;"
        );

        // Hover sutil
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(
                "-fx-background-color: #00d4ff08;" +
                        "-fx-border-color: #00d4ff55;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 16 12 16;" +
                        "-fx-cursor: hand;"
        ));

        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(
                "-fx-background-color: #0f0f1a;" +
                        "-fx-border-color: #00d4ff22;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 16 12 16;" +
                        "-fx-cursor: hand;"
        ));

        return tarjeta;
    }

    private VBox construirPanelLegal(Caso caso) {
        Label leyLabel = new Label(caso.getLeyColombia());
        leyLabel.setStyle(
                "-fx-font-family: 'DM Mono';" +
                        "-fx-font-size: 17px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-text-fill: #00d4ff;" +
                        "-fx-wrap-text: true;"
        );
        leyLabel.setWrapText(true);

        Label penaLabel = new Label("Pena: " + caso.getPenaAplicable());
        penaLabel.setWrapText(true);
        penaLabel.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-text-fill: #6b6b8a;" +
                        "-fx-line-spacing: 2;" +
                        "-fx-wrap-text: true;"
        );

        VBox panel = new VBox(8, leyLabel, penaLabel);
        panel.setStyle(
                "-fx-background-color: #0d0d16;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 16 14 16;"
        );

        return panel;
    }

    /**
     * Barra visual que representa la gravedad del caso del 1 al 10.
     * El color va de amarillo (leve) a rojo (grave).
     */
    private VBox construirBarraGravedad(int gravedad) {
        Label label = new Label("GRAVEDAD DEL DELITO");
        label.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-text-fill: #3a3a5c;" +
                        "-fx-letter-spacing: 0.12em;"
        );

        HBox segmentos = new HBox(3);
        segmentos.setAlignment(Pos.CENTER_LEFT);

        for (int i = 1; i <= 10; i++) {
            VBox seg = new VBox();
            seg.setPrefWidth(20);
            seg.setPrefHeight(6);

            String color;
            if (i <= 3)       color = i <= gravedad ? "#ffd166" : "#1e1e2e";
            else if (i <= 6)  color = i <= gravedad ? "#ff9f1c" : "#1e1e2e";
            else if (i <= 8)  color = i <= gravedad ? "#ff6b6b" : "#1e1e2e";
            else               color = i <= gravedad ? "#ff0044" : "#1e1e2e";

            seg.setStyle(
                    "-fx-background-color: " + color + ";" +
                            "-fx-background-radius: 2;"
            );

            segmentos.getChildren().add(seg);
        }

        Label valorLabel = new Label(gravedad + " / 10");
        valorLabel.setStyle(
                "-fx-font-family: 'DM Mono';" +
                        "-fx-font-size: 19px;" +
                        "-fx-text-fill: #6b6b8a;"
        );

        HBox fila = new HBox(12, segmentos, valorLabel);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox contenedor = new VBox(8, label, fila);
        VBox.setMargin(contenedor, new Insets(4, 0, 0, 0));

        return contenedor;
    }

    private void animarEntradaEscalonada(VBox nodo, int delayMs) {
        nodo.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), nodo);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.play();
    }
}