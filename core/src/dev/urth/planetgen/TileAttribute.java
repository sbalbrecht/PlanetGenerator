package dev.urth.planetgen;

import java.util.Objects;

public class TileAttribute implements Comparable<TileAttribute> {
    private String name;
    private float value;

    public TileAttribute() {
        this.name = "";
        this.value = 0.0f;
    }

    public TileAttribute(float value) {
        this.name = "";
        this.value = value;
    }

    public TileAttribute(String n) {
        this.name = n;
        this.value = 0.0f;
    }

    public TileAttribute(String name, float value) {
        this.name = name;
        this.value = value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%12s: %2.3f", name, value);
    }

    @Override
    public int compareTo(TileAttribute o) {
        return Float.compare(o.getValue(), this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TileAttribute that = (TileAttribute) o;
        return Float.compare(that.value, value) == 0 && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
