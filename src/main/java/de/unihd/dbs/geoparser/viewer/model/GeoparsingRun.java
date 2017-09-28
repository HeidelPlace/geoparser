package de.unihd.dbs.geoparser.viewer.model;

import com.google.common.base.Stopwatch;

public class GeoparsingRun {
	public final GeoparsingApproach geoparsingApproach;
	public final GeoparsingResult results;
	public GeoparsingStep lastStep;
	public Stopwatch stopwatch;

	public GeoparsingRun(final GeoparsingApproach methods, final GeoparsingResult results) {
		this.geoparsingApproach = methods;
		this.results = results;
		this.lastStep = GeoparsingStep.NONE;
		this.stopwatch = Stopwatch.createUnstarted();
	}

}