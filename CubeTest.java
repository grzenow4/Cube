package concurrentcube;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class CubeTest {
    private final int COUNT = 20;
    private final Semaphore mutex = new Semaphore(1);

    public class SequentialRotate implements Runnable {
        private final Cube cube;
        private final int side;
        private final int layer;

        public SequentialRotate(Cube cube, int side, int layer) {
            this.cube = cube;
            this.side = side;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                mutex.acquire();
                cube.rotate(side, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutex.release();
            }
        }
    }

    public class ConcurrentRotate implements Runnable {
        private final Cube cube;
        private final int side;
        private final int layer;

        public ConcurrentRotate(Cube cube, int side, int layer) {
            this.cube = cube;
            this.side = side;
            this.layer = layer;
        }

        @Override
        public void run() {
            try {
                cube.rotate(side, layer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class Show implements Runnable {
        private final Cube cube;

        public Show(Cube cube) {
            this.cube = cube;
        }

        @Override
        public void run() {
            try {
                cube.show();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void testSequential() {
        Cube cube = new Cube(4,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {},
                () -> {},
                () -> {}
        );
        int layer = 0;

        Thread[] threads = new Thread[COUNT];
        for (int i = 0; i < COUNT; i++) {
            threads[i] = new Thread(new SequentialRotate(cube, 0, layer));
            layer = (layer + 1) % cube.getSize();
        }

        for (Thread thread : threads)
            thread.start();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void testConcurrent() {
        Cube cube = new Cube(4,
                (x, y) -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (x, y) -> {},
                () -> {},
                () -> {}
        );

        Thread[] threads = new Thread[COUNT];
        for (int i = 0; i < COUNT; i++)
            threads[i] = new Thread(new ConcurrentRotate(cube, 0, i % 4));

        for (Thread thread : threads)
            thread.start();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Sprawdza poprawność obrotów.
    @Test
    void test1() {
        Cube cube = new Cube(3,
                (x, y) -> {},
                (x, y) -> {},
                () -> {},
                () -> {}
        );

        try {
            for (int i = 0; i < 5; i++)
                for (int layer = 0; layer < 3; layer++)
                    for (int side = 0; side < 6; side++)
                        cube.rotate(side, layer);
            String status = cube.show();
            assert (status.equals("340020421211105115012212302033355533430434504421545542"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Sprawdza współbieżność implementacji.
    @Test
    void test2() {
        long timeWhenStarted1 = System.currentTimeMillis();
        testSequential();
        long timeWhenFinish1 = System.currentTimeMillis();
        long time1 = timeWhenFinish1 - timeWhenStarted1;
        long timeWhenStarted2 = System.currentTimeMillis();
        testConcurrent();
        long timeWhenFinish2 = System.currentTimeMillis();
        long time2 = timeWhenFinish2 - timeWhenStarted2;
        assert (time1 > time2);
    }

    // Sprawdza poprawność beforeRotation i afterRotation.
    @Test
    void test3() {
        var counter1 = new Object() { final AtomicInteger value = new AtomicInteger(0); };
        var counter2 = new Object() { final AtomicInteger value = new AtomicInteger(0); };

        Cube cube = new Cube(4,
                (x, y) -> { counter1.value.incrementAndGet(); },
                (x, y) -> { counter2.value.incrementAndGet(); },
                () -> {},
                () -> {}
        );

        Thread[] threads = new Thread[COUNT];

        for (int i = 0; i < COUNT; i++)
            threads[i] = new Thread(new ConcurrentRotate(cube, i % 6, i % 4));

        for (Thread thread : threads)
            thread.start();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assert (counter1.value.get() == COUNT && counter2.value.get() == COUNT);
    }

    // Sprawdza poprawność beforeShowing i afterShowing.
    @Test
    void test4() {
        var counter1 = new Object() { final AtomicInteger value = new AtomicInteger(0); };
        var counter2 = new Object() { final AtomicInteger value = new AtomicInteger(0); };

        Cube cube = new Cube(4,
                (x, y) -> {},
                (x, y) -> {},
                () -> { counter1.value.incrementAndGet(); },
                () -> { counter2.value.incrementAndGet(); }
        );

        Thread[] threads = new Thread[COUNT];

        for (int i = 0; i < COUNT; i++)
            threads[i] = new Thread(new Show(cube));

        for (Thread thread : threads)
            thread.start();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assert (counter1.value.get() == COUNT && counter2.value.get() == COUNT);
    }
}
