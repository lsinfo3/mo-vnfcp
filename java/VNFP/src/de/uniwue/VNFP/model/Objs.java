package de.uniwue.VNFP.model;

import java.util.ArrayList;

/**
 * This Enum-like class lists all possible objective functions for the objective vectors.
 * All values are to be minimized.
 *
 * Prefix TOTAL indicates a sum of all respective values.
 * Prefix MEAN indicates the average of all respective values.
 * Suffix INDEX indicates the ratio (value) / (minimum possible value).
 * Infix ROOTED indicates that, before summing up the values, their square root has been taken.
 */
public class Objs {
	private int nextOrdinal = 0;
	private ArrayList<Obj> values = new ArrayList<>();

	// Unfeasible solution:
	public final Obj UNFEASIBLE = new Obj("UNFEASIBLE", "Unfeasible");

	// Migrations:
	public final Obj NUMBER_OF_VNF_REPLACEMENTS = new Obj("NUMBER_OF_VNF_REPLACEMENTS", "Number of VNF Replacements");
	public final Obj TOTAL_FLOW_MIGRATION_PENALTY = new Obj("TOTAL_FLOW_MIGRATION_PENALTY", "Total Flow Migration Penalty");

	// Link resources:
	public final Obj MEAN_DELAY_INDEX = new Obj("MEAN_DELAY_INDEX", "Mean Delay Index");
	public final Obj MEDIAN_DELAY_INDEX = new Obj("MEDIAN_DELAY_INDEX", "Median Delay Index");
	public final Obj TOTAL_DELAY = new Obj("TOTAL_DELAY", "Total Delay");
	public final Obj MAX_DELAY_INDEX = new Obj("MAX_DELAY_INDEX", "Max Delay Index");
	public final Obj MEAN_HOPS_INDEX = new Obj("MEAN_HOPS_INDEX", "Mean Hops Index");
	public final Obj MEDIAN_HOPS_INDEX = new Obj("MEDIAN_HOPS_INDEX", "Median Hops Index");
	public final Obj NUMBER_OF_HOPS = new Obj("NUMBER_OF_HOPS", "Total Number of Hops");
	public final Obj MAX_HOPS_INDEX = new Obj("MAX_HOPS_INDEX", "Max Hops Index");

	// Node resources:
	public final Obj[] TOTAL_USED_RESOURCES;

	// VNF instance resources:
	public final Obj MEAN_INVERSE_LOAD_INDEX = new Obj("MEAN_INVERSE_LOAD_INDEX", "Mean Inverse Load Index");
	public final Obj MEDIAN_INVERSE_LOAD_INDEX = new Obj("MEDIAN_INVERSE_LOAD_INDEX", "Median Inverse Load Index");
	public final Obj NUMBER_OF_VNF_INSTANCES = new Obj("NUMBER_OF_VNF_INSTANCES", "Number of VNF Instances");
	public final Obj TOTAL_ROOTED_VNF_LOADS = new Obj("TOTAL_ROOTED_VNF_LOADS", "Total Rooted VNF Loads");

	// Indicators of unfeasibility:
	// OVERLOADED refers to VNF instances on nodes with exhausted resources.
	// EXCESSIVE refers to VNF types with more instances than permitted.
	public final Obj NUMBER_OF_DELAY_VIOLATIONS = new Obj("NUMBER_OF_DELAY_VIOLATIONS", "Number of Delay Violations");
	public final Obj NUMBER_OF_RESOURCE_VIOLATIONS = new Obj("NUMBER_OF_RESOURCE_VIOLATIONS", "Number of Resource Violations");
	public final Obj NUMBER_OF_EXCESSIVE_VNFS = new Obj("NUMBER_OF_EXCESSIVE_VNFS", "Number of Excessive VNFs");
	public final Obj NUMBER_OF_CONGESTED_LINKS = new Obj("NUMBER_OF_CONGESTED_LINKS", "Number of Congested Links");
	public final Obj TOTAL_OVERLOADED_VNF_CAPACITY = new Obj("TOTAL_OVERLOADED_VNF_CAPACITY", "Total Overloaded VNF Capacity");
	public final Obj TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY = new Obj("TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY", "Total Rooted Excessive VNF Capacity");

	public Objs(String[] resources) {
		TOTAL_USED_RESOURCES = new Obj[resources.length];
		for (int i = 0; i < resources.length; i++) {
			TOTAL_USED_RESOURCES[i] = new Obj("TOTAL_USED_RESOURCE_" + resources[i].replace(" ", "_").toUpperCase(), "Total Used " + resources[i]);
		}
	}

	public class Obj {
		public final int i;
		public final String name;
		public final String s;

		private Obj(String name, String s) {
			this.name = name;
			this.s = s;
			this.i = nextOrdinal;
			nextOrdinal++;
			values.add(this);
		}

		public String toString() {
			return s;
		}
	}

	public Obj[] values() {
		return values.toArray(new Obj[values.size()]);
	}
}
