package com.sentinel.surveillance.simulation;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simulates virtual camera feeds with moving entities.
 * Each camera has a unique scene with people, vehicles, and objects
 * that move around realistically for detection modules to analyze.
 * Uses CopyOnWriteArrayList for thread-safe entity access between
 * the FX animation thread and detection thread pool.
 */
public class CameraSimulator {

    /** Represents a moving entity in a camera scene */
    public static class Entity {
        public double x, y, width, height;
        public double dx, dy; // velocity
        public String type;   // "person", "vehicle", "object"
        public long spawnTime;

        public Entity(double x, double y, double w, double h,
                      double dx, double dy, String type) {
            this.x = x; this.y = y; this.width = w; this.height = h;
            this.dx = dx; this.dy = dy; this.type = type;
            this.spawnTime = System.currentTimeMillis();
        }
    }

    /** Camera definition with name, position, and scene configuration */
    public static class Camera {
        public final String id;
        public final String name;
        public final double mapX, mapY; // Position on map
        public final List<Entity> entities = new CopyOnWriteArrayList<>();
        public boolean active = true;

        public Camera(String id, String name, double mapX, double mapY) {
            this.id = id; this.name = name;
            this.mapX = mapX; this.mapY = mapY;
        }
    }

    private final List<Camera> cameras = new ArrayList<>();
    private final Random random = new Random();
    private final double canvasWidth;
    private final double canvasHeight;

    public CameraSimulator(double canvasWidth, double canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        // Initialize 3 cameras with unique positions
        Camera gate = new Camera("CAM-01", "Gate 1", 60, 250);
        Camera perimeter = new Camera("CAM-02", "Perimeter East", 200, 100);
        Camera watchtower = new Camera("CAM-03", "Watchtower 2", 250, 200);

        cameras.add(gate);
        cameras.add(perimeter);
        cameras.add(watchtower);

        // Populate initial entities for each camera
        for (Camera cam : cameras) {
            populateScene(cam);
        }
    }

    /** Populate a camera scene with random entities */
    private void populateScene(Camera cam) {
        int personCount = 2 + random.nextInt(3); // 2-4 people
        for (int i = 0; i < personCount; i++) {
            double x = 30 + random.nextDouble() * (canvasWidth - 60);
            double y = 50 + random.nextDouble() * (canvasHeight - 100);
            double dx = (random.nextDouble() - 0.5) * 3;
            double dy = (random.nextDouble() - 0.5) * 2;
            cam.entities.add(new Entity(x, y, 16, 32, dx, dy, "person"));
        }
        // Maybe add a vehicle
        if (random.nextDouble() < 0.5) {
            double x = random.nextBoolean() ? -40 : canvasWidth + 40;
            double y = canvasHeight * 0.6 + random.nextDouble() * (canvasHeight * 0.3);
            double dx = x < 0 ? 1.5 + random.nextDouble() : -(1.5 + random.nextDouble());
            cam.entities.add(new Entity(x, y, 50, 25, dx, 0, "vehicle"));
        }
    }

    /** Update all entity positions (called each frame) */
    public void update() {
        for (Camera cam : cameras) {
            if (!cam.active) continue;
            List<Entity> toRemove = new ArrayList<>();
            for (Entity e : cam.entities) {
                e.x += e.dx;
                e.y += e.dy;

                // Bounce persons off walls
                if ("person".equals(e.type)) {
                    if (e.x < 10 || e.x > canvasWidth - 10) {
                        e.dx *= -1;
                        e.x = Math.max(10, Math.min(canvasWidth - 10, e.x));
                    }
                    if (e.y < 20 || e.y > canvasHeight - 20) {
                        e.dy *= -1;
                        e.y = Math.max(20, Math.min(canvasHeight - 20, e.y));
                    }
                    // Occasional direction change
                    if (random.nextDouble() < 0.02) {
                        e.dx = (random.nextDouble() - 0.5) * 3;
                        e.dy = (random.nextDouble() - 0.5) * 2;
                    }
                }

                // Mark vehicles that go off-screen for removal
                if ("vehicle".equals(e.type)) {
                    if (e.x < -60 || e.x > canvasWidth + 60) {
                        toRemove.add(e);
                    }
                }
            }
            cam.entities.removeAll(toRemove);

            // Randomly spawn new entities
            if (random.nextDouble() < 0.005 && cam.entities.size() < 8) {
                double x = 30 + random.nextDouble() * (canvasWidth - 60);
                double y = 50 + random.nextDouble() * (canvasHeight - 100);
                cam.entities.add(new Entity(x, y, 16, 32,
                        (random.nextDouble() - 0.5) * 2,
                        (random.nextDouble() - 0.5) * 1.5, "person"));
            }
            if (random.nextDouble() < 0.002) {
                double x = random.nextBoolean() ? -40 : canvasWidth + 40;
                double y = canvasHeight * 0.6 + random.nextDouble() * (canvasHeight * 0.25);
                cam.entities.add(new Entity(x, y, 50, 25,
                        x < 0 ? 1.5 : -1.5, 0, "vehicle"));
            }
        }
    }

    /** Get a specific camera by name */
    public Camera getCamera(String name) {
        return cameras.stream().filter(c -> c.name.equals(name)).findFirst().orElse(cameras.get(0));
    }

    /** Get all cameras */
    public List<Camera> getCameras() { return cameras; }

    /** Get entity positions as double arrays for detection modules */
    public List<double[]> getEntityPositions(String cameraName) {
        Camera cam = getCamera(cameraName);
        List<double[]> positions = new ArrayList<>();
        for (Entity e : cam.entities) {
            positions.add(new double[]{e.x, e.y, e.width, e.height});
        }
        return positions;
    }
}
