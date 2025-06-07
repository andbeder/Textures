package com.beder.texture;

import java.util.TreeMap;

public class Parameters extends TreeMap<String, Double> {
    public double get(String s, double def) {
        if (containsKey(s)) {
            return super.get(s); // FIX: use super.get to avoid recursive call
        }
        return def;
    }

    public void put(String key, String val) {
        double d = Double.parseDouble(val);
        put(key, d);
    }
}
