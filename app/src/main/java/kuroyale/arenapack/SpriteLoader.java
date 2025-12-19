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

    private static final Image LONG_BRIDGE = new Image(
            SpriteLoader.class.getResource("/kuroyale/images/longBridge.png").toExternalForm());

    private static ImageView slice(Image sheet, int width, int dx, int dy) {
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

        view.setTranslateX(dx);
        view.setTranslateY(dy);

        return view;
    }

    private static ImageView specialBridgeSlice(Image sheet, int tileSize) {
        double spriteWidth = LONG_BRIDGE.getWidth();
        double spriteHeight = LONG_BRIDGE.getHeight();

        Rectangle2D viewport = new Rectangle2D(
                0,
                0,
                spriteWidth,
                spriteHeight);

        ImageView view = new ImageView(LONG_BRIDGE);
        view.setViewport(viewport);

        view.setFitWidth(2*tileSize);
        view.setPreserveRatio(true);

        return view;
    }

    public static ImageView getSprite(ArenaObjectType type, int tileSize) {
        return switch (type) {
            case OUR_TOWER -> slice(OUR_TOWER, 3*tileSize, -2*tileSize, -2*tileSize);
            case OUR_KING -> slice(OUR_KING, 4*tileSize, -3*tileSize, -3*tileSize);
            
            case ENEMY_TOWER -> slice(ENEMY_TOWER, 3*tileSize, -2*tileSize, -2*tileSize);
            case ENEMY_KING -> slice(ENEMY_KING, 4*tileSize, -3*tileSize, -3*tileSize);

            case BRIDGE -> slice(BRIDGE, 1*tileSize, 0, 0);

            case ENTITY -> new ImageView("/kuroyale/images/icon.png");
        };
    }

    public static ImageView getBuilderBridgeSprite(int tileSize) {
        return specialBridgeSlice(LONG_BRIDGE, tileSize);
    }
}
