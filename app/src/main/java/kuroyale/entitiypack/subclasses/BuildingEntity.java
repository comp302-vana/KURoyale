package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.BuildingCard;

public class BuildingEntity extends AliveEntity {
    private final BuildingCard card;
    private final double initialHealth;

    private double timeSinceLastSpawn = 0.0;
    private double timeSinceLastElixirGeneration = 0.0;
    private int elixirGenerated = 0;


    public BuildingEntity(BuildingCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
        this.initialHealth = getHP();
    }

    public void reduceLifetime(double deltaTime) {
        if (getLifetime() == 0) {
            return;
        } else {
            double hpLoss = initialHealth / getLifetime();
            reduceHP(deltaTime * hpLoss);
        }
    }

    protected double getLifetime() {
        return card.getLifetime();
    }

    public double getTimeSinceLastSpawn() {
        return timeSinceLastSpawn;
    }

    public void addTimeSinceLastSpawn(double deltaTime){
        timeSinceLastSpawn += deltaTime;
    }

    public void resetTimeSinceLastSpawn() {
        timeSinceLastSpawn = 0.0;
    }
    
    public double getTimeSinceLastElixirGeneration() {
        return timeSinceLastElixirGeneration;
    }
    
    public void addTimeSinceLastElixirGeneration(double deltaTime) {
        timeSinceLastElixirGeneration += deltaTime;
    }
    
    public void resetTimeSinceLastElixirGeneration() {
        timeSinceLastElixirGeneration = 0.0;
    }
    
    public int getElixirGenerated() {
        return elixirGenerated;
    }
    
    public void incrementElixirGenerated() {
        elixirGenerated++;
    }
    
    public BuildingCard getBuildingCard() {
        return card;
    }
}
