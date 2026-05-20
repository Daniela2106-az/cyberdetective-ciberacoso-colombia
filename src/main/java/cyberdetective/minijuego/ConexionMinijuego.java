package cyberdetective.minijuego;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minijuego "Conexión":
 * Dos columnas de nodos (términos a la izquierda, definiciones a la derecha).
 * El jugador conecta los nodos dibujando líneas entre ellos.
 */
public class ConexionMinijuego extends Minijuego {

    private static final double W = 700;
    private static final double H = 550;

    private Pane drawingPane;
    private VBox leftCol;
    private VBox rightCol;

    private ConexionNode selectedLeft = null;
    private ConexionNode selectedRight = null;

    private Line currentTempLine = null;
    private List<Line> permanentLines = new ArrayList<>();
    private int pairsCompleted = 0;

    public ConexionMinijuego() {
        setPrefSize(W, H);
        setMaxSize(W, H);
        setStyle("-fx-background-color: #0d0d1a; -fx-border-color: #1e1e3f; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
    }

    private int targetPairId = -1;

    @Override
    public void iniciar() {
        getChildren().clear();
        permanentLines.clear();
        selectedLeft = null;
        selectedRight = null;
        targetPairId = -1;

        Label instruccion = new Label("🔗 Conecta la agresión CORRECTA con su definición correspondiente");
        instruccion.setStyle("-fx-text-fill:#00d4ff;-fx-font-size:13px;-fx-font-weight:600;");
        instruccion.setLayoutX(100);
        instruccion.setLayoutY(20);

        drawingPane = new Pane();
        drawingPane.setPrefSize(W, H - 100);
        drawingPane.setLayoutY(90);
        
        javafx.scene.control.Button btnVerEvidencia = new javafx.scene.control.Button("🔍 Revisar Evidencia");
        btnVerEvidencia.getStyleClass().add("btn-secundario");
        btnVerEvidencia.setLayoutX(W/2 - 70);
        btnVerEvidencia.setLayoutY(50);
        btnVerEvidencia.setOnAction(e -> {
            if (onVerEvidenciaRequest != null) onVerEvidenciaRequest.run();
        });
        
        // Intercept mouse movements on drawing pane to draw a temp line if something is selected
        drawingPane.setOnMouseMoved(e -> {
            if (currentTempLine != null) {
                currentTempLine.setEndX(e.getX());
                currentTempLine.setEndY(e.getY());
            }
        });

        leftCol = new VBox(20);
        leftCol.setAlignment(Pos.CENTER_RIGHT);
        leftCol.setLayoutX(30);
        leftCol.setLayoutY(20);

        rightCol = new VBox(20);
        rightCol.setAlignment(Pos.CENTER_LEFT);
        rightCol.setLayoutX(W - 330);
        rightCol.setLayoutY(20);

        drawingPane.getChildren().addAll(leftCol, rightCol);
        getChildren().addAll(instruccion, btnVerEvidencia, drawingPane);

        String data = datosExtra;
        if (data == null || data.isEmpty()) {
            data = "*Injuria:Ofensa al honor|Amenaza:Anuncio de un mal|Extorsión:Exigencia por no dañar";
        }

        String[] pairsData = data.split("\\|");
        List<ConexionNode> leftNodes = new ArrayList<>();
        List<ConexionNode> rightNodes = new ArrayList<>();

        for (int i = 0; i < pairsData.length; i++) {
            String[] parts = pairsData[i].split(":");
            if (parts.length == 2) {
                String leftText = parts[0];
                if (leftText.startsWith("*")) {
                    targetPairId = i;
                    leftText = leftText.substring(1);
                }
                ConexionNode leftNode = new ConexionNode(leftText, i, true);
                ConexionNode rightNode = new ConexionNode(parts[1], i, false);
                leftNodes.add(leftNode);
                rightNodes.add(rightNode);
            }
        }
        
        // Si no se especificó un target con *, el target es el 0
        if (targetPairId == -1) targetPairId = 0;

        Collections.shuffle(leftNodes);
        Collections.shuffle(rightNodes);

        leftCol.getChildren().addAll(leftNodes);
        rightCol.getChildren().addAll(rightNodes);

        tiempoInicio = System.currentTimeMillis();
    }

    @Override
    public void finalizar() {
        // ...
    }

    private void handleNodeClick(ConexionNode node) {
        if (node.isMatched) return;

        if (node.isLeft) {
            if (selectedLeft != null) selectedLeft.setSelected(false);
            selectedLeft = node;
            selectedLeft.setSelected(true);
        } else {
            if (selectedRight != null) selectedRight.setSelected(false);
            selectedRight = node;
            selectedRight.setSelected(true);
        }

        updateTempLine();

        if (selectedLeft != null && selectedRight != null) {
            checkMatch();
        }
    }

    private void updateTempLine() {
        if (currentTempLine != null) {
            drawingPane.getChildren().remove(currentTempLine);
            currentTempLine = null;
        }

        ConexionNode activeNode = selectedLeft != null ? selectedLeft : selectedRight;
        if (activeNode != null && (selectedLeft == null || selectedRight == null)) {
            Point2D anchor = activeNode.getAnchorPoint();
            currentTempLine = new Line(anchor.getX(), anchor.getY(), anchor.getX(), anchor.getY());
            currentTempLine.setStroke(Color.web("#00d4ff80"));
            currentTempLine.setStrokeWidth(3);
            currentTempLine.setMouseTransparent(true);
            drawingPane.getChildren().add(0, currentTempLine);
        }
    }

    private void checkMatch() {
        if (currentTempLine != null) {
            drawingPane.getChildren().remove(currentTempLine);
            currentTempLine = null;
        }

        if (selectedLeft.pairId == selectedRight.pairId && selectedLeft.pairId == targetPairId) {
            // Correct match AND it is the target!
            Point2D p1 = selectedLeft.getAnchorPoint();
            Point2D p2 = selectedRight.getAnchorPoint();

            Line successLine = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            successLine.setStroke(Color.web("#00ff88"));
            successLine.setStrokeWidth(3);
            drawingPane.getChildren().add(0, successLine);
            permanentLines.add(successLine);

            selectedLeft.setMatched();
            selectedRight.setMatched();
            
            completar();
        } else {
            // Wrong match or correct match but wrong target - show red briefly
            Point2D p1 = selectedLeft.getAnchorPoint();
            Point2D p2 = selectedRight.getAnchorPoint();

            Line errorLine = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            errorLine.setStroke(Color.web("#ff4466"));
            errorLine.setStrokeWidth(3);
            drawingPane.getChildren().add(errorLine);
            
            selectedLeft.showError();
            selectedRight.showError();

            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
            p.setOnFinished(e -> drawingPane.getChildren().remove(errorLine));
            p.play();
        }

        selectedLeft.setSelected(false);
        selectedRight.setSelected(false);
        selectedLeft = null;
        selectedRight = null;
    }

    private class ConexionNode extends HBox {
        int pairId;
        boolean isLeft;
        boolean isMatched = false;
        Rectangle anchorPoint;
        Label textLabel;

        public ConexionNode(String text, int pairId, boolean isLeft) {
            this.pairId = pairId;
            this.isLeft = isLeft;

            setAlignment(Pos.CENTER);
            setSpacing(10);
            setStyle("-fx-background-color: #1a1a2a; -fx-border-color: #2a2a3c; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand;");
            setPrefWidth(isLeft ? 250 : 300);
            setMinHeight(60);

            textLabel = new Label(text);
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 19px;");
            textLabel.setMaxWidth(isLeft ? 200 : 250);

            anchorPoint = new Rectangle(12, 12);
            anchorPoint.setArcWidth(6);
            anchorPoint.setArcHeight(6);
            anchorPoint.setFill(Color.web("#3a3a5c"));

            if (isLeft) {
                getChildren().addAll(textLabel, anchorPoint);
            } else {
                getChildren().addAll(anchorPoint, textLabel);
            }

            setOnMouseClicked(e -> {
                handleNodeClick(this);
            });
        }

        public void setSelected(boolean sel) {
            if (isMatched) return;
            if (sel) {
                setStyle("-fx-background-color: #1a1a2a; -fx-border-color: #00d4ff; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand;");
                anchorPoint.setFill(Color.web("#00d4ff"));
            } else {
                setStyle("-fx-background-color: #1a1a2a; -fx-border-color: #2a2a3c; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand;");
                anchorPoint.setFill(Color.web("#3a3a5c"));
            }
        }

        public void showError() {
            if (isMatched) return;
            setStyle("-fx-background-color: #ff446620; -fx-border-color: #ff4466; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 15;");
            anchorPoint.setFill(Color.web("#ff4466"));
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
            p.setOnFinished(e -> {
                if (!isMatched) {
                    setStyle("-fx-background-color: #1a1a2a; -fx-border-color: #2a2a3c; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand;");
                    anchorPoint.setFill(Color.web("#3a3a5c"));
                }
            });
            p.play();
        }

        public void setMatched() {
            isMatched = true;
            setStyle("-fx-background-color: #00ff8820; -fx-border-color: #00ff88; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 15;");
            anchorPoint.setFill(Color.web("#00ff88"));
            textLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 19px; -fx-font-weight: bold;");
            setDisable(true);
        }

        public Point2D getAnchorPoint() {
            // Calculate absolute position of the anchorPoint relative to drawingPane
            javafx.geometry.Bounds bounds = anchorPoint.localToScene(anchorPoint.getBoundsInLocal());
            javafx.geometry.Bounds drawingBounds = drawingPane.sceneToLocal(bounds);
            return new Point2D(drawingBounds.getMinX() + drawingBounds.getWidth() / 2,
                               drawingBounds.getMinY() + drawingBounds.getHeight() / 2);
        }
    }
}
