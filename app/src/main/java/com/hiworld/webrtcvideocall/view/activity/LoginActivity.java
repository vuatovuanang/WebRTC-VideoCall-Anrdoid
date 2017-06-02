package com.hiworld.webrtcvideocall.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hiworld.webrtcvideocall.R;
import com.hiworld.webrtcvideocall.utility.MqttClientHelper;
import com.hiworld.webrtcvideocall.utility.Constants;
import com.hiworld.webrtcvideocall.utility.LogUtils;
import com.hiworld.webrtcvideocall.utility.MySharePreference;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.edtUsername)
    EditText edtUsername;
    @BindView(R.id.edtPassword)
    EditText edtPassword;
    @BindView(R.id.edtFriendUsername)
    EditText edtFriendUsername;
    @BindView(R.id.btnCall)
    Button btnCall;
    @BindView(R.id.tvName)
    TextView tvName;
    @BindView(R.id.btnLogin)
    Button btnLogin;
    private String myUser;
    private String friendUser;
    private boolean isLogin;


    @Override
    protected void onDestroy() {
        MqttClientHelper.getInstance(getApplicationContext()).unsubscribeToTopic(myUser);
        MqttClientHelper.getInstance(getApplicationContext()).release();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        MqttClientHelper.getInstance(getApplicationContext()).addListener(new MqttClientListener());
        myUser = MySharePreference.getUserName(this);
        edtUsername.setText(myUser);
    }

    @OnClick({R.id.btnLogin, R.id.btnCall})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnLogin:
                if (!isLogin) {
                    login();
                } else {
                    logout();
                }
                break;
            case R.id.btnCall:
                callToFriend();
                break;
        }
    }


    private void callToFriend() {
        friendUser = edtFriendUsername.getText().toString().trim();
        if (TextUtils.isEmpty(MySharePreference.getUserName(this))) {
            Toast.makeText(this, "Please Login!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(friendUser)) {
            Toast.makeText(this, "Username is not null", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("call_user", myUser);
            jsonObject.put("action", Constants.ACTION_CALL);
            MqttClientHelper.getInstance(getApplicationContext()).publishMessage(friendUser, jsonObject.toString());

            Intent it = new Intent(LoginActivity.this, VideoCallActivity.class);
            it.putExtra("friend_name", friendUser);
            startActivity(it);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        MqttClientHelper.getInstance(getApplicationContext()).unsubscribeToTopic(myUser, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                LogUtils.e("logout: onSuccess");
                isLogin = false;
                tvName.setText("Offline");
                edtFriendUsername.setEnabled(false);
                btnLogin.setText("Login");
                MySharePreference.setUserName(LoginActivity.this, "");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(LoginActivity.this, "Logout Fail", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login() {
        myUser = edtUsername.getText().toString().trim();
        if (TextUtils.isEmpty(myUser)) {
            Toast.makeText(this, "Username is not null", Toast.LENGTH_SHORT).show();
            return;
        }
        MqttClientHelper.getInstance(getApplicationContext()).subscribeToTopic(myUser);
    }

    private class MqttClientListener implements MqttClientHelper.IMqttClientListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            isLogin = true;
            tvName.setText("Online");
            edtFriendUsername.setEnabled(true);
            btnLogin.setText("Logout");
            MySharePreference.setUserName(LoginActivity.this, myUser);
            LogUtils.e("subscribeToTopic");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Toast.makeText(LoginActivity.this, "onFailure", Toast.LENGTH_SHORT).show();
            LogUtils.e("onFailure");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            try {
                String msgContent = new String(message.getPayload());
                JSONObject jsonObject = new JSONObject(msgContent);
                if (jsonObject.has("action")) {
                    LogUtils.e("LoginActivity - messageArrived: " + new String(message.getPayload()));
                    String action = jsonObject.getString("action");
                    if (action.equalsIgnoreCase(Constants.ACTION_CALL)) {
                        Intent it = new Intent(LoginActivity.this, IncomingCallActivity.class);
                        it.putExtra("call_user", jsonObject.getString("call_user"));
                        startActivity(it);
                    }
                } else if (jsonObject.has("type")) {
                    String type = jsonObject.getString("type");
                    if (type.equalsIgnoreCase("offer")) {
                        LogUtils.e("LoginActivity - messageArrived: " + new String(message.getPayload()));
                    } else if (type.equalsIgnoreCase("answer")) {
                        LogUtils.e("LoginActivity - messageArrived: " + new String(message.getPayload()));
                    } else if (type.equalsIgnoreCase("candidate")) {
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
