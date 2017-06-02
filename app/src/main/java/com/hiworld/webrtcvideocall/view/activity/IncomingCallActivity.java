package com.hiworld.webrtcvideocall.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hiworld.webrtcvideocall.R;
import com.hiworld.webrtcvideocall.utility.MqttClientHelper;
import com.hiworld.webrtcvideocall.utility.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class IncomingCallActivity extends Activity {

    @BindView(R.id.btnAcceptCall)
    ImageButton btnAcceptCall;
    @BindView(R.id.btnRejectCall)
    ImageButton btnRejectCall;
    @BindView(R.id.tvCallerName)
    TextView tvCallerName;

    String friendUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ButterKnife.bind(this);
        friendUser = getIntent().getStringExtra("call_user");
        tvCallerName.setText(friendUser);
    }

    @OnClick({R.id.btnAcceptCall, R.id.btnRejectCall})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnAcceptCall:
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("action", Constants.ACTION_ACCEPT_CALL);
                    MqttClientHelper.getInstance(getApplicationContext()).publishMessage(friendUser, jsonObject.toString());
                    Intent it = new Intent(IncomingCallActivity.this, VideoCallActivity.class);
                    it.putExtra("friend_name", friendUser);
                    startActivity(it);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.finish();
                break;
            case R.id.btnRejectCall:
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("action", Constants.ACTION_REJECT_CALL);
                    MqttClientHelper.getInstance(getApplicationContext()).publishMessage(friendUser, jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.finish();
                break;
        }
    }
}
