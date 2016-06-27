package cognition.com.nengo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NengoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nengo);
    }

    @Override
    protected void onStart() {
        super.onStart();

        startService(new Intent(this, NengoService.class));
    }
}
