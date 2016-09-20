/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package hosts.file.localvpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;


public class LocalVPN extends ActionBarActivity {
    private static final String TAG = LocalVPN.class.getSimpleName();
    private static final int VPN_REQUEST_CODE = 0x0F;

    private boolean waitingForVPNStart;
    private String appStartTime = new SimpleDateFormat("h:m:s a MMMM d, yyyy").format(new Date());

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalVPNService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
                if (intent.getBooleanExtra("running", false))
                    waitingForVPNStart = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting up");
        setContentView(R.layout.activity_local_vpn);
        final Switch vpnButton = (Switch) findViewById(R.id.vpn_switch);
        vpnButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startVPN();
                } else {
                    stopVPN();
                }
            }
        });
        waitingForVPNStart = false;
        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalVPNService.BROADCAST_VPN_STATE));
        final TextView startText = (TextView) findViewById(R.id.appStart);
        startText.setText(startText.getText().toString() + '\n' + appStartTime);

    }

    private void stopVPN() {
        Log.i(TAG, "Trying to stop.");
        LocalVPNService.getService().stopVPN();
    }

    private void startVPN() {
        Log.i(TAG, "Trying to start.");
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            waitingForVPNStart = true;
            startService(new Intent(this, LocalVPNService.class));
            updateVPNSwitchState(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isRunning = waitingForVPNStart || LocalVPNService.isRunning();
        Log.d(TAG, "Set switch to " + isRunning);
        updateVPNSwitchState(isRunning);
    }

    private void updateVPNSwitchState(boolean isOn) {
        final Switch vpnButton = (Switch) findViewById(R.id.vpn_switch);
        vpnButton.setChecked(isOn);
    }
}
