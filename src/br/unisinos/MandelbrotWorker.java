package br.unisinos;

import java.awt.*;
import java.util.Stack;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static br.unisinos.MandelbrotMain.Block;


public class MandelbrotWorker extends Thread {
    private Block currentBlock;
    private Stack<Block> taskStack;
    private ReentrantLock stackLock;
    private ReentrantLock pictureLock;
    private Condition taskCompleted;

    public MandelbrotWorker(Stack<Block> taskStack, ReentrantLock stackLock, ReentrantLock pictureLock, Condition taskCompleted) {
        this.taskStack = taskStack;
        this.stackLock = stackLock;
        this.pictureLock = pictureLock;
        this.taskCompleted = taskCompleted;
    }

    @Override
    public void run() {
//        System.out.println("["+this.getId()+"] start");
        while (true) {
            stackLock.lock();
            if (taskStack.isEmpty()) { // Todas as tarefas concluídas, encerra a thread
                stackLock.unlock();
//                System.out.println("["+this.getId()+"] finish");
                return;
            }
            currentBlock = taskStack.pop();
            stackLock.unlock();

//            System.out.println("["+this.getId()+"] Iniciando tarefa " + currentBlock.taskId);
            if (MandelbrotMain.grayout) {
                grayoutBlock();
            }
            Color[][] result = processBlock();
            setPixels(result);
//            System.out.println("["+this.getId()+"] Tarefa " + currentBlock.taskId + " concluída");
        }

    }

    private void grayoutBlock() {
        pictureLock.lock();
        Color gray = new Color(190, 190, 190);
        for (int i = currentBlock.start.x; i < currentBlock.sizeX + currentBlock.start.x; i++) {
            for (int j = currentBlock.start.y; j < currentBlock.sizeY + currentBlock.start.y; j++) {
                MandelbrotMain.picture.set(i, j, gray);
            }
        }
        pictureLock.unlock();

        stackLock.lock();
        taskCompleted.signal(); // Notifica a thread que atualiza a tela
        stackLock.unlock();
    }

    private void setPixels(Color[][] result) {
        pictureLock.lock();
        for (int i = currentBlock.start.x; i < currentBlock.sizeX + currentBlock.start.x; i++) {
            for (int j = currentBlock.start.y; j < currentBlock.sizeY + currentBlock.start.y; j++) {
                MandelbrotMain.picture.set(i, j, result[i - currentBlock.start.x][j - currentBlock.start.y]);
            }
        }
        pictureLock.unlock();

        stackLock.lock();
        MandelbrotMain.tasksRemaining--;
        taskCompleted.signal(); // Notifica a thread que atualiza a tela
        stackLock.unlock();
    }

    private Color[][] processBlock() {
        Color[][] result = new Color[currentBlock.sizeX][currentBlock.sizeY];
        for (int i = currentBlock.start.x; i < currentBlock.sizeX + currentBlock.start.x; i++) {
            for (int j = currentBlock.start.y; j < currentBlock.sizeY + currentBlock.start.y; j++) {
                double re = MandelbrotMain.size * i / MandelbrotMain.windowSizePixels + (MandelbrotMain.centerX - MandelbrotMain.size / 2);
                double im = MandelbrotMain.size * j / MandelbrotMain.windowSizePixels + (MandelbrotMain.centerY - MandelbrotMain.size / 2);
                Complex c = new Complex(re, im);
                int iterations = MandelbrotMain.mandelbrot(c, MandelbrotMain.maxIterations);
                Color color;
                if (iterations == 0) { // Fora da área
                    color = new Color(127, 127, 127);
                } else if (iterations == MandelbrotMain.maxIterations) { // Não escapou a tempo
                    color = new Color(0, 0, 0);
                } else { // Escapou em N iterações
                    float hue = (float) ((MandelbrotMain.hueAddend + iterations) / MandelbrotMain.hueDivisor);
                    color = Color.getHSBColor(hue, (float) MandelbrotMain.saturation, (float) MandelbrotMain.brightness);
                }
                result[i - currentBlock.start.x][j - currentBlock.start.y] = color;
            }
        }
        return result;
    }
}
