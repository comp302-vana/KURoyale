package kuroyale.mainpack.managers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;
import kuroyale.entitiypack.subclasses.AliveEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Handles visual effects for combo triggers.
 * Creates animated visual feedback when combos are activated.
 */
public class ComboVisualEffects {

    private final Pane effectLayer;
    private final int tileSize;
    private final Random random = new Random();
    private EntityRenderer entityRenderer;

    // Track active effects by entity for cleanup
    private final Map<AliveEntity, javafx.scene.Node> activeEffects = new HashMap<>();

    public ComboVisualEffects(Pane effectLayer, int tileSize) {
        this.effectLayer = effectLayer;
        this.tileSize = tileSize;
    }

    public void setEntityRenderer(EntityRenderer entityRenderer) {
        this.entityRenderer = entityRenderer;
    }

    /**
     * Helper to bind effect position to entity sprite.
     * Returns true if binding was successful, false otherwise.
     */
    private boolean bindToEntity(AliveEntity entity, javafx.scene.Node effectNode, double offsetX, double offsetY) {
        if (entityRenderer == null)
            return false;

        javafx.scene.image.ImageView sprite = entityRenderer.getEntitySprite(entity);
        if (sprite != null) {
            effectNode.layoutXProperty().bind(sprite.layoutXProperty().add(offsetX));
            effectNode.layoutYProperty().bind(sprite.layoutYProperty().add(offsetY));
            effectNode.translateXProperty().bind(sprite.translateXProperty());
            effectNode.translateYProperty().bind(sprite.translateYProperty());
            return true;
        }
        return false;
    }

    /**
     * Show gold glow effect around an entity (Tank + Support combo).
     * Creates a pulsing golden circle around the ranged unit.
     */
    public void showGoldGlow(AliveEntity entity, double x, double y) {
        Platform.runLater(() -> {
            // Create golden glow circle
            Circle glowCircle = new Circle(tileSize * 0.8);
            glowCircle.setFill(Color.TRANSPARENT);
            glowCircle.setStroke(Color.GOLD);
            glowCircle.setStrokeWidth(3);

            // Add glow effect
            DropShadow goldShadow = new DropShadow();
            goldShadow.setColor(Color.GOLD);
            goldShadow.setRadius(15);
            goldShadow.setSpread(0.5);

            Glow glow = new Glow(0.8);
            goldShadow.setInput(glow);
            glowCircle.setEffect(goldShadow);

            // Position at entity center
            boolean bound = bindToEntity(entity, glowCircle, tileSize / 2.0, tileSize / 2.0);
            if (!bound) {
                // Fallback to static position if sprite not found
                glowCircle.setCenterX(x + tileSize / 2.0);
                glowCircle.setCenterY(y + tileSize / 2.0);
            } else {
                // If bound, set center to 0,0 relative to layout properies
                glowCircle.setCenterX(0);
                glowCircle.setCenterY(0);
            }

            glowCircle.setMouseTransparent(true);

            effectLayer.getChildren().add(glowCircle);
            activeEffects.put(entity, glowCircle);

            // Pulse animation (scale up and down)
            ScaleTransition pulse = new ScaleTransition(Duration.millis(500), glowCircle);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.3);
            pulse.setToY(1.3);
            pulse.setCycleCount(6); // 3 full cycles
            pulse.setAutoReverse(true);

            // Fade out at the end
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), glowCircle);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.millis(2500));
            fadeOut.setOnFinished(e -> {
                effectLayer.getChildren().remove(glowCircle);
                activeEffects.remove(entity);
            });

            pulse.play();
            fadeOut.play();

            System.out.println("Gold Glow effect shown for Tank+Support combo");
        });
    }

    /**
     * Show sparkle effect at a position (Spell Synergy combo).
     * Creates multiple small stars that spread and fade.
     */
    public void showSparkleEffect(double centerX, double centerY) {
        Platform.runLater(() -> {
            Pane sparkleContainer = new Pane();
            sparkleContainer.setMouseTransparent(true);
            sparkleContainer.setLayoutX(centerX);
            sparkleContainer.setLayoutY(centerY);

            // Create multiple sparkle stars
            int numSparkles = 8;
            for (int i = 0; i < numSparkles; i++) {
                // Create a small star shape
                Polygon star = createStar(5, 3, 6);
                star.setFill(Color.WHITE);
                star.setStroke(Color.CYAN);
                star.setStrokeWidth(1);

                // Add glow effect
                DropShadow starGlow = new DropShadow();
                starGlow.setColor(Color.CYAN);
                starGlow.setRadius(8);
                starGlow.setSpread(0.6);
                star.setEffect(starGlow);

                // Start at center (relative to container)
                star.setLayoutX(0);
                star.setLayoutY(0);

                sparkleContainer.getChildren().add(star);

                // Random direction for spreading
                double angle = (2 * Math.PI * i) / numSparkles + random.nextDouble() * 0.5;
                double distance = 30 + random.nextDouble() * 20;
                double targetX = Math.cos(angle) * distance;
                double targetY = Math.sin(angle) * distance;

                // Spread animation
                TranslateTransition spread = new TranslateTransition(Duration.millis(600), star);
                spread.setToX(targetX);
                spread.setToY(targetY);

                // Fade and shrink
                FadeTransition fade = new FadeTransition(Duration.millis(600), star);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);

                ScaleTransition shrink = new ScaleTransition(Duration.millis(600), star);
                shrink.setFromX(1.0);
                shrink.setFromY(1.0);
                shrink.setToX(0.3);
                shrink.setToY(0.3);

                // Rotation
                star.setRotate(random.nextDouble() * 360);

                ParallelTransition sparkleAnim = new ParallelTransition(spread, fade, shrink);
                sparkleAnim.setDelay(Duration.millis(i * 50)); // Stagger the animations
                sparkleAnim.play();
            }

            effectLayer.getChildren().add(sparkleContainer);

            // Remove container after animations complete
            Timeline cleanup = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
                effectLayer.getChildren().remove(sparkleContainer);
            }));
            cleanup.play();

            System.out.println("Sparkle effect shown for Spell Synergy combo");
        });
    }

    /**
     * Show speed lines effect on an entity (Swarm Attack combo).
     * Creates horizontal lines streaming behind the unit.
     */
    public void showSpeedLines(AliveEntity entity, double x, double y) {
        Platform.runLater(() -> {
            Pane speedLinesContainer = new Pane();
            speedLinesContainer.setMouseTransparent(true);

            // Create multiple speed lines
            int numLines = 5;
            double entityCenterY = tileSize / 2.0; // Relative to container's Y

            for (int i = 0; i < numLines; i++) {
                double lineY = entityCenterY + (i - numLines / 2) * 4;

                Line speedLine = new Line();
                speedLine.setStartX(-5); // Relative to container's X
                speedLine.setStartY(lineY);
                speedLine.setEndX(-25); // Relative to container's X
                speedLine.setEndY(lineY);
                speedLine.setStroke(Color.LIGHTBLUE);
                speedLine.setStrokeWidth(2);
                speedLine.setOpacity(0.8);

                // Glow effect
                DropShadow lineGlow = new DropShadow();
                lineGlow.setColor(Color.CYAN);
                lineGlow.setRadius(5);
                speedLine.setEffect(lineGlow);

                speedLinesContainer.getChildren().add(speedLine);

                // Animate: extend and fade
                Timeline lineAnim = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(speedLine.startXProperty(), -5),
                                new KeyValue(speedLine.endXProperty(), -15),
                                new KeyValue(speedLine.opacityProperty(), 0.0)),
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(speedLine.startXProperty(), -10),
                                new KeyValue(speedLine.endXProperty(), -35),
                                new KeyValue(speedLine.opacityProperty(), 0.9)),
                        new KeyFrame(Duration.millis(600),
                                new KeyValue(speedLine.startXProperty(), -20),
                                new KeyValue(speedLine.endXProperty(), -50),
                                new KeyValue(speedLine.opacityProperty(), 0.0)));
                lineAnim.setCycleCount(3);
                lineAnim.setDelay(Duration.millis(i * 80));
                lineAnim.play();
            }

            // Bind container to entity
            boolean bound = bindToEntity(entity, speedLinesContainer, tileSize / 2.0, tileSize / 2.0);
            if (!bound) {
                speedLinesContainer.setLayoutX(x);
                speedLinesContainer.setLayoutY(y);
            }

            effectLayer.getChildren().add(speedLinesContainer);
            activeEffects.put(entity, speedLinesContainer);

            // Remove after animation completes
            Timeline cleanup = new Timeline(new KeyFrame(Duration.millis(2000), e -> {
                effectLayer.getChildren().remove(speedLinesContainer);
                activeEffects.remove(entity);
            }));
            cleanup.play();

            System.out.println("Speed Lines effect shown for Swarm Attack combo");
        });
    }

    /**
     * Show shield icon effect on an entity (Building Defense combo).
     * Creates a blue shield that pulses above the building.
     */
    public void showShieldIcon(AliveEntity entity, double x, double y) {
        Platform.runLater(() -> {
            // Create shield shape (simplified shield polygon)
            Polygon shield = new Polygon(
                    0, -12, // Top
                    12, -6, // Top right
                    12, 6, // Bottom right
                    0, 15, // Bottom point
                    -12, 6, // Bottom left
                    -12, -6 // Top left
            );
            shield.setFill(Color.rgb(50, 150, 255, 0.7));
            shield.setStroke(Color.WHITE);
            shield.setStrokeWidth(2);

            // Add glow effect
            DropShadow shieldGlow = new DropShadow();
            shieldGlow.setColor(Color.DODGERBLUE);
            shieldGlow.setRadius(10);
            shieldGlow.setSpread(0.4);
            shield.setEffect(shieldGlow);

            // Position above entity
            boolean bound = bindToEntity(entity, shield, tileSize / 2.0, -10);
            if (!bound) {
                shield.setLayoutX(x + tileSize / 2.0);
                shield.setLayoutY(y - 10);
            }

            shield.setMouseTransparent(true);

            effectLayer.getChildren().add(shield);
            activeEffects.put(entity, shield);

            // Pulse animation
            ScaleTransition pulse = new ScaleTransition(Duration.millis(400), shield);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.2);
            pulse.setToY(1.2);
            pulse.setCycleCount(10); // 5 full cycles
            pulse.setAutoReverse(true);

            // Subtle floating animation
            TranslateTransition float_anim = new TranslateTransition(Duration.millis(800), shield);
            float_anim.setFromY(0);
            float_anim.setToY(-5);
            float_anim.setCycleCount(6);
            float_anim.setAutoReverse(true);

            // Fade out at the end
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), shield);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.millis(4500));
            fadeOut.setOnFinished(e -> {
                effectLayer.getChildren().remove(shield);
                activeEffects.remove(entity);
            });

            pulse.play();
            float_anim.play();
            fadeOut.play();

            System.out.println("Shield Icon effect shown for Building Defense combo");
        });
    }

    /**
     * Helper method to create a star polygon.
     */
    private Polygon createStar(double outerRadius, double innerRadius, int points) {
        double[] starPoints = new double[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / 2 + (Math.PI * i) / points;
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            starPoints[i * 2] = Math.cos(angle) * radius;
            starPoints[i * 2 + 1] = Math.sin(angle) * radius;
        }
        return new Polygon(starPoints);
    }

    /**
     * Remove any active effect associated with an entity.
     */
    public void removeEffect(AliveEntity entity) {
        javafx.scene.Node effect = activeEffects.remove(entity);
        if (effect != null) {
            Platform.runLater(() -> effectLayer.getChildren().remove(effect));
        }
    }

    /**
     * Clear all active effects.
     */
    public void clearAllEffects() {
        Platform.runLater(() -> {
            for (javafx.scene.Node effect : activeEffects.values()) {
                effectLayer.getChildren().remove(effect);
            }
            activeEffects.clear();
        });
    }
}
