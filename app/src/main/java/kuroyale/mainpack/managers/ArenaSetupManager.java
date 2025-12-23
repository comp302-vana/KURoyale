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

    public ArenaSetupManager(ArenaMap arenaMap, GridPane arenaGrid, int rows, int cols, int tileSize,
                            EntityRenderer entityRenderer) {
        this.arenaMap = arenaMap;
        this.arenaGrid = arenaGrid;
        this.rows = rows;
        this.cols = cols;
        this.tileSize = tileSize;
        this.entityRenderer = entityRenderer;
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
                    if (event.getDragboard().hasString())
                        event.acceptTransferModes(TransferMode.COPY);
                    event.consume();
                });

                tile.setOnDragDropped(event -> placementManager.handleCardDrop(event, r, c));

                arenaGrid.add(tile, col, row);
            }
        }
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
