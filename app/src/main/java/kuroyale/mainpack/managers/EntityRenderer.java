package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.PlacedObject;
import kuroyale.arenapack.SpriteLoader;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;
import kuroyale.mainpack.PointsCounter;

/**
 * Handles all rendering of entities, health bars, and static objects.
 * High cohesion: All rendering-related operations in one place.
 */
public class EntityRenderer {
    private final ArenaMap arenaMap;
    private final Pane entityLayer;
    private final Pane staticLayer;
    private final PointsCounter pointsCounter;
    private final int rows;
    private final int cols;
    private final int tileSize;

    private final Map<AliveEntity, ImageView> entitySprites = new HashMap<>();
    private final Map<AliveEntity, Pane> healthBarsByEntity = new HashMap<>();
    private final Map<Long, ImageView> staticSpritesByCell = new HashMap<>();
    private final IdentityHashMap<AliveEntity, Boolean> seenTowers = new IdentityHashMap<>();
    
    private boolean entityDirty = false;
    private boolean staticDirty = false;

    public EntityRenderer(ArenaMap arenaMap, Pane entityLayer, Pane staticLayer, 
                         PointsCounter pointsCounter, int rows, int cols, int tileSize) {
        this.arenaMap = arenaMap;
        this.entityLayer = entityLayer;
        this.staticLayer = staticLayer;
        this.pointsCounter = pointsCounter;
        this.rows = rows;
        this.cols = cols;
        this.tileSize = tileSize;
    }

    public boolean isEntityDirty() { return entityDirty; }
    public void setEntityDirty(boolean dirty) { this.entityDirty = dirty; }
    public boolean isStaticDirty() { return staticDirty; }
    public void setStaticDirty(boolean dirty) { this.staticDirty = dirty; }

    private long cellKey(int r, int c) {
        return (((long) r) << 32) ^ (c & 0xffffffffL);
    }

    public void ensureHealthBar(AliveEntity e, boolean isTower) {
        if (healthBarsByEntity.containsKey(e)) return;

        var aliveCard = (AliveCard) e.getCard();
        double maxHP = aliveCard != null ? aliveCard.getHp() : e.getHP();

        Pane hb = createHealthBar(e.getHP(), maxHP, isTower, e.isPlayer());
        healthBarsByEntity.put(e, hb);
        entityLayer.getChildren().add(hb);
    }

    public void ensureEntityNode(AliveEntity e, Card card) {
        if (entitySprites.containsKey(e)) return;

        ImageView iv = getEntitySpriteFromCard(card);
        if (iv == null) return;

        ensureHealthBar(e, e instanceof TowerEntity);

        entitySprites.put(e, iv);
        entityLayer.getChildren().addAll(iv);
    }

    public void updateHealthBarFor(AliveEntity e) {
        Pane hb = healthBarsByEntity.get(e);
        if (hb == null) return;

        var aliveCard = (AliveCard) e.getCard();
        double maxHP = aliveCard != null ? aliveCard.getHp() : e.getHP();
        if (maxHP <= 0) return;

        double percent = Math.max(0, Math.min(1, e.getHP() / maxHP));

        Rectangle bg = (Rectangle) hb.getChildren().get(0);
        Rectangle fg = (Rectangle) hb.getChildren().get(1);
        fg.setWidth(bg.getWidth() * percent);
    }

    public void positionEntityNode(AliveEntity e, int row, int col) {
        ImageView iv = entitySprites.get(e);
        Pane hb = healthBarsByEntity.get(e);
        if (iv == null || hb == null) return;

        double x = col * tileSize;
        double y = row * tileSize;

        double spriteW = iv.getFitWidth();
        double spriteH = iv.getFitHeight();
        iv.relocate(x + (tileSize - spriteW) / 2.0, y);

        hb.relocate(x, y + spriteH - hb.getPrefHeight());

        updateHealthBar(hb, e);
    }

    private void updateHealthBar(Pane hb, AliveEntity e) {
        double currentHP = e.getHP();
        double maxHP = ((AliveCard) e.getCard()).getHp();
        double healthPercent = Math.max(0, Math.min(1, currentHP / maxHP));

        if (hb.getChildren().size() < 2) return;

        Rectangle bg = (Rectangle) hb.getChildren().get(0);
        Rectangle health = (Rectangle) hb.getChildren().get(1);

        double barWidth = bg.getWidth();

        health.setWidth(barWidth * healthPercent);
        if (e.isPlayer()) {
            health.setFill(Color.DODGERBLUE);
        } else {
            health.setFill(Color.ORANGERED);
        }
    }

    private void positionTowerHealthBar(TowerEntity tower, int bottomRightRow, int bottomRightCol) {
        int size = tower.isKing() ? 4 : 3;

        int topLeftRow = bottomRightRow;
        int topLeftCol = bottomRightCol;

        Pane hb = healthBarsByEntity.get(tower);
        if (hb == null) return;

        double footprintW = size * tileSize;
        double x = topLeftCol * tileSize;
        double y = topLeftRow * tileSize;

        double barW = hb.getPrefWidth();
        double barH = hb.getPrefHeight();
        hb.relocate(x + (footprintW - barW) / 2.0, y - barH);
    }

    public void renderTowerHealthBars() {
        seenTowers.clear();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity e = arenaMap.getEntity(r, c);
                if (e instanceof TowerEntity tower) {
                    if (tower.getHP() <= 0) continue;

                    if (seenTowers.put(tower, true) != null) continue;

                    ensureHealthBar(tower, true);
                    updateHealthBarFor(tower);
                    positionTowerHealthBar(tower, tower.getRow(), tower.getCol());
                }
            }
        }
    }

    public void renderStaticObjects() {
        staticLayer.getChildren().clear();
        staticSpritesByCell.clear();
        staticLayer.getChildren().add(pointsCounter);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                PlacedObject obj = arenaMap.getObject(r, c);
                if (obj == null) continue;
                if (obj.getType() == ArenaObjectType.ENTITY) continue;

                ImageView iv = SpriteLoader.getSprite(obj.getType(), tileSize);
                if (iv == null) continue;

                iv.relocate(c * tileSize, r * tileSize);

                staticLayer.getChildren().add(iv);
                staticSpritesByCell.put(cellKey(r, c), iv);
            }
        }

        staticDirty = false;
    }

    public void renderEntities() {
        List<AliveEntity> aliveNow = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity e = arenaMap.getEntity(r, c);
                if (e != null && e.getHP() > 0 && !(e instanceof TowerEntity)) {
                    aliveNow.add(e);
                    ensureEntityNode(e, e.getCard());
                    positionEntityNode(e, r, c);
                }
            }
        }

        entitySprites.keySet().removeIf(e -> {
            boolean dead = e.getHP() <= 0 || !aliveNow.contains(e);
            if (dead) {
                ImageView iv = entitySprites.get(e);
                Pane hb = healthBarsByEntity.get(e);
                entityLayer.getChildren().removeAll(iv, hb);
                healthBarsByEntity.remove(e);
            }
            return dead;
        });
    }

    public void removeEntitySprite(AliveEntity entity) {
        Pane hb = healthBarsByEntity.remove(entity);
        if (hb != null) {
            entityLayer.getChildren().remove(hb);
        }
        ImageView iv = entitySprites.remove(entity);
        if (iv != null) {
            entityLayer.getChildren().remove(iv);
        }
    }

    private Pane createHealthBar(double currentHP, double maxHP, boolean isTower, boolean isPlayer) {
        int barHeight = isTower ? 8 : 4;
        int barWidth = isTower ? (int) (tileSize * 1.8) : tileSize;

        Pane healthBarContainer = new Pane();
        healthBarContainer.setPrefWidth(barWidth);
        healthBarContainer.setPrefHeight(barHeight);

        Color teamColor = isPlayer ? Color.DODGERBLUE : Color.ORANGERED;

        Rectangle bg = new Rectangle(barWidth, barHeight);
        bg.setStroke(Color.BLACK);
        bg.setFill(teamColor.darker().darker());
        bg.setStrokeWidth(0.5);

        double healthPercent = Math.max(0, Math.min(1, currentHP / maxHP));
        Rectangle health = new Rectangle(barWidth * healthPercent, barHeight);
        health.setFill(teamColor);

        healthBarContainer.getChildren().addAll(bg, health);
        return healthBarContainer;
    }

    private ImageView getEntitySpriteFromCard(Card card) {
        if (card == null) return null;

        String cardName = card.getName().toLowerCase().replaceAll(" ", "");
        System.out.println("Loading sprite for card: " + card.getName() + " (normalized: " + cardName + ")");

        String arenaImagePath = "/kuroyale/images/cards/arena/" + cardName + ".png";
        String regularImagePath = "/kuroyale/images/cards/" + cardName + ".png";
        System.out.println("  Trying paths: " + arenaImagePath + " and " + regularImagePath);

        ImageView img = new ImageView();
        int spriteSize = 24;
        img.setFitWidth(spriteSize);
        img.setPreserveRatio(true);
        img.setSmooth(true);
        img.setImage(null);

        java.io.InputStream arenaStream = getClass().getResourceAsStream(arenaImagePath);
        final boolean arenaExists = (arenaStream != null);
        if (arenaStream != null) {
            try { arenaStream.close(); } catch (Exception e) {}
        }

        java.io.InputStream regularStream = getClass().getResourceAsStream(regularImagePath);
        final boolean regularExists = (regularStream != null);
        if (regularStream != null) {
            try { regularStream.close(); } catch (Exception e) {}
        }

        String imagePath;
        final boolean useArena;

        if (arenaExists) {
            imagePath = arenaImagePath;
            useArena = true;
        } else if (regularExists) {
            imagePath = regularImagePath;
            useArena = false;
        } else {
            System.err.println("No image found for card: " + cardName);
            return null;
        }

        final String finalImagePath = imagePath;
        final String finalRegularPath = regularImagePath;

        java.net.URL imageURL = getClass().getResource(finalImagePath);
        final String actualImagePath;

        if (imageURL == null) {
            System.err.println("Image URL is null for: " + finalImagePath);
            if (useArena && regularExists) {
                imageURL = getClass().getResource(finalRegularPath);
                if (imageURL == null) {
                    System.err.println("Fallback image URL also null for: " + finalRegularPath);
                    return null;
                }
                actualImagePath = finalRegularPath;
            } else {
                return null;
            }
        } else {
            actualImagePath = finalImagePath;
        }

        javafx.scene.image.Image image = new javafx.scene.image.Image(imageURL.toExternalForm());

        if (image.isError()) {
            if (useArena && regularExists) {
                System.out.println("  Primary image failed, trying fallback: " + finalRegularPath);
                java.net.URL fallbackURL = getClass().getResource(finalRegularPath);
                if (fallbackURL != null) {
                    javafx.scene.image.Image fallback = new javafx.scene.image.Image(fallbackURL.toExternalForm());
                    if (!fallback.isError()) {
                        img.setImage(fallback);
                        System.out.println("  Successfully set fallback image");
                        return img;
                    } else {
                        System.err.println("Both images failed immediately for: " + cardName);
                        return null;
                    }
                } else {
                    System.err.println("Fallback URL is null for: " + finalRegularPath);
                    return null;
                }
            } else {
                System.err.println("Image failed and no fallback for: " + cardName);
                return null;
            }
        }

        final boolean[] imageLoaded = { false };
        final boolean[] imageFailed = { false };

        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                imageLoaded[0] = true;
                Platform.runLater(() -> {
                    if (!img.getImage().isError()) {
                        img.setImage(image);
                        System.out.println("  Image loaded successfully: " + actualImagePath);
                    }
                });
            }
        });

        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (isError) {
                System.err.println("Image failed to load: " + actualImagePath + " for card: " + cardName);
                imageFailed[0] = true;

                if (useArena && regularExists) {
                    java.net.URL fallbackURL = getClass().getResource(finalRegularPath);
                    if (fallbackURL != null) {
                        javafx.scene.image.Image fallback = new javafx.scene.image.Image(fallbackURL.toExternalForm());
                        fallback.progressProperty().addListener((obs2, oldProgress2, newProgress2) -> {
                            if (newProgress2.doubleValue() >= 1.0 && !fallback.isError()) {
                                imageLoaded[0] = true;
                                imageFailed[0] = false;
                                Platform.runLater(() -> {
                                    img.setImage(fallback);
                                    System.out.println("  Fallback image loaded successfully");
                                });
                            }
                        });
                        fallback.errorProperty().addListener((obs2, wasError2, isError2) -> {
                            if (isError2) {
                                System.err.println("Fallback image also failed for: " + cardName);
                                imageFailed[0] = true;
                            }
                        });
                    } else {
                        imageFailed[0] = true;
                    }
                }
            }
        });

        final boolean[] imageSet = { false };

        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0 && !image.isError() && image.getWidth() > 0 && !imageSet[0]) {
                imageSet[0] = true;
                Platform.runLater(() -> {
                    if (!image.isError() && image.getWidth() > 0) {
                        img.setImage(image);
                        System.out.println("  Image loaded and set: " + actualImagePath);
                    }
                });
            }
        });

        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (isError && !imageSet[0]) {
                System.err.println("Image failed before setting: " + actualImagePath);
            }
        });

        if (image.getWidth() > 0 && !image.isError()) {
            img.setImage(image);
            imageSet[0] = true;
            System.out.println("  Image already loaded: " + actualImagePath);
            return img;
        }

        Platform.runLater(() -> {
            if (image.getWidth() > 0 && !image.isError() && !imageSet[0]) {
                imageSet[0] = true;
                img.setImage(image);
                System.out.println("  Image loaded synchronously: " + actualImagePath);
            }
        });

        return img;
    }
}
