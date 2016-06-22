/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "NetworkImpl.java". Description:
"Default implementation of Network"

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
 * Created on 23-May-2006
 */
package ca.nengo.model.impl;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import ca.nengo.model.Ensemble;
import ca.nengo.model.InstantaneousOutput;
import ca.nengo.model.Network;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.Probeable;
import ca.nengo.model.Projection;
import ca.nengo.model.SimulationException;
import ca.nengo.model.SimulationMode;
import ca.nengo.model.StepListener;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.nef.impl.DecodableEnsembleImpl;
import ca.nengo.model.nef.impl.NEFEnsembleImpl;
import ca.nengo.model.neuron.Neuron;
import ca.nengo.sim.Simulator;
import ca.nengo.sim.impl.LocalSimulator;
import ca.nengo.util.Probe;
import ca.nengo.util.ScriptGenException;
import ca.nengo.util.TaskSpawner;
import ca.nengo.util.ThreadTask;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.VisiblyMutable;
import ca.nengo.util.VisiblyMutableUtils;
import ca.nengo.util.impl.ProbeTask;
import ca.nengo.util.impl.ScriptGenerator;

/**
 * Default implementation of Network.
 *
 * @author Bryan Tripp
 */
public class NetworkImpl implements Network, VisiblyMutable, VisiblyMutable.Listener, TaskSpawner {

	/**
	 * Default name for a Network
	 */
	public static final String DEFAULT_NAME = "Network";

	private static final long serialVersionUID = 1L;

	private Map<String, Node> myNodeMap; //keyed on name
	private Map<Termination, Projection> myProjectionMap; //keyed on Termination
	private String myName;
	private SimulationMode myMode;
	private List<SimulationMode> myFixedModes;
	private Simulator mySimulator;
	private float myStepSize;
	private Map<String, Probeable> myProbeables;
	private Map<String, String> myProbeableStates;
	private Map<String, Origin> myExposedOrigins;
	private Map<String, Termination> myExposedTerminations;

	private LinkedList <Origin> OrderedExposedOrigins;
	private LinkedList <Termination> OrderedExposedTerminations;

	private String myDocumentation;
	private Map<String, Object> myMetaData;

	private Map<Origin, String> myExposedOriginNames;
	private Map<Termination, String> myExposedTerminationNames;

	private transient List<VisiblyMutable.Listener> myListeners;

	protected int myNumGPU = 0;
	protected int myNumJavaThreads = 1;
	protected boolean myUseGPU = true;

    private transient Collection<StepListener> myStepListeners;

	private transient Map<String, Object> myMetadata;
	
    

	/**
	 * Sets up a network's data structures
	 */
	public NetworkImpl() {
		myNodeMap = new HashMap<String, Node>(20);
		myProjectionMap	= new HashMap<Termination, Projection>(50);
		myName = DEFAULT_NAME;
		myStepSize = .001f;
		myProbeables = new HashMap<String, Probeable>(30);
		myProbeableStates = new HashMap<String, String>(30);
		myExposedOrigins = new HashMap<String, Origin>(10);
		myExposedOriginNames = new HashMap<Origin, String>(10);
		myExposedTerminations = new HashMap<String, Termination>(10);
		myExposedTerminationNames = new HashMap<Termination, String>(10);
		myMode = SimulationMode.DEFAULT;
		myFixedModes = null;
		myMetaData = new HashMap<String, Object>(20);
		myListeners = new ArrayList<Listener>(10);

		OrderedExposedOrigins = new LinkedList <Origin> ();
		OrderedExposedTerminations = new LinkedList <Termination> ();
		
		myStepListeners = new ArrayList<StepListener>(1);		
	}

	/**
	 * @param simulator Simulator with which to run this Network
	 */
	public void setSimulator(Simulator simulator) {
		mySimulator = simulator;
		mySimulator.initialize(this);
	}

	/**
	 * @return Simulator used to run this Network (a LocalSimulator by default)
	 */
	public Simulator getSimulator() {
		if (mySimulator == null) {
			mySimulator = new LocalSimulator();
			mySimulator.initialize(this);
		}
		return mySimulator;
	}

	/**
	 * @param stepSize New timestep size at which to simulate Network (some components of the network
	 * 		may run with different step sizes, but information is exchanged between components with
	 * 		this step size). Defaults to 0.001s.
	 */
	public void setStepSize(float stepSize) {
		myStepSize = stepSize;
	}

	/**
	 * @return Timestep size at which Network is simulated.
	 */
	public float getStepSize() {
		return myStepSize;
	}

	/**
	 * @param time The current simulation time. Sets the current time on the Network's subnodes.
   * (Mainly for NEFEnsembles).
	 */
	public void setTime(float time) {
			Node[] nodes = getNodes();
			
			for(int i = 0; i < nodes.length; i++){
				Node workingNode = nodes[i];
				
				if(workingNode instanceof DecodableEnsembleImpl){
					((DecodableEnsembleImpl) workingNode).setTime(time);
				}else if(workingNode instanceof NetworkImpl){
					((NetworkImpl) workingNode).setTime(time);
				}
			}
	}
	
	/**
	 * @see ca.nengo.model.Network#addNode(ca.nengo.model.Node)
	 */
	public void addNode(Node node) throws StructuralException {
		if (myNodeMap.containsKey(node.getName())) {
			throw new StructuralException("This Network already contains a Node named " + node.getName());
		}

		myNodeMap.put(node.getName(), node);
		node.addChangeListener(this);

		getSimulator().initialize(this);
		fireVisibleChangeEvent();
	}

	/**
	 * Counts how many neurons are contained within this network.
	 * 
	 * @return number of neurons in this network
	 */
	public int countNeurons()
	{
		Node[] myNodes = getNodes();
		int count = 0;
		for(Node node : myNodes)
		{
			if(node instanceof NetworkImpl)
				count += ((NetworkImpl)node).countNeurons();
			else if(node instanceof Ensemble)
				count += ((Ensemble)node).getNodes().length;
			else if(node instanceof Neuron)
				count += 1;
		}
		
		return count;
	}
	
	/***
	 * Kills a certain percentage of neurons in the network (recursively including subnetworks).
	 *
	 * @param killrate the percentage (0.0 to 1.0) of neurons to kill
	 */
	public void killNeurons(float killrate)
	{
		killNeurons(killrate, false);
	}

	/***
	 * Kills a certain percentage of neurons in the network (recursively including subnetworks).
	 *
	 * @param killrate the percentage (0.0 to 1.0) of neurons to kill
	 * @param saveRelays if true, exempt populations with only one node from the slaughter
	 */
	public void killNeurons(float killrate, boolean saveRelays)
	{
		Node[] nodes = getNodes();
		for (Node node : nodes) {
			if(node instanceof NetworkImpl) {
                ((NetworkImpl)node).killNeurons(killrate, saveRelays);
            } else if(node instanceof NEFEnsembleImpl) {
                ((NEFEnsembleImpl)node).killNeurons(killrate, saveRelays);
            }
		}

	}

	/**
	 * Kills a certain percentage of the dendritic inputs in the network (recursively including subnetworks).
	 *
	 * @param killrate the percentage (0.0 to 1.0) of dendritic inputs to kill
	 */
//	public void killDendrites(float killrate)
//	{
//		Node[] nodes = getNodes();
//		for(int i = 0; i < nodes.length; i++)
//		{
//			if(nodes[i] instanceof NetworkImpl)
//				((NetworkImpl)nodes[i]).killDendrites(killrate);
//			else if(nodes[i] instanceof NEFEnsembleImpl)
//				((NEFEnsembleImpl)nodes[i]).killDendrites(killrate);
//		}
//
//	}

	/**
	 * Handles any changes/errors that may arise from objects within the network changing.
	 *
	 * @see ca.nengo.util.VisiblyMutable.Listener#changed(ca.nengo.util.VisiblyMutable.Event)
	 */
	public void changed(Event e) throws StructuralException {
		if (e instanceof VisiblyMutable.NameChangeEvent) {
			VisiblyMutable.NameChangeEvent ne = (VisiblyMutable.NameChangeEvent) e;

			if (myNodeMap.containsKey(ne.getNewName()) && !ne.getNewName().equals(ne.getOldName())) {
				throw new StructuralException("This Network already contains a Node named " + ne.getNewName());
			}

			/*
			 * Only do the swap if the name has changed.
			 * Otherwise, the node will be dereferenced from the map.
			 * 
			 * Also only do the swap if the node being changed is already in myNodeMap.
			 */
			if (!ne.getOldName().equals(ne.getNewName()) && ((Node)ne.getObject() == getNode(ne.getOldName()))) {
				myNodeMap.put(ne.getNewName(), (Node)ne.getObject());
				myNodeMap.remove(ne.getOldName());
			}
		}
		
		fireVisibleChangeEvent();
	}

	/**
	 * Gathers all the terminations of nodes contained in this network.
	 *
	 * @return arraylist of terminations
	 */
	public ArrayList<Termination> getNodeTerminations()
	{
		ArrayList<Termination> nodeTerminations = new ArrayList<Termination>();
		Node[] nodes = getNodes();
		for (Node node : nodes) {
			Termination[] terms = node.getTerminations();
			for (Termination term : terms) {
                nodeTerminations.add(term);
            }
		}

		return nodeTerminations;
	}

	/**
	 * Gathers all the origins of nodes contained in this network.
	 *
	 * @return arraylist of origins
	 */
	public ArrayList<Origin> getNodeOrigins()
	{
		ArrayList<Origin> nodeOrigins = new ArrayList<Origin>();
		Node[] nodes = getNodes();
		for (Node node : nodes) {
			Origin[] origs = node.getOrigins();
			for (Origin orig : origs) {
                nodeOrigins.add(orig);
            }
		}

		return nodeOrigins;
	}

	/**
	 * @see ca.nengo.model.Network#getNodes()
	 */
	public Node[] getNodes() {
		return myNodeMap.values().toArray(new Node[0]);
	}

	/**
	 * @see ca.nengo.model.Network#getNode(java.lang.String)
	 */
	public Node getNode(String name) throws StructuralException {
		if (!myNodeMap.containsKey(name)) {
			throw new StructuralException("No Node named " + name + " in this Network");
		}
		return myNodeMap.get(name);
	}

	/**
	 * @return number of top-level nodes
	 */
	public int getNodeCount(){
		return getNodes().length;
	}

	/**
	 * @return number of neurons in all levels
	 */
	public int getNeuronCount(){
		int neuron_count = 0;
		Node[] nodes = getNodes();

		for (Node node : nodes) {
			if(node instanceof NetworkImpl) {
                neuron_count += ((NetworkImpl)node).getNeuronCount();
            } else if(node instanceof NEFEnsembleImpl) {
                neuron_count += ((NEFEnsembleImpl)node).getNeuronCount();
            }
		}

		return neuron_count;
	}

	/**
	 * @see ca.nengo.model.Network#removeNode(java.lang.String)
	 */
	public void removeNode(String name) throws StructuralException {
		if (myNodeMap.containsKey(name)) {
			Node node = myNodeMap.get(name);

			if(node instanceof Network)
			{
				Network net = (Network)node;
				Probe[] probes = net.getSimulator().getProbes();
				for (Probe probe : probes) {
                    try
					{
						net.getSimulator().removeProbe(probe);
					}
					catch(SimulationException se)
					{
						System.err.println(se);
						return;
					}
                }

				Node[] nodes = net.getNodes();
				for (Node node2 : nodes) {
                    net.removeNode(node2.getName());
                }
			}
			else if(node instanceof DecodableEnsembleImpl)
			{
				NEFEnsembleImpl pop = (NEFEnsembleImpl)node;
				Origin[] origins = pop.getOrigins();
				for (Origin origin : origins) {
					String exposedName = getExposedOriginName(origin);
					if(exposedName != null) {
                        hideOrigin(exposedName);
                    }
				}
			}
			else if(node instanceof SocketUDPNode)
			{
				// If the node to be removed is a SocketUDPNode, make sure to close down the socket
				// before removing it.
				((SocketUDPNode) node).close();
			}

			myNodeMap.remove(name);
			node.removeChangeListener(this);
//			VisiblyMutableUtils.nodeRemoved(this, node, myListeners);
			
			getSimulator().initialize(this);
			fireVisibleChangeEvent();
		} else {
			throw new StructuralException("No Node named " + name + " in this Network");
		}
	}

	/**
	 * @see ca.nengo.model.Network#addProjection(ca.nengo.model.Origin, ca.nengo.model.Termination)
	 */
	public Projection addProjection(Origin origin, Termination termination) throws StructuralException {
		if (myProjectionMap.containsKey(termination)) {
			throw new StructuralException("There is already an Origin connected to the specified Termination");
		} else if (origin.getDimensions() != termination.getDimensions()) {
			throw new StructuralException("Can't connect Origin of dimension " + origin.getDimensions()
					+ " to Termination of dimension " + termination.getDimensions());
		} else {
			Projection result = new ProjectionImpl(origin, termination, this);
			myProjectionMap.put(termination, result);
			getSimulator().initialize(this);
			fireVisibleChangeEvent();
	
			return result;
		}
	}

	/**
	 * @see ca.nengo.model.Network#getProjections()
	 */
	public Projection[] getProjections() {
		return myProjectionMap.values().toArray(new Projection[0]);
	}
	
	public Map<Termination, Projection> getProjectionMap() {
		return myProjectionMap;
	}

	/**
	 * @see ca.nengo.model.Network#removeProjection(ca.nengo.model.Termination)
	 */
	public void removeProjection(Termination termination) throws StructuralException {
		if (myProjectionMap.containsKey(termination)) {
			Projection p = myProjectionMap.get(termination);
			p.getTermination().reset(false);
			
			myProjectionMap.remove(termination);
		} else {
			throw new StructuralException("The Network contains no Projection ending on the specified Termination");
		}

		getSimulator().initialize(this);
		fireVisibleChangeEvent();
	}

	/**
	 * @see ca.nengo.model.Node#getName()
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @param name New name of Network (must be unique within any networks of which this one
	 * 		will be a part)
	 */
	public void setName(String name) throws StructuralException {
		if (!myName.equals(name)) {
			myName = name;
			VisiblyMutableUtils.nameChanged(this, getName(), name, myListeners);
		}
	}


	/**
	 * @see ca.nengo.model.Node#setMode(ca.nengo.model.SimulationMode)
	 */
	public void setMode(SimulationMode mode) {
		if(myFixedModes != null && !myFixedModes.contains(mode))
			return;
		myMode = mode;

		Iterator<Node> it = myNodeMap.values().iterator();
		while (it.hasNext()) {
			Node node = it.next();
            node.setMode(mode);
		}
	}

	/**
	 * Used to just change the mode of this network (without recursively
	 * changing the mode of nodes in the network)
	 */
	protected void setMyMode(SimulationMode mode) {
		if(myFixedModes == null || myFixedModes.contains(mode)) {
            myMode = mode;
        }
	}

	/**
	 * Fix the simulation mode to the current mode.
	 */
	public void fixMode() {
		fixMode(new SimulationMode[]{getMode()});
	}
	
	/**
	 * Set the allowed simulation modes.
	 */
	public void fixMode(SimulationMode[] modes) {
		myFixedModes = Arrays.asList(modes);
	}
	
	/**
	 * @see ca.nengo.model.Node#getMode()
	 */
	public SimulationMode getMode() {
		return myMode;
	}

	/**
	 * @see ca.nengo.model.Node#run(float, float)
	 */
	public void run(float startTime, float endTime) throws SimulationException {
		getSimulator().run(startTime, endTime, myStepSize);
	}

	/**
	 * Runs the model with the optional parameter topLevel.
	 *
     * @param startTime simulation time at which running starts (s)
     * @param endTime simulation time at which running ends (s)
	 * @param topLevel true if the network being run is the top level network, false if it is a subnetwork
	 * @throws SimulationException if there's an error in the simulation
	 */
	public void run(float startTime, float endTime, boolean topLevel) throws SimulationException
	{
		getSimulator().run(startTime, endTime, myStepSize, topLevel);
	}

	/**
	 * @see ca.nengo.model.Resettable#reset(boolean)
	 */
	public void reset(boolean randomize) {
		Iterator<String> it = myNodeMap.keySet().iterator();
		while (it.hasNext()) {
			Node n = myNodeMap.get(it.next());
			n.reset(randomize);
		}
	}

	/**
	 * @param use Use GPU?
	 */
	public void setUseGPU(boolean use)
	{
		//myUseGPU = use;
		
		Node[] nodes = getNodes();

		for (Node workingNode : nodes) {
			if(workingNode instanceof NEFEnsembleImpl) {
				((NEFEnsembleImpl) workingNode).setUseGPU(use);
			} else if(workingNode instanceof NetworkImpl) {
				((NetworkImpl) workingNode).setUseGPU(use);
			}
		}
	}

	/**
	 * @return Using GPU?
	 */
	public boolean getUseGPU(){
		Node[] nodes = getNodes();

		for (Node workingNode : nodes) {
			if(workingNode instanceof NEFEnsembleImpl) {
				if(!((NEFEnsembleImpl) workingNode).getUseGPU()){
					return false;
				}
			} else if(workingNode instanceof NetworkImpl) {
				if(!((NetworkImpl) workingNode).getUseGPU()){
					return false;
				}
			}
		}
		
		//return myMode == SimulationMode.DEFAULT || myMode == SimulationMode.RATE;
		return true;
	}

	/**
	 * @see ca.nengo.model.Probeable#getHistory(java.lang.String)
	 */
	public TimeSeries getHistory(String stateName) throws SimulationException {
		Probeable p = myProbeables.get(stateName);
		String n = myProbeableStates.get(stateName);

		return p.getHistory(n);
	}

	/**
	 * @see ca.nengo.model.Probeable#listStates()
	 */
	public Properties listStates() {
		Properties result = new Properties();

		Iterator<String> it = myProbeables.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			Probeable p = myProbeables.get(key);
			String n = myProbeableStates.get(key);
			result.put(key, p.listStates().getProperty(n));
		}

		return result;
	}
	
	/**
	 * @see ca.nengo.model.Network#exposeOrigin(ca.nengo.model.Origin,
	 *      java.lang.String)
	 */
	public void exposeOrigin(Origin origin, String name) {
		Origin temp;

		temp = new OriginWrapper(this, origin, name);

		myExposedOrigins.put(name, temp );
		myExposedOriginNames.put(origin, name);
		OrderedExposedOrigins.add(temp);

		// automatically add exposed origin to exposed states
		if (origin.getNode() instanceof Probeable) {
			Probeable p=(Probeable)(origin.getNode());
			try {
				exposeState(p,origin.getName(),name);
			} catch (StructuralException e) {
			}
		}
		fireVisibleChangeEvent();
	}

	/**
	 * @see ca.nengo.model.Network#hideOrigin(java.lang.String)
	 */
	public void hideOrigin(String name) throws StructuralException {
		if(myExposedOrigins.get(name) == null) {
            throw new StructuralException("No origin named " + name + " exists");
        }

		OrderedExposedOrigins.remove(myExposedOrigins.get(name));
		OriginWrapper originWr = (OriginWrapper)myExposedOrigins.remove(name);


		if (originWr != null) {
			myExposedOriginNames.remove(originWr.myWrapped);


			// remove the automatically exposed state
			if (originWr.myWrapped.getNode() instanceof Probeable) {
				this.hideState(name);
			}
		}

		fireVisibleChangeEvent();
	}

	/**
	 * @see ca.nengo.model.Network#getExposedOriginName(ca.nengo.model.Origin)
	 */
	public String getExposedOriginName(Origin insideOrigin) {
		return myExposedOriginNames.get(insideOrigin);
	}

	/**
	 * @see ca.nengo.model.Network#getOrigin(java.lang.String)
	 */
	public Origin getOrigin(String name) throws StructuralException {
		if ( !myExposedOrigins.containsKey(name) ) {
			throw new StructuralException("There is no exposed Origin named " + name);
		}
		return myExposedOrigins.get(name);
	}

	/**
	 * @see ca.nengo.model.Network#getOrigins()
	 */
	public Origin[] getOrigins() {
		if (myExposedOrigins.values().size() == 0) {
            return myExposedOrigins.values().toArray(new Origin[0]);
        }
		return OrderedExposedOrigins.toArray(new Origin [0]);
	}

	/**
	 * @see ca.nengo.model.Network#exposeTermination(ca.nengo.model.Termination, java.lang.String)
	 */
	public void exposeTermination(Termination termination, String name) {
		Termination term;
		
			term = new TerminationWrapper(this, termination, name);
			
			myExposedTerminations.put(name, term);
			myExposedTerminationNames.put(termination, name);
			OrderedExposedTerminations.add(term);
		
		fireVisibleChangeEvent();
	}

	/**
	 * @see ca.nengo.model.Network#hideTermination(java.lang.String)
	 */
	public void hideTermination(String name) {
		Termination term = myExposedTerminations.get(name);
		
		if(term == null) return;
		
		OrderedExposedTerminations.remove(term);
		TerminationWrapper termination = (TerminationWrapper)myExposedTerminations.remove(name);
		if (termination != null) {
			myExposedTerminationNames.remove(termination.myWrapped);
		}
		fireVisibleChangeEvent();
	}

	/**
	 * @see ca.nengo.model.Network#getExposedTerminationName(ca.nengo.model.Termination)
	 */
	public String getExposedTerminationName(Termination insideTermination) {
		return myExposedTerminationNames.get(insideTermination);
	}

	/**
	 * @see ca.nengo.model.Network#getTermination(java.lang.String)
	 */
	public Termination getTermination(String name) throws StructuralException {
		if ( !myExposedTerminations.containsKey(name) ) {
			throw new StructuralException("There is no exposed Termination named " + name);
		}
		return myExposedTerminations.get(name);
	}

	/**
	 * @see ca.nengo.model.Network#getTerminations()
	 */
	public Termination[] getTerminations() {
		if (myExposedTerminations.values().size() == 0) {
            return myExposedTerminations.values().toArray(new Termination[0]);
        }
		return OrderedExposedTerminations.toArray(new Termination[0]);
	}

	/**
	 * @see ca.nengo.model.Network#exposeState(ca.nengo.model.Probeable, java.lang.String, java.lang.String)
	 */
	public void exposeState(Probeable probeable, String stateName, String name) throws StructuralException {
		if (probeable.listStates().get(stateName) == null) {
			throw new StructuralException("The state " + stateName + " does not exist");
		}

		myProbeables.put(name, probeable);
		myProbeableStates.put(name, stateName);
	}

	/**
	 * @see ca.nengo.model.Network#hideState(java.lang.String)
	 */
	public void hideState(String name) {
		myProbeables.remove(name);
		myProbeableStates.remove(name);
	}
	
	/**
     * @see ca.nengo.util.impl.TaskSpawner#getTasks()
     */
    public ThreadTask[] getTasks(){
    	
    	if(mySimulator == null)
    		return new ThreadTask[0];
    		
    	Probe[] probes = mySimulator.getProbes();
    	ProbeTask[] probeTasks = new ProbeTask[probes.length];
    	
    	for(int i = 0; i < probes.length; i++){
    		probeTasks[i] = probes[i].getProbeTask();
    	}

    	return probeTasks;
    }

    /**
     * @see ca.nengo.util.impl.TaskSpawner#setTasks()
     */
    public void setTasks(ThreadTask[] tasks){
    }

    /**
     * @see ca.nengo.util.impl.TaskSpawner#addTasks()
     */
    public void addTasks(ThreadTask[] tasks){
    }

	/**
	 * Wraps an Origin with a new name (for exposing outside Network).
	 *
	 * @author Bryan Tripp
	 */
	public class OriginWrapper implements Origin {

		private static final long serialVersionUID = 1L;

		private Node myNode;
		private Origin myWrapped;
		private String myName;

		/**
		 * @param node Parent node
		 * @param wrapped Warpped Origin
		 * @param name Name of new origin
		 */
		public OriginWrapper(Node node, Origin wrapped, String name) {
			myNode = node;
			myWrapped = wrapped;
			myName = name;
		}

		/**
		 * Default constructor
		 * TODO: Is this necessary?
		 */
		public OriginWrapper() {
			this(null, null, "exposed");
		}

		/**
		 * @return The underlying wrapped Origin
		 */
		public Origin getWrappedOrigin() {
			return myWrapped;
		}

		/**
		 * Unwraps Origin until it finds one that isn't wrapped
		 *
		 * @return Base origin if there are multiple levels of wrapping
		 */
		public Origin getBaseOrigin(){
			if(myWrapped instanceof OriginWrapper) {
                return ((OriginWrapper) myWrapped).getBaseOrigin();
            } else {
                return myWrapped;
            }
		}

		/**
		 * @param wrapped Set the underlying wrapped Origin
		 */
		public void setWrappedOrigin(Origin wrapped) {
			myWrapped = wrapped;
		}

		public String getName() {
			return myName;
		}

		/**
		 * @param name Name
		 */
		public void setName(String name) {
			myName = name;
		}

		public int getDimensions() {
			return myWrapped.getDimensions();
		}

		public InstantaneousOutput getValues() throws SimulationException {
			return myWrapped.getValues();
		}

		public void setValues(InstantaneousOutput values) {
			myWrapped.setValues(values);
		}
		
		public Node getNode() {
			return myNode;
		}

		/**
		 * @param node Parent node
		 */
		public void setNode(Node node) {
			myNode = node;
		}

		@Override
		public Origin clone() throws CloneNotSupportedException {
			return (Origin) super.clone();
		}
		
		public Origin clone(Node node) throws CloneNotSupportedException {
			return this.clone();
		}

		public void setRequiredOnCPU(boolean val){
		    myWrapped.setRequiredOnCPU(val);
		}
		    
		public boolean getRequiredOnCPU(){
		   return myWrapped.getRequiredOnCPU();
		}
	}

	/**
	 * Wraps a Termination with a new name (for exposing outside Network).
	 *
	 * @author Bryan Tripp
	 */
	public class TerminationWrapper implements Termination {

		private static final long serialVersionUID = 1L;

		private Node myNode;
		private Termination myWrapped;
		private String myName;

		/**
		 * @param node Parent node
		 * @param wrapped Termination being wrapped
		 * @param name New name
		 */
		public TerminationWrapper(Node node, Termination wrapped, String name) {
			myNode = node;
			myWrapped = wrapped;
			myName = name;
		}

		/**
		 * @return Wrapped Termination
		 */
		public Termination getWrappedTermination() {
			return myWrapped;
		}

		/**
		 * Unwraps terminations until it finds one that isn't wrapped
		 *
		 * @return Underlying Termination, not wrapped
		 */
		public Termination getBaseTermination(){
			if(myWrapped instanceof TerminationWrapper) {
                return ((TerminationWrapper) myWrapped).getBaseTermination();
            } else {
                return myWrapped;
            }
		}

		public String getName() {
			return myName;
		}

		public int getDimensions() {
			return myWrapped.getDimensions();
		}

		public void setValues(InstantaneousOutput values) throws SimulationException {
			myWrapped.setValues(values);
		}

		public Node getNode() {
			return myNode;
		}

		public boolean getModulatory() {
			return myWrapped.getModulatory();
		}

		public float getTau() {
			return myWrapped.getTau();
		}

		public void setModulatory(boolean modulatory) {
			myWrapped.setModulatory(modulatory);
		}

		public void setTau(float tau) throws StructuralException {
			myWrapped.setTau(tau);
		}
		
		/**
		 * @return Extract the input to the termination.
		 */
		public InstantaneousOutput getInput(){
			return myWrapped.getInput();
		}

		/**
		 * @see ca.nengo.model.Resettable#reset(boolean)
		 */
		public void reset(boolean randomize) {
			myWrapped.reset(randomize);
		}

		@Override
		public TerminationWrapper clone() throws CloneNotSupportedException {
			return this.clone(myNode);
		}
		
		public TerminationWrapper clone(Node node) throws CloneNotSupportedException {
			throw new CloneNotSupportedException("TerminationWrapper not cloneable");
//			TerminationWrapper result = (TerminationWrapper) super.clone();
//			result.myNode = node;
//			return result;
		}

	}
	
	public void dumpToScript() throws FileNotFoundException
	{
		File file = new File(this.getName().replace(' ', '_') + ".py");
		
		ScriptGenerator scriptGen = new ScriptGenerator(file);
		scriptGen.startDFS(this);
	}
	
	public void dumpToScript(String filepath) throws FileNotFoundException
	{
		File file = new File(filepath);
		
		ScriptGenerator scriptGen = new ScriptGenerator(file);
		scriptGen.startDFS(this);
	}
	

	/**
	 * @see ca.nengo.model.Node#getDocumentation()
	 */
	public String getDocumentation() {
		return myDocumentation;
	}

	/**
	 * @see ca.nengo.model.Node#setDocumentation(java.lang.String)
	 */
	public void setDocumentation(String text) {
		myDocumentation = text;
	}

	/**
	 * @see ca.nengo.model.Network#getMetaData(java.lang.String)
	 */
	public Object getMetaData(String key) {
		return myMetaData.get(key);
	}

	/**
	 * @see ca.nengo.model.Network#setMetaData(java.lang.String, java.lang.Object)
	 */
	public void setMetaData(String key, Object value) {
		if ( !(value instanceof Serializable) ) {
			throw new RuntimeException("Metadata must be serializable");
		}
		myMetaData.put(key, value);
	}

	/**
	 * @see ca.nengo.util.VisiblyMutable#addChangeListener(ca.nengo.util.VisiblyMutable.Listener)
	 */
	public void addChangeListener(Listener listener) {
		if (myListeners == null) {
			myListeners = new ArrayList<Listener>(1);
		}
		myListeners.add(listener);
	}

	/**
	 * @see ca.nengo.util.VisiblyMutable#removeChangeListener(ca.nengo.util.VisiblyMutable.Listener)
	 */
	public void removeChangeListener(Listener listener) {
		if (myListeners != null) {
            myListeners.remove(listener);
        }
	}

	private void fireVisibleChangeEvent() {
		VisiblyMutableUtils.changed(this, myListeners);
	}

    public String toScript(HashMap<String, Object> scriptData) throws ScriptGenException {
		StringBuilder py = new StringBuilder();
        String pythonNetworkName = scriptData.get("prefix") + myName.replaceAll("\\p{Blank}|\\p{Punct}", ((Character)scriptData.get("spaceDelim")).toString());

        py.append("\n\n# Network " + myName + " Start\n");

        if ((Boolean)scriptData.get("isSubnet"))
        {
            py.append(String.format("%s = %s.make_subnetwork('%s')\n", 
                    pythonNetworkName,
                    scriptData.get("netName"),
                    myName
                    ));
        }
        else
        {
            py.append(String.format("%s = nef.Network('%s')\n", 
                    pythonNetworkName, 
                    myName));
        }

        py.append("\n# " + myName + " - Nodes\n");
        
        return py.toString();
    }

	@Override
	public Network clone() throws CloneNotSupportedException {
		NetworkImpl result = (NetworkImpl) super.clone();

		result.myNodeMap = new HashMap<String, Node>(10);
		for (Node oldNode : myNodeMap.values()) {
			Node newNode = oldNode.clone();
			result.myNodeMap.put(newNode.getName(), newNode);
			newNode.addChangeListener(result);
		}

		//TODO: Exposed states aren't handled currently, pending redesign of Probes (it should be possible
		//		to probe things that are nested deeply, in which case exposing state woulnd't be necessary)
//		result.myProbeables
//		result.myProbeableStates

		//TODO: this works with a single Projection impl & no params; should add Projection.copy(Origin, Termination, Network)?
		result.myProjectionMap = new HashMap<Termination, Projection>(10);
		for (Projection oldProjection : getProjections()) {
			try {
				Origin newOrigin = result.getNode(oldProjection.getOrigin().getNode().getName())
					.getOrigin(oldProjection.getOrigin().getName());
				Termination newTermination = result.getNode(oldProjection.getTermination().getNode().getName())
					.getTermination(oldProjection.getTermination().getName());
				Projection newProjection = new ProjectionImpl(newOrigin, newTermination, result);
				result.myProjectionMap.put(newTermination, newProjection);
			} catch (StructuralException e) {
				throw new CloneNotSupportedException("Problem copying Projectio: " + e.getMessage());
			}
		}

		result.myExposedOrigins = new HashMap<String, Origin>(10);
		result.myExposedOriginNames = new HashMap<Origin, String>(10);
		result.OrderedExposedOrigins = new LinkedList <Origin> ();
		for (Origin exposed : getOrigins()) {
			String name = exposed.getName();
			Origin wrapped = ((OriginWrapper) exposed).getWrappedOrigin();
			try {
				// Check to see if referenced node is the network itself. If it is, handle the origin differently.
				if (wrapped.getNode().getName() != myName ) {
					Origin toExpose = result.getNode(wrapped.getNode().getName()).getOrigin(wrapped.getName());
					result.exposeOrigin(toExpose, name);
				}
			} catch (StructuralException e) {
				throw new CloneNotSupportedException("Problem exposing Origin: " + e.getMessage());
			}
		}

		result.myExposedTerminations = new HashMap<String, Termination>(10);
		result.myExposedTerminationNames = new HashMap<Termination, String>(10);
		result.OrderedExposedTerminations = new LinkedList <Termination> ();
		for (Termination exposed : getTerminations()) {
			String name = exposed.getName();
			Termination wrapped = ((TerminationWrapper) exposed).getWrappedTermination();
			try {
				// Check to see if referenced node is the network itself. If it is, handle the termination differently.
				if (wrapped.getNode().getName() != myName ) {
					Termination toExpose = result.getNode(wrapped.getNode().getName()).getTermination(wrapped.getName());
					result.exposeTermination(toExpose, name);
				}
			} catch (StructuralException e) {
				throw new CloneNotSupportedException("Problem exposing Termination: " + e.getMessage());
			}
		}

		result.myListeners = new ArrayList<Listener>(5);

		result.myMetaData = new HashMap<String, Object>(10);
		for (String key : myMetaData.keySet()) {
			Object o = myMetaData.get(key);
			if (o instanceof Cloneable) {
				Object copy = tryToClone((Cloneable) o);
				result.myMetaData.put(key, copy);
			} else {
				result.myMetaData.put(key, o);
			}
		}

		//TODO: take another look at Probe design (maybe Probeables reference Probes?)
		result.mySimulator = mySimulator.clone();
		result.mySimulator.initialize(result);
		Probe[] oldProbes = mySimulator.getProbes();
		for (Probe oldProbe : oldProbes) {
			Probeable target = oldProbe.getTarget();
			if (target instanceof Node) {
				Node oldNode = (Node) target;
				if (oldProbe.isInEnsemble()) {
					try {
						Ensemble oldEnsemble = (Ensemble) getNode(oldProbe.getEnsembleName());
						int neuronIndex = -1;
						for (int j = 0; j < oldEnsemble.getNodes().length && neuronIndex < 0; j++) {
							if (oldNode == oldEnsemble.getNodes()[j]) {
                                neuronIndex = j;
                            }
						}
						result.mySimulator.addProbe(oldProbe.getEnsembleName(), neuronIndex, oldProbe.getStateName(), true);
					} catch (SimulationException e) {
						Log.w("NetworkImpl", "Problem copying Probe", e);
					} catch (StructuralException e) {
						Log.w("NetworkImpl", "Problem copying Probe", e);
					}
				} else {
					try {
						result.mySimulator.addProbe(oldNode.getName(), oldProbe.getStateName(), true);
					} catch (SimulationException e) {
						Log.w("NetworkImpl", "Problem copying Probe", e);
					}
				}
			} else {
				Log.w("NetworkImpl", "Can't copy Probe on type " + target.getClass().getName()
						+ " (to be addressed in a future release)");
			}
		}

		return result;
	}

	private static Object tryToClone(Cloneable o) {
		Object result = null;

		try {
			Method cloneMethod = o.getClass().getMethod("clone", new Class[0]);
			result = cloneMethod.invoke(o, new Object[0]);
		} catch (Exception e) {
            Log.w("NetworkImpl", "Couldn't clone data of type " + o.getClass().getName(), e);
		}

		return result;
	}
	
	public void addStepListener(StepListener listener) {
		if (myStepListeners == null) {
			myStepListeners = new ArrayList<StepListener>(1);
		}
        myStepListeners.add(listener);
	}
	public void removeStepListener(StepListener listener) {
		if (myStepListeners == null) {
			myStepListeners = new ArrayList<StepListener>(1);
		}
        myStepListeners.remove(listener);
	}
	
	public void fireStepListeners(float time) {
		if (myStepListeners == null) {
			myStepListeners = new ArrayList<StepListener>(1);
		}
		for (StepListener listener: myStepListeners) {
			listener.stepStarted(time);
		}
	}

	public Node[] getChildren() {
		return getNodes();
	}

	@SuppressWarnings("unchecked")
	public String toPostScript(HashMap<String, Object> scriptData) throws ScriptGenException {
		StringBuilder py = new StringBuilder();

		String pythonNetworkName = scriptData.get("prefix") + myName.replaceAll("\\p{Blank}|\\p{Punct}", ((Character)scriptData.get("spaceDelim")).toString());

        py.append("\n# " + myName + " - Templates\n");
		
        if (myMetaData.get("NetworkArray") != null) {
            Iterator iter = ((HashMap)myMetaData.get("NetworkArray")).values().iterator();
            while (iter.hasNext())
            {
                HashMap array = (HashMap)iter.next();

                if (!myNodeMap.containsKey(array.get("name")))
                {
                    continue;
                }

            	/*
                py.append(String.format("nef.templates.networkarray.make(%s, name='%s', neurons=%d, length=%d, radius=%.1f, rLow=%f, rHigh=%f, iLow=%f, iHigh=%f, encSign=%d, useQuick=%s)\n",
                            pythonNetworkName,
                            array.get("name"),
                            (Integer)array.get("neurons"),
                            (Integer)array.get("length"),
                            (Double)array.get("radius"),
                            (Double)array.get("rLow"),
                            (Double)array.get("rHigh"),
                            (Double)array.get("iLow"),
                            (Double)array.get("iHigh"),
                            (Integer)array.get("encSign"),
                            useQuick));
                            */
            	
            	 py.append(String.format("%s.make_array(name='%s', neurons=%d, length=%d, dimensions=%d",
            			 	pythonNetworkName,
            			 	array.get("name"),
            			 	(Integer)array.get("neurons"),
                            (Integer)array.get("length"),
                            (Integer)array.get("dimensions")
                            ));
            	 
            	 if(array.containsKey("radius")){ py.append(", radius=" + Double.toString((Double)array.get("radius"))); }
            	 
            	 if(array.containsKey("rLow") && array.containsKey("rHigh"))
            	 { py.append(", max_rate=(" + Double.toString((Double)array.get("rLow")) + ", " + Double.toString((Double)array.get("rHigh")) + ")"); }
            	 
            	 if(array.containsKey("iLow") && array.containsKey("iHigh"))
            	 { py.append(", intercept=(" + Double.toString((Double)array.get("iLow")) + ", " + Double.toString((Double)array.get("iHigh")) + ")"); }
            	 
            	 if(array.containsKey("useQuick"))
            	 {  
            		String useQuick = (Boolean)array.get("useQuick") ? "True" : "False";
            	 	py.append(", quick=" + useQuick);
            	 }
            	 
            	 if(array.containsKey("encoders")){ py.append(", encoders=" + array.get("encoders")); }
            	 py.append(")\n");
            }
        } 
		
        if (myMetaData.get("BasalGanglia") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("BasalGanglia")).values().iterator();
            while (iter.hasNext())
            {
                HashMap bg = (HashMap)iter.next();

                if (!myNodeMap.containsKey(bg.get("name")))
                {
                    continue;
                }

            	String same_neurons = (Boolean)bg.get("same_neurons") ? "True" : "False";

                py.append(String.format("nef.templates.basalganglia.make(%s, name='%s', dimensions=%d, neurons=%d, pstc=%.3f, same_neurons=%s)\n",
                            pythonNetworkName,
                            bg.get("name"),
                            (Integer)bg.get("dimensions"),
                            (Integer)bg.get("neurons"),
                            (Double)bg.get("pstc"),
                            same_neurons));
            }
        } 
		
        if (myMetaData.get("Thalamus") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("Thalamus")).values().iterator();
            while (iter.hasNext())
            {
                HashMap thal = (HashMap)iter.next();

                if (!myNodeMap.containsKey(thal.get("name")))
                {
                    continue;
                }

            	String useQuick = (Boolean)thal.get("useQuick") ? "True" : "False";

                py.append(String.format("nef.templates.thalamus.make(%s, name='%s', neurons=%d, dimensions=%d, inhib_scale=%d, tau_inhib=%.3f, useQuick=%s)\n",
                            pythonNetworkName,
                            thal.get("name"),
                            (Integer)thal.get("neurons"),
                            (Integer)thal.get("dimensions"),
                            (Integer)thal.get("inhib_scale"),
                            (Double)thal.get("tau_inhib"),
                            useQuick));
            }
        }
		
        if (myMetaData.get("integrator") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("integrator")).values().iterator();
            while (iter.hasNext())
            {
                HashMap integrator = (HashMap)iter.next();

                if (!myNodeMap.containsKey(integrator.get("name")))
                {
                    continue;
                }

                py.append(String.format("nef.templates.integrator.make(%s, name='%s', neurons=%d, dimensions=%d, tau_feedback=%g, tau_input=%g, scale=%g)\n",
                			pythonNetworkName,
                            integrator.get("name"),
                            (Integer)integrator.get("neurons"),
                            (Integer)integrator.get("dimensions"),
                            (Double)integrator.get("tau_feedback"),
                            (Double)integrator.get("tau_input"),
                            (Double)integrator.get("scale")));
            }
        }  
		
        if (myMetaData.get("oscillator") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("oscillator")).values().iterator();
            while (iter.hasNext())
            {
                HashMap oscillator = (HashMap)iter.next();

                String controlled = (Boolean)oscillator.get("controlled") ? "True" : "False";

                if (!myNodeMap.containsKey(oscillator.get("name")))
                {
                    continue;
                }

                py.append(String.format("nef.templates.oscillator.make(%s, name='%s', neurons=%d, dimensions=%d, frequency=%g, tau_feedback=%g, tau_input=%g, scale=%g, controlled=%s)\n",
                			pythonNetworkName,
                            oscillator.get("name"),
                            (Integer)oscillator.get("neurons"),
                            (Integer)oscillator.get("dimensions"),
                            (Double)oscillator.get("frequency"),
                            (Double)oscillator.get("tau_feedback"),
                            (Double)oscillator.get("tau_input"),
                            (Double)oscillator.get("scale"),
                            controlled));
            }
        }      

        if (myMetaData.get("linear") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("linear")).values().iterator();
            while (iter.hasNext())
            {
                HashMap linear = (HashMap)iter.next();

                if (!myNodeMap.containsKey(linear.get("name")))
                {
                    continue;
                }

                StringBuilder a = new StringBuilder("[");
                double[][] arr = (double[][])linear.get("A");
                for (int i = 0; i < arr.length; i++)
                {
                    a.append("[");
                    for (int j = 0; j < arr[i].length; j++)
                    {
                        a.append(arr[i][j]);
                        if ((j+1) < arr[i].length)
                        {
                            a.append(",");
                        }
                    }
                    a.append("]");
                    if ((i + 1) < arr.length)
                    {
                        a.append(",");
                    }
                }
                a.append("]");

                py.append(String.format("nef.templates.linear_system.make(%s, name='%s', neurons=%d, A=%s, tau_feedback=%g)\n",
                			pythonNetworkName,
                            linear.get("name"),
                            (Integer)linear.get("neurons"),
                            a.toString(),
                            (Double)linear.get("tau_feedback")));
            }
        } 

        if (myMetaData.get("learnedterm") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("learnedterm")).values().iterator();
            while (iter.hasNext())
            {
                HashMap learnedterm = (HashMap)iter.next();

                if (!myNodeMap.containsKey(learnedterm.get("errName")))
                {
                    continue;
                }

                py.append(String.format("nef.templates.learned_termination.make(%s, errName='%s', N_err=%d, preName='%s', postName='%s', rate=%g)\n",
                			pythonNetworkName,
                            learnedterm.get("errName"),
                            (Integer)learnedterm.get("N_err"),
                            (String)learnedterm.get("preName"),
                            (String)learnedterm.get("postName"),
                            (Double)learnedterm.get("rate")));
            }
        }   

        if (myMetaData.get("convolution") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("convolution")).values().iterator();
            while (iter.hasNext())
            {
                HashMap binding = (HashMap)iter.next();

                if (!myNodeMap.containsKey(binding.get("name")))
                {
                    continue;
                }
                
                String invert_first = (Boolean)binding.get("invert_first") ? "True" : "False";
                String invert_second = (Boolean)binding.get("invert_second") ? "True" : "False";
                String quick = (Boolean)binding.get("quick") ? "True" : "False";
                String A = (String)binding.get("A") == null ? "None" : "'" + (String)binding.get("A") + "'";
                String B = (String)binding.get("B") == null ? "None" : "'" + (String)binding.get("B") + "'";

                StringBuilder encoders = new StringBuilder("[");
                double[][] arr = (double[][])binding.get("encoders");
                for (int i = 0; i < arr.length; i++)
                {
                    encoders.append("[");
                    for (int j = 0; j < arr[i].length; j++)
                    {
                        encoders.append(arr[i][j]);
                        if ((j+1) < arr[i].length)
                        {
                            encoders.append(",");
                        }
                    }
                    encoders.append("]");
                    if ((i + 1) < arr.length)
                    {
                        encoders.append(",");
                    }
                }
                encoders.append("]");
                
                py.append(String.format("nef.convolution.make_convolution(%s, name='%s', A=%s, B=%s, C='%s', N_per_D=%d, quick=%s, encoders=%s, radius=%d, pstc_out=%g, pstc_in=%g, pstc_gate=%g, invert_first=%s, invert_second=%s, mode='%s', output_scale=%d)\n",
                			pythonNetworkName,
                            binding.get("name"),
                            A,
                            B,
                            (String)binding.get("C"),
                            (Integer)binding.get("N_per_D"),
                            quick,
                            encoders.toString(),
                            (Integer)binding.get("radius"),
                            (Double)binding.get("pstc_out"),
                            (Double)binding.get("pstc_in"),
                            (Double)binding.get("pstc_gate"),
                            invert_first,
                            invert_second,
                            (String)binding.get("mode"),
                            (Integer)binding.get("output_scale")));
            }
        } 

        if (myMetaData.get("bgrule") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("bgrule")).values().iterator();
            while (iter.hasNext())
            {
                HashMap bgrule = (HashMap)iter.next();

                if (!myNodeMap.containsKey(bgrule.get("name")))
                {
                    continue;
                }
                
                String use_single_input = (Boolean)bgrule.get("use_single_input") ? "True" : "False";
                
                // going to assume the current network is the BG...BG_rules can only be added on BG networks.
                py.append(String.format("nef.templates.basalganglia_rule.make(%s, %s.network.getNode('%s'), index=%d, dim=%d, pattern='%s', pstc=%g, use_single_input=%s)\n",
                            pythonNetworkName,
                            pythonNetworkName,
                            (String)bgrule.get("name"),
                            (Integer)bgrule.get("index"),
                            (Integer)bgrule.get("dim"),
                            (String)bgrule.get("pattern"),
                            (Double)bgrule.get("pstc"),
                            use_single_input));
            }
        } 

        if (myMetaData.get("gate") != null)
        {
            Iterator iter = ((HashMap)myMetaData.get("gate")).values().iterator();
            while (iter.hasNext())
            {
                HashMap gate = (HashMap)iter.next();

                if (!myNodeMap.containsKey(gate.get("name")))
                {
                    continue;
                }

                py.append(String.format("nef.templates.gate.make(%s, name='%s', gated='%s', neurons=%d, pstc=%g)\n",
                			pythonNetworkName,
                            gate.get("name"),
                            (String)gate.get("gated"),
                            (Integer)gate.get("neurons"),
                            (Double)gate.get("pstc")));
            }
        }    

		return py.toString();
	}
	
	public Object getMetadata(String key) {
		if (myMetadata==null) myMetadata = new LinkedHashMap<String, Object>(2);
		return myMetadata.get(key);
	}
	public void setMetadata(String key, Object value) {
		if (myMetadata==null) myMetadata = new LinkedHashMap<String, Object>(2);		
		myMetadata.put(key, value);
	}
}
