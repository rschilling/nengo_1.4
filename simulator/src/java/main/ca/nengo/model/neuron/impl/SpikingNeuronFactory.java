/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "SpikingNeuronFactory.java". Description: 
"Creates spiking neurons by delegating to a SynapticIntegratorFactory and a 
  SpikeGeneratorFactory"

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

/**
 * 
 */
package ca.nengo.model.neuron.impl;

import java.awt.Frame;

import ca.nengo.config.ConfigUtil;
import ca.nengo.math.PDF;
import ca.nengo.math.impl.IndicatorPDF;
import ca.nengo.model.ExpandableNode;
import ca.nengo.model.Node;
import ca.nengo.model.StructuralException;
import ca.nengo.model.impl.NodeFactory;
import ca.nengo.model.neuron.SpikeGenerator;
import ca.nengo.model.neuron.SynapticIntegrator;

/**
 * Creates spiking neurons by delegating to a SynapticIntegratorFactory and a 
 * SpikeGeneratorFactory. 
 * 
 * @author Bryan Tripp
 */
public class SpikingNeuronFactory implements NodeFactory {

	private static final long serialVersionUID = 1L;
	
	private SynapticIntegratorFactory myIntegratorFactory;
	private SpikeGeneratorFactory myGeneratorFactory;
	private PDF myScale;
	private PDF myBias;
	
	public SpikingNeuronFactory(SynapticIntegratorFactory intFact, SpikeGeneratorFactory genFact, PDF scale, PDF bias) {
		myIntegratorFactory = intFact;
		myGeneratorFactory = genFact;
		myScale = scale;
		myBias = bias;
	}
	
	public SynapticIntegratorFactory getIntegratorFactory() {
		return myIntegratorFactory;
	}
	
	public void setIntegratorFactory(SynapticIntegratorFactory factory) {
		myIntegratorFactory = factory;
	}
	
	public SpikeGeneratorFactory getGeneratorFactory() {
		return myGeneratorFactory;
	}
	
	public void setGeneratorFactory(SpikeGeneratorFactory factory) {
		myGeneratorFactory = factory;
	}
	
	/**
	 * @see ca.nengo.model.impl.NodeFactory#getTypeDescription()
	 */
	public String getTypeDescription() {
		return "Customizable Neuron";
	}

	/**
	 * @see ca.nengo.model.impl.NodeFactory#make(java.lang.String)
	 */
	public Node make(String name) throws StructuralException {
		SynapticIntegrator integrator = myIntegratorFactory.make();
		SpikeGenerator generator = myGeneratorFactory.make();
		float scale = myScale.sample()[0];
		float bias = myBias.sample()[0];
		
		Node result = null;
		
		if (integrator instanceof ExpandableNode) {
			result = new ExpandableSpikingNeuron(integrator, generator, scale, bias, name);
		} else {
			result = new SpikingNeuron(integrator, generator, scale, bias, name);
		}
		
		return result;		
	}
	
	public static void main(String[] args) {
		SpikingNeuronFactory factory = new SpikingNeuronFactory(
				new LinearSynapticIntegrator.Factory(), 
				new LIFSpikeGenerator.Factory(), 
				new IndicatorPDF(1), 
				new IndicatorPDF(0));
		
		ConfigUtil.configure((Frame) null, factory);
	}

}
