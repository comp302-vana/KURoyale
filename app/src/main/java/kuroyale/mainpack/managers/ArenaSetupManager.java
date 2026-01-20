package kuroyale.mainpack.managers;

import java.io.File;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.input.TransferMode;
import kuroyale.arenapack.ArenaMap;
import kuroyale.mainpack.managers.EntityPlacementManager;

/**
 * Handles arena grid setup, tile creation, and default arena loading.
 * High cohesion: All arena initialization logic in one place.
 */
public class ArenaSetupManager {
    private final ArenaMap arenaMap;
    private final GridPane arenaGrid;
    private final int rows;
    private final int cols;
    private final int tileSize;
    private final EntityRenderer entityRenderer;

    // Spell range indicator manager (set after construction)
    private SpellRangeIndicatorManager spellRangeIndicatorManager;

    public ArenaSetupManager(ArenaMap arenaMap, GridPane arenaGrid, int rows, int cols, int tileSize,
            EntityRenderer entityRenderer) {
        this.arenaMap = arenaMap;
        this.arenaGrid = arenaGrid;
        this.rows = rows;
        this.cols = cols;
        this.tileSize = tileSize;
        this.entityRenderer = entityRenderer;
    }

    /**
     * Set the spell range indicator manager for showing spell effect areas during
     * drag.
     */
    public void setSpellRangeIndicatorManager(SpellRangeIndicatorManager manager) {
        this.spellRangeIndicatorManager = manager;
    }

    public void fillArenaGrid(EntityPlacementManager placementManager) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Pane tile = new Pane();
                tile.setPrefWidth(tileSize);
                tile.setPrefHeight(tileSize);

                if (col >= cols / 2 - 1 && col <= cols / 2) {
                    tile.setStyle(
                            "-fx-background-image: url('/kuroyale/images/water.png');" +
                                    "-fx-background-size: cover;");
                } else {
                    if ((col + row) % 2 == 0) {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/darkGrass.png');" +
                                        "-fx-background-size: cover;");
                    } else {
                        tile.setStyle(
                                "-fx-background-image: url('/kuroyale/images/lightGrass.png');" +
                                        "-fx-background-size: cover;");
                    }
                }

                int r = row;
                int c = col;

                tile.setOnDragOver(event -> {
                    if (event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.COPY);

                        // Show spell range indicator if dragging a spell card
                        if (spellRangeIndicatorManager != null) {
                            try {
                                int cardId = Integer.parseInt(event.getDragboard().getString());
                                if (SpellRangeIndicatorManager.isSpellCard(cardId)) {
                                    // Calculate center position of the tile in the arena grid
                                    double centerX = c * tileSize + tileSize / 2.0;
                                    double centerY = r * tileSize + tileSize / 2.0;
                                    spellRangeIndicatorManager.showIndicator(cardId, centerX, centerY);
                                } else {
                                    // Not a spell card, hide indicator
                                    spellRangeIndicatorManager.hideIndicator();
                                }
                            } catch (NumberFormatException e) {
                                // Not a numeric card ID (e.g., "ourtower"), hide indicator
                                spellRangeIndicatorManager.hideIndicator();
                            }
                        }
                    }
                    event.consume();
                });

                // Hide spell indicator when drag exits the tile
                tile.setOnDragExited(event -> {
                    // Note: We don't hide here because we want the indicator to stay visible
                    // as long as we're over *any* arena tile. The indicator will be hidden
                    // when the drag is complete or exits the entire arena.
                    event.consume();
                });

                tile.setOnDragDropped(event -> {
                    // Hide spell indicator when card is dropped
                    if (spellRangeIndicatorManager != null) {
                        spellRangeIndicatorManager.hideIndicator();
                    }
                    placementManager.handleCardDrop(event, r, c);
                });

                arenaGrid.add(tile, col, row);
            }
        }

        // Add drag done handler to the arena grid to hide indicator when drag ends
        arenaGrid.setOnDragDone(event -> {
            if (spellRangeIndicatorManager != null) {
                spellRangeIndicatorManager.hideIndicator();
            }
        });

        // Hide indicator when drag exits the arena
        arenaGrid.setOnDragExited(event -> {
            if (spellRangeIndicatorManager != null) {
                spellRangeIndicatorManager.hideIndicator();
            }
        });
    }

    public void loadDefaultArenaIfExists() {
        try {
            File f = new File("saves/default.txt");
            if (!f.exists()) {
                return; // no default set
            }

            String fileName = new String(java.nio.file.Files.readAllBytes(f.toPath())).trim();
            if (fileName.isEmpty()) {
                return;
            }

            File arenaFile = new File("saves/" + fileName);
            if (!arenaFile.exists()) {
                System.out.println("Default file missing: " + fileName);
                return;
            }

            // Load arena contents
            arenaMap.loadFromFile(arenaFile.getAbsolutePath());

            // Redraw sprites
            entityRenderer.setEntityDirty(true);
            System.out.println("Loaded default arena: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
