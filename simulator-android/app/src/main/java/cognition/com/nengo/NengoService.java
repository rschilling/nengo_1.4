package cognition.com.nengo;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Debug;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;

import ca.nengo.examples.IntegratorExample;
import ca.nengo.io.FileManager;
import ca.nengo.io.MatlabExporter;
import ca.nengo.math.Function;
import ca.nengo.math.impl.ConstantFunction;
import ca.nengo.model.Ensemble;
import ca.nengo.model.Network;
import ca.nengo.model.SimulationException;
import ca.nengo.model.StepListener;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.model.impl.FunctionInput;
import ca.nengo.model.impl.NetworkImpl;
import ca.nengo.model.nef.NEFEnsemble;
import ca.nengo.model.nef.NEFEnsembleFactory;
import ca.nengo.model.nef.impl.NEFEnsembleFactoryImpl;
import ca.nengo.plot.Plotter;
import ca.nengo.sim.Simulator;
import ca.nengo.util.Probe;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.VisiblyMutable;

/**
 * Created by Richard Schilling (rschilling@custom-control.com) on 6/25/16.
 */
public class NengoService extends Service {


    /**
     * The ensemble that is loaded when the service starts.
     */
    private NEFEnsemble mStartupEnsemble;

    @Override
    public void onCreate() {
        super.onCreate();


        /*

        generates a StreamCorruptedExeption - probably because the object output stream
        wrote some incompatable data.

        // read the network from a file and execute it.
        Resources res = getResources();
        InputStream is = res.openRawResource(R.raw.startup);

        try {
            mStartupEnsemble = (NEFEnsemble)new FileManager().load(is);
            is.close();
        } catch (ClassNotFoundException cnfe){
            throw new RuntimeException(cnfe);
        } catch (IOException ioe){
            throw new RuntimeException(ioe);
        }
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mStartupEnsemble = null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread (){
            public void run(){
                integratorExample();
            }
        }.start();


        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void integratorExample(){
        Log.i("Service", "start (begin tracing)");
        Debug.startMethodTracing("nengo-" + SystemClock.elapsedRealtime(), 1000000);
        try {
            Log.i("Service", "create network");
            Network network = createNetwork();
            Log.i("Service", "get simulator");
            Simulator simulator = network.getSimulator();

            Log.i("Service", "add probs");
            Probe inputRecorder = simulator.addProbe("input", "input", true);
            Probe integratorRecorder = simulator.addProbe("integrator", NEFEnsemble.X, true);
            Probe neuronRecorder = simulator.addProbe("integrator", 0, "V", true);

            long startTime = System.currentTimeMillis();
            Log.i("Service", "run sumulator");
            simulator.run(0f, 1f, .0002f);
            System.out.println("Run time: " + ((System.currentTimeMillis() - startTime)/1000f) );

            Log.i("Service", "get data");
            TimeSeries integratorData = integratorRecorder.getData();
            integratorData.getLabels()[0] = "decoded output";

            Log.i("Service", "plot");
            Plotter.plot(inputRecorder.getData(), "Input");
            Plotter.plot(integratorData, .005f, "Integrator");
            Plotter.plot(neuronRecorder.getData(), "Neuron #0");

            Plotter.plot(((Ensemble) network.getNode("integrator")).getSpikePattern());

            Log.i("Service", "write to file");
            MatlabExporter me = new MatlabExporter();
            me.add("input", inputRecorder.getData());
            me.add("integrator", integratorRecorder.getData(), .01f);
            me.add("neuron", neuronRecorder.getData());
            me.write(new File(getFilesDir(), "export.mat"));

        } catch (SimulationException e) {
            e.printStackTrace();
        } catch (StructuralException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Log.i("Service", "done (stop tracing)");

            Debug.stopMethodTracing();
        }
    }

    private Network createNetwork() throws StructuralException {


        // instantiate network
        Network network = new NetworkImpl();

        // add listeners
        network.addStepListener(new StepListener() {
            @Override
            public void stepStarted(float time) {
                Log.i("Network", "step started: " + time);
            }
        });
        network.addChangeListener(new VisiblyMutable.Listener() {
            @Override
            public void changed(VisiblyMutable.Event e) throws StructuralException {
                Log.i("Network", "changed: " + e.toString());
            }
        });


        // create a function
        Function f = new ConstantFunction(1, 1f);

//		Function f = new SineFunction();

        // associate the function with a function input and add it to the network.
        FunctionInput input = new FunctionInput("input", new Function[]{f}, Units.UNK);
        network.addNode(input);

        // create an ensemble factory and set it's database for output.
        NEFEnsembleFactoryImpl ef = new NEFEnsembleFactoryImpl();
        ef.setDatabase(getFilesDir());

        // use the ensemble factory to create an ensemble and add it to the network.
        NEFEnsemble integrator = ef.make("integrator", 2, 1, "integrator1", true);
        network.addNode(integrator);
        integrator.collectSpikes(true);
        integrator.addChangeListener(new VisiblyMutable.Listener() {
            @Override
            public void changed(VisiblyMutable.Event e) throws StructuralException {
                Log.i("integrator", "changed: " + e.toString());
            }
        });

        // tell a plotter to plot the integrator and its origin.
        Plotter.plot(integrator);
        Plotter.plot(integrator, NEFEnsemble.X);

        float tau = .05f;

        // create a termination for input into integrator.
        Termination interm = integrator.addDecodedTermination("input", new float[][]{new float[]{tau}}, tau, false);
//		Termination interm = integrator.addDecodedTermination("input", new float[][]{new float[]{1f}}, tau);

        // add a projection between the FunctionInput created above and the terminator/integrator.
        network.addProjection(input.getOrigin(FunctionInput.ORIGIN_NAME), interm);

        // add a termination for input into the integrator called feedback.
        Termination fbterm = integrator.addDecodedTermination("feedback", new float[][]{new float[]{1f}}, tau, false);

        // add a projection between integrator and feedback terminator.
        network.addProjection(integrator.getOrigin(NEFEnsemble.X), fbterm);

        //System.out.println("Network creation: " + (System.currentTimeMillis() - start));
        return network;
    }
}
