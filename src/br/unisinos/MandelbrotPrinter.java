package br.unisinos;

import java.awt.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class MandelbrotPrinter extends Thread {

    private double fps = 60;
    private double maxFrameTimeMilis = 1000.0 / fps;

    private ReentrantLock stackLock;
    private ReentrantLock pictureLock;
    private Condition taskCompleted;

    public MandelbrotPrinter(ReentrantLock stackLock, ReentrantLock pictureLock, Condition taskCompleted) {
        this.stackLock = stackLock;
        this.pictureLock = pictureLock;
        this.taskCompleted = taskCompleted;
    }

    @Override
    public void run() {
        if (MandelbrotMain.whiteout) {
            whiteout();
        }

        long time = System.nanoTime();
        int prints = 0;
        stackLock.lock();
        try {
            while (MandelbrotMain.tasksRemaining > 0) {
                taskCompleted.await(); // Aguarda a notificação de conclusão de uma tarefa
                double frameTime = (System.nanoTime() - time) / 1_000_000.0;
                if (frameTime < maxFrameTimeMilis) continue;
                stackLock.unlock();

                pictureLock.lock();
                time = System.nanoTime();
                if (MandelbrotMain.showAxis)
                    showAxis();
                MandelbrotMain.picture.show();
                prints++;
                pictureLock.unlock();

                stackLock.lock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            stackLock.unlock();
            MandelbrotMain.picture.show();
            System.out.println("prints:" + prints);
        }

    }

    private void showAxis() {
        for (int i = 0; i < MandelbrotMain.windowSizePixels; i++) {
            MandelbrotMain.picture.set(MandelbrotMain.windowSizePixels / 2, i, new Color(255, 255, 255));
            MandelbrotMain.picture.set(i, MandelbrotMain.windowSizePixels / 2, new Color(255, 255, 255));
        }
    }

    private void whiteout() {
        pictureLock.lock();
        Color white = new Color(255, 255, 255);
        for (int i = 0; i < MandelbrotMain.windowSizePixels; i++) {
            for (int j = 0; j < MandelbrotMain.windowSizePixels; j++) {
                MandelbrotMain.picture.set(i, j, white);
            }
        }
        MandelbrotMain.picture.show();
        pictureLock.unlock();
    }

}
