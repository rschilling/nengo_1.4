package cognition.com.nengo;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;

import ca.nengo.io.FileManager;
import ca.nengo.model.nef.NEFEnsemble;

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

        // read the network from a file and execute it.
        Resources res = Resources.getSystem();
        InputStream is = res.openRawResource(R.raw.startup);

        try {
            mStartupEnsemble = (NEFEnsemble)new FileManager().load(is);
            is.close();
        } catch (ClassNotFoundException cnfe){
            throw new RuntimeException(cnfe);
        } catch (IOException ioe){
            throw new RuntimeException(ioe);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mStartupEnsemble = null;

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
