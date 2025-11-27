package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.subclasses.BuildingCard;

public class TowerEntity extends BuildingEntity {
    private static BuildingCard towerCard(boolean isKing) {
        int id = isKing ? 29 : 30;
        String name = isKing ? "King Tower" : "Crown Tower";
        double Hp = isKing ? 4750 : 3000;

        BuildingCard card = new BuildingCard(
            id, name, null, 0, CardCategory.DEFENSIVE_BUILDING, CardTarget.AIR_GROUND,
            Hp, 100, 0.8, 7.5, 0.0, 0
        );

        return card;
    }

    public TowerEntity(boolean isKing, boolean isPlayer) {
        super(towerCard(isKing), isPlayer);
    }
}
