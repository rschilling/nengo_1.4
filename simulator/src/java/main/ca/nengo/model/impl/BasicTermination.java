/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "BasicTermination.java". Description:
"A basic implementation of Termination with configurable dynamics and no special
  integrative features.

  @author Bryan Tripp"

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
 * Created on 3-Apr-07
 */
package ca.nengo.model.impl;

import android.util.Log;
import ca.nengo.dynamics.DynamicalSystem;
import ca.nengo.dynamics.Integrator;
import ca.nengo.dynamics.impl.CanonicalModel;
import ca.nengo.dynamics.impl.LTISystem;
import ca.nengo.model.InstantaneousOutput;
import ca.nengo.model.Node;
import ca.nengo.model.RealOutput;
import ca.nengo.model.Resettable;
import ca.nengo.model.SimulationException;
import ca.nengo.model.SpikeOutput;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.impl.TimeSeriesImpl;

/**
 * A basic implementation of Termination with configurable dynamics and no special
 * integrative features.
 *
 * @author Bryan Tripp
 */
public class BasicTermination implements Termination, Resettable {

	private static final long serialVersionUID = 1L;

	private Node myNode;
	private DynamicalSystem myDynamics;
	private Integrator myIntegrator;
	private String myName;
	private InstantaneousOutput myInput;
	private TimeSeries myOutput;
	private boolean myModulatory;

	/**
	 * @param node Node that owns this termination
	 * @param dynamics Dynamical System that defines the dynamics
	 * @param integrator Integrator for the DS
	 * @param name Name of the termination
	 */
	public BasicTermination(Node node, DynamicalSystem dynamics, Integrator integrator, String name) {
		myNode = node;
		myDynamics = dynamics;
		myIntegrator = integrator;
		myName = name;
		myModulatory = false;
	}

	/**
	 * @see ca.nengo.model.Termination#getDimensions()
	 */
	public int getDimensions() {
		return myDynamics.getInputDimension();
	}

	/**
	 * @see ca.nengo.model.Termination#getName()
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @see ca.nengo.model.Termination#setValues(ca.nengo.model.InstantaneousOutput)
	 */
	public void setValues(InstantaneousOutput values) throws SimulationException {
		myInput = values;
	}

	/**
	 * Runs the Termination, making a TimeSeries of output from this Termination
	 * available from getOutput().
	 *
	 * @param startTime simulation time at which running starts (s)
	 * @param endTime simulation time at which running ends (s)
	 * @throws SimulationException if a problem is encountered while trying to run
	 */
	public void run(float startTime, float endTime) throws SimulationException {
		float[] input = null;
		if (myInput instanceof RealOutput) {
			input = ((RealOutput) myInput).getValues();
		} else if (myInput instanceof SpikeOutput) {
			boolean[] spikes = ((SpikeOutput) myInput).getValues();
			input = new float[spikes.length];
			float amplitude = 1f / (endTime - startTime);
			for (int i = 0; i < spikes.length; i++) {
				if (spikes[i]) {
                    input[i] = amplitude;
                }
			}
		}

		TimeSeries inSeries = new TimeSeriesImpl(new float[]{startTime, endTime}, new float[][]{input, input}, Units.uniform(Units.UNK, input.length));
		myOutput = myIntegrator.integrate(myDynamics, inSeries);
	}

	/**
	 * Note: typically called by the Node to which the Termination belongs.
	 *
	 * @return The most recent input multiplied
	 */
	public TimeSeries getOutput() {
		return myOutput;
	}

	/**
	 * @see ca.nengo.model.Termination#getNode()
	 */
	public Node getNode() {
		return myNode;
	}

	/**
	 * @see ca.nengo.model.Resettable#reset(boolean)
	 */
	public void reset(boolean randomize) {
		myInput = null;
	}

	/**
	 * @see ca.nengo.model.Termination#getModulatory()
	 */
	public boolean getModulatory() {
		return myModulatory;
	}

	/**
	 * @see ca.nengo.model.Termination#getTau()
	 */
	public float getTau() {
		if (myDynamics instanceof LTISystem) {
			return CanonicalModel.getDominantTimeConstant((LTISystem) myDynamics);
		} else {
			Log.w("Termination", "Can't get time constant for non-LTI dynamics. Returning 0.");
			return 0;
		}
	}

	/**
	 * @see ca.nengo.model.Termination#setModulatory(boolean)
	 */
	public void setModulatory(boolean modulatory) {
		myModulatory = modulatory;
	}

	/**
	 * @see ca.nengo.model.Termination#setTau(float)
	 */
	public void setTau(float tau) throws StructuralException {
		if (myDynamics instanceof LTISystem) {
			CanonicalModel.changeTimeConstant((LTISystem) myDynamics, tau);
		} else {
			throw new StructuralException("Can't set time constant of non-LTI dynamics");
		}
	}
	
	/**
	 * @return Extract the input to the termination.
	 */
	public InstantaneousOutput getInput(){
		return myInput;
	}

	@Override
	public BasicTermination clone() throws CloneNotSupportedException {
		return this.clone(myNode);
	}
	
	public BasicTermination clone(Node node) throws CloneNotSupportedException {
		BasicTermination result = (BasicTermination) super.clone();
		result.myNode = node;
		result.myDynamics = myDynamics.clone();
		result.myIntegrator = myIntegrator.clone();
		result.myInput = myInput.clone();
		result.myOutput = myOutput.clone();
		return result;
	}

}
