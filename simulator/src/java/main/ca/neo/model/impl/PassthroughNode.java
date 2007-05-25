/*
 * Created on 24-May-07
 */
package ca.neo.model.impl;

import ca.neo.model.InstantaneousOutput;
import ca.neo.model.Node;
import ca.neo.model.Origin;
import ca.neo.model.SimulationException;
import ca.neo.model.SimulationMode;
import ca.neo.model.StructuralException;
import ca.neo.model.Termination;
import ca.neo.model.Units;
import ca.neo.util.Configuration;
import ca.neo.util.impl.ConfigurationImpl;

/**
 * <p>A Node that passes values through unaltered.</p>
 * 
 * <p>This can be useful if an input to a Network is actually routed to multiple destinations, 
 * but you want to handle this connectivity within the Network rather than expose multiple 
 * terminations.</p>  
 * 
 * TODO: unit tests
 * 
 * @author Bryan Tripp
 */
public class PassthroughNode implements Node {

	public static final String TERMINATION = "termination";
	public static final String ORIGIN = "origin";
	
	private static final long serialVersionUID = 1L;
	
	private String myName;
	private PassthroughTermination myTermination;
	private PassthroughOrigin myOrigin;

	public PassthroughNode(String name, int dimension) {
		myName = name;
		myTermination = new PassthroughTermination(dimension);
		myOrigin = new PassthroughOrigin(dimension);
		reset(false);
	}
	
	/**
	 * @see ca.neo.model.Node#getName()
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @see ca.neo.model.Node#getOrigin(java.lang.String)
	 */
	public Origin getOrigin(String name) throws StructuralException {
		if (ORIGIN.equals(name)) {
			return myOrigin;
		} else {
			throw new StructuralException("Unknown origin: " + name);
		}
	}

	/**
	 * @see ca.neo.model.Node#getOrigins()
	 */
	public Origin[] getOrigins() {
		return new Origin[]{myOrigin};
	}

	/**
	 * @see ca.neo.model.Node#getTermination(java.lang.String)
	 */
	public Termination getTermination(String name) throws StructuralException {
		if (TERMINATION.equals(name)) {
			return myTermination;
		} else {
			throw new StructuralException("Unknown termination: " + name);
		}
	}

	/**
	 * @see ca.neo.model.Node#getTerminations()
	 */
	public Termination[] getTerminations() {
		return new Termination[]{myTermination};
	}

	/**
	 * @see ca.neo.model.Node#run(float, float)
	 */
	public void run(float startTime, float endTime) throws SimulationException {
		myOrigin.setValues(myTermination.getValues());
	}

	/**
	 * @see ca.neo.model.Resettable#reset(boolean)
	 */
	public void reset(boolean randomize) {
		myOrigin.setValues(new RealOutputImpl(new float[myOrigin.getDimensions()], Units.UNK));
	}

	/**
	 * @see ca.neo.model.SimulationMode.ModeConfigurable#getMode()
	 */
	public SimulationMode getMode() {
		return SimulationMode.DEFAULT;
	}

	/**
	 * Does nothing (only DEFAULT mode is supported). 
	 * 
	 * @see ca.neo.model.SimulationMode.ModeConfigurable#setMode(ca.neo.model.SimulationMode)
	 */
	public void setMode(SimulationMode mode) {
	}
	
	private static class PassthroughOrigin implements Origin {
		
		private static final long serialVersionUID = 1L;
		
		private int myDimension;
		private InstantaneousOutput myValues;
		
		public PassthroughOrigin(int dimension) {
			myDimension = dimension;
		}

		public int getDimensions() {
			return myDimension;
		}

		public String getName() {
			return ORIGIN;
		}

		public InstantaneousOutput getValues() throws SimulationException {
			return myValues;
		}
		
		public void setValues(InstantaneousOutput values) {
			myValues = values;
		}
		
	}
	
	private static class PassthroughTermination implements Termination {
		
		private static final long serialVersionUID = 1L;
		
		private int myDimension;
		private Configuration myConfiguration;
		private InstantaneousOutput myValues;
		
		public PassthroughTermination(int dimension) {
			myDimension = dimension;
			myConfiguration = new ConfigurationImpl(this);
		}

		public int getDimensions() {
			return myDimension;
		}

		public String getName() {
			return TERMINATION;
		}

		public void setValues(InstantaneousOutput values) throws SimulationException {
			myValues = values;
		}
		
		public InstantaneousOutput getValues() {
			return myValues;
		}

		public Configuration getConfiguration() {
			return myConfiguration;
		}

		public void propertyChange(String propertyName, Object newValue) throws StructuralException {
		}
		
	}

}