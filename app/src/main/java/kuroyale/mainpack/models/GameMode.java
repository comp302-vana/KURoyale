package kuroyale.mainpack.models;

/**
 * Enum representing the game mode.
 * SINGLE_PLAYER_AI: Player vs AI opponent (existing mode)
 * LOCAL_PVP: Two human players on the same device (new mode)
 * NETWORK_MULTIPLAYER: Two human players on a network (future mode)
 */
public enum GameMode {
    SINGLE_PLAYER_AI,
    LOCAL_PVP,
    NETWORK_MULTIPLAYER
}
