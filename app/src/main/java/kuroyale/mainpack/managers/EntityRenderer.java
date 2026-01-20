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
import kuroyale.mainpack.network.CoordinateTransformer;

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
    private boolean isClient = false; // True if this is the client (needs coordinate transformation)

    public EntityRenderer(ArenaMap arenaMap, Pane entityLayer, Pane staticLayer,
            PointsCounter pointsCounter, int rows, int cols, int tileSize) {
        this.arenaMap = arenaMap;
        this.entityLayer = entityLayer;
        this.staticLayer = staticLayer;
        this.pointsCounter = pointsCounter;
        this.rows = rows;
        this.cols = cols;
        this.tileSize = tileSize;

        // Add clipping to prevent rendering outside map bounds
        // Use Platform.runLater to ensure nodes are fully initialized before setting
        // clip
        javafx.application.Platform.runLater(() -> {
            javafx.scene.shape.Rectangle clipEntity = new javafx.scene.shape.Rectangle(cols * tileSize,
                    rows * tileSize);
            javafx.scene.shape.Rectangle clipStatic = new javafx.scene.shape.Rectangle(cols * tileSize,
                    rows * tileSize);
            entityLayer.setClip(clipEntity);
            staticLayer.setClip(clipStatic);
        });
    }

    public boolean isEntityDirty() {
        return entityDirty;
    }

    public void setEntityDirty(boolean dirty) {
        this.entityDirty = dirty;
    }

    public boolean isStaticDirty() {
        return staticDirty;
    }

    public void setStaticDirty(boolean dirty) {
        this.staticDirty = dirty;
    }

    public void setIsClient(boolean isClient) {
        this.isClient = isClient;
    }

    public boolean isClient() {
        return isClient;
    }

    /**
     * Get the ImageView sprite for a given entity.
     * Useful for attaching visual effects.
     */
    public ImageView getEntitySprite(AliveEntity entity) {
        return entitySprites.get(entity);
    }

    private long cellKey(int r, int c) {
        return (((long) r) << 32) ^ (c & 0xffffffffL);
    }

    public void ensureHealthBar(AliveEntity e, boolean isTower) {
        if (healthBarsByEntity.containsKey(e))
            return;

        var aliveCard = (AliveCard) e.getCard();
        double maxHP = aliveCard != null ? aliveCard.getHp() : e.getHP();

        Pane hb = createHealthBar(e.getHP(), maxHP, isTower, e.isPlayer());
        healthBarsByEntity.put(e, hb);
        entityLayer.getChildren().add(hb);
    }

    public void ensureEntityNode(AliveEntity e, Card card) {
        if (entitySprites.containsKey(e))
            return;

        ImageView iv = getEntitySpriteFromCard(card);
        if (iv == null)
            return;

        ensureHealthBar(e, e instanceof TowerEntity);

        entitySprites.put(e, iv);
        entityLayer.getChildren().addAll(iv);
    }

    public void updateHealthBarFor(AliveEntity e) {
        Pane hb = healthBarsByEntity.get(e);
        if (hb == null)
            return;

        var aliveCard = (AliveCard) e.getCard();
        double maxHP = aliveCard != null ? aliveCard.getHp() : e.getHP();
        if (maxHP <= 0)
            return;

        double percent = Math.max(0, Math.min(1, e.getHP() / maxHP));

        Rectangle bg = (Rectangle) hb.getChildren().get(0);
        Rectangle fg = (Rectangle) hb.getChildren().get(1);
        fg.setWidth(bg.getWidth() * percent);
    }

    public void positionEntityNode(AliveEntity e, int row, int col) {
        ImageView iv = entitySprites.get(e);
        Pane hb = healthBarsByEntity.get(e);
        if (iv == null || hb == null)
            return;

        // Transform coordinates for client view
        int renderRow = row;
        int renderCol = col;
        if (isClient) {
            int[] clientCoords = CoordinateTransformer.absoluteToClient(row, col, rows, cols);
            renderRow = clientCoords[0];
            renderCol = clientCoords[1];
        }

        double x = renderCol * tileSize;
        double y = renderRow * tileSize;

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

        if (hb.getChildren().size() < 2)
            return;

        Rectangle bg = (Rectangle) hb.getChildren().get(0);
        Rectangle health = (Rectangle) hb.getChildren().get(1);

        double barWidth = bg.getWidth();

        health.setWidth(barWidth * healthPercent);
        // Health bar color: blue for own team, red for enemy
        // Only flip colors for entities (not towers) on client side
        boolean isTower = e instanceof TowerEntity;
        boolean shouldBeBlue = e.isPlayer();
        if (isClient && !isTower) {
            // Flip color only for regular entities on client, not towers
            shouldBeBlue = !shouldBeBlue;
        }
        if (shouldBeBlue) {
            health.setFill(Color.DODGERBLUE);
        } else {
            health.setFill(Color.ORANGERED);
        }
    }

    /**
     * Compute tower bounding box and return top-left + width/height.
     * 
     * @param tower The tower entity
     * @return int array [topLeftRow, topLeftCol, widthTiles, heightTiles]
     */
    private int[] computeTowerBoundingBox(TowerEntity tower) {
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        boolean found = false;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == tower) {
                    found = true;
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }

        if (!found) {
            // Fallback: use stored position (assumes bottom-right anchor)
            int footprintSize = tower.isKing() ? 4 : 3;
            int bottomRightRow = tower.getRow();
            int bottomRightCol = tower.getCol();
            int topLeftRow = bottomRightRow - (footprintSize - 1);
            int topLeftCol = bottomRightCol - (footprintSize - 1);
            return new int[] { topLeftRow, topLeftCol, footprintSize, footprintSize };
        }

        int widthTiles = maxCol - minCol + 1;
        int heightTiles = maxRow - minRow + 1;
        return new int[] { minRow, minCol, widthTiles, heightTiles };
    }

    private void positionTowerHealthBar(TowerEntity tower, int bottomRightRow, int bottomRightCol) {
        // Compute bounding box for accurate positioning
        int[] bbox = computeTowerBoundingBox(tower);
        int absoluteTopLeftRow = bbox[0];
        int absoluteTopLeftCol = bbox[1];
        int widthTiles = bbox[2];
        int heightTiles = bbox[3];

        // Transform top-left to client view if needed (using footprint-aware transform)
        int renderTopLeftRow = absoluteTopLeftRow;
        int renderTopLeftCol = absoluteTopLeftCol;
        if (isClient) {
            // Use footprint-aware transform for perfect symmetry
            int[] clientCoords = CoordinateTransformer.absoluteTopLeftToClientTopLeft(
                    absoluteTopLeftRow, absoluteTopLeftCol, widthTiles, rows, cols);
            renderTopLeftRow = clientCoords[0];
            renderTopLeftCol = clientCoords[1];
        }

        Pane hb = healthBarsByEntity.get(tower);
        if (hb == null)
            return;

        double footprintW = widthTiles * tileSize;
        double x = renderTopLeftCol * tileSize;
        double y = renderTopLeftRow * tileSize;

        double barW = hb.getPrefWidth();
        double barH = hb.getPrefHeight();
        hb.relocate(x + (footprintW - barW) / 2.0, y - barH);
    }

    public void renderTowerHealthBars() {
        // Collect all alive towers first
        java.util.Set<TowerEntity> aliveTowers = new java.util.HashSet<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity e = arenaMap.getEntity(r, c);
                if (e instanceof TowerEntity tower && tower.getHP() > 0) {
                    aliveTowers.add(tower);
                }
            }
        }

        // Remove health bars for dead/destroyed towers
        healthBarsByEntity.entrySet().removeIf(entry -> {
            if (entry.getKey() instanceof TowerEntity tower) {
                if (!aliveTowers.contains(tower) || tower.getHP() <= 0) {
                    Pane hb = entry.getValue();
                    if (hb != null) {
                        entityLayer.getChildren().remove(hb);
                    }
                    return true;
                }
            }
            return false;
        });

        // Render health bars for alive towers
        seenTowers.clear();
        for (TowerEntity tower : aliveTowers) {
            if (seenTowers.put(tower, true) != null)
                continue;

            ensureHealthBar(tower, true);
            updateHealthBarFor(tower);
            positionTowerHealthBar(tower, tower.getRow(), tower.getCol());
        }
    }

    public void renderStaticObjects() {
        staticLayer.getChildren().clear();
        staticSpritesByCell.clear();
        staticLayer.getChildren().add(pointsCounter);

        // Track which towers we've already rendered (use IdentityHashMap for object
        // identity)
        java.util.Map<PlacedObject, Boolean> renderedTowers = new java.util.IdentityHashMap<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                PlacedObject obj = arenaMap.getObject(r, c);
                if (obj == null)
                    continue;
                if (obj.getType() == ArenaObjectType.ENTITY)
                    continue;

                // Check if this is a tower (multi-cell object)
                boolean isTower = obj.getType() == ArenaObjectType.OUR_TOWER ||
                        obj.getType() == ArenaObjectType.ENEMY_TOWER ||
                        obj.getType() == ArenaObjectType.OUR_KING ||
                        obj.getType() == ArenaObjectType.ENEMY_KING;

                if (isTower) {
                    // Render each tower only once
                    if (renderedTowers.containsKey(obj)) {
                        continue; // Already rendered this tower
                    }
                    renderedTowers.put(obj, true);

                    // Determine tower size (princess=3, king=4)
                    int towerSize;
                    if (obj.getType() == ArenaObjectType.OUR_KING || obj.getType() == ArenaObjectType.ENEMY_KING) {
                        towerSize = 4; // King is 4x4
                    } else {
                        towerSize = 3; // Princess is 3x3
                    }

                    // Find the anchor cell (bottom-right) where this PlacedObject is stored
                    // In ArenaMap, towers are stored with bottom-right anchor
                    int anchorRow = r; // Current cell (r, c) is where we found the PlacedObject
                    int anchorCol = c;

                    ImageView iv = SpriteLoader.getSprite(obj.getType(), tileSize);
                    if (iv != null) {
                        // SpriteLoader.getSprite() already applies translate offsets:
                        // - Princess (3x3): translateX = -2*tileSize, translateY = -2*tileSize
                        // - King (4x4): translateX = -3*tileSize, translateY = -3*tileSize
                        // These offsets shift the visual content relative to the ImageView position.
                        //
                        // The sprite is designed to be positioned at the anchor cell (bottom-right).
                        // The translate offsets will shift the visual content to show the full tower
                        // footprint.
                        double spriteX;
                        double spriteY;

                        if (isClient) {
                            // Transform the anchor cell (bottom-right) to client view using footprint-aware
                            // transform
                            // This ensures the mirrored anchor position accounts for the tower's footprint
                            // size
                            int[] clientAnchorCoords = CoordinateTransformer.absoluteBottomRightToClientBottomRight(
                                    anchorRow, anchorCol, towerSize, rows, cols);

                            spriteX = (clientAnchorCoords[1] + (obj.getType() == ArenaObjectType.OUR_KING
                                    || obj.getType() == ArenaObjectType.ENEMY_KING ? 3 : 2)) * tileSize;
                            spriteY = clientAnchorCoords[0] * tileSize;
                        } else {
                            // Host: Position at anchor cell, translate offsets handle the visual
                            // positioning
                            spriteX = anchorCol * tileSize;
                            spriteY = anchorRow * tileSize;
                        }

                        iv.relocate(spriteX, spriteY);
                        staticLayer.getChildren().add(iv);
                        // Store reference using anchor cell key
                        staticSpritesByCell.put(cellKey(anchorRow, anchorCol), iv);
                    }
                } else {
                    // Regular single-cell static object (bridges, etc.)
                    ImageView iv = SpriteLoader.getSprite(obj.getType(), tileSize);
                    if (iv == null)
                        continue;

                    // Transform coordinates for client view (1x1 footprint)
                    int renderRow = r;
                    int renderCol = c;
                    if (isClient) {
                        int[] clientCoords = CoordinateTransformer.absoluteToClient(r, c, rows, cols);
                        renderRow = clientCoords[0];
                        renderCol = clientCoords[1];
                    }

                    iv.relocate(renderCol * tileSize, renderRow * tileSize);
                    staticLayer.getChildren().add(iv);
                    staticSpritesByCell.put(cellKey(r, c), iv);
                }
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
                if (iv != null) {
                    entityLayer.getChildren().remove(iv);
                }
                if (hb != null) {
                    entityLayer.getChildren().remove(hb);
                }
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

        // Only flip colors for entities (not towers) on client side
        boolean shouldBeBlue = isPlayer;
        if (isClient && !isTower) {
            // Flip color only for regular entities on client, not towers
            shouldBeBlue = !shouldBeBlue;
        }
        Color teamColor = shouldBeBlue ? Color.DODGERBLUE : Color.ORANGERED;

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
        if (card == null)
            return null;

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
            try {
                arenaStream.close();
            } catch (Exception e) {
            }
        }

        java.io.InputStream regularStream = getClass().getResourceAsStream(regularImagePath);
        final boolean regularExists = (regularStream != null);
        if (regularStream != null) {
            try {
                regularStream.close();
            } catch (Exception e) {
            }
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
