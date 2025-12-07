package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;
import kuroyale.arenapack.ArenaMap;
import java.util.ArrayDeque;
import java.util.Queue;

public class AliveEntity extends Entity {
    private AliveCard card;
    private double HP;

    public AliveEntity(AliveCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
        this.HP = card.getHp();
    }

    public void reduceHP(double damage) {
        HP -= damage;
        if (HP < 0) {

        }
    }

    public double getHP() {
        return HP;
    }

    public void setHP(double value) {
        HP = value;
    }

    public double getRange() {
        return card.getRange();
    }

    private int row;
    private int col;

    public void setPosition(int r, int c) {
        row = r;
        col = c;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public Entity findClosestTarget(ArenaMap map) {
        int rows = map.getRows();
        int cols = map.getCols();
        boolean[][] visited = new boolean[rows][cols];

        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{row, col});
        visited[row][col] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];

            Entity e = map.getEntity(r, c);
            if (e instanceof AliveEntity a && a.isPlayer() != isPlayer())
                return a;

            var obj = map.getObject(r, c);
            if (obj != null && obj.getType() != null) {
                switch (obj.getType()) {
                    case ENEMY_TOWER, ENEMY_KING -> {
                        if (isPlayer()) return map.getEntity(r, c);
                    }
                    case OUR_TOWER, OUR_KING -> {
                        if (!isPlayer()) return map.getEntity(r, c);
                    }
                    default -> {}
                }
            }

            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr>=0 && nr<rows && nc>=0 && nc<cols && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }
        return null;
    }


}
