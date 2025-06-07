package com.beder.texture.scatter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpriteRepository {
    private static SpriteRepository instance;
    private final List<BufferedImage> sprites;
    private final List<Integer> weights;

    private SpriteRepository() {
        sprites = new ArrayList<>();
        weights = new ArrayList<>();
    }

    public static synchronized SpriteRepository getInstance() {
        if (instance == null) {
            instance = new SpriteRepository();
        }
        return instance;
    }

    /**
     * Add a sprite with its associated weight. Weight must be >= 1.
     */
    public synchronized void addSprite(BufferedImage img, int weight) {
        if (img == null) {
            throw new IllegalArgumentException("Sprite image cannot be null");
        }
        if (weight < 1) {
            throw new IllegalArgumentException("Weight must be >= 1");
        }
        sprites.add(img);
        weights.add(weight);
    }

    public synchronized int getWeight(int index) {
        return weights.get(index);
    }

    
    /**
     * Remove all sprites and weights.
     */
    public synchronized void clear() {
        sprites.clear();
        weights.clear();
    }

    /**
     * @return total sum of all weights (0 if none).
     */
    public synchronized int getTotalWeight() {
        int sum = 0;
        for (int w : weights) {
            sum += w;
        }
        return sum;
    }

    /**
     * Choose a random sprite index according to weights.
     * @throws IllegalStateException if repository is empty.
     */
    public synchronized int getRandomIndex(Random rnd) {
        int total = getTotalWeight();
        if (total == 0 || sprites.isEmpty()) {
            throw new IllegalStateException("No sprites available for selection");
        }
        int r = rnd.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < weights.size(); i++) {
            cumulative += weights.get(i);
            if (r < cumulative) {
                return i;
            }
        }
        return weights.size() - 1;
    }

    /**
     * @return sprite buffered image at index.
     */
    public synchronized BufferedImage getSprite(int index) {
        return sprites.get(index);
    }

    /**
     * @return number of loaded sprites.
     */
    public synchronized int getCount() {
        return sprites.size();
    }
}