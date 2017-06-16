package de.lexej.VNFP.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * This container class can be used to store multiple weights
 * for different objects, helping with the weighted randomness selection
 * in different parts of the algorithm.
 *
 * @author alex
 */
public class ObjectWeights<T> {
    /**
     * The object the weights are stored for.
     */
    public final T content;
    /**
     * All weight values for this object.
     */
    public double[] w;

    /**
     * Creates a new weight container with the given content and weight.
     *
     * @param content The object the weight is stored for.
     * @param w       All weight values for this object.
     */
    public ObjectWeights(T content, double... w) {
        this.content = Objects.requireNonNull(content);
        this.w = Objects.requireNonNull(w);
    }

    @Override
    public String toString() {
        return "WeightContainer{"+ Arrays.toString(w)+": "+content.toString()+"}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectWeights<?> that = (ObjectWeights<?>) o;

        return content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
