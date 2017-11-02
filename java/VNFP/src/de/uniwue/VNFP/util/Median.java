package de.uniwue.VNFP.util;

import java.util.List;
import java.util.Random;

/**
 * Small utility-class for median selection in O(n) expected time.
 *
 * @author alex
 */
public class Median {
    /**
     * Finds the ((n-1)/2)-th smallest element in the given array and returns it,
     * where n = a.length.
     *
     * @param a Array containing all values.
     * @return The ((n-1)/2)-th smallest element
     */
    public static double median(double[] a) {
        if (a.length == 0) return 0.0;
        return randomizedSelect(a, (a.length-1)/2, new Random());
    }

    /**
     * Finds the i-th smallest element in the list using quick sort's randomized partition.
     * Treats one of ObjectWeights' values as input array.
     * Does not change the list's elements' order.
     *
     * @param a     ObjectWeights-List containing all values.
     * @param index Which values inside the ObjectWeights should be used.
     * @param i     Number of smallest element to be found.
     * @param rand  Used for random number generation.
     * @return The i-th smallest element.
     */
    public static double randomizedSelect(List<? extends ObjectWeights<?>> a, int index, int i, Random rand) {
        double[] b = a.stream().mapToDouble(w -> w.w[index]).toArray();
        return randomizedSelect(b, 0, b.length - 1, i, rand);
    }

    /**
     * Finds the i-th smallest element in an array using quick sort's randomized partition.
     * Does not change the array's elements' order.
     *
     * @param a    Array containing all values.
     * @param i    Number of smallest element to be found.
     * @param rand Used for random number generation.
     * @return The i-th smallest element.
     */
    public static double randomizedSelect(double[] a, int i, Random rand) {
        double[] b = new double[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        return randomizedSelect(b, 0, b.length - 1, i, rand);
    }

    /**
     * Finds the i-th smallest element in an array using quick sort's randomized partition.
     * Changes the array's elements' order.
     *
     * @param a    Array containing all values.
     * @param l    Left boundary index.
     * @param r    Right boundary index.
     * @param i    Number of smallest element to be found.
     * @param rand Used for random number generation.
     * @return The i-th smallest element.
     */
    private static double randomizedSelect(double[] a, int l, int r, int i, Random rand) {
        if (l == r) return a[l];
        int m = randomizedPartition(a, l, r, rand);
        int k = m - l + 1;

        if (i+1 == k) {
            return a[m];
        }
        else {
            if (i+1 < k) {
                return randomizedSelect(a, l, m-1, i, rand);
            }
            else {
                return randomizedSelect(a, m+1, r, i-k, rand);
            }
        }
    }

    /**
     * Picks a random pivot element, swaps all smaller elements to the beginning of the array,
     * all bigger elements to the end of it.
     * Returns the position of the pivot after the swapping.
     *
     * @param a    Array containing all values.
     * @param l    Left boundary index.
     * @param r    Right boundary index.
     * @param rand Used for random number generation.
     * @return The index of the pivot after rearranging.
     */
    private static int randomizedPartition(double[] a, int l, int r, Random rand) {
        int k = rand.nextInt(r-l+1)+l;
        swap(a, r, k);

        double pivot = a[r];
        int i = l;
        for (int j = l; j < r; j++) {
            if (a[j] <= pivot) {
                swap(a, i, j);
                i++;
            }
        }
        swap(a, i, r);
        return i;
    }

    /**
     * Swaps the position of the i-th and j-th element in the array.
     *
     * @param a Array containing all values.
     * @param i First index to be swapped.
     * @param j Second index to be swapped.
     */
    private static void swap(double[] a, int i, int j) {
        double tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
}
