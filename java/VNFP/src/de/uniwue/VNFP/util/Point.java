package de.uniwue.VNFP.util;

/**
 * Stores 2 double values to provide X/Y coordinates.
 */
public class Point {
	public final double x;
	public final double y;

	/**
	 * Creates a new X/Y point with the given coordinates.
	 * @param x X-coordinate of the new point.
	 * @param y Y-coordinate of the new point.
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates a new X/Y point with the given coordinates.
	 * Parses the String values into doubles.
	 * @param x X-coordinate of the new point.
	 * @param y Y-coordinate of the new point.
	 */
	public Point(String x, String y) {
		this.x = Double.parseDouble(x);
		this.y = Double.parseDouble(y);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Point point = (Point) o;

		if (Double.compare(point.x, x) != 0) return false;
		return Double.compare(point.y, y) == 0;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "("+x+","+y+")";
	}
}
