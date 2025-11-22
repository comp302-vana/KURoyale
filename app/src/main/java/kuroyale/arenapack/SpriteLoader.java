package kuroyale.arenapack;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SpriteLoader {
private static final Image TOWER_OUR = 
    new Image(SpriteLoader.class.getResource("/kuroyale/images/Tower 06.png").toExternalForm());

private static final Image TOWER_ENEMY = 
    new Image(SpriteLoader.class.getResource("/kuroyale/images/Tower 05.png").toExternalForm());

    private static final int SPRITES_PER_ROW = 3;  

    private static ImageView slice(Image sheet, int index) {
        double spriteWidth = sheet.getWidth() / SPRITES_PER_ROW;
        double spriteHeight = sheet.getHeight();

        Rectangle2D viewport = new Rectangle2D(
            index * spriteWidth,   
            0,                     
            spriteWidth,
            spriteHeight
        );

        ImageView view = new ImageView(sheet);
        view.setViewport(viewport);

        view.setFitHeight(45);
        view.setPreserveRatio(true);

        return view;
    }

    public static ImageView getSprite(ArenaObjectType type) {
        return switch (type) {
            case OUR_TOWER -> slice(TOWER_OUR, 1);
            case ENEMY_TOWER -> slice(TOWER_ENEMY, 1);

            case OUR_KING -> slice(TOWER_OUR, 2);
            case ENEMY_KING -> slice(TOWER_ENEMY, 2);

            case BRIDGE -> null;
        };
    }
}
