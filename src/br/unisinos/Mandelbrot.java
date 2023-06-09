package br.unisinos;

import java.awt.*;
import java.util.Scanner;

public class Mandelbrot {
    public double STARTING_XC = 0.0;
    public double STARTING_YC = 0.0;
    public double STARTING_SIZE = 4.0;

    public double centerX, centerY; // Center coordinates
    public double size; // Visible area
    public int maxIterations = 300;  // maximum number of iterations

    public int blockRows = 8;
    public int blockColumns = 8;
    public Block[] blocks = new Block[blockRows * blockColumns]; // Divisões da tela para processamento

    public int windowSizePixels = 1024; // Tamanho da tela (altura e largura) em pixels
    public Picture picture = new Picture(windowSizePixels, windowSizePixels); // Janela do fractal

    public Scanner scanner = new Scanner(System.in);
    public String action = "+";

    public static int mandelbrot(Complex z0, int maxIterations) {
        Complex z = z0;
        for (int t = 0; t < maxIterations; t++) {
            boolean escapedSet = z.abs() > 2.0;
            if (escapedSet) return t;
            z = z.times(z).plus(z0);
        }
        return maxIterations;
    }

    public void main() {
        centerX = STARTING_XC;
        centerY = STARTING_YC;
        size = STARTING_SIZE;

        for (int i = 0; i < blockRows; i++) {
            for (int j = 0; j < blockColumns; j++) {
                blocks[blockRows * i + j] = (new Block(
                        new Point(i * windowSizePixels / blockRows, j * windowSizePixels / blockColumns),
                        windowSizePixels / blockRows,
                        windowSizePixels / blockColumns)
                );
            }
        }

        while (true) {
            renderFractal(blocks);
            waitForInput();
            System.out.println("centerX: " + centerX + "\ncenterY: " + centerY + "\nsize: " + size + "\nmax_iterations:" + maxIterations);
        }

    }

    private void renderFractal(Block[] blocks) {
        for (Block block : blocks) {
            for (int i = block.start.x; i < block.sizeX + block.start.x; i++) {
                for (int j = block.start.y; j < block.sizeY + block.start.y; j++) {
                    double a = centerX - size / 2 + size * i / windowSizePixels;
                    double b = centerY - size / 2 + size * j / windowSizePixels;
                    Complex z = new Complex(a, b);
                    int iterations = mandelbrot(z, maxIterations);
                    Color color;
                    if (iterations == 0) {
                        color = new Color(127, 127, 127); // Fora da área
                    } else if (iterations == maxIterations) {
                        color = new Color(0, 0, 0); // Não escapou a tempo
                    } else {
//                        RGB
//                        Color minColor = new Color(0, 63, 63);
//                        double redWeight = 0.0;
//                        double greenWeight = 0.0;
//                        double blueWeight = 1.0;
//                        color = new Color(
//                                (int) (((255 - minColor.getRed()) / (double) maxIterations) * iterations * redWeight) + minColor.getRed(),
//                                (int) (((255 - minColor.getGreen()) / (double) maxIterations) * iterations * greenWeight) + minColor.getGreen(),
//                                (int) (((255 - minColor.getBlue()) / (double) maxIterations) * iterations * blueWeight) + minColor.getBlue()
//                        );
                        color = Color.getHSBColor((iterations * 10.0f) / (float) maxIterations, 1.0f, 1.0f);
                    }

                    picture.set(i, windowSizePixels - 1 - j, color);
                }
            }
            picture.show();
        }
    }

    private void waitForInput() {

        System.out.println("Waiting for input");
        String str = scanner.nextLine();
        if (!str.isEmpty())
            action = str;
        System.out.println("Action: " + action);

        Point mousePosition = picture.getMousePosition();
        Point screenPosition = picture.getScreenPosition();
        Point target = new Point(mousePosition.x - screenPosition.x, mousePosition.y - screenPosition.y);
        double targetX = centerX + (target.x - windowSizePixels / 2.0) / (windowSizePixels / size);
        double targetY = centerY - (target.y - windowSizePixels / 2.0) / (windowSizePixels / size);
//            System.out.println("image: " + picture.getScreenPosition() + ", mouse: " + picture.getMousePosition() + ", screen_target: " + screenTarget + ", targetX: " + targetX + ", targetY: " + targetY);

        double step = 0.5;
        switch (action) {
            case "+":
                System.out.println("zoom in");
                centerX += (targetX - centerX) * step;
                centerY += (targetY - centerY) * step;
                size *= step;
                break;
            case "-":
                System.out.println("zoom out");
                centerX -= (targetX - centerX) * step;
                centerY -= (targetY - centerY) * step;
                size /= step;
                break;
            case "reset":
                System.out.println("reset");
                centerX = STARTING_XC;
                centerY = STARTING_YC;
                size = STARTING_SIZE;
                break;
            case "o":
                System.out.println("maxIterations++");
                maxIterations++;
                break;
            case "p":
                System.out.println("maxIterations--");
                maxIterations--;
                break;
            default:
                System.out.println("Unknown action");
        }
    }

    private static class Block {
        public Point start;
        public int sizeX;
        public int sizeY;

        public Block(Point start, int sizeX, int sizeY) {
            this.start = start;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
        }
    }
}
