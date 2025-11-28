package kuroyale.arenapack;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SpriteLoader {
    private static final Image OUR_TOWER = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/our_tower.png").toExternalForm());

    private static final Image OUR_KING = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/our_king.png").toExternalForm());

    private static final Image ENEMY_TOWER = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/enemy_tower.png").toExternalForm());

    private static final Image ENEMY_KING = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/enemy_king.png").toExternalForm());

    private static final Image BRIDGE = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/singleBridge.png").toExternalForm());

    private static ImageView slice(Image sheet, int width) {
        double spriteWidth = sheet.getWidth();
        double spriteHeight = sheet.getHeight();

        Rectangle2D viewport = new Rectangle2D(
                0,
                0,
                spriteWidth,
                spriteHeight);

        ImageView view = new ImageView(sheet);
        view.setViewport(viewport);

        view.setFitWidth(width);
        view.setPreserveRatio(true);

        return view;
    }

    public static ImageView getSprite(ArenaObjectType type, int tileSize) {
        return switch (type) {
            case OUR_TOWER -> slice(OUR_TOWER, 3 * tileSize);
            case OUR_KING -> slice(OUR_KING, 4 * tileSize);
            
            case ENEMY_TOWER -> slice(ENEMY_TOWER, 3 * tileSize);
            case ENEMY_KING -> slice(ENEMY_KING, 4 * tileSize);

            case BRIDGE -> slice(BRIDGE, 2 * tileSize);

            case ENTITY -> new ImageView("/kuroyale/images/icon.png");
        };
    }
}
