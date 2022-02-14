package concurrentcube;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class Cube {
    private final int SIDES = 6;
    private final int[] cubeArr;
    private final int size;
    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;
    private final Semaphore[] semLayers;
    private static final Semaphore mutex = new Semaphore(1);
    private static final Semaphore queue = new Semaphore(1, true);
    private static final Semaphore waitForAxis = new Semaphore(1);
    private static final AtomicInteger currAxis = new AtomicInteger(-1);
    private static final AtomicInteger ilePrzyKostce = new AtomicInteger(0);

    public Cube(int size,
                BiConsumer<Integer, Integer> beforeRotation,
                BiConsumer<Integer, Integer> afterRotation,
                Runnable beforeShowing,
                Runnable afterShowing) {

        this.size = size;
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;
        cubeArr = new int[size * size * SIDES];
        for (int i = 0; i < SIDES; i++)
            for (int j = 0; j < size * size; j++)
                cubeArr[size * size * i + j] = i;

        semLayers = new Semaphore[size];
        for (int i = 0; i < size; i++)
            semLayers[i] = new Semaphore(1);
    }

    public int getSize() {
        return size;
    }

    private int getOppositeSide(int side) {
        switch (side) {
            case 0: return 5;
            case 1: return 3;
            case 2: return 4;
            case 3: return 1;
            case 4: return 2;
            case 5: return 0;
            default: return -1;
        }
    }

    // Obraca ścianę kostki o 90 stopni w praow lub w lewo.
    private void rotateSide(int side, int direction) {
        int[] copyArr = new int[size * size * SIDES];
        System.arraycopy(cubeArr, 0, copyArr, 0, copyArr.length);

        if (direction == 1)
            for (int i = 0; i < size; i++)
                for (int j = 0; j < size; j++)
                    cubeArr[size * size * side + (size - 1 - i) + size * j] = copyArr[size * size * side + size * i + j];
        else // direction == -1
            for (int i = 0; i < size; i++)
                for (int j = 0; j < size; j++)
                    cubeArr[size * size * side + i + size * (size - 1 - j)] = copyArr[size * size * side + size * i + j];
    }

    // Obraca względem ściany 1 warstwę layer w prawo lub w lewo.
    private void rotateOX(int layer, int direction) {
        int[] sides = {0, 2, 5, 4};
        int[] copyArr = new int[size * size * SIDES];
        System.arraycopy(cubeArr, 0, copyArr, 0, copyArr.length);

        if (direction == 1) {
            if (layer == 0) rotateSide(1, 1);
            if (layer == size - 1) rotateSide(3, -1);

            for (int i = 0; i < size; i++) {
                cubeArr[size * size * sides[1] + layer + size * i] = copyArr[size * size * sides[0] + layer + size * i];
                cubeArr[size * size * sides[2] + layer + size * i] = copyArr[size * size * sides[1] + layer + size * i];
                cubeArr[size * size * sides[3] + (size - 1 - layer) + size * (size - 1 - i)] = copyArr[size * size * sides[2] + layer + size * i];
                cubeArr[size * size * sides[0] + layer + size * (size - 1 - i)] = copyArr[size * size * sides[3] + (size - 1 - layer) + size * i];
            }
        }
        else { // direction == -1
            if (layer == 0) rotateSide(1, -1);
            if (layer == size - 1) rotateSide(3, 1);

            for (int i = 0; i < size; i++) {
                cubeArr[size * size * sides[0] + layer + size * i] = copyArr[size * size * sides[1] + layer + size * i];
                cubeArr[size * size * sides[1] + layer + size * i] = copyArr[size * size * sides[2] + layer + size * i];
                cubeArr[size * size * sides[2] + layer + size * (size - 1 - i)] = copyArr[size * size * sides[3] + (size - 1 - layer) + size * i];
                cubeArr[size * size * sides[3] + (size - 1 - layer) + size * (size - 1 - i)] = copyArr[size * size * sides[0] + layer + size * i];
            }
        }
    }

    // Obraca względem ściany 0 warstwę layer w prawo lub w lewo.
    private void rotateOY(int layer, int direction) {
        int[] sides = {1, 4, 3, 2};
        int[] copyArr = new int[size * size * SIDES];
        System.arraycopy(cubeArr, 0, copyArr, 0, copyArr.length);

        if (direction == 1) {
            if (layer == 0) rotateSide(0, 1);
            if (layer == size - 1) rotateSide(5, -1);

            for (int i = 0; i < size; i++) {
                cubeArr[size * size * sides[1] + size * layer + i] = copyArr[size * size * sides[0] + size * layer + i];
                cubeArr[size * size * sides[2] + size * layer + i] = copyArr[size * size * sides[1] + size * layer + i];
                cubeArr[size * size * sides[3] + size * layer + i] = copyArr[size * size * sides[2] + size * layer + i];
                cubeArr[size * size * sides[0] + size * layer + i] = copyArr[size * size * sides[3] + size * layer + i];
            }
        }
        else { // direction == -1
            if (layer == 0) rotateSide(0, -1);
            if (layer == size - 1) rotateSide(5, 1);

            for (int i = 0; i < size; i++) {
                cubeArr[size * size * sides[0] + size * layer + i] = copyArr[size * size * sides[1] + size * layer + i];
                cubeArr[size * size * sides[1] + size * layer + i] = copyArr[size * size * sides[2] + size * layer + i];
                cubeArr[size * size * sides[2] + size * layer + i] = copyArr[size * size * sides[3] + size * layer + i];
                cubeArr[size * size * sides[3] + size * layer + i] = copyArr[size * size * sides[0] + size * layer + i];
            }
        }
    }

    // Obraca względem ściany 2 warstwę layer w prawo lub w lewo.
    private void rotateOZ(int layer, int direction) {
        int[] sides = {0, 3, 5, 1};
        int[] copyArr = new int[size * size * SIDES];
        System.arraycopy(cubeArr, 0, copyArr, 0, copyArr.length);

        if (direction == 1) {
            if (layer == 0) rotateSide(2, 1);
            if (layer == size - 1) rotateSide(4, -1);

            for (int i = 0; i < size; i++) {
                cubeArr[size * size * sides[1] + layer + size * i] = copyArr[size * size * sides[0] + size * (size - 1 - layer) + i];
                cubeArr[size * size * sides[2] + size * layer + (size - 1 - i)] = copyArr[size * size * sides[1] + layer + size * i];
                cubeArr[size * size * sides[3] + (size - 1 - layer) + size * i] = copyArr[size * size * sides[2] + size * layer + i];
                cubeArr[size * size * sides[0] + size * (size - 1 - layer) + (size - 1 - i)] = copyArr[size * size * sides[3] + (size - 1 - layer) + size * i];
            }
        }
        else { // direction == -1
            if (layer == 0) rotateSide(2, -1);
            if (layer == size - 1) rotateSide(4, 1);

            for (int i = 0; i < size; i++) {
                cubeArr[size * size * sides[0] + size * (size - 1 - layer) + i] = copyArr[size * size * sides[1] + layer + size * i];
                cubeArr[size * size * sides[1] + layer + size * (size - 1 - i)] = copyArr[size * size * sides[2] + size * layer + i];
                cubeArr[size * size * sides[2] + size * layer + i] = copyArr[size * size * sides[3] + (size - 1 - layer) + size * i];
                cubeArr[size * size * sides[3] + (size - 1 - layer) + size * (size - 1 - i)] = copyArr[size * size * sides[0] + size * (size - 1 - layer) + i];
            }
        }
    }

    public void rotate(int side, int layer) throws InterruptedException {
        int direction = 1;
        if (side == 3 || side == 4 || side == 5) {
            side = getOppositeSide(side);
            layer = size - 1 - layer;
            direction = -1;
        }

        queue.acquire();
        if (currAxis.get() != side) {
            waitForAxis.acquire();
            currAxis.set(side);
        }
        ilePrzyKostce.incrementAndGet();
        queue.release();
        semLayers[layer].acquire();

        beforeRotation.accept(side, layer);
        switch (side) {
            case 0:
                rotateOY(layer, direction);
                break;
            case 1:
                rotateOX(layer, direction);
                break;
            case 2:
                rotateOZ(layer, direction);
                break;
        }
        afterRotation.accept(side, layer);

        semLayers[layer].release();
        if (ilePrzyKostce.decrementAndGet() == 0) {
            currAxis.set(-1);
            waitForAxis.release();
        }
    }

    public String show() throws InterruptedException {
        queue.acquire();
        if (currAxis.get() != 3) {
            waitForAxis.acquire();
            currAxis.set(3);
        }
        ilePrzyKostce.incrementAndGet();
        queue.release();

        beforeShowing.run();
        StringBuilder res = new StringBuilder();
        for (int i : cubeArr)
            res.append(i);
        afterShowing.run();

        if (ilePrzyKostce.decrementAndGet() == 0) {
            currAxis.set(-1);
            waitForAxis.release();
        }

        return res.toString();
    }
}
