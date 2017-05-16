package com.ritualsoftheold.terra.mesher.test;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by NemesisMate on 13/03/17.
 */
// Imported from
// https://github.com/NemesisMate/UtilJME3-Base/blob/master/src/main/java/com/nx/util/jme3/base/ImageUtil.java
// exporting images correctly (jME fails to do so)
// Note: we only need exporting when debugging, so this is in tests
public final class ImageUtil {

    static Logger logger = LoggerFactory.getLogger(ImageUtil.class);

    private ImageUtil() {

    }

    public static Image createEmptyImage(int size) {
        return createEmptyImage(size, true);
    }

    public static Image createEmptyImage(int size, boolean transparency) {
        return new Image(transparency ? Image.Format.BGRA8 : Image.Format.BGR8, size, size, BufferUtils.createByteBuffer(size * size * 4), null, ColorSpace.Linear);
    }

    public static Image createEmptyImage(Image.Format format, int size, int depth) {
        return createEmptyImage(format, size, size, depth);
    }

    public static Image createEmptyImage(Image.Format format, int width, int height, int depth) {
        int bufferSize = width * height * (format.getBitsPerPixel() >> 3);
        if (depth < 2) {
            return new Image(format, width, height, BufferUtils.createByteBuffer(bufferSize), com.jme3.texture.image.ColorSpace.Linear);
        }
        ArrayList<ByteBuffer> data = new ArrayList<ByteBuffer>(depth);
        for (int i = 0; i < depth; ++i) {
            data.add(BufferUtils.createByteBuffer(bufferSize));
        }
        return new Image(Image.Format.RGB8, width, height, depth, data, com.jme3.texture.image.ColorSpace.Linear);
    }


//    private static BufferedImage getABGR8ImageInternal(Image image) {
//        ByteBuffer bb = image.getData(0);
//        bb.clear();
//
//        LoggerFactory.getLogger(ImageUtil.class).debug("Converting ABGR8 image to ARGB");
//
//        BufferedImage awtImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        int[] raw = new int[bb.limit()];
//        for (int i = 0; i < bb.limit() / 4; i++) {
//            raw[i] = ((bb.get(4 * i + 1) & 0xFF)) |
//                    ((bb.get(4 * i + 0) & 0xFF) << 24) |
//                    ((bb.get(4 * i + 3) & 0xFF) << 16) |
//                    ((bb.get(4 * i + 2) & 0xFF) << 8);
//
//        }
//
//        awtImage.setRGB(0, 0, image.getWidth(), image.getHeight(), raw, 0, image.getWidth());
//
//
////        String[] imageStrings = new String[image.getWidth()];
////        for(int x = 0; x < awtImage.getWidth(); x++) {
////            String imageString = "";
////            for(int y = 0; y < awtImage.getHeight(); y++) {
//////                int color = raw[x * awtImage.getHeight() + y];
////                int color = awtImage.getRGB(x, y);
////                //                            int color = awtImage.getRGB(x, y);
////                imageString += color + " - ARGB(" + ((color & 0xFF000000) >> 24) + "," + ((color & 0x00FF0000) >> 16) + "," + ((color & 0x0000FF00) >> 8) + "," + (color & 0x000000FF) + "), ";
////            }
////            imageStrings[x] = imageString + "\n";
////        }
////        LoggerFactory.getLogger(VisualUtil.class).debug("IMAGE IS: {}.", Arrays.toString(imageStrings));
//
//        return awtImage;
//    }

    public static void writeImage(Image image, File file) throws IOException {
        FileOutputStream outStream = new FileOutputStream(file);
        try {
            writeImage(image, outStream);
        } catch (Exception e) {
            throw e;
        } finally {
            outStream.close();
        }
    }

    public static void writeImage(Image image, FileOutputStream saveStream) throws IOException {
        writeImage(getWriteImageFrom(image), saveStream);
    }

    public static void writeImage(BufferedImage image, FileOutputStream saveStream) throws IOException {
        ImageIO.write(image, "png", saveStream);
    }

    public static BufferedImage getWriteImageFrom(Image image) {
//        return getARGBImageFrom(image);
        int bpp = image.getFormat().getBitsPerPixel();
        if(bpp == 32) {
            return getARGB8ImageFrom(image);
        } else if(bpp == 24) {
            return getRGB8ImageFrom(image);
        }

        throw new IllegalArgumentException();
    }


//    public static BufferedImage getARGBImageFrom(Image image) {
//        int bpp = image.getFormat().getBitsPerPixel();
//
//        if(bpp != 32 && bpp != 24) {
//            throw new IllegalArgumentException();
//        }
//
//        int A = 0, R, G, B;
//        switch (image.getFormat()) {
//            case RGBA8:
//                R = 0; G = 1; B = 2; A = 3;
//                break;
//            case ABGR8:
//                A = 0; B = 1; G = 2; R = 3;
//                break;
//            case ARGB8:
//                A = 0; R = 1; G = 2; B = 3;
//                break;
//            case BGRA8:
//                B = 0; G = 1; R = 2; A = 3;
//                break;
//            case RGB8:
//                R = 0; G = 1; B = 2;
//                break;
//            case BGR8:
//                B = 0; G = 1; R = 2;
//                break;
//            default:
//                throw new IllegalArgumentException();
//        }
//
//
//
//        if(logger.isDebugEnabled()) {
//            logger.debug("Converting {} image to ARGB", image.getFormat());
//        }
//
//        ByteBuffer bb = image.getData(0);
//        bb.clear();
//
//        BufferedImage awtImage = new BufferedImage(image.getWidth(), image.getHeight(), bpp == 32 ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
//
//        int channels = bpp / 8;
//        int steps = bb.limit() / channels;
//
//        int[] raw = new int[steps * 4];
//
//        for (int i = 0; i < steps; i++) {
//            raw[i] = ((bb.get(4 * i + A) & 0xFF) << 24) |
//                     ((bb.get(4 * i + R) & 0xFF) << 16) |
//                     ((bb.get(4 * i + G) & 0xFF) << 8) |
//                     ((bb.get(4 * i + B) & 0xFF));
//        }
//        awtImage.setRGB(0, 0, image.getWidth(), image.getHeight(), raw, 0, image.getWidth());
//
//        awtImage = verticalFlip(awtImage);
//
//        return awtImage;
//    }

    public static BufferedImage getARGB8ImageFrom(Image image) {
        int A, R, G, B;
        switch (image.getFormat()) {
            case RGBA8:
                R = 0; G = 1; B = 2; A = 3;
                break;
            case ABGR8:
                A = 0; B = 1; G = 2; R = 3;
                break;
            case ARGB8:
                A = 0; R = 1; G = 2; B = 3;
                break;
            case BGRA8:
                B = 0; G = 1; R = 2; A = 3;
                break;
            default:
                throw new IllegalArgumentException();
        }

        if(logger.isDebugEnabled()) {
            logger.debug("Converting {} image to ARGB8", image.getFormat());
        }

        ByteBuffer bb = image.getData(0);
        bb.clear();

        BufferedImage awtImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        int steps = bb.limit() / 4;
        int[] raw = new int[bb.limit()];

        for (int i = 0; i < steps; i++) {
            raw[i] = ((bb.get(4 * i + B) & 0xFF)) |
                    ((bb.get(4 * i + A) & 0xFF) << 24) |
                    ((bb.get(4 * i + R) & 0xFF) << 16) |
                    ((bb.get(4 * i + G) & 0xFF) << 8);

        }
        awtImage.setRGB(0, 0, image.getWidth(), image.getHeight(), raw, 0, image.getWidth());

        awtImage = verticalFlip(awtImage);

        return awtImage;
    }


    public static BufferedImage getRGB8ImageFrom(Image image) {
        int R, G, B;
        switch (image.getFormat()) {
            case RGB8:
                R = 0; G = 1; B = 2;
                break;
            case BGR8:
                B = 0; G = 1; R = 2;
                break;
            default:
                throw new IllegalArgumentException();
        }

        ByteBuffer bb = image.getData(0);
        bb.clear();

        BufferedImage awtImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        int steps = bb.limit() / 3;
        int[] raw = new int[steps * 4]; // this is like: bb.limit() * 4 / 3

        for (int i = 0; i < steps; i++) {
            raw[i] = 0xFF000000 | // Maybe not needed for rgb, but it doesn't annoy anyone, and so, it can be used as ARGB too.
                    ((bb.get(3 * i + R) & 0xFF) << 16) |
                    ((bb.get(3 * i + G) & 0xFF) << 8) |
                    ((bb.get(3 * i + B) & 0xFF));
        }
        awtImage.setRGB(0, 0, image.getWidth(), image.getHeight(), raw, 0, image.getWidth());

        awtImage = verticalFlip(awtImage);

        return awtImage;
    }

    @Deprecated
    public static BufferedImage getARGB8ImageFromABGR8(Image image) {

        return getARGB8ImageFrom(image);
//        ByteBuffer bb = image.getData(0);
//        bb.clear();
//
//        LoggerFactory.getLogger(ImageUtil.class).debug("Converting ABGR8 image to ARGB");
//        BufferedImage awtImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
//
//        int[] raw = new int[bb.limit()];
//        for (int i = 0; i < bb.limit() / 4; i++) {
//
//            // The '& 0xFF' is for getting the unsigned byte (if the byte is negative, it gives the positive equivalent, that is: +128, not an absolute)
//            // Composes the pixel color as int values from byte values. So an int is the mix of the 4 bytes forming the pixel.
//            raw[i] = ((bb.get(4 * i + 1) & 0xFF)) |         // B (index: 1)
//                    ((bb.get(4 * i + 0) & 0xFF) << 24) |    // A (index: 0)
//                    ((bb.get(4 * i + 3) & 0xFF) << 16) |    // R (index: 3)
//                    ((bb.get(4 * i + 2) & 0xFF) << 8);      // G (index: 2)
//
//
//        }
//
//        // If the alpha channel is not being saved, look at vertical flip.
//        awtImage.setRGB(0, 0, image.getWidth(), image.getHeight(), raw, 0, image.getWidth());
//
//        awtImage = verticalFlip(awtImage);
//
//        return awtImage;
    }


    public static BufferedImage verticalFlip(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        int adjustedHeight = height - 1;

        // If the height is odd, then the middle one isn't touched (so, all fine :D)
        height /= 2;
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                int auxRGBA = original.getRGB(x, y);

                original.setRGB(x, y, original.getRGB(x, adjustedHeight - y));
                original.setRGB(x, adjustedHeight - y, auxRGBA);
            }
        }

        return original;
    }

    public static BufferedImage verticalFlip(BufferedImage original, BufferedImage store) {

        int width = original.getWidth();
        int height = original.getHeight();

        for(int x = 0; x < width; x++) {
            for(int y = 1; y <= height; y++) {
                store.setRGB(x, height - y, original.getRGB(x, y));
            }
        }

        return store;


//        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
//        tx.translate(0, -original.getHeight());
//        AffineTransformOp transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
//
//        LoggerFactory.getLogger(VisualUtil.class).debug("Vertical flipping image of type: {}.", original.getType());
//        BufferedImage awtImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
////        BufferedImage awtImage = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
//        Graphics2D g2d = awtImage.createGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
//                RenderingHints.VALUE_RENDER_SPEED);
//        g2d.drawImage(original, transformOp, 0, 0);
//        g2d.dispose();
//        return awtImage;
    }

    public static Image verticalFlip(Image original) {
        int width = original.getWidth();
        int height = original.getHeight();

        ImageRaster io = ImageRaster.create(original);

        // If the height is odd, then the middle one isn't touched (so, all fine :D)
        for(int x = 0; x < width; x++) {
            for(int y = 1; y <= height / 2; y++) {
                ColorRGBA auxRGBA = io.getPixel(x, y);

                io.setPixel(x, y, io.getPixel(x, height - y));
                io.setPixel(x, height - y, auxRGBA);
            }
        }

        return original;
    }

    public static void fillColor(Image image, ColorRGBA color) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageRaster io = ImageRaster.create(image);

        for(int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                io.setPixel(w, h, new ColorRGBA(0, 1, 0, 0.5f));
            }
        }
    }




    public static void setBorderColor(Image image, ColorRGBA color, int thickness) {
        int width = image.getWidth();
        int height = image.getHeight();

        if(thickness > width / 2f) {
            throw new RuntimeException();
        }

        if(thickness > height / 2f) {
            throw new RuntimeException();
        }

        int right = width - 1;
        int bottom = height - 1;

//        int depth = image.getDepth();
//
//        float[] colorArray = color.getColorArray();
//        byte[] applyColors = new byte[colorArray.length];
//
//        for(int i = 0; i < colorArray.length; i++) {
//            applyColors[i] = (byte) (colorArray[i] * 255);
//        }
//
//        for(int i = 0; i < depth; i++) {
//            ByteBuffer buffer = image.getData(i);
//            byte c = applyColors[i];
//            for(int h = 0; h < height; h++) {
//                buffer.put(width * h, c);
//                buffer.put(width * h + right, c);
//            }
//        }


        ImageRaster io = ImageRaster.create(image);



        for(int i = 0; i < thickness; i++) {
            int first = i;
            int last = right - i;

            for(int h = 0; h < height; h++) {
                io.setPixel(first, h, color);
                io.setPixel(last, h, color);
            }

            last = bottom - i;
            for(int w = 0; w < width; w++) {
                io.setPixel(w, first, color);
                io.setPixel(w, last, color);
            }
        }


    }

}
