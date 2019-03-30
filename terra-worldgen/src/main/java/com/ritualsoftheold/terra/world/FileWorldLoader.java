package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.world.enumerators.Legend;
import com.ritualsoftheold.terra.world.enumerators.Slope;
import com.ritualsoftheold.terra.world.location.Area;
import com.ritualsoftheold.terra.world.location.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;


public class FileWorldLoader {

    private File map;
    private HashMap<String, Area> areaMap;

    public FileWorldLoader(File map) {
        this.map = map;
        areaMap = new HashMap<>();
    }

    public ArrayList<Area> loadWorld() throws IOException {
        BufferedImage image = ImageIO.read(map);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int clr = image.getRGB(x, y);
                if (!areaMap.containsKey(String.valueOf(clr))) {
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;
                    Area newArea = getArea(new Color(red, green, blue));
                    if(newArea != null) {
                        Point newPoint = new Point(x, y, newArea);
                        newArea.add(newPoint);
                        areaMap.put(String.valueOf(clr), newArea);
                    }
                } else {
                    Area area = areaMap.get(String.valueOf(clr));
                    Point newPoint = new Point(x, y, area);
                    area.add(newPoint);
                }
            }
        }

        return new ArrayList<>(areaMap.values());
    }

    private Area getArea(Color color) {
        if (color.equals(Legend.OCEAN.color)) {
            return new Area((int)Legend.OCEAN.min, (int)Legend.OCEAN.max, Slope.HORIZONTAL);
        } else if (color.equals(Legend.SEA.color)) {
            return new Area((int)Legend.SEA.min, (int)Legend.SEA.max, Slope.HORIZONTAL);
        } else if (color.equals(Legend.SHORELINE.color)) {
            return new Area((int)Legend.SHORELINE.min, (int)Legend.SHORELINE.max, Slope.DECREASE);
        } else if (color.equals(Legend.PLAIN.color)) {
            return new Area((int)Legend.PLAIN.min, (int)Legend.PLAIN.max, Slope.HORIZONTAL);
        } else if (color.equals(Legend.HILL.color)) {
            return new Area((int)Legend.HILL.min, (int)Legend.HILL.max, Slope.INCREASE);
        } else if (color.equals(Legend.MOUNTAIN.color)) {
            return new Area((int)Legend.MOUNTAIN.min, (int)Legend.MOUNTAIN.max, Slope.INCREASE);
        }else {
            return null;
        }
    }
}