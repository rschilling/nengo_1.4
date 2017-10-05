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

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



import ca.nengo.math.Function;
import ca.nengo.model.nef.NEFEnsemble;
import ca.nengo.util.DataUtils;
import ca.nengo.util.SpikePattern;
import ca.nengo.util.TimeSeries;

/** 
 * Factory for frequently-used plots. 
 * 
 * @author Bryan Tripp
 */
public abstract class Plotter {

	/**
	 * @param series A TimeSeries to which to apply a 1-D linear filter
	 * @param tauFilter Filter time constant
	 * @return Filtered TimeSeries
	 */
	public static TimeSeries filter(Context ctx, TimeSeries series, float tauFilter) {
		return DataUtils.filter(series, tauFilter);
	}

	
}
