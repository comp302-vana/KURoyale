package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.subclasses.BuildingCard;

public class TowerEntity extends BuildingEntity {
    private static BuildingCard towerCard(boolean isKing) {
        int id = isKing ? 29 : 30;
        String name = isKing ? "King Tower" : "Crown Tower";
        double Hp = isKing ? 3000 : 2000;

        BuildingCard card = new BuildingCard(
            id, name, null, 0, CardCategory.DEFENSIVE_BUILDING, CardTarget.AIR_GROUND,
            Hp, 30, 1, 7.0, 0.0, 0
        );

        return card;
    }

    public TowerEntity(boolean isKing) {
        super(towerCard(isKing));
    }
}
