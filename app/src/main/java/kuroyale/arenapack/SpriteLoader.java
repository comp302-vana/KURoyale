package kuroyale.arenapack;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SpriteLoader {
    private static final int tileSize = 24;

    private static final Image TOWER_OUR = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/Tower 06.png").toExternalForm());

    private static final Image TOWER_ENEMY = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/Tower 05.png").toExternalForm());

    private static final Image BRIDGE = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/singleBridge.png").toExternalForm());

    private static ImageView slice(Image sheet, int index, int sprites_per_row, int w) {
        double spriteWidth = sheet.getWidth() / sprites_per_row;
        double spriteHeight = sheet.getHeight();

        Rectangle2D viewport = new Rectangle2D(
                index * spriteWidth,
                0,
                spriteWidth,
                spriteHeight);

        ImageView view = new ImageView(sheet);
        view.setViewport(viewport);

        view.setFitWidth(tileSize * w);
        view.setPreserveRatio(true);

        return view;
    }

    public static ImageView getSprite(ArenaObjectType type) {
        return switch (type) {
            case OUR_TOWER -> slice(TOWER_OUR, 0, 2, 3);
            case ENEMY_TOWER -> slice(TOWER_ENEMY, 0, 2, 3);

            case OUR_KING -> slice(TOWER_OUR, 1, 2, 4);
            case ENEMY_KING -> slice(TOWER_ENEMY, 1, 2, 4);

            case BRIDGE -> slice(BRIDGE, 0, 1, 2);

            case ENTITY -> new ImageView("/kuroyale/images/icon.png");
        };
    }
}
