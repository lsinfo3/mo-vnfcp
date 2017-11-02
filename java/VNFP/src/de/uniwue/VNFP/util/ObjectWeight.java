package de.uniwue.VNFP.util;

import java.util.Objects;

/**
 * This container class can be used to store weights
 * for different objects, helping with the weighted randomness selection
 * in different parts of the algorithm.
 *
 * @author alex
 */
public class ObjectWeight<T> implements Comparable<ObjectWeight> {
    /**
     * The object the weight is stored for.
     */
    public final T content;
    /**
     * The weight of this object, used for the random drawing.
     */
    public double weight;

    /**
     * Creates a new weight container with the given content and weight.
     *
     * @param content The object the weight is stored for.
     * @param weight  The weight of this object, used for the random drawing.
     */
    public ObjectWeight(T content, double weight) {
        this.content = Objects.requireNonNull(content);
        this.weight = weight;
    }

    @Override
    public int compareTo(ObjectWeight o) {
        return Double.compare(weight, o.weight);
    }

    @Override
    public String toString() {
        return "WeightContainer{"+weight+": "+content.toString()+"}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectWeight<?> that = (ObjectWeight<?>) o;

        return content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
