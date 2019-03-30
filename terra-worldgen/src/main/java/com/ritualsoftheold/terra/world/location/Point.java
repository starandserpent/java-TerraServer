package com.ritualsoftheold.terra.world.location;

public class Point {
    private int x;
    private int y;
    private int height;
    private Area area;

    public Point(int x, int y, Area area){
        this.x = x;
        this.y = y;
        this.area = area;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHeight() {
        return height;
    }

    public Area getArea() {
        return area;
    }
}
