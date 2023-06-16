package br.unisinos;

import java.lang.reflect.Field;

public class Main {

    public static void main(String[] args) throws IllegalAccessException, InterruptedException {
        MandelbrotMain mandelbrot = new MandelbrotMain();
        setProperties(args, mandelbrot);
        printProperties(mandelbrot);
        mandelbrot.run();
    }

    private static void printProperties(MandelbrotMain mandelbrot) throws IllegalAccessException {
        System.out.println("\n---- Parâmetros aplicados: ----");
        for (Field field : MandelbrotMain.class.getFields()) {
            System.out.printf("\t%s:%s\n", field.getName(), field.get(mandelbrot));
        }
        System.out.println("-------------------------------\n");
    }

    public static void setProperties(String[] args, MandelbrotMain mandelbrot) throws IllegalAccessException {
        for (String arg : args) {
            int separatorIndex = arg.indexOf(':');
            if (separatorIndex != -1) {
                String key = arg.substring(0, separatorIndex);
                String value = arg.substring(separatorIndex + 1);
                mandelbrot.setProp(key, value);
            }
        }
    }
}
