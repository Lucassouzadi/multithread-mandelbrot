package br.unisinos;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MandelbrotMain {
    private double STARTING_XC;
    private double STARTING_YC;
    private double STARTING_SIZE;

    public static double centerX = 0.0, centerY = 0.0; // Center coordinates
    public static double size = 4.0; // Visible area
    public static int maxIterations = 1000;  // maximum number of iterations

    public int numThreads = 1;
    public static int blockRows = 8;
    public static int blockColumns = 8;
    public static boolean shuffle = false;
    protected static int tasksRemaining;

    public String action = "+";
    public double zoomStep = 0.5;

    public static int windowSizePixels = 960; // Tamanho da tela (altura e largura) em pixels
    protected static Picture picture; // Janela do fractal

    public static boolean grayout = true;
    public static boolean whiteout = true;
    public static boolean showAxis = false;

    public static double hueAddend = 0.0;
    public static double hueDivisor = 127.0;
    public static double saturation = 1.0;
    public static double brightness = 1.0;

    private final Scanner scanner = new Scanner(System.in);

    public static int mandelbrot(Complex c, int maxIterations) {
        Complex z = new Complex(0.0, 0.0);
        for (int t = 0; t < maxIterations; t++) {
            z.times(z);
            z.plus(c);
            if (z.abs() > 2.0) return t; // Escaped set
        }
        return maxIterations;
    }

    public void run() throws InterruptedException {
        picture = new Picture(windowSizePixels, windowSizePixels);
        picture.setOriginLowerLeft();
        STARTING_XC = centerX;
        STARTING_YC = centerY;
        STARTING_SIZE = size;

        while (true) {
            renderFractal();
            System.out.println("centerX:" + centerX + "\ncenterY:" + centerY + "\nsize:" + size + "\nmaxIterations:" + maxIterations + "\n");
            waitForInput();
        }
    }

    private void renderFractal() throws InterruptedException {
        Block[] blocks = new Block[blockRows * blockColumns];
        tasksRemaining = blockRows * blockColumns;

        for (int i = 0; i < blockRows; i++) {
            for (int j = 0; j < blockColumns; j++) {
                blocks[blockColumns * i + j] = (new Block(blockColumns * i + j,
                        new Point(j * windowSizePixels / blockColumns, i * windowSizePixels / blockRows),
                        windowSizePixels / blockColumns,
                        windowSizePixels / blockRows)
                );
            }
        }

        long totalTime = System.currentTimeMillis();

        Stack<Block> taskStack = new Stack<>();

        ReentrantLock stackMutex = new ReentrantLock();
        ReentrantLock pictureMutex = new ReentrantLock();
        Condition taskCompleted = stackMutex.newCondition();

        for (Block block : blocks) {
            taskStack.push(block);
        }
        if (MandelbrotMain.shuffle)
            taskStack.sort(Comparator.comparingInt(Object::hashCode));

        Thread resultThread = new MandelbrotPrinter(stackMutex, pictureMutex, taskCompleted);
        resultThread.start();

        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            Thread workerThread = new MandelbrotWorker(taskStack, stackMutex, pictureMutex, taskCompleted);
            workers.add(workerThread);
            workerThread.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        resultThread.join();
        System.out.println("Todas as tarefas foram concluídas. " + numThreads + " Threads - Tempo: " + ((System.currentTimeMillis() - totalTime) / 1000.0));
    }

    private void waitForInput() {
        System.out.println("Waiting for input...");
        String str = scanner.nextLine();

        int separatorIndex = str.indexOf(':');
        if (separatorIndex != -1) {
            try {
                Main.setProperties(str.split(" "), this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return;
        }

        if (!str.isEmpty()) {
            action = str;
        }
        System.out.println("Action: " + action);

        Point mousePosition = picture.getMousePosition();
        Point screenPosition = picture.getScreenPosition();
        Point target = new Point(mousePosition.x - screenPosition.x, mousePosition.y - screenPosition.y);
        double targetX = centerX + (target.x - windowSizePixels / 2.0) / (windowSizePixels / size);
        double targetY = centerY - (target.y - windowSizePixels / 2.0) / (windowSizePixels / size);
//            System.out.println("image: " + picture.getScreenPosition() + ", mouse: " + picture.getMousePosition() + ", screen_target: " + screenTarget + ", targetX: " + targetX + ", targetY: " + targetY);


        switch (action) {
            case "+":
                System.out.println("zoom in");
                centerX += (targetX - centerX) * (1.0 - zoomStep);
                centerY += (targetY - centerY) * (1.0 - zoomStep);
                size *= zoomStep;
                break;
            case "-":
                System.out.println("zoom out");
                centerX -= (targetX - centerX) * (1.0 - zoomStep);
                centerY -= (targetY - centerY) * (1.0 - zoomStep);
                size /= zoomStep;
                break;
            case "++":
                System.out.println("maxIterations++");
                maxIterations++;
                break;
            case "--":
                System.out.println("maxIterations--");
                maxIterations--;
                break;
            case "reset":
                System.out.println("reset");
                centerX = STARTING_XC;
                centerY = STARTING_YC;
                size = STARTING_SIZE;
                break;
            case "refresh":
                System.out.println("refresh");
                break;
            case "exit":
                System.exit(0);
            default:
                System.out.println("Unknown action");

        }
    }

    public void setProp(String key, String value) {
        try {
            Field field = MandelbrotMain.class.getDeclaredField(key);
            if (Modifier.isPublic(field.getModifiers())) {
                switch (field.getType().toString()) {
                    case "boolean":
                        field.set(this, Boolean.valueOf(value));
                        break;
                    case "int":
                        field.set(this, Integer.valueOf(value));
                        break;
                    case "double":
                        field.set(this, Double.valueOf(value));
                        break;
                    default:
                        field.set(this, value);
                        break;
                }
            } else {
                System.err.printf("Campo não é público: \"%s\"\n", key);
            }
        } catch (NoSuchFieldException e) {
            System.err.printf("Parâmetro desconhecido: \"%s\"\n", key);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Block {
        public int taskId;
        public Point start;
        public int sizeX;
        public int sizeY;

        public Block(int id, Point start, int sizeX, int sizeY) {
            this.taskId = id;
            this.start = start;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
        }
    }
}
