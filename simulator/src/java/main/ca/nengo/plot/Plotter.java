/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "Plotter.java". Description: 
"Factory for frequently-used plots"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU 
Public License license (the GPL License), in which case the provisions of GPL 
License are applicable  instead of those above. If you wish to allow use of your 
version of this file only under the terms of the GPL License and not to allow 
others to use your version of this file under the MPL, indicate your decision 
by deleting the provisions above and replace  them with the notice and other 
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on 15-Jun-2006
 */
package ca.nengo.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


//import ca.nengo.dynamics.Integrator;
//import ca.nengo.dynamics.impl.EulerIntegrator;
//import ca.nengo.dynamics.impl.LTISystem;
//import ca.nengo.dynamics.impl.SimpleLTISystem;
import ca.nengo.math.Function;
import ca.nengo.model.nef.NEFEnsemble;
import ca.nengo.plot.impl.DefaultPlotter;
import ca.nengo.util.DataUtils;
import ca.nengo.util.Environment;
import ca.nengo.util.SpikePattern;
import ca.nengo.util.TimeSeries;

/** 
 * Factory for frequently-used plots. 
 * 
 * @author Bryan Tripp
 */
public abstract class Plotter {

	private static Plotter ourInstance;

	private List<Object> myPlotFrames;

	public Plotter() {
		myPlotFrames = new ArrayList<Object>(10);
	}

	private synchronized static Plotter getInstance() {
		if (ourInstance == null) {
			//this can be made configurable if we get more plotters
			ourInstance = new DefaultPlotter();
		}

		return ourInstance;
	}

	
	/**
	 * Static convenience method for producing a TimeSeries plot.
	 *  
	 * @param series TimeSeries to plot
	 * @param title Plot title
	 */
	public static void plot(TimeSeries series, String title) {
		getInstance().doPlot(series, title);
	}
	
	/**
	 * As plot(TimeSeries) but series is filtered before plotting. This is useful when plotting 
	 * NEFEnsemble output (which may consist of spikes) in a manner more similar to the way it would 
	 * appear within post-synaptic neurons. 
	 * 
	 * @param series TimeSeries to plot
	 * @param tauFilter Time constant of display filter (s) 
	 * @param title Plot title
	 */
	public static void plot(TimeSeries series, float tauFilter, String title) {
		series = filter(series, tauFilter);
		getInstance().doPlot(series, title);
	}
	
	/**
	 * @param series A TimeSeries to which to apply a 1-D linear filter
	 * @param tauFilter Filter time constant
	 * @return Filtered TimeSeries
	 */
	public static TimeSeries filter(TimeSeries series, float tauFilter) {
		return DataUtils.filter(series, tauFilter);
	}

	/**
	 * Plots ideal and actual TimeSeries' together. 
	 *  
	 * @param ideal Ideal time series 
	 * @param actual Actual time series
	 * @param title Plot title
	 */
	public static void plot(TimeSeries ideal, TimeSeries actual, String title) {
		getInstance().doPlot(ideal, actual, title);
	}
	
	/**
	 * Plots multiple TimeSeries and/or SpikePatterns together in the same plot.
	 *   
	 * @param series A list of TimeSeries to plot (can be null if none)  
	 * @param patterns A list of SpikePatterns to plot (can be null if none)
	 * @param title Plot title
	 */
	public static void plot(List<TimeSeries> series, List<SpikePattern> patterns, String title) {
		getInstance().doPlot(series, patterns, title);
	}

	/**
	 * Plots ideal and actual TimeSeries' together, with each series filtered before plotting. 
	 * 
	 * @param ideal Ideal time series 
	 * @param actual Actual time series
	 * @param tauFilter Time constant of display filter (s) 
	 * @param title Plot title
	 */
	public static void plot(TimeSeries ideal, TimeSeries actual, float tauFilter, String title) {
		//ideal = filter(ideal, tauFilter);
		final TimeSeries filtActual = filter(actual, tauFilter);
		getInstance().doPlot(ideal, filtActual, title);
	}

	/**
	 * @param series TimeSeries to plot
	 * @param title Plot title
	 */
	public abstract void doPlot(TimeSeries series, String title);
	
	/**
	 * @param ideal Ideal time series 
	 * @param actual Actual time series 
	 * @param title Plot title
	 */
	public abstract void doPlot(TimeSeries ideal, TimeSeries actual, String title);
	
	/**
	 * @param series A list of TimeSeries to plot (can be null if none)  
	 * @param patterns A list of SpikePatterns to plot (can be null if none)
	 * @param title Plot title
	 */
	public abstract void doPlot(List<TimeSeries> series, List<SpikePattern> patterns, String title);
	
	/**
	 * Static convenience method for producing a decoding error plot of an NEFEnsemble origin. 
	 * 
	 * @param ensemble NEFEnsemble from which origin arises
	 * @param origin Name of origin (must be a DecodedOrigin, not one derived from a combination of 
	 * 		neuron origins)
	 */
	public static void plot(NEFEnsemble ensemble, String origin) {
		getInstance().doPlot(ensemble, origin);
	}
	
	/**
	 * @param ensemble NEFEnsemble from which origin arises
	 * @param origin Name of origin (must be a DecodedOrigin, not one derived from a combination of 
	 * 		neuron origins)
	 */
	public abstract void doPlot(NEFEnsemble ensemble, String origin);
	
	/**
	 * Static convenience method for producing a plot of CONSTANT_RATE responses over range 
	 * of inputs. 
	 *  
	 * @param ensemble An NEFEnsemble  
	 */
	public static void plot(NEFEnsemble ensemble) {
		getInstance().doPlot(ensemble);
	}
	
	/**
	 * @param ensemble An NEFEnsemble  
	 */
	public abstract void doPlot(NEFEnsemble ensemble);
	
	/**
	 * Static convenience method for plotting a spike raster. 
	 * 
	 * @param pattern SpikePattern to plot
	 */
	public static void plot(SpikePattern pattern) {
		getInstance().doPlot(pattern);
	}
	
	/**
	 * @param pattern A SpikePattern for which to plot a raster
	 */
	public abstract void doPlot(SpikePattern pattern);

	/**
	 * Static convenience method for plotting a Function. 
	 * 
	 * @param function Function to plot
	 * @param start Minimum of input range 
	 * @param increment Size of incrememnt along input range 
	 * @param end Maximum of input range
	 * @param title Display title of plot
	 */
	public static void plot(Function function, float start, float increment, float end, String title) {
		getInstance().doPlot(function, start, increment, end, title);
	}
	
	/**
	 * @param function Function to plot
	 * @param start Minimum of input range 
	 * @param increment Size of incrememnt along input range 
	 * @param end Maximum of input range
	 * @param title Display title of plot
	 */
	public abstract void doPlot(Function function, float start, float increment, float end, String title);
	
	/**
	 * Static convenience method for plotting a vector. 
	 * 
	 * @param vector Vector of points to plot
	 * @param title Display title of plot
	 */
	public static void plot(float[] vector, String title) {
		getInstance().doPlot(vector, title);
	}
	
	/**
	 * @param vector Vector of points to plot
	 * @param title Display title of plot
	 */
	public abstract void doPlot(float[] vector, String title);
	
	/**
	 * Static convenience method for plotting a vector. 
	 *
	 * @param domain Vector of domain values 
	 * @param vector Vector of range values
	 * @param title Display title of plot
	 */
	public static void plot(float[] domain, float[] vector, String title) {
		getInstance().doPlot(domain, vector, title);
	}
	
	/**
	 * @param domain Vector of domain values 
	 * @param vector Vector of range values
	 * @param title Display title of plot
	 */
	public abstract void doPlot(float[] domain, float[] vector, String title);
	
}
