package game;

public enum Direction {
    TOP_LEFT, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, LEFT;

    public int getValue() {
        return this.ordinal();
    }
}
