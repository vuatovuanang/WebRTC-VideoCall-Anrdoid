package com.hiworld.webrtcvideocall.utility;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

/**
 * Created by daint on 5/23/2017.
 */

public class MqttClientHelper {
    private static final MqttClientHelper ourInstance = new MqttClientHelper();

    private static final String serverUri = "tcp://iot.eclipse.org:1883";
    private static String clientId = "daint";
    private static MqttAndroidClient mqttAndroidClient;

    private static ArrayList<IMqttClientListener> mqttClientListeners;

    private MqttClientHelper() {
    }

    public static MqttClientHelper getInstance(Context context) {
        if (mqttAndroidClient == null) {
            initMqttClient(context);
        }
        return ourInstance;
    }

    private static void initMqttClient(Context context) {
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId + System.currentTimeMillis());
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LogUtils.e("connectComplete -  reconnect: " + reconnect);
            }


            @Override
            public void connectionLost(Throwable cause) {
                LogUtils.e("connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        //add more info
        mqttConnectOptions.setConnectionTimeout(60);
        mqttConnectOptions.setKeepAliveInterval(60);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    LogUtils.e("connect: onSuccess");
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    LogUtils.e("connect: onFailure");
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    public interface IMqttClientListener {
        void onSuccess(IMqttToken asyncActionToken);

        void onFailure(IMqttToken asyncActionToken, Throwable exception);

        void messageArrived(String topic, MqttMessage message);
    }

    public void addListener(IMqttClientListener listener) {
        if (mqttClientListeners == null) {
            mqttClientListeners = new ArrayList<>();
        }
        mqttClientListeners.add(listener);
    }

    public void removeListener() {
        mqttClientListeners.remove(mqttClientListeners.size() - 1);
    }

    public void release() {
        try {
            mqttAndroidClient.unregisterResources();
            mqttAndroidClient.close();
            mqttAndroidClient.disconnect();
            mqttAndroidClient = null;

            mqttClientListeners.clear();
            mqttClientListeners = null;
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeToTopic(String topic) {
        try {
            mqttAndroidClient.unsubscribe(topic);
        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void unsubscribeToTopic(String topic, IMqttActionListener listener) {
        try {
            mqttAndroidClient.unsubscribe(topic, null, listener);
        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    for (IMqttClientListener listener : mqttClientListeners) {
                        listener.onSuccess(asyncActionToken);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    for (IMqttClientListener listener : mqttClientListeners) {
                        listener.onFailure(asyncActionToken, exception);
                    }
                }
            });

            mqttAndroidClient.subscribe(topic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    for (IMqttClientListener listener : mqttClientListeners) {
                        listener.messageArrived(topic, message);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(e.getMessage());
        }

    }

    public void publishMessage(String topic, String msg) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            mqttAndroidClient.publish(topic, message);
            if (!mqttAndroidClient.isConnected()) {
                LogUtils.e(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
            LogUtils.e(e.getMessage());
        }
    }

}
