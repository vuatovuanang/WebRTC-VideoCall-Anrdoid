package com.hiworld.webrtcvideocall.view.activity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hiworld.webrtcvideocall.R;
import com.hiworld.webrtcvideocall.utility.MqttClientHelper;
import com.hiworld.webrtcvideocall.utility.Constants;
import com.hiworld.webrtcvideocall.utility.LogUtils;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoCallActivity extends AppCompatActivity {

    @BindView(R.id.friendSurfaceView)
    GLSurfaceView friendSurfaceView;
    @BindView(R.id.btnCamera)
    ImageView btnCamera;
    @BindView(R.id.btnVoice)
    ImageView btnVoice;
    @BindView(R.id.btnChangeCam)
    ImageView btnChangeCam;
    @BindView(R.id.btnEndCall)
    ImageView btnEndCall;
    @BindView(R.id.tvFriendName)
    TextView tvFriendName;
    @BindView(R.id.tvDuration)
    TextView tvDuration;

    private String friendName;

    private static final String AUDIO_CODEC_OPUS = "opus";
    private static final String AUDIO_CODEC_ISAC = "ISAC";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
    private static final String VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03/Enabled/";
    private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String AUDIO_LEVEL_CONTROL_CONSTRAINT = "levelControl";
    private VideoSource videoSource;
    private VideoRenderer remoteVideoRender;
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String LOCAL_MEDIA_STREAM_ID = "ARDAMS";
    private MediaStream localStream;
    private VideoTrack localVideoTrack;
    private Boolean isFrontCam = true, enableCam = true, enableVoice = true;
    private AudioTrack localAudioTrack;
    private VideoCapturerAndroid videoCapture;
    private SessionDescription sdp;
    private VideoRenderer.Callbacks localRender;

    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private MediaConstraints pcConstraints = new MediaConstraints();


    @Override
    protected void onPause() {
        super.onPause();
        this.friendSurfaceView.onPause();
        this.videoSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.friendSurfaceView.onResume();
        this.videoSource.restart();
    }

    @Override
    protected void onDestroy() {
        endCall();
        if (this.videoSource != null) {
            this.videoSource.stop();
        }
        MqttClientHelper.getInstance(getApplicationContext()).removeListener();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        ButterKnife.bind(this);
        MqttClientHelper.getInstance(getApplicationContext()).addListener(new MqttClientListener());
        friendName = getIntent().getStringExtra("friend_name");
        tvFriendName.setText(friendName);
        initWebRtc();

    }

    private void initWebRtc() {
        // First, we initiate the PeerConnectionFactory with our application context and some options.
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true);// Hardware Acceleration Enabled

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();

        // Returns the number of cams & front/back face device name
        int camNumber = CameraEnumerationAndroid.getDeviceCount();
        String frontFacingCam = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
        String backFacingCam = CameraEnumerationAndroid.getNameOfBackFacingDevice();

        // Creates a VideoCapturerAndroid instance for the device name
        videoCapture = (VideoCapturerAndroid) VideoCapturerAndroid.create(frontFacingCam, new VideoCapturerAndroid.CameraEventsHandler() {
            @Override
            public void onCameraError(String s) {

            }

            @Override
            public void onCameraFreezed(String s) {

            }

            @Override
            public void onCameraOpening(int i) {

            }

            @Override
            public void onFirstFrameAvailable() {

            }

            @Override
            public void onCameraClosed() {

            }
        });

        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "720"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "1280"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "480"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "640"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "30"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", "30"));


        // First create a Video Source, then we can make a Video Track
        videoSource = pcFactory.createVideoSource(videoCapture, videoConstraints);
        localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

        // First we create an AudioSource then we can create our AudioTrack
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_LEVEL_CONTROL_CONSTRAINT, "true"));
        AudioSource audioSource = pcFactory.createAudioSource(audioConstraints);
        localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);


        // Then we set that view, and pass a Runnable to run once the surface is ready
        friendSurfaceView.setPreserveEGLContextOnPause(true);
        friendSurfaceView.setKeepScreenOn(true);
        VideoRendererGui.setView(friendSurfaceView, null);

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom
        VideoRenderer.Callbacks remoteRender = VideoRendererGui.create(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
        remoteVideoRender = new VideoRenderer(remoteRender);
        localRender = VideoRendererGui.create(72, 65, 25, 25, RendererCommon.ScalingType.SCALE_ASPECT_FIT, true);


        // We start out with an empty MediaStream object, created with help from our PeerConnectionFactory
        //  Note that LOCAL_MEDIA_STREAM_ID can be any string
        localStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        // Now we can add our tracks.
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);
        localVideoTrack.addRenderer(new VideoRenderer(localRender));

        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.services.mozilla.com"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.bistri.com:80", "homeo", "homeo"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.anyfirewall.com:443?transport=tcp", "webrtc", "webrtc"));

        // Extra Defaults - 19 STUN servers + 4 initial = 23 severs (+2 padding) = Array cap 25
        iceServers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("stun:stun01.sipphone.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ekiga.net"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.fwdnet.net"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ideasip.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.iptel.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.rixtelecom.se"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.schlund.de"));
        iceServers.add(new PeerConnection.IceServer("stun:stunserver.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.softjoys.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voiparound.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voipbuster.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voipstunt.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voxgratia.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.xten.com"));

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        peerConnection = pcFactory.createPeerConnection(iceServers, pcConstraints, peer);
        peerConnection.addStream(localStream);
        createAnswer();
    }


    private PeerConnection peerConnection;
    private Peer peer = new Peer();

    private class Peer implements SdpObserver, PeerConnection.Observer {
        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            LogUtils.e("onCreateSuccess: " + sdp.type.canonicalForm());
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);

                JSONObject message = new JSONObject();
                message.put("to", friendName);
                message.put("type", sdp.type.canonicalForm());
                message.put("payload", payload);

                MqttClientHelper.getInstance(getApplicationContext()).publishMessage(friendName, message.toString());

                peerConnection.setLocalDescription(Peer.this, sdp);
            } catch (JSONException e) {
                e.printStackTrace();
                LogUtils.e(e.getMessage());
            }
        }

        @Override
        public void onSetSuccess() {
            LogUtils.e("onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            LogUtils.e("onCreateFailure");
        }

        @Override
        public void onSetFailure(String s) {
            LogUtils.e("onSetFailure");
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            LogUtils.d("onSignalingChange");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            LogUtils.d("onIceConnectionChange");
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            LogUtils.d("onIceConnectionReceivingChange");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            LogUtils.d("onIceGatheringChange");
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("label", candidate.sdpMLineIndex);
                payload.put("id", candidate.sdpMid);
                payload.put("candidate", candidate.sdp);

                JSONObject message = new JSONObject();
                message.put("to", friendName);
                message.put("type", "candidate");
                message.put("payload", payload);

                MqttClientHelper.getInstance(getApplicationContext()).publishMessage(friendName, message.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onAddStream(MediaStream mediaStream) {
            LogUtils.e("onAddStream");
//            peerConnection.addStream(mediaStream);
            mediaStream.videoTracks.get(0).addRenderer(remoteVideoRender);
//            VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
//            VideoRendererGui.update(localRender, 72, 65, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            LogUtils.e("onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            LogUtils.e("onDataChannel");
        }

        @Override
        public void onRenegotiationNeeded() {
            LogUtils.e("onRenegotiationNeeded");
        }

    }


    @OnClick({R.id.btnCamera, R.id.btnVoice, R.id.btnChangeCam, R.id.btnEndCall})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnCamera:
                enableCam = !enableCam;
                btnCamera.setSelected(!enableCam);
                localVideoTrack.setEnabled(enableCam);
                break;
            case R.id.btnVoice:
                enableVoice = !enableVoice;
                btnVoice.setSelected(!enableVoice);
                localAudioTrack.setEnabled(enableVoice);
                break;
            case R.id.btnChangeCam:
                LogUtils.e("change cam");
                videoCapture.switchCamera(new VideoCapturerAndroid.CameraSwitchHandler() {
                    @Override
                    public void onCameraSwitchDone(final boolean b) {
                        LogUtils.e("is Front Camera: " + b);
                        VideoCallActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isFrontCam = b;
                                btnChangeCam.setSelected(!isFrontCam);
//                                VideoRendererGui.update(localRender, 72, 65, 25, 25, RendererCommon.ScalingType.SCALE_ASPECT_FIT, isFrontCam);
                            }
                        });

                    }

                    @Override
                    public void onCameraSwitchError(String s) {
                        LogUtils.e("onCameraSwitchError: ");
                    }
                });
                break;
            case R.id.btnEndCall:
                endCall();
                break;
        }
    }

    private void endCall() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("action", Constants.ACTION_END_CALL);
            MqttClientHelper.getInstance(getApplicationContext()).publishMessage(friendName, jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        finish();
    }

    private class MqttClientListener implements MqttClientHelper.IMqttClientListener {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            LogUtils.e("subscribeToTopic");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            LogUtils.e("onFailure");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            try {
                String msgContent = new String(message.getPayload());
                JSONObject jsonObject = new JSONObject(msgContent);
                if (jsonObject.has("action")) {
                    String action = jsonObject.getString("action");
                    if (action.equalsIgnoreCase(Constants.ACTION_ACCEPT_CALL)) {
                        LogUtils.e("Accept Call");
                        LogUtils.e("VideoCallActivity - messageArrived: " + new String(message.getPayload()));
                        peerConnection.createOffer(peer, pcConstraints);
                    } else if (action.equalsIgnoreCase(Constants.ACTION_REJECT_CALL)) {
                        VideoCallActivity.this.finish();
                    } else if (action.equalsIgnoreCase(Constants.ACTION_END_CALL)) {
                        endCall();
                    }
                } else {
                    if (jsonObject.has("type")) {
                        String type = jsonObject.getString("type");
                        if (type.equalsIgnoreCase("offer")) {
                            LogUtils.e("VideoCallActivity - messageArrived: " + new String(message.getPayload()));
                            JSONObject payloadObj = jsonObject.getJSONObject("payload");
                            sdp = new SessionDescription(
                                    SessionDescription.Type.fromCanonicalForm(payloadObj.getString("type")),
                                    payloadObj.getString("sdp")
                            );
                            createAnswer();
                        } else if (type.equalsIgnoreCase("answer")) {
                            LogUtils.e("VideoCallActivity - messageArrived: " + new String(message.getPayload()));
                            JSONObject payloadObj = jsonObject.getJSONObject("payload");
                            SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(payloadObj.getString("type")), payloadObj.getString("sdp"));

                            peerConnection.setRemoteDescription(peer, sdp);
                        } else if (type.equalsIgnoreCase("candidate")) {
                            if (peerConnection.getRemoteDescription() != null) {
                                JSONObject payloadObj = jsonObject.getJSONObject("payload");
                                IceCandidate candidate = new IceCandidate(
                                        payloadObj.getString("id"),
                                        payloadObj.getInt("label"),
                                        payloadObj.getString("candidate")
                                );
                                peerConnection.addIceCandidate(candidate);
                            } else {
                                LogUtils.e("Dai: candidate null");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(e.getMessage());
            }
        }
    }

    private void createAnswer() {
        if (peerConnection != null && sdp != null) {
            peerConnection.setRemoteDescription(peer, sdp);
            peerConnection.createAnswer(peer, pcConstraints);
        }
    }
}
