package cyberdetective.view;

import cyberdetective.controller.JuegoController;
import cyberdetective.controller.MultiplayerJuegoController;
import cyberdetective.data.NivelesData;
import cyberdetective.network.GameMessage;
import cyberdetective.network.NetworkClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.InetAddress;

public class PantallaLobby {
    private Stage stage;
    private NetworkClient client;
    private String playerName;
    private Label lblEstado;
    private Button btnAccion;
    private TextField txtIP;
    private boolean isHosting = false;
    private MultiplayerJuegoController multiController;

    public PantallaLobby(Stage stage, boolean isHosting) {
        this.stage = stage;
        this.client = new NetworkClient();
        this.isHosting = isHosting;
    }

    public void mostrar() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #0a0a0f;");

        Label titulo = new Label(isHosting ? "Crear Nueva Investigación" : "Unirse a Investigación");
        titulo.setStyle("-fx-font-size: 34px; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(350);

        Label lblNombre = new Label("TU NOMBRE DE DETECTIVE:");
        lblNombre.getStyleClass().add("etiqueta-seccion");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Escribe tu nombre...");
        txtNombre.getStyleClass().add("input-nombre");

        txtIP = new TextField("localhost");
        txtIP.getStyleClass().add("input-nombre");
        Label lblIP = new Label("DIRECCIÓN IP DEL SERVIDOR:");
        lblIP.getStyleClass().add("etiqueta-seccion");

        if (isHosting) {
            try {
                String ip = InetAddress.getLocalHost().getHostAddress();
                txtIP.setText("localhost");
                txtIP.setEditable(false);
                lblIP.setText("TU IP (Pásala a tu compañero): " + ip);
            } catch (Exception e) {
                lblIP.setText("TU IP: No detectada");
            }
        }

        btnAccion = new Button(isHosting ? "Iniciar Servidor y Esperar" : "Conectar con el Host");
        btnAccion.getStyleClass().add("btn-primario");
        btnAccion.setPrefWidth(350);

        lblEstado = new Label("Estado: Listo");
        lblEstado.setStyle("-fx-text-fill: #5a5a7a; -fx-font-size: 19px;");

        btnAccion.setOnAction(e -> {
            playerName = txtNombre.getText().trim();
            if (playerName.isEmpty()) {
                lblEstado.setText("Error: Ingresa tu nombre.");
                lblEstado.setTextFill(Color.web("#ff4444"));
                return;
            }
            if (isHosting) {
                lblEstado.setText("Iniciando servidor...");
                lblEstado.setTextFill(Color.web("#00d4ff"));
                
                cyberdetective.server.GameServer.startInBackground();
                
                // Pequeña pausa para asegurar que el server abrió el puerto
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1000));
                pause.setOnFinished(ev -> {
                    lblEstado.setText("✔ SERVIDOR ACTIVO - Esperando conexión...");
                    lblEstado.setTextFill(Color.web("#00ff88"));
                    conectar("127.0.0.1", playerName);
                });
                pause.play();
            } else {
                conectar(txtIP.getText().trim(), playerName);
            }
        });

        Button btnVolver = new Button("← Volver al menú");
        btnVolver.getStyleClass().add("btn-secundario");
        btnVolver.setOnAction(e -> new MenuPrincipal(stage).mostrar());

        form.getChildren().addAll(lblNombre, txtNombre);
        if (!isHosting) form.getChildren().addAll(lblIP, txtIP);
        else form.getChildren().add(lblIP);

        root.getChildren().addAll(titulo, form, btnAccion, lblEstado, btnVolver);

        // Reutilizar la escena si ya existe
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        stage.setFullScreen(true);
    }

    private void conectar(String ip, String name) {
        lblEstado.setText("Conectando...");
        btnAccion.setDisable(true);
        client.connect(ip, 12345, name, this::handleMessage);
    }

    private void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case WAITING:
                lblEstado.setText((String) msg.getData());
                lblEstado.setTextFill(Color.web("#00ff88"));
                break;
            case START:
                int suspectIdx = (int) msg.getData();
                iniciarPartidaMultijugador(suspectIdx);
                break;
            case CHAT:
                lblEstado.setText((String) msg.getData());
                lblEstado.setTextFill(Color.web("#ff4444"));
                btnAccion.setDisable(false);
                break;
        }
    }

    private void iniciarPartidaMultijugador(int suspectIdx) {
        multiController = new MultiplayerJuegoController(playerName, client);
        multiController.setSospechoso(suspectIdx);
        MapaInvestigacion mapa = new MapaInvestigacion(stage, multiController);
        mapa.mostrar();
    }
}
