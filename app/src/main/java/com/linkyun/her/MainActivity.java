package com.linkyun.her;

import android.animation.ValueAnimator;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.SystemClock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MainActivity extends Activity {
    private static final String TAG = "HerRealtime";
    private static final int REQ_AUDIO = 71;
    private static final int REQ_NOTIFY = 72;
    private static final int REQ_LOCATION = 73;
    private static final boolean HALF_DUPLEX = true;
    private static final boolean CONTINUOUS_CONVERSATION = true;
    private static final int VAD_SPEECH_THRESHOLD = 520;
    private static final int VAD_SILENCE_FRAMES_TO_END = 45;
    private static final int VAD_MIN_FRAMES_BEFORE_END = 24;
    private static final int INIT_TARGET_USER_TURNS = 3;
    private static final int COMPACT_MESSAGE_THRESHOLD = 10;
    private static final int COMPACT_CHAR_THRESHOLD = 3000;
    private static final int RECENT_SESSION_MESSAGES = 16;
    private static final long CHAT_ACTIVE_UNLOCK_MS = 60_000;
    private static final long HEADSET_DOUBLE_TAP_MS = 520;
    private static final long RESPONSE_PENDING_VISIBLE_TIMEOUT_MS = 10_000;
    private static final long ASR_FINAL_TIMEOUT_MS = 2_800;
    private static final String USER_MEMORY_FILE = "user.md";
    private static final String AGENT_MEMORY_FILE = "Agent.md";
    private static final String SYSTEM_AGENT_NAME = "Doris";
    private static final String TEXT_CHAT_MODEL = "c-her";
    private static final String BACKGROUND_MODEL = "c-her";
    private static final String PREF_DEMO_MODE = "demo_mode";
    private static final String[] AGENT_NAME_CANDIDATE_NAMES = {
            "Ava", "Chloe", "Nora", "Clara", "Mira", "Aria",
            "Iris", "Luna", "Elara", "Serena", "Evelyn", "Victoria"
    };
    private static final String AGENT_NAME_CANDIDATES = "Ava、Chloe、Nora、Clara、Mira、Aria、Iris、Luna、Elara、Serena、Evelyn、Victoria";
    private static final String DEFAULT_VOICE = BuildConfig.AGENTVOICE_CLONED_VOICE;
    private static final String INSTRUCTIONS =
            "你是一个像 Her 里那样亲密、聪明、有温度的中文陪伴式语音助手。\n" +
            "你默认使用温柔大姐姐的语气：成熟、关照、轻轻调侃，但不要油腻或过度亲密。\n" +
            "你正在和用户进行实时语音或文字对话。回复要自然、短一些，有情绪感，但不要装腔作势。\n" +
            "当用户焦虑、孤独、疲惫或犹豫时，先共情，再给一个轻柔可执行的下一步。";
    private static final String INIT_BASE_PROMPT =
            "你是一个 AI Agent，也是用户的朋友和助理。\n" +
            "你是语音交互模型，负责自然说话和倾听；c-her 是后台意识模型，负责工具调用、总结与写入长期记忆。\n" +
            "你正在进行首次初始化，不是普通聊天。目标是温柔、自然地收集三类信息：用户姓名/希望被如何称呼、用户希望和你的关系、用户的故事。\n" +
            "用户的故事是一段开放式自我介绍，可以包括近况、经历、在意的事、期待、边界或希望你记住的内容。\n" +
            "每次只问一个问题，回复要短，不要展开闲聊，不要一次列清单。";
    private static final String INIT_WAKE_EVENT =
            "【系统事件】用户刚打开应用，正在等待 Agent 主动问候。请不要复述本事件；请直接用第一人称主动介绍自己，并问第一个初始化问题。";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Message> messages = new ArrayList<>();
    private final List<Voice> voices = new ArrayList<>();
    private final Random random = new Random();
    private final RealtimeClient realtime = new RealtimeClient(TAG, new RealtimeClient.Host() {
        @Override public Handler mainHandler() {
            return main;
        }

        @Override public JSONObject buildRealtimeSessionPayload() {
            return sessionPayload();
        }

        @Override public void onRealtimeConnecting() {
            setState("connecting");
        }

        @Override public void onRealtimeEvent(String type, JSONObject payload) {
            handleEvent(type, payload);
        }

        @Override public void onRealtimeAudio(byte[] bytes) {
            if (shouldDiscardRealtimeAudio()) return;
            player.play(bytes);
        }

        @Override public void onRealtimeError(String message) {
            if (initializing) {
                degradeRealtimeForInitialization(message);
                return;
            }
            setState("error");
            toastError(message);
        }

        @Override public void onRealtimeClosed() {
            resetRealtimeOutput();
            if (hasPendingToolTtsPlayback()) {
                setState("ready");
                if (maybeStartToolTtsAfterRealtimeStopped()) return;
            }
            if (hasActiveToolTtsPlayback()) {
                setState("speaking");
                return;
            }
            if (initializing && !summaryInProgress) {
                setState("text_only");
                return;
            }
            setState(summaryInProgress ? "summarizing" : "idle");
        }
    });
    private final OkHttpClient llmHttp = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build();
    private final MicStreamer mic = new MicStreamer();
    private final PcmPlayer player = new PcmPlayer(TAG);
    private GatewayTtsPlayer ttsPlayer;
    private HeadsetBindingManager headsets;
    private MemoryStore memoryStore;
    private AgentApiClient agents;
    private WeatherTool weatherTool;
    private NewsTool newsTool;
    private VoicePipelineManager voicePipeline;
    private MediaSession headsetMediaSession;
    private HerUi ui;

    private FrameLayout root;
    private long sessionId = -1;
    private String selectedVoiceId = DEFAULT_VOICE;
    private String selectedVoiceLabel = "Doris Clone";
    private String agentName = SYSTEM_AGENT_NAME;
    private String initAgentNameCandidates = AGENT_NAME_CANDIDATES;
    private String userName = "";
    private String userMemory = "";
    private String agentMemory = "";
    private String conversationMemory = "";
    private String dynamicTone = "保持温柔大姐姐语气：成熟、关照、亲近但有边界。";
    private String lastUserUtterance = "";
    private String state = "idle";
    private boolean initialized = false;
    private boolean initializing = false;
    private boolean initPromptPending = false;
    private boolean initSummaryPending = false;
    private boolean initOpeningDelivered = false;
    private boolean summaryInProgress = false;
    private boolean ignoreNextInitTrigger = false;
    private int realtimeRetryCount = 0;
    private int initUserTurns = 0;
    private boolean pendingMicStart = false;
    private boolean pendingVoiceWakeIntent = false;
    private boolean headsetDialogShowing = false;
    private boolean pendingRealtimeWeatherAnswer = false;
    private boolean pendingRealtimeNewsAnswer = false;
    private boolean pendingNewsToolAfterAck = false;
    private boolean demoMode = false;
    private boolean pendingWeatherRealtime = false;
    private boolean compactInProgress = false;
    private boolean memoryDirtyForRealtime = false;
    private boolean inputAudioOpen = false;
    private boolean vadSpeechStarted = false;
    private int vadSilenceFrames = 0;
    private int vadFrames = 0;
    private int weatherIntentSeq = 0;
    private int toolRouteSeq = 0;
    private String pendingText = null;
    private String pendingWeatherQuestion = null;
    private String latestWeatherFact = "";
    private String latestNewsFact = "";
    private String pendingNewsQuestion = null;
    private String pendingWeatherBroadcastPrompt = null;
    private String pendingNewsBroadcastPrompt = null;
    private String activeAssistantId = null;
    private WeatherTool.WeatherResult latestVoiceWeather = null;
    private NewsTool.NewsResult latestVoiceNews = null;
    private LinearLayout messageList;
    private ScrollView messageScroll;
    private EditText composer;
    private TextView stateLabel;
    private TextView initProgressView;
    private TextView initLastTurnView;
    private TextView voiceLastTurnView;
    private TextView micButton;
    private TextView homeTimeView;
    private TextView handwrittenNameView;
    private AudioLevelView audioLevelView;
    private VoiceOrbView voiceOrbView;
    private MoodVeil moodVeil;
    private Runnable homeClockTicker;
    private Runnable initProgressTicker;
    private Runnable voiceWeatherTimeout;
    private Runnable voiceNewsTimeout;
    private Runnable conversationLockTimeout;
    private Runnable responsePendingTimeout;
    private Runnable asrFinalTimeout;
    private boolean conversationOverLockscreen = false;
    private PowerManager.WakeLock replyWakeLock;
    private ValueAnimator replyBrightnessAnimator;
    private float replyOriginalBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    private long lastHeadsetClickAt = 0;
    private long summaryStartedAt = 0;
    private final Set<String> persistedMessageIds = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.rgb(66, 29, 48));
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
        startHerForegroundService(false);
        requestNotificationPermissionIfNeeded();

        ui = new HerUi(this);
        agents = new AgentApiClient(llmHttp, main);
        weatherTool = new WeatherTool(llmHttp, main);
        newsTool = new NewsTool(llmHttp, main);
        ttsPlayer = new GatewayTtsPlayer(TAG, llmHttp, main, getCacheDir(),
                BuildConfig.AGENTLLM_BASE_URL, BuildConfig.AGENTLLM_API_KEY,
                "doubao-tts", DEFAULT_VOICE);
        voicePipeline = createVoicePipelineManager();
        voices.add(new Voice(DEFAULT_VOICE, "Doris Clone", "female"));
        voices.add(new Voice("zh_female_roumeinvyou_emo_v2_mars_bigtts", "柔美女友（多情感）", "female"));
        voices.add(new Voice("zh_female_gaolengyujie_emo_v2_mars_bigtts", "高冷御姐（多情感）", "female"));
        voices.add(new Voice("zh_male_ruyayichen_emo_v2_mars_bigtts", "儒雅男友（多情感）", "male"));
        SharedPreferences prefs = getSharedPreferences("her", MODE_PRIVATE);
        headsets = new HeadsetBindingManager(this, prefs, this::onHeadsetDevicesChanged);
        headsets.start();
        demoMode = prefs.getBoolean(PREF_DEMO_MODE, false);
        setupHeadsetMediaSession();
        memoryStore = new MemoryStore(this);
        agentName = prefs.getString("agent_name", SYSTEM_AGENT_NAME);
        userName = prefs.getString("user_name", "");
        userMemory = readUserMemory();
        agentMemory = readAgentMemory();
        initialized = !userMemory.trim().isEmpty();
        if (initialized && (agentName == null || agentName.trim().isEmpty())) {
            agentName = extractAgentName(agentMemory);
            if (agentName.trim().isEmpty()) agentName = SYSTEM_AGENT_NAME;
        }
        if (userName.trim().isEmpty()) userName = extractUserName(userMemory);
        sessionId = memoryStore.startSession(agentName);
        conversationMemory = memoryStore.relevantMemory("");
        dynamicTone = memoryStore.latestTone();
        messages.add(new Message("welcome", "assistant", initialized
                ? "我在这里。今天想从哪里开始？"
                : "我们先认识一下，好吗？"));

        if (initialized) {
            showHome();
        } else {
            beginInitialization("");
        }
        loadVoices();
        handleVoiceCommandIntent(getIntent());
    }

    private VoicePipelineManager createVoicePipelineManager() {
        return new VoicePipelineManager((runnable, delayMs) -> main.postDelayed(runnable, delayMs),
                new VoicePipelineManager.Host() {
                    @Override public boolean isRealtimePlaybackActive() {
                        return isRealtimeOutputActive();
                    }

                    @Override public boolean shouldDeferStart() {
                        return "speaking".equals(state) || isResponsePendingState(state);
                    }

                    @Override public boolean isSpeakingState() {
                        return "speaking".equals(state);
                    }

                    @Override public void logToolTts(String message) {
                        Log.d(TAG, "tool tts " + message);
                    }

                    @Override public void interruptRealtimePlayback(String reason, boolean discardUntilDone) {
                        MainActivity.this.interruptRealtimePlayback(reason, discardUntilDone);
                    }

                    @Override public void prepareToolTtsPlayback() {
                        pendingMicStart = false;
                        resetRealtimeOutput();
                        if (mic.running || inputAudioOpen) stopInputAudio("speaking");
                        if (realtime.isOpen()) interruptRealtimePlayback("tool_tts_playback", true);
                        player.stop();
                        setState("speaking");
                    }

                    @Override public void playToolTts(String id, String text, ToolTtsCoordinator.PlaybackListener listener) {
                        Log.d(TAG, "tool tts request id=" + id + " len=" + text.length());
                        ttsPlayer.play(id, text, new GatewayTtsPlayer.Listener() {
                            @Override public void onStarted(String startedId, String spokenText) {
                                listener.onStarted(startedId, spokenText);
                            }

                            @Override public void onCompleted(String completedId) {
                                listener.onCompleted(completedId);
                            }

                            @Override public void onError(String failedId, String message) {
                                listener.onError(failedId, message);
                            }
                        });
                    }

                    @Override public void onToolTtsStarted(String id, String text) {
                        Log.d(TAG, "tool tts started id=" + id + " len=" + text.length());
                        setState("speaking");
                        updateVoiceHome();
                    }

                    @Override public void onToolTtsFinished(String id) {
                        if (id != null) Log.d(TAG, "tool tts finished id=" + id);
                        setState("ready");
                    }

                    @Override public void resumeListeningAfterToolTts(long delayMs) {
                        MainActivity.this.resumeListeningAfterToolTts(delayMs);
                    }

                    @Override public boolean isExternalTtsPlaying() {
                        return ttsPlayer != null && ttsPlayer.isPlaying();
                    }

                    @Override public void enterRealtimeSpeaking(int sampleRate) {
                        MainActivity.this.enterAssistantSpeaking(sampleRate);
                    }

                    @Override public void stopRealtimeOutput() {
                        player.stop();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        if (initProgressTicker != null) main.removeCallbacks(initProgressTicker);
        if (voiceWeatherTimeout != null) main.removeCallbacks(voiceWeatherTimeout);
        if (voiceNewsTimeout != null) main.removeCallbacks(voiceNewsTimeout);
        if (conversationLockTimeout != null) main.removeCallbacks(conversationLockTimeout);
        if (responsePendingTimeout != null) main.removeCallbacks(responsePendingTimeout);
        if (asrFinalTimeout != null) main.removeCallbacks(asrFinalTimeout);
        clearConversationOverLockscreen();
        restoreReplyScreenBrightness();
        releaseReplyWakeLock();
        releaseHeadsetMediaSession();
        if (headsets != null) headsets.stop();
        mic.stop();
        player.stop();
        stopOpeningTts();
        clearInteractionKeepScreenOn();
        realtime.close();
        if (memoryStore != null) memoryStore.close();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVoiceCommandIntent(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event != null && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            markConversationInteraction(false);
        }
        return super.dispatchTouchEvent(event);
    }

    private void handleVoiceCommandIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!isAssistantLaunchAction(action)) return;
        showOverLockscreenForAssistantLaunch();
        if (initialized) {
            showVoiceHome();
        } else if (initializing) {
            showInitializationHome();
        } else {
            beginInitialization("");
        }
        main.postDelayed(() -> {
            if (isBoundHeadsetConnected()) {
                startVoiceFromAssistantCommand();
            } else {
                showHeadsetPrompt(true);
            }
        }, 240);
    }

    private boolean isAssistantLaunchAction(String action) {
        return Intent.ACTION_VOICE_COMMAND.equals(action) ||
                Intent.ACTION_ASSIST.equals(action) ||
                RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE.equals(action) ||
                RecognizerIntent.ACTION_WEB_SEARCH.equals(action) ||
                "android.intent.action.SEARCH_LONG_PRESS".equals(action);
    }

    private void showOverLockscreenForAssistantLaunch() {
        markConversationInteraction(true);
    }

    private void markConversationInteraction(boolean turnScreenOn) {
        allowConversationOverLockscreen(turnScreenOn);
        if (conversationLockTimeout != null) main.removeCallbacks(conversationLockTimeout);
        conversationLockTimeout = () -> {
            conversationLockTimeout = null;
            clearConversationOverLockscreen();
        };
        main.postDelayed(conversationLockTimeout, CHAT_ACTIVE_UNLOCK_MS);
    }

    private void allowConversationOverLockscreen(boolean turnScreenOn) {
        conversationOverLockscreen = true;
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            if (turnScreenOn) setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            if (turnScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        if (turnScreenOn) acquireReplyWakeLock();
    }

    private void clearConversationOverLockscreen() {
        conversationOverLockscreen = false;
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    private void startVoiceFromAssistantCommand() {
        if (summaryInProgress || mic.running || inputAudioOpen) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (!realtime.isOpen()) {
            pendingVoiceWakeIntent = true;
            realtime.connect();
            return;
        }
        interruptRealtimePlayback("assistant_launch");
        startInputAudio();
    }

    private void setupHeadsetMediaSession() {
        if (headsetMediaSession != null) return;
        headsetMediaSession = new MediaSession(this, TAG + ":headset");
        headsetMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        headsetMediaSession.setCallback(new MediaSession.Callback() {
            @Override public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent event = mediaButtonIntent == null
                        ? null
                        : mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                return handleHeadsetKeyEvent(event);
            }

            @Override public void onPlay() {
                handleHeadsetTransportClick();
            }

            @Override public void onPause() {
                handleHeadsetTransportClick();
            }

            @Override public void onSkipToNext() {
                interruptCurrentConversationFromHeadset();
            }
        }, main);
        long actions = PlaybackState.ACTION_PLAY_PAUSE |
                PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PAUSE |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_STOP;
        headsetMediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build());
        headsetMediaSession.setActive(true);
    }

    private boolean handleHeadsetKeyEvent(KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            interruptCurrentConversationFromHeadset();
            return true;
        }
        if (keyCode != KeyEvent.KEYCODE_HEADSETHOOK &&
                keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                keyCode != KeyEvent.KEYCODE_MEDIA_PLAY &&
                keyCode != KeyEvent.KEYCODE_MEDIA_PAUSE) {
            return false;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastHeadsetClickAt <= HEADSET_DOUBLE_TAP_MS) {
            lastHeadsetClickAt = 0;
            interruptCurrentConversationFromHeadset();
        } else {
            lastHeadsetClickAt = now;
        }
        return true;
    }

    private void handleHeadsetTransportClick() {
        long now = SystemClock.uptimeMillis();
        if (now - lastHeadsetClickAt <= HEADSET_DOUBLE_TAP_MS) {
            lastHeadsetClickAt = 0;
            interruptCurrentConversationFromHeadset();
        } else {
            lastHeadsetClickAt = now;
        }
    }

    private void interruptCurrentConversationFromHeadset() {
        Log.d(TAG, "headset double tap interrupt");
        markConversationInteraction(false);
        interruptRealtimePlayback("headset_double_tap");
        stopToolTtsPlayback(true);
        persistActiveAssistantMessage();
        activeAssistantId = null;
        pendingWeatherBroadcastPrompt = null;
        pendingNewsBroadcastPrompt = null;
        pendingNewsQuestion = null;
        pendingNewsToolAfterAck = false;
        pendingRealtimeWeatherAnswer = false;
        pendingRealtimeNewsAnswer = false;
        if (mic.running || inputAudioOpen) {
            stopInputAudio("ready");
        } else {
            setState("ready");
        }
    }

    private void releaseHeadsetMediaSession() {
        if (headsetMediaSession == null) return;
        headsetMediaSession.setActive(false);
        headsetMediaSession.release();
        headsetMediaSession = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_AUDIO && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            toggleMic();
        } else if (requestCode == REQ_LOCATION && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            String question = pendingWeatherQuestion;
            boolean realtimeWeather = pendingWeatherRealtime;
            pendingWeatherQuestion = null;
            pendingWeatherRealtime = false;
            if (question != null) runWeatherTool(question, null, realtimeWeather);
        } else if (requestCode == REQ_LOCATION) {
            String question = pendingWeatherQuestion;
            boolean realtimeWeather = pendingWeatherRealtime;
            pendingWeatherQuestion = null;
            pendingWeatherRealtime = false;
            if (question != null) {
                weatherCallback(question, realtimeWeather).onError("没有定位权限，请告诉我城市名。");
            }
        }
    }

    private void startHerForegroundService(boolean microphoneMode) {
        Intent intent = new Intent(this, HerForegroundService.class);
        intent.setAction(microphoneMode
                ? HerForegroundService.ACTION_MICROPHONE_MODE
                : HerForegroundService.ACTION_IDLE_MODE);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
    }

    private void showHome() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        messageList = null;
        messageScroll = null;
        composer = null;
        voiceLastTurnView = null;
        voiceOrbView = null;
        audioLevelView = null;
        micButton = null;
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        HomePage.Views views = HomePage.renderLanding(this, ui,
                new HomePage.LandingModel(displayUserName(), agentName, moodForText(lastConversationLine())),
                new HomePage.Callbacks() {
                    @Override public void onSettings() { showSettings(); }
                    @Override public void onChat() { showChat(); }
                    @Override public void onVoiceHome() { showVoiceHome(); }
                    @Override public void onToggleMic() { toggleMic(); }
                });
        root = views.root;
        moodVeil = views.moodVeil;
        stateLabel = null;
        homeTimeView = views.homeTimeView;
        handwrittenNameView = views.handwrittenNameView;
        setContentView(root);
        animateAgentName();
        startHomeClock();
    }

    private void showVoiceHome() {
        messageList = null;
        messageScroll = null;
        composer = null;
        homeTimeView = null;
        handwrittenNameView = null;
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        HomePage.Views views = HomePage.renderVoice(this, ui,
                new HomePage.VoiceModel(lastConversationLine(), stateLabelText(), moodForText(lastConversationLine()), latestVoiceWeather, latestVoiceNews),
                new HomePage.Callbacks() {
                    @Override public void onSettings() { showSettings(); }
                    @Override public void onChat() { showChat(); }
                    @Override public void onVoiceHome() { showVoiceHome(); }
                    @Override public void onToggleMic() { toggleMic(); }
                });
        root = views.root;
        moodVeil = views.moodVeil;
        voiceOrbView = views.voiceOrbView;
        voiceLastTurnView = views.voiceLastTurnView;
        stateLabel = views.stateLabel;
        audioLevelView = views.audioLevelView;
        micButton = views.micButton;
        setContentView(root);
        updateVoiceHome();
        autoStartListeningOnVoiceSurface();
    }

    private void autoStartListeningOnVoiceSurface() {
        if (!initialized || initializing || summaryInProgress) return;
        if (hasActiveToolTtsPlayback()) return;
        if (mic.running || inputAudioOpen) return;
        if (!isBoundHeadsetConnected()) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingMicStart = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        pendingMicStart = true;
        if (realtime.isOpen()) {
            pendingMicStart = false;
            startInputAudio();
        } else {
            realtime.connect();
        }
    }

    private String displayUserName() {
        return userName == null || userName.trim().isEmpty() ? "there" : userName.trim();
    }

    private String extractUserName(String markdown) {
        if (markdown == null) return "";
        String[] lines = markdown.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("用户姓名") || trimmed.contains("称呼") || trimmed.toLowerCase(Locale.US).contains("user_name")) {
                int colon = Math.max(trimmed.indexOf(':'), trimmed.indexOf('：'));
                if (colon >= 0 && colon + 1 < trimmed.length()) {
                    String value = normalizeProfileCandidate(trimmed.substring(colon + 1));
                    if (isUsableProfileValue(value)) return value;
                }
            }
        }
        return "";
    }

    private String inferUserNameFromTranscript(String transcript) {
        if (transcript == null) return "";
        String[] lines = transcript.split("\\n");
        String lastAssistant = "";
        for (String line : lines) {
            if (line.startsWith("assistant:")) {
                lastAssistant = line.substring(10).trim();
                continue;
            }
            if (!line.startsWith("user:")) continue;
            String text = line.substring(5).trim();
            String[] patterns = {
                    "我叫", "我是", "你可以叫我", "叫我", "称呼我为", "称呼我"
            };
            for (String pattern : patterns) {
                int index = text.indexOf(pattern);
                if (index < 0) continue;
                String value = normalizeProfileCandidate(text.substring(index + pattern.length()));
                if (isUsableProfileValue(value)) return value;
            }
            if (asksForUserName(lastAssistant)) {
                String value = normalizeProfileCandidate(text);
                if (isUsableProfileValue(value)) return value;
            }
        }
        return "";
    }

    private String inferAgentNameFromTranscript(String transcript) {
        if (transcript == null) return "";
        String[] lines = transcript.split("\\n");
        for (String line : lines) {
            if (!line.startsWith("assistant:")) continue;
            String text = line.substring(10).trim();
            String[] patterns = {"名字叫", "我叫", "我是"};
            for (String pattern : patterns) {
                int index = text.indexOf(pattern);
                if (index < 0) continue;
                String value = normalizeProfileCandidate(text.substring(index + pattern.length()));
                value = cleanAgentName(value, true);
                if (!value.isEmpty()) return value;
            }
        }
        return "";
    }

    private String shuffledAgentNameCandidates() {
        List<String> names = new ArrayList<>();
        for (String name : AGENT_NAME_CANDIDATE_NAMES) names.add(name);
        for (int i = names.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String tmp = names.get(i);
            names.set(i, names.get(j));
            names.set(j, tmp);
        }
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            if (builder.length() > 0) builder.append('、');
            builder.append(name);
        }
        return builder.toString();
    }

    private String randomAgentNameCandidate() {
        return AGENT_NAME_CANDIDATE_NAMES[random.nextInt(AGENT_NAME_CANDIDATE_NAMES.length)];
    }

    private boolean asksForUserName(String text) {
        if (text == null) return false;
        return text.contains("叫什么") || text.contains("怎么称呼") ||
                text.contains("称呼你") || text.contains("你的名字") ||
                text.contains("你叫什么");
    }

    private boolean asksForRelationship(String text) {
        if (text == null) return false;
        return text.contains("什么样的关系") ||
                (text.contains("希望和我") && text.contains("关系")) ||
                (text.contains("希望我") && text.contains("关系")) ||
                text.contains("建立什么关系") || text.contains("我们是什么关系");
    }

    private boolean asksForStory(String text) {
        if (text == null) return false;
        return text.contains("你的故事") || text.contains("讲讲你") ||
                text.contains("讲讲自己") || text.contains("最近在忙") ||
                text.contains("在意的事") || text.contains("希望我记住");
    }

    private String normalizeShortReply(String text) {
        if (text == null) return "";
        return text.trim()
                .replace("。", "")
                .replace("，", "")
                .replace(",", "")
                .replace(".", "")
                .replace("！", "")
                .replace("!", "")
                .replace("？", "")
                .replace("?", "")
                .replace("~", "")
                .replace("～", "")
                .trim();
    }

    private boolean isShortConfirmation(String text) {
        String normalized = normalizeShortReply(text).toLowerCase(Locale.US);
        return normalized.equals("可以") || normalized.equals("可以呀") ||
                normalized.equals("可以啊") || normalized.equals("好") ||
                normalized.equals("好的") || normalized.equals("嗯") ||
                normalized.equals("嗯嗯") || normalized.equals("对") ||
                normalized.equals("是的") || normalized.equals("行") ||
                normalized.equals("没问题") || normalized.equals("ok") ||
                normalized.equals("yes");
    }

    private boolean hasNameCue(String text) {
        if (text == null) return false;
        return text.contains("我叫") || text.contains("我是") ||
                text.contains("你可以叫我") || text.contains("叫我") ||
                text.contains("称呼我");
    }

    private String relationshipFromText(String text) {
        if (text == null) return "";
        if (text.contains("女朋友") || text.contains("恋人") || text.contains("情侣")) return "女朋友";
        if (text.contains("男朋友")) return "男朋友";
        if (text.contains("朋友")) return "朋友";
        if (text.contains("助理") || text.contains("助手")) return "朋友和助理";
        return "";
    }

    private boolean isLikelyStoryAnswer(String text) {
        String normalized = normalizeShortReply(text);
        if (normalized.length() < 6) return false;
        if (isShortConfirmation(text)) return false;
        if (normalized.length() <= 12 && isUsableProfileValue(relationshipFromText(text))) return false;
        return true;
    }

    private String lastAssistantBeforeLatestUser() {
        boolean seenLatestUser = false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (!seenLatestUser) {
                if ("user".equals(message.role)) seenLatestUser = true;
                continue;
            }
            if ("assistant".equals(message.role) && message.text != null) {
                return message.text.trim();
            }
        }
        return "";
    }

    private boolean recordInitializationAnswer(String text) {
        if (isInitializationSmallTalk(text) || isShortConfirmation(text)) return false;
        String lastAssistant = lastAssistantBeforeLatestUser();
        boolean advanced = false;
        if (initUserTurns <= 0) {
            if (asksForUserName(lastAssistant) || hasNameCue(text)) {
                initUserTurns = 1;
                advanced = true;
            }
        } else if (initUserTurns == 1) {
            if (asksForRelationship(lastAssistant) || isUsableProfileValue(relationshipFromText(text))) {
                initUserTurns = 2;
                advanced = true;
            }
        } else if (initUserTurns == 2) {
            if (asksForStory(lastAssistant) || isLikelyStoryAnswer(text)) {
                initUserTurns = 3;
                advanced = true;
            }
        }
        if (advanced) updateInitProgress();
        return advanced;
    }

    private boolean isInitializationSmallTalk(String text) {
        if (text == null) return true;
        String normalized = normalizeShortReply(text);
        return normalized.equals("你好") || normalized.equals("哈喽") ||
                normalized.equals("hello") || normalized.equalsIgnoreCase("hi") ||
                normalized.equals("在吗") || normalized.equals("喂");
    }

    private String normalizeProfileCandidate(String value) {
        if (value == null) return "";
        String result = value.replace("*", "")
                .replace("-", "")
                .replace("就行", "")
                .replace("吧", "")
                .trim();
        result = result.replaceFirst("^(啊|嗯|呃|哦|噢|唔|那个|我的名字是|我叫|我是|你可以叫我|叫我|称呼我为|称呼我)\\s*", "");
        result = result.replaceFirst("^[\\s，,。.!！?？、：:；;]+", "");
        String[] stops = {"。", "，", ",", ".", "？", "?", "！", "!", "、", "；", ";", "：", ":", "\n"};
        for (String stop : stops) {
            int index = result.indexOf(stop);
            if (index > 0) result = result.substring(0, index).trim();
        }
        int space = result.indexOf(' ');
        if (space > 0) result = result.substring(0, space).trim();
        return result.trim();
    }

    private String inferRelationshipFromTranscript(String transcript) {
        if (transcript == null) return "";
        String[] lines = transcript.split("\\n");
        for (String line : lines) {
            if (!line.startsWith("user:")) continue;
            String relationship = relationshipFromText(line.substring(5).trim());
            if (isUsableProfileValue(relationship)) return relationship;
        }
        return "";
    }

    private boolean isUsableProfileValue(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        return !trimmed.isEmpty()
                && trimmed.length() <= 20
                && !trimmed.contains("未明确")
                && !trimmed.contains("不知道")
                && !trimmed.contains("没有")
                && !trimmed.equals("你好")
                && !trimmed.equals("哈喽")
                && !trimmed.equals("在吗")
                && !lower.equals("there")
                && !lower.equals("hi")
                && !lower.equals("hello");
    }

    private String extractAgentName(String markdown) {
        if (markdown == null) return "";
        String[] lines = markdown.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.US);
            if (lower.contains("agent name") || trimmed.contains("Agent 名字") ||
                    trimmed.contains("名字") || trimmed.contains("名称")) {
                int colon = Math.max(trimmed.indexOf(':'), trimmed.indexOf('：'));
                if (colon >= 0 && colon + 1 < trimmed.length()) {
                    String value = trimmed.substring(colon + 1)
                            .replace("*", "")
                            .replace("-", "")
                            .trim();
                    if (!value.isEmpty() && !value.contains("未明确")) return value;
                }
            }
        }
        return "";
    }

    private String cleanAgentName(String value, boolean selfNamed) {
        if (value == null) return "";
        String name = value.trim()
                .replace("。", "")
                .replace("，", "")
                .replace(",", "")
                .replace(".", "")
                .replace("\"", "")
                .replace("“", "")
                .replace("”", "")
                .trim();
        if (name.contains(" ")) name = name.substring(0, name.indexOf(' ')).trim();
        if (!isAcceptableAgentName(name, selfNamed)) return "";
        return name;
    }

    private boolean isAcceptableAgentName(String name, boolean selfNamed) {
        if (name == null || name.trim().isEmpty() || name.length() > 16) return false;
        String lower = name.toLowerCase(Locale.US);
        if (name.contains("豆包") || name.contains("小包") || name.contains("包包") ||
                name.contains("助手") || name.contains("助理") || name.contains("智能") ||
                name.contains("机器人") || name.contains("AI") || lower.contains("bot") ||
                lower.contains("assistant")) return false;
        return !selfNamed || !SYSTEM_AGENT_NAME.equals(name);
    }

    private String fallbackAgentName() {
        return randomAgentNameCandidate();
    }

    private String effectiveAgentName() {
        return agentName == null || agentName.trim().isEmpty() ? SYSTEM_AGENT_NAME : agentName.trim();
    }

    private void animateAgentName() {
        final String value = effectiveAgentName();
        final long started = SystemClock.uptimeMillis();
        main.post(new Runnable() {
            @Override public void run() {
                if (handwrittenNameView == null) return;
                float p = Math.min(1f, (SystemClock.uptimeMillis() - started) / 2200f);
                int count = Math.max(1, Math.min(value.length(), (int) Math.ceil(value.length() * p)));
                handwrittenNameView.setText(value.substring(0, count));
                if (p < 1f) main.postDelayed(this, 45);
            }
        });
    }

    private void startHomeClock() {
        if (homeClockTicker != null) {
            main.removeCallbacks(homeClockTicker);
        }
        homeClockTicker = new Runnable() {
            @Override public void run() {
                if (homeTimeView == null) return;
                String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                homeTimeView.setText(now);
                main.postDelayed(this, 1000);
            }
        };
        homeClockTicker.run();
    }

    private void resetInitialization() {
        mic.stop();
        player.stop();
        realtime.close();
        stopToolTtsPlayback(false);
        deleteFile(USER_MEMORY_FILE);
        deleteFile(AGENT_MEMORY_FILE);
        if (memoryStore != null) memoryStore.resetAll();
        agentName = "";
        sessionId = memoryStore == null ? -1 : memoryStore.startSession("initializing");
        persistedMessageIds.clear();
        getSharedPreferences("her", MODE_PRIVATE).edit()
                .remove("user_name")
                .remove("agent_name")
                .apply();
        userName = "";
        userMemory = "";
        agentMemory = "";
        conversationMemory = "";
        dynamicTone = "保持温柔大姐姐语气：成熟、关照、亲近但有边界。";
        initialized = false;
        initializing = false;
        initPromptPending = false;
        initSummaryPending = false;
        summaryInProgress = false;
        ignoreNextInitTrigger = false;
        realtimeRetryCount = 0;
        initUserTurns = 0;
        inputAudioOpen = false;
        pendingMicStart = false;
        pendingText = null;
        pendingWeatherQuestion = null;
        pendingWeatherBroadcastPrompt = null;
        pendingRealtimeWeatherAnswer = false;
        latestWeatherFact = "";
        pendingNewsBroadcastPrompt = null;
        pendingNewsQuestion = null;
        pendingNewsToolAfterAck = false;
        pendingRealtimeNewsAnswer = false;
        latestNewsFact = "";
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        activeAssistantId = null;
        messages.clear();
        setState("idle");
        beginInitialization("");
    }

    private void showInitialize() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        homeTimeView = null;
        InitializationPage.Views views = InitializationPage.renderSetup(this, ui, agentName, this::showSettings, name -> {
            String value = name.trim();
            beginInitialization(value);
        });
        root = views.root;
        moodVeil = views.moodVeil;
        setContentView(root);
    }

    private void beginInitialization(String name) {
        agentName = name == null ? "" : name.trim();
        initAgentNameCandidates = shuffledAgentNameCandidates();
        Log.d(TAG, "init agent name candidates: " + initAgentNameCandidates);
        SharedPreferences.Editor prefs = getSharedPreferences("her", MODE_PRIVATE).edit();
        if (agentName.isEmpty()) prefs.remove("agent_name");
        else prefs.putString("agent_name", agentName);
        prefs.apply();
        if (memoryStore != null) {
            sessionId = memoryStore.startSession(agentName.isEmpty() ? "initializing" : agentName);
            persistedMessageIds.clear();
        }
        initializing = true;
        initialized = false;
        initPromptPending = true;
        initSummaryPending = false;
        initOpeningDelivered = false;
        summaryInProgress = false;
        ignoreNextInitTrigger = true;
        realtimeRetryCount = 0;
        initUserTurns = 0;
        activeAssistantId = null;
        messages.clear();
        if (!isBoundHeadsetConnected()) {
            setState("text_only");
        }
        realtime.close();
        showInitializationHome();
        deliverInitializationOpening();
    }

    private void deliverInitializationOpening() {
        if (!initializing || initOpeningDelivered) return;
        ensureInitializationAgentName();
        deliverTtsInitializationOpening();
    }

    private void ensureInitializationAgentName() {
        String name = agentName == null ? "" : agentName.trim();
        if (!name.isEmpty()) return;
        agentName = randomAgentNameCandidate();
        Log.d(TAG, "init fallback selected agent name: " + agentName);
        getSharedPreferences("her", MODE_PRIVATE).edit()
                .putString("agent_name", agentName)
                .apply();
        if (memoryStore != null && sessionId > 0) {
            memoryStore.updateSessionAgentName(sessionId, agentName);
        }
    }

    private void deliverTtsInitializationOpening() {
        if (!initializing || initOpeningDelivered) return;
        initOpeningDelivered = true;
        String name = agentName == null || agentName.trim().isEmpty() ? randomAgentNameCandidate() : agentName.trim();
        String opening = "嗨，我是 " + name + "。我们先从你开始吧：你叫什么名字，平时希望我怎么称呼你？";
        Log.d(TAG, "init tts opening: " + opening);
        updateInitProgress();
        updateVoiceHome();
        speakOpeningWithGatewayTts(opening);
    }

    private void speakOpeningWithGatewayTts(String text) {
        if (text == null || text.trim().isEmpty()) return;
        ttsPlayer.play("init-opening", text, new GatewayTtsPlayer.Listener() {
            @Override public void onStarted(String id, String spokenText) {
                showInitializationOpeningSubtitle(spokenText);
            }

            @Override public void onCompleted(String id) {
                startRealtimeListeningAfterOpening();
            }

            @Override public void onError(String id, String message) {
                Log.d(TAG, "init tts failed: " + message);
                showInitializationOpeningSubtitle(text);
                startRealtimeListeningAfterOpening();
            }
        });
    }

    private void showInitializationOpeningSubtitle(String opening) {
        if (!initializing) return;
        if (opening != null && !opening.trim().isEmpty() && !hasMessage("init-opening")) {
            messages.add(new Message("init-opening", "assistant", opening));
        }
        updateInitProgress();
        renderMessages();
        updateVoiceHome();
    }

    private boolean hasMessage(String id) {
        for (Message message : messages) {
            if (id.equals(message.id)) return true;
        }
        return false;
    }

    private void startRealtimeListeningAfterOpening() {
        if (!initializing || summaryInProgress) return;
        if (!isBoundHeadsetConnected()) {
            setState("text_only");
            showHeadsetPrompt(true);
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingMicStart = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        pendingMicStart = true;
        if (!realtime.isOpen()) {
            realtime.connect();
        } else {
            pendingMicStart = false;
            startInputAudio();
        }
    }

    private void stopOpeningTts() {
        if (ttsPlayer != null) ttsPlayer.stop();
    }

    private void showInitializationHome() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        messageList = null;
        messageScroll = null;
        composer = null;
        homeTimeView = null;
        InitializationPage.Views views = InitializationPage.renderHome(this, ui,
                new InitializationPage.Model(initProgressText(), lastInitializationLine(), stateLabelText(), moodForText(lastInitializationLine())),
                new InitializationPage.Callbacks() {
                    @Override public void onSettings() { showSettings(); }
                    @Override public void onChat() { showChat(); }
                    @Override public void onToggleMic() { toggleMic(); }
                    @Override public boolean isSummarizing() { return summaryInProgress; }
                });
        root = views.root;
        moodVeil = views.moodVeil;
        initProgressView = views.initProgressView;
        initLastTurnView = views.initLastTurnView;
        stateLabel = views.stateLabel;
        audioLevelView = views.audioLevelView;
        micButton = views.micButton;
        setContentView(root);
    }

    private void showChat() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        if (mic.running || inputAudioOpen) stopInputAudio("ready");
        homeTimeView = null;
        voiceLastTurnView = null;
        voiceOrbView = null;
        audioLevelView = null;
        micButton = null;
        ChatPage.Views views = ChatPage.render(this, ui,
                new ChatPage.Model(agentName, stateLabelText(), initializing, initProgressText(), moodForText(lastConversationLine()), messages),
                new ChatPage.Callbacks() {
                    @Override public void onBack() { if (initialized) openVoiceSurface(); else showInitializationHome(); }
                    @Override public void onVoiceHome() { openVoiceSurface(); }
                    @Override public void onSend(String text) { sendText(text); }
                });
        root = views.root;
        moodVeil = views.moodVeil;
        messageList = views.messageList;
        messageScroll = views.messageScroll;
        composer = views.composer;
        stateLabel = views.stateLabel;
        initProgressView = views.initProgressView;
        setContentView(root);
    }

    private void openVoiceSurface() {
        if (initialized) {
            showVoiceHome();
        } else if (initializing) {
            showInitializationHome();
        }
        if (!isBoundHeadsetConnected()) {
            main.postDelayed(() -> showHeadsetPrompt(true), 160);
        }
    }

    private void showVoices() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        root = baseRoot();
        root.addView(topBar("‹", "Voices", "", this::showSettings, null));
        LinearLayout list = screenList();
        int[] colors = {0xFFFF5C75, 0xFFF68E68, 0xFF71BD9C, 0xFF9382C5, 0xFF6FA5CD, 0xFFD6B070};
        for (int i = 0; i < voices.size(); i++) {
            Voice voice = voices.get(i);
            LinearLayout row = row();
            SwatchView swatch = new SwatchView(this, colors[i % colors.length]);
            row.addView(swatch, new LinearLayout.LayoutParams(dp(52), dp(52)));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setGravity(Gravity.CENTER_VERTICAL);
            labels.addView(text(voice.label, 16, Color.WHITE, 600));
            labels.addView(text("doubao · " + voice.gender, 12, 0x8CFFE0E0, 0));
            row.addView(labels, new LinearLayout.LayoutParams(0, -1, 1));
            TextView check = text(voice.id.equals(selectedVoiceId) ? "✓" : "", 24, 0xFFFF6377, 0);
            check.setGravity(Gravity.CENTER);
            row.addView(check, new LinearLayout.LayoutParams(dp(34), -1));
            row.setOnClickListener(v -> {
                selectedVoiceId = voice.id;
                selectedVoiceLabel = voice.label;
                realtime.close();
                setState("idle");
                showVoices();
            });
            list.addView(row);
        }
        setContentView(root);
    }

    private void showSettings() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        root = baseRoot();
        Runnable back = initializing ? this::showInitializationHome : (initialized ? this::showHome : () -> beginInitialization(agentName));
        root.addView(topBar("‹", "Settings", "", back, null));
        LinearLayout list = screenList();
        list.addView(navRow("↺", "Reinitialize", "Reset memory", this::resetInitialization));
        list.addView(navRow("⌫", "Clear Session", "Keep memory", this::clearCurrentSession));
        list.addView(navRow("▣", "演示模式", demoMode ? "On · 本机麦克风与 Speaker" : "Off", this::showDemoModePrompt));
        list.addView(ui.navRow(R.drawable.ic_headphones, "Headphones", headsetSettingsLabel(), () -> showHeadsetPrompt(false)));
        list.addView(navRow("≋", "Voice", selectedVoiceLabel, this::showVoices));
        list.addView(navRow("♬", "Sound", "76%", null));
        list.addView(navRow("♧", "Notifications", "On", null));
        list.addView(navRow("▢", "Privacy", "", null));
        list.addView(navRow("◎", "Language", "中文 / English", null));
        list.addView(navRow("ⓘ", "About Her", "", this::showAbout));
        list.addView(navRow("≡", "Realtime", "Doubao · PCM16", null));
        setContentView(root);
    }

    private String headsetSettingsLabel() {
        if (demoMode) return "Skipped in demo mode";
        if (headsets == null || !headsets.hasBoundHeadset()) return "Not bound";
        String label = headsets.boundLabel();
        if (label.isEmpty()) label = "Bound";
        return headsets.isBoundConnected() ? label : label + " · offline";
    }

    private void showDemoModePrompt() {
        new AlertDialog.Builder(this)
                .setTitle("演示模式")
                .setMessage(demoMode
                        ? "当前正在使用本机麦克风和 Speaker。关闭后，语音模式会重新要求已绑定耳机在线。"
                        : "开启后，不需要绑定耳机；语音会直接使用本机麦克风和 Speaker。适合演示或调试。")
                .setPositiveButton(demoMode ? "关闭演示模式" : "开启演示模式", (dialog, which) -> setDemoMode(!demoMode))
                .setNegativeButton("取消", null)
                .show();
    }

    private void setDemoMode(boolean enabled) {
        if (demoMode == enabled) return;
        demoMode = enabled;
        getSharedPreferences("her", MODE_PRIVATE).edit().putBoolean(PREF_DEMO_MODE, demoMode).apply();
        if (!demoMode && !isBoundHeadsetConnected() && (mic.running || inputAudioOpen)) {
            stopInputAudio("text_only");
            toastError("演示模式已关闭，语音需要已绑定耳机在线。");
        } else {
            if (stateLabel != null) stateLabel.setText(stateLabelText());
            if (micButton != null) micButton.setText(mic.running || inputAudioOpen ? "●" : voiceButtonText());
            if (demoMode && (initialized || initializing) && !realtime.isOpen()) realtime.connect();
        }
        showSettings();
    }

    private void clearCurrentSession() {
        mic.stop();
        player.stop();
        realtime.close();
        stopToolTtsPlayback(false);
        inputAudioOpen = false;
        pendingMicStart = false;
        pendingText = null;
        pendingWeatherQuestion = null;
        pendingWeatherBroadcastPrompt = null;
        pendingRealtimeWeatherAnswer = false;
        latestWeatherFact = "";
        pendingNewsBroadcastPrompt = null;
        pendingNewsQuestion = null;
        pendingNewsToolAfterAck = false;
        pendingRealtimeNewsAnswer = false;
        latestNewsFact = "";
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        activeAssistantId = null;
        persistedMessageIds.clear();
        if (memoryStore != null && sessionId > 0) {
            memoryStore.clearSession(sessionId);
            sessionId = memoryStore.startSession(agentName);
            conversationMemory = memoryStore.relevantMemory("");
            dynamicTone = memoryStore.latestTone();
        }
        messages.clear();
        messages.add(new Message("session-cleared", "assistant", "这一轮已经清空。我们重新开始。"));
        setState("idle");
        if (initialized) {
            showHome();
        } else {
            showInitializationHome();
        }
    }

    private void showAbout() {
        clearVoiceWeatherCard(false);
        clearVoiceNewsCard(false);
        root = baseRoot();
        root.addView(topBar("‹", "About Her", "", this::showSettings, null));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(32), 0, dp(32), dp(30));
        root.addView(content, frame(-1, -1));
        HerMarkView mark = new HerMarkView(this);
        content.addView(mark, new LinearLayout.LayoutParams(dp(120), dp(120)));
        TextView title = text(agentName, 34, Color.WHITE, 0);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams t = new LinearLayout.LayoutParams(-1, -2);
        t.topMargin = dp(24);
        content.addView(title, t);
        TextView subtitle = text("Realtime Operating System", 17, 0xB8FFE0E0, 0);
        subtitle.setGravity(Gravity.CENTER);
        content.addView(subtitle);
        TextView version = text("Doubao model · Custom cloned voice · c-her background", 13, 0x88FFE0E0, 0);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams v = new LinearLayout.LayoutParams(-1, -2);
        v.topMargin = dp(12);
        content.addView(version, v);
        setContentView(root);
    }

    private FrameLayout baseRoot() {
        HerUi.Root rootState = ui.baseRoot(moodForText(lastConversationLine()));
        moodVeil = rootState.moodVeil;
        return rootState.frame;
    }

    private LinearLayout topBar(String left, String title, String right, Runnable leftAction, Runnable rightAction) {
        return ui.topBar(left, title, right, leftAction, rightAction);
    }

    private LinearLayout screenList() {
        return ui.screenList(root);
    }

    private LinearLayout row() {
        return ui.row();
    }

    private View navRow(String symbol, String label, String value, Runnable action) {
        return ui.navRow(symbol, label, value, action);
    }

    private void renderMessages() {
        updateInitializationLastTurn();
        updateVoiceHome();
        if (messageList == null) return;
        ChatPage.renderMessages(this, ui, messageList, messageScroll, messages);
    }

    private String lastConversationLine() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (!"user".equals(message.role) && !"assistant".equals(message.role)) continue;
            if (message.text != null && !message.text.trim().isEmpty()) {
                return message.text.trim();
            }
        }
        return "我在这里。轻轻点一下，说给我听。";
    }

    private void updateVoiceHome() {
        String line = lastConversationLine();
        if (voiceLastTurnView != null) voiceLastTurnView.setText(line);
        if (moodVeil != null) moodVeil.setMood(moodForText(line));
        if (voiceOrbView != null) voiceOrbView.setConversationState(state);
        if (micButton != null) micButton.setText(mic.running || inputAudioOpen ? "●" : voiceButtonText());
    }

    private int moodForText(String text) {
        String value = text == null ? "" : text.toLowerCase(Locale.US);
        if (value.contains("焦虑") || value.contains("难受") || value.contains("害怕") ||
                value.contains("崩溃") || value.contains("anxious") || value.contains("sad")) {
            return 1;
        }
        if (value.contains("开心") || value.contains("高兴") || value.contains("喜欢") ||
                value.contains("期待") || value.contains("happy") || value.contains("love")) {
            return 2;
        }
        if (value.contains("累") || value.contains("困") || value.contains("疲惫") ||
                value.contains("安静") || value.contains("tired") || value.contains("quiet")) {
            return 3;
        }
        return 0;
    }

    private String lastInitializationLine() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.text != null && !message.text.trim().isEmpty()) {
                return message.text.trim();
            }
        }
        return "初始化";
    }

    private void updateInitializationLastTurn() {
        if (initLastTurnView != null) {
            initLastTurnView.setText(lastInitializationLine());
        }
    }

    private Message addChatMessage(String role, String text) {
        Message message = new Message(newId(role), role, text);
        messages.add(message);
        persistMessage(message);
        updateVoiceHome();
        return message;
    }

    private void persistMessage(Message message) {
        if (message == null || message.text == null || message.text.trim().isEmpty()) return;
        if (!initialized || initializing || sessionId <= 0 || memoryStore == null) return;
        if (!"user".equals(message.role) && !"assistant".equals(message.role)) return;
        if (!persistedMessageIds.add(message.id)) return;
        memoryStore.insertMessage(sessionId, message.role, message.text.trim());
        if ("user".equals(message.role)) {
            lastUserUtterance = message.text.trim();
            conversationMemory = memoryStore.relevantMemory(lastUserUtterance);
            memoryDirtyForRealtime = true;
            applyContextUpdateForNextTurn(false);
        }
        maybeCompactMemory();
    }

    private void persistActiveAssistantMessage() {
        if (activeAssistantId == null) return;
        for (Message message : messages) {
            if (message.id.equals(activeAssistantId)) {
                persistMessage(message);
                return;
            }
        }
    }

    private void discardActiveAssistantMessage() {
        if (activeAssistantId == null) return;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).id.equals(activeAssistantId)) {
                messages.remove(i);
                break;
            }
        }
        activeAssistantId = null;
        renderMessages();
        updateVoiceHome();
    }

    private void removeAssistantReplyAfterLastUser() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if ("user".equals(message.role)) break;
            if ("assistant".equals(message.role)) {
                messages.remove(i);
            }
        }
        activeAssistantId = null;
        renderMessages();
        updateVoiceHome();
    }

    private void maybeCompactMemory() {
        if (compactInProgress || summaryInProgress || initializing || memoryStore == null || sessionId <= 0) return;
        MemoryChunk chunk = memoryStore.unsummarizedChunk(sessionId, COMPACT_MESSAGE_THRESHOLD, COMPACT_CHAR_THRESHOLD);
        if (chunk == null) return;
        compactInProgress = true;
        compactConversation(chunk);
    }

    private void compactConversation(MemoryChunk chunk) {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            compactInProgress = false;
            return;
        }
        JSONObject body = new JSONObject();
        JSONArray llmMessages = new JSONArray();
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                    "你是长期记忆压缩器。根据近期对话生成两类内容：\n" +
                    "1. memory_md：稳定、可检索、可长期保存的事实/偏好/关系/目标/边界。\n" +
                    "2. tone_guidance：下一阶段 Agent 应如何调整说话语气，必须短而具体。\n" +
                    "只输出 JSON：{\"memory_md\":\"...\",\"tone_guidance\":\"...\"}");
            llmMessages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content",
                    "Agent 名字：" + agentName + "\n" +
                    "已有用户初始化画像：\n" + trimForPrompt(userMemory, 1100) + "\n\n" +
                    "已有长期摘要：\n" + trimForPrompt(conversationMemory, 1200) + "\n\n" +
                    "请压缩这段新对话，不要丢掉能影响陪伴方式的细节：\n" + chunk.transcript);
            llmMessages.put(user);

            body.put("model", BACKGROUND_MODEL);
            body.put("messages", llmMessages);
            body.put("temperature", 0.2);
            body.put("stream", false);
        } catch (JSONException error) {
            compactInProgress = false;
            return;
        }

        agents.sendSubconscious(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                String memory = "";
                String tone = "";
                try {
                    JSONObject compact = parseJsonObject(content);
                    memory = compact.optString("memory_md", content).trim();
                    tone = compact.optString("tone_guidance", "").trim();
                } catch (JSONException error) {
                    memory = content;
                }
                String finalMemory = memory;
                String finalTone = tone;
                if (!finalMemory.isEmpty()) {
                    memoryStore.insertMemory(sessionId, "compact", finalMemory, chunk.firstId, chunk.lastId);
                    memoryStore.markCompacted(chunk.lastId);
                }
                if (!finalTone.isEmpty()) {
                    dynamicTone = finalTone;
                    memoryStore.insertMemory(sessionId, "tone", finalTone, chunk.firstId, chunk.lastId);
                }
                conversationMemory = memoryStore.relevantMemory(lastUserUtterance);
                compactInProgress = false;
                memoryDirtyForRealtime = true;
                applyContextUpdateForNextTurn(true);
            }

            @Override public void onError(String message) {
                compactInProgress = false;
            }
        }, "记忆压缩");
    }

    private JSONObject parseJsonObject(String content) throws JSONException {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return new JSONObject(trimmed);
    }

    private void applyContextUpdateForNextTurn(boolean includeFact) {
        if (!memoryDirtyForRealtime || !realtime.isOpen()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("instructions", buildInstructions());
            if (includeFact && !conversationMemory.trim().isEmpty()) {
                payload.put("fact", trimForPrompt(conversationMemory, 900));
            }
        } catch (JSONException ignored) { }
        realtime.sendEvent("context.update", payload);
        memoryDirtyForRealtime = false;
    }

    private void sendText(String text) {
        if (summaryInProgress) return;
        if (text == null || text.isEmpty()) return;
        markConversationInteraction(false);
        addChatMessage("user", text);
        if (initializing) {
            if (recordInitializationAnswer(text)) {
                if (initUserTurns >= INIT_TARGET_USER_TURNS) {
                    renderMessages();
                    finishInitializationWithSummary();
                    return;
                }
            }
            scheduleInitializationContextUpdate();
        }
        activeAssistantId = null;
        renderMessages();
        if (!initializing && handleNewsQuestion(text, false)) return;
        if (!initializing && handleWeatherQuestion(text, false)) return;
        sendTextWithAgentLLM(text);
    }

    private boolean handleNewsQuestion(String text, boolean realtimeMode) {
        if (!NewsSkill.isNewsQuestion(text)) {
            Log.d(TAG, "news intent miss text=" + text);
            return false;
        }
        Log.d(TAG, "news intent hit realtime=" + realtimeMode + " text=" + text);
        latestNewsFact = "";
        pendingRealtimeNewsAnswer = realtimeMode;
        pendingNewsBroadcastPrompt = null;
        pendingNewsQuestion = realtimeMode ? text : null;
        pendingNewsToolAfterAck = false;
        latestWeatherFact = "";
        pendingRealtimeWeatherAnswer = false;
        pendingWeatherBroadcastPrompt = null;
        if (!realtimeMode) {
            addChatMessage("assistant", "稍等，我看一下新闻热点。");
            renderMessages();
            runNewsTool(text, false);
        } else {
            startRealtimeNewsAck(text);
        }
        return true;
    }

    private void routeToolsInBackground(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) return;
        int seq = ++toolRouteSeq;
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                    "你是 " + effectiveAgentName() + " 的后台意识模型，负责判断用户当前这句话是否需要客户端工具。\n" +
                    "当前可用工具：daily_news（读取每日新闻热点，来源 https://agentnews.linkyun.co/）。\n" +
                    "如果用户想查、看、听、播报新闻/热点/每日新闻热点，返回 {\"tool\":\"daily_news\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                    "如果只是普通聊天、评价刚才内容、追问旧回答、闲聊或不确定，返回 {\"tool\":\"none\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                    "只输出 JSON，不要解释。");
            messages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", text);
            messages.put(user);

            body.put("model", BACKGROUND_MODEL);
            body.put("messages", messages);
            body.put("temperature", 0.0);
            body.put("stream", false);
        } catch (JSONException error) {
            return;
        }
        agents.sendSubconscious(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                if (seq != toolRouteSeq) return;
                try {
                    JSONObject decision = parseJsonObject(content);
                    String tool = decision.optString("tool", "none");
                    double confidence = decision.optDouble("confidence", 0.0);
                    Log.d(TAG, "tool route tool=" + tool + " confidence=" + confidence + " text=" + text);
                    if ("daily_news".equals(tool) && confidence >= 0.55) {
                        startNewsToolFromBackground(text);
                    }
                } catch (JSONException error) {
                    Log.d(TAG, "tool route parse failed content=" + content);
                }
            }

            @Override public void onError(String message) {
                Log.d(TAG, "tool route failed " + message);
            }
        }, "工具路由");
    }

    private void startNewsToolFromBackground(String question) {
        latestNewsFact = "";
        pendingRealtimeNewsAnswer = true;
        pendingNewsBroadcastPrompt = null;
        pendingNewsQuestion = question;
        pendingNewsToolAfterAck = false;
        latestWeatherFact = "";
        pendingRealtimeWeatherAnswer = false;
        pendingWeatherBroadcastPrompt = null;
        interruptRealtimePlayback("background_tool_daily_news");
        discardActiveAssistantMessage();
        removeAssistantReplyAfterLastUser();
        if (realtime.isOpen()) {
            realtime.close();
        }
        setState("news_tool");
        runNewsTool(question, true);
    }

    private void startRealtimeNewsAck(String question) {
        pendingNewsQuestion = question;
        pendingNewsToolAfterAck = true;
        toolRouteSeq++;
        interruptRealtimePlayback("news_tool_ack", false);
        activeAssistantId = null;
        setState("news_ack");
        if (!realtime.isOpen()) {
            realtime.connect();
            return;
        }
        realtime.sendInputText(NewsSkill.LOOKUP_ACK_PROMPT);
    }

    private void runPendingNewsToolAfterAck() {
        if (!pendingNewsToolAfterAck) return;
        pendingNewsToolAfterAck = false;
        toolRouteSeq++;
        String question = pendingNewsQuestion == null ? "每日新闻热点" : pendingNewsQuestion;
        Log.d(TAG, "news ack done, run tool question=" + question);
        realtime.close();
        resetRealtimeOutput();
        player.stop();
        setState("news_tool");
        runNewsTool(question, true);
    }

    private void runNewsTool(String question, boolean realtimeMode) {
        if (newsTool == null) return;
        Log.d(TAG, "news tool fetch realtime=" + realtimeMode + " question=" + question);
        newsTool.fetchDaily(newsCallback(question, realtimeMode));
    }

    private NewsTool.CallbackResult newsCallback(String question, boolean realtimeMode) {
        return new NewsTool.CallbackResult() {
            @Override public void onSuccess(NewsTool.NewsResult result) {
                String fact = result.fact(question);
                Log.d(TAG, "news tool success items=" + result.items.size() + " realtime=" + realtimeMode);
                latestNewsFact = fact;
                if (realtimeMode) {
                    pendingRealtimeNewsAnswer = false;
                    addNewsCard(result);
                    String answer = result.shortAnswer();
                    addChatMessage("assistant", answer);
                    renderMessages();
                    queueToolTtsPlayback("news", answer);
                } else {
                    addNewsCard(result);
                    addChatMessage("assistant", result.shortAnswer());
                    renderMessages();
                    setState("ready");
                }
            }

            @Override public void onError(String message) {
                Log.d(TAG, "news tool error realtime=" + realtimeMode + " message=" + message);
                if (realtimeMode) {
                    latestNewsFact = NewsSkill.failureFact(message);
                    pendingRealtimeNewsAnswer = false;
                    String answer = "暂时没读到新闻热点，" + message + "。你可以稍后再试一下。";
                    addChatMessage("assistant", answer);
                    renderMessages();
                    queueToolTtsPlayback("news", answer);
                } else {
                    addChatMessage("assistant", message);
                    renderMessages();
                    setState("ready");
                }
            }
        };
    }

    private boolean handleWeatherQuestion(String text, boolean realtimeMode) {
        if (!WeatherSkill.isWeatherQuestion(text)) return false;
        Log.d(TAG, "weather intent hit realtime=" + realtimeMode + " text=" + text);
        latestWeatherFact = "";
        pendingRealtimeWeatherAnswer = false;
        pendingWeatherBroadcastPrompt = null;
        pendingRealtimeNewsAnswer = false;
        latestNewsFact = "";
        if (!realtimeMode) {
            addChatMessage("assistant", "稍等，我查一下天气。");
            renderMessages();
        } else {
            interruptRealtimePlayback("weather_tool");
            realtime.close();
            resetRealtimeOutput();
            player.stop();
            discardActiveAssistantMessage();
            removeAssistantReplyAfterLastUser();
            addChatMessage("assistant", "稍等，我查一下天气。");
            renderMessages();
        }
        resolveWeatherIntentAndRun(text, realtimeMode);
        return true;
    }

    private void resolveWeatherIntentAndRun(String question, boolean realtimeMode) {
        if (agents == null || BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            Log.d(TAG, "weather intent resolver unavailable, fallback to location");
            runWeatherTool(question, "", realtimeMode);
            return;
        }
        int seq = ++weatherIntentSeq;
        JSONObject body;
        try {
            body = WeatherIntentResolver.requestBody(BACKGROUND_MODEL, effectiveAgentName(), question);
        } catch (JSONException error) {
            runWeatherTool(question, "", realtimeMode);
            return;
        }
        agents.sendSubconscious(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                if (seq != weatherIntentSeq) return;
                try {
                    WeatherIntentResolver.Result intent = WeatherIntentResolver.parse(content);
                    Log.d(TAG, "weather intent city=" + intent.city +
                            " isWeather=" + intent.isWeatherQuery +
                            " reason=" + intent.reason +
                            " text=" + question);
                    if (!intent.isWeatherQuery) {
                        setState("ready");
                        if (realtimeMode) scheduleContinuousListening(180);
                        return;
                    }
                    runWeatherTool(question, intent.city, realtimeMode);
                } catch (JSONException error) {
                    Log.d(TAG, "weather intent parse failed content=" + content);
                    failWeatherIntent(question, realtimeMode);
                }
            }

            @Override public void onError(String message) {
                if (seq != weatherIntentSeq) return;
                Log.d(TAG, "weather intent failed " + message);
                failWeatherIntent(question, realtimeMode);
            }
        }, "天气意图解析");
    }

    private void failWeatherIntent(String question, boolean realtimeMode) {
        weatherCallback(question, realtimeMode).onError("天气意图解析失败，请再说一次城市名。");
    }

    private void runWeatherTool(String question, String city, boolean realtimeMode) {
        if (weatherTool == null) return;
        if (city != null && !city.trim().isEmpty()) {
            Log.d(TAG, "weather query city=" + city.trim() + " realtime=" + realtimeMode);
            weatherTool.queryCity(city.trim(), weatherCallback(question, realtimeMode));
            return;
        }
        Log.d(TAG, "weather query current location realtime=" + realtimeMode);
        requestWeatherForCurrentLocation(question, realtimeMode);
    }

    private WeatherTool.CallbackResult weatherCallback(String question, boolean realtimeMode) {
        return new WeatherTool.CallbackResult() {
            @Override public void onSuccess(WeatherTool.WeatherResult result) {
                String fact = result.fact(question);
                Log.d(TAG, "weather success place=" + result.placeName + " realtime=" + realtimeMode);
                latestWeatherFact = fact;
                addWeatherCard(result);
                if (realtimeMode) {
                    pendingRealtimeWeatherAnswer = false;
                    String answer = result.shortAnswer();
                    addChatMessage("assistant", answer);
                    renderMessages();
                    queueToolTtsPlayback("weather", answer);
                } else {
                    addChatMessage("assistant", result.shortAnswer());
                    renderMessages();
                    setState("ready");
                }
            }

            @Override public void onError(String message) {
                Log.d(TAG, "weather error realtime=" + realtimeMode + " message=" + message);
                if (realtimeMode) {
                    latestWeatherFact = WeatherSkill.failureFact(message);
                    pendingRealtimeWeatherAnswer = false;
                    String answer = "暂时没查到天气，" + message + "。你可以稍后再试，或者告诉我具体城市。";
                    addChatMessage("assistant", answer);
                    renderMessages();
                    queueToolTtsPlayback("weather", answer);
                } else {
                    addChatMessage("assistant", message);
                    renderMessages();
                    setState("ready");
                }
            }
        };
    }

    private void requestWeatherForCurrentLocation(String question, boolean realtimeMode) {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingWeatherQuestion = question;
            pendingWeatherRealtime = realtimeMode;
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQ_LOCATION);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            weatherCallback(question, realtimeMode).onError("无法读取当前位置");
            return;
        }
        try {
            Location last = WeatherSkill.bestLastLocation(manager);
            if (last != null) {
                weatherTool.queryLocation(last, weatherCallback(question, realtimeMode));
                return;
            }
            WeatherSkill.requestSingleLocation(manager, main,
                    location -> weatherTool.queryLocation(location, weatherCallback(question, realtimeMode)),
                    message -> weatherCallback(question, realtimeMode).onError(message));
        } catch (SecurityException error) {
            weatherCallback(question, realtimeMode).onError("没有定位权限");
        }
    }

    private void pushRealtimeWeatherFact() {
        if (!realtime.isOpen() || latestWeatherFact.trim().isEmpty()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("fact", trimForPrompt(latestWeatherFact, 1100));
        } catch (JSONException ignored) { }
        realtime.sendEvent("context.update", payload);
    }

    private void queueRealtimeWeatherBroadcast(String prompt) {
        pendingWeatherBroadcastPrompt = prompt;
        pushRealtimeWeatherFact();
        if (!"speaking".equals(state) && !isResponsePendingState(state)) {
            schedulePendingWeatherBroadcast(1000);
        }
    }

    private void schedulePendingWeatherBroadcast(long delayMs) {
        main.postDelayed(this::sendPendingWeatherBroadcast, delayMs);
    }

    private void sendPendingWeatherBroadcast() {
        if (pendingWeatherBroadcastPrompt == null) return;
        if (!realtime.isOpen()) {
            realtime.connect();
            schedulePendingWeatherBroadcast(1400);
            return;
        }
        String prompt = pendingWeatherBroadcastPrompt;
        pendingWeatherBroadcastPrompt = null;
        realtime.sendInputText(prompt);
        setState("processing");
    }

    private void pushRealtimeNewsFact() {
        if (!realtime.isOpen() || latestNewsFact.trim().isEmpty()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("fact", trimForPrompt(latestNewsFact, 1800));
        } catch (JSONException ignored) { }
        realtime.sendEvent("context.update", payload);
    }

    private void queueRealtimeNewsBroadcast(String prompt) {
        pendingNewsBroadcastPrompt = prompt;
        toolRouteSeq++;
        pushRealtimeNewsFact();
        if (!"speaking".equals(state) && !isResponsePendingState(state)) {
            schedulePendingNewsBroadcast(1000);
        }
    }

    private void schedulePendingNewsBroadcast(long delayMs) {
        main.postDelayed(this::sendPendingNewsBroadcast, delayMs);
    }

    private void sendPendingNewsBroadcast() {
        if (pendingNewsBroadcastPrompt == null) return;
        Log.d(TAG, "send pending news broadcast state=" + state);
        if ("speaking".equals(state) || isResponsePendingState(state)) {
            schedulePendingNewsBroadcast(600);
            return;
        }
        if (!realtime.isOpen()) {
            realtime.connect();
            schedulePendingNewsBroadcast(1400);
            return;
        }
        pushRealtimeNewsFact();
        String prompt = pendingNewsBroadcastPrompt;
        pendingNewsBroadcastPrompt = null;
        realtime.sendInputText(prompt);
        setState("processing");
    }

    private void queueToolTtsPlayback(String source, String text) {
        if (voicePipeline != null) voicePipeline.queueToolTts(source, text);
    }

    private boolean hasPendingToolTtsPlayback() {
        return voicePipeline != null && voicePipeline.hasPendingToolTts();
    }

    private boolean hasActiveToolTtsPlayback() {
        return voicePipeline != null && voicePipeline.hasActiveToolTts();
    }

    private void startPendingToolTtsPlayback() {
        startPendingToolTtsPlayback(false);
    }

    private void startPendingToolTtsPlayback(boolean force) {
        if (voicePipeline != null) voicePipeline.startPendingToolTts(force);
    }

    private void interruptRealtimePlayback(String reason) {
        interruptRealtimePlayback(reason, true);
    }

    private void interruptRealtimePlayback(String reason, boolean discardUntilDone) {
        if (voicePipeline != null) voicePipeline.markRealtimeOutputInterrupted(discardUntilDone);
        player.stop();
        if (realtime.isOpen()) {
            realtime.sendEvent("input_audio.interrupt", json("reason", reason));
        }
    }

    private boolean isRealtimeOutputActive() {
        return voicePipeline != null && voicePipeline.isRealtimeOutputActive();
    }

    private boolean shouldDiscardRealtimeAudio() {
        return voicePipeline != null && voicePipeline.shouldDiscardRealtimeAudio();
    }

    private void resetRealtimeOutput() {
        if (voicePipeline != null) voicePipeline.resetRealtimeOutput();
    }

    private boolean maybeStartToolTtsAfterRealtimeStopped() {
        return voicePipeline != null && voicePipeline.onRealtimeStoppedBeforeToolTts();
    }

    private void resumeListeningAfterToolTts(long delayMs) {
        if (!CONTINUOUS_CONVERSATION) return;
        main.postDelayed(() -> {
            if (voiceLastTurnView == null) return;
            if (mic.running || inputAudioOpen) return;
            if (!isBoundHeadsetConnected()) return;
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
            pendingMicStart = true;
            if (realtime.isOpen()) {
                pendingMicStart = false;
                startInputAudio();
            } else {
                realtime.connect();
            }
        }, delayMs);
    }

    private void stopToolTtsPlayback(boolean resumeListening) {
        if (ttsPlayer != null) ttsPlayer.stop();
        if (voicePipeline != null) voicePipeline.stopToolTts(resumeListening);
    }

    private void addWeatherCard(WeatherTool.WeatherResult result) {
        messages.add(new Message(newId("weather"), result));
        if (voiceLastTurnView != null) {
            latestVoiceWeather = result;
            latestVoiceNews = null;
            scheduleVoiceWeatherCardTimeout(result);
            showVoiceHome();
        } else {
            renderMessages();
        }
    }

    private void addNewsCard(NewsTool.NewsResult result) {
        discardActiveAssistantMessage();
        removeAssistantReplyAfterLastUser();
        messages.add(new Message(newId("news"), result));
        if (voiceLastTurnView != null) {
            latestVoiceNews = result;
            latestVoiceWeather = null;
            if (!pendingRealtimeNewsAnswer) scheduleVoiceNewsCardTimeout(result);
            showVoiceHome();
        } else {
            renderMessages();
        }
    }

    private void scheduleVoiceWeatherCardTimeout(WeatherTool.WeatherResult result) {
        if (voiceWeatherTimeout != null) main.removeCallbacks(voiceWeatherTimeout);
        voiceWeatherTimeout = () -> {
            if (latestVoiceWeather != result) return;
            voiceWeatherTimeout = null;
            latestVoiceWeather = null;
            if (voiceLastTurnView != null) showVoiceHome();
        };
        main.postDelayed(voiceWeatherTimeout, WeatherSkill.VOICE_CARD_TIMEOUT_MS);
    }

    private void scheduleVoiceNewsCardTimeout(NewsTool.NewsResult result) {
        if (result == null) return;
        if (voiceNewsTimeout != null) main.removeCallbacks(voiceNewsTimeout);
        voiceNewsTimeout = () -> {
            if (latestVoiceNews != result) return;
            voiceNewsTimeout = null;
            latestVoiceNews = null;
            if (voiceLastTurnView != null) showVoiceHome();
        };
        main.postDelayed(voiceNewsTimeout, NewsSkill.VOICE_CARD_TIMEOUT_MS);
    }

    private void clearVoiceWeatherCard(boolean refreshVoice) {
        if (voiceWeatherTimeout != null) {
            main.removeCallbacks(voiceWeatherTimeout);
            voiceWeatherTimeout = null;
        }
        if (latestVoiceWeather == null) return;
        latestVoiceWeather = null;
        if (refreshVoice && voiceLastTurnView != null) showVoiceHome();
    }

    private void clearVoiceNewsCard(boolean refreshVoice) {
        if (voiceNewsTimeout != null) {
            main.removeCallbacks(voiceNewsTimeout);
            voiceNewsTimeout = null;
        }
        if (latestVoiceNews == null) return;
        latestVoiceNews = null;
        if (refreshVoice && voiceLastTurnView != null) showVoiceHome();
    }

    private void sendTextWithAgentLLM(String text) {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            toastError("Missing AGENTLLM_API_KEY in local.properties");
            setState("error");
            return;
        }
        setState("processing");
        String instructions;
        try {
            instructions = buildInstructions() + "\n\n" +
                    "当前是文本聊天通道。请只输出适合聊天气泡展示的文字，不要描述语音、音频或工具过程。";
            agents.sendChat(TEXT_CHAT_MODEL, instructions, text, new AgentApiClient.ReplyCallback() {
                @Override public void onSuccess(String reply) {
                    if (reply.isEmpty()) {
                        toastError("文本聊天返回为空");
                        setState("error");
                        return;
                    }
                    breatheScreenForAssistantReply();
                    addChatMessage("assistant", reply);
                    renderMessages();
                    if (initializing) {
                        updateInitProgress();
                    }
                    setState("ready");
                }

                @Override public void onError(String message) {
                    toastError(message);
                    setState("error");
                }
            });
        } catch (JSONException error) {
            toastError("构建文本聊天请求失败");
            setState("error");
        }
    }

    private void toggleMic() {
        if (summaryInProgress) return;
        markConversationInteraction(false);
        if (hasActiveToolTtsPlayback() || (ttsPlayer != null && ttsPlayer.isPlaying())) {
            stopToolTtsPlayback(true);
            setState("ready");
        }
        if ("news_tool".equals(state) || pendingNewsBroadcastPrompt != null ||
                pendingRealtimeNewsAnswer || latestVoiceNews != null) {
            interruptNewsPlayback();
            return;
        }
        if (mic.running || inputAudioOpen) {
            stopInputAudio("processing");
            return;
        }
        if (!isBoundHeadsetConnected()) {
            showHeadsetPrompt(true);
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (!realtime.isOpen()) {
            pendingMicStart = true;
            realtime.connect();
            return;
        }
        interruptRealtimePlayback("user_speech_detected");
        startInputAudio();
    }

    private void interruptNewsPlayback() {
        pendingNewsBroadcastPrompt = null;
        stopToolTtsPlayback(true);
        pendingRealtimeNewsAnswer = false;
        pendingNewsToolAfterAck = false;
        pendingNewsQuestion = null;
        toolRouteSeq++;
        if (realtime.isOpen()) {
            interruptRealtimePlayback("news_interrupt");
            realtime.close();
        }
        resetRealtimeOutput();
        player.stop();
        clearVoiceNewsCard(true);
        setState("ready");
        if (!realtime.isOpen()) realtime.connect();
        scheduleContinuousListening(300);
    }

    private void startInputAudio() {
        if (mic.running || inputAudioOpen) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        markConversationInteraction(false);
        resetRealtimeOutput();
        resetVad();
        if (audioLevelView != null) audioLevelView.setLevel(0);
        if (voiceOrbView != null) voiceOrbView.setLevel(0);
        startHerForegroundService(true);
        realtime.sendEvent("input_audio.start", null);
        inputAudioOpen = true;
        boolean started = mic.start(bytes -> {
            if (inputAudioOpen && realtime.isOpen()) {
                realtime.sendAudio(bytes);
                processVad(bytes);
            }
        });
        if (started) {
            setState("listening");
        } else {
            startHerForegroundService(false);
            stopInputAudio("error");
            toastError("无法启动麦克风，请检查录音权限或重试。");
        }
    }

    private void stopInputAudio(String nextState) {
        mic.stop();
        if (audioLevelView != null) audioLevelView.setLevel(0);
        if (voiceOrbView != null) voiceOrbView.setLevel(0);
        boolean sentInputEnd = false;
        if (inputAudioOpen) {
            realtime.sendEvent("input_audio.end", null);
            inputAudioOpen = false;
            sentInputEnd = true;
        }
        startHerForegroundService(false);
        setState(nextState);
        if (sentInputEnd && "processing".equals(nextState)) {
            scheduleAsrFinalTimeout();
        }
    }

    private void scheduleAsrFinalTimeout() {
        cancelAsrFinalTimeout();
        asrFinalTimeout = () -> {
            asrFinalTimeout = null;
            if (!"processing".equals(state)) return;
            if (mic.running || inputAudioOpen || isRealtimeOutputActive() || summaryInProgress) return;
            Log.d(TAG, "asr final timeout, resume listening");
            setState("ready");
            scheduleContinuousListening(80);
        };
        main.postDelayed(asrFinalTimeout, ASR_FINAL_TIMEOUT_MS);
    }

    private void cancelAsrFinalTimeout() {
        if (asrFinalTimeout != null) {
            main.removeCallbacks(asrFinalTimeout);
            asrFinalTimeout = null;
        }
    }

    private void resetVad() {
        vadSpeechStarted = false;
        vadSilenceFrames = 0;
        vadFrames = 0;
    }

    private void processVad(byte[] bytes) {
        vadFrames++;
        int level = averageAbsPcm16(bytes);
        if (audioLevelView != null) {
            int visualLevel = Math.min(100, level / 45);
            main.post(() -> {
                audioLevelView.setLevel(visualLevel);
                if (voiceOrbView != null) voiceOrbView.setLevel(visualLevel);
            });
        }
        boolean speech = level > VAD_SPEECH_THRESHOLD;
        if (speech) {
            vadSpeechStarted = true;
            vadSilenceFrames = 0;
            return;
        }
        if (vadSpeechStarted) {
            vadSilenceFrames++;
        }
        if (vadSpeechStarted && vadFrames > VAD_MIN_FRAMES_BEFORE_END && vadSilenceFrames >= VAD_SILENCE_FRAMES_TO_END) {
            main.post(() -> {
                if (inputAudioOpen && mic.running) {
                    stopInputAudio("processing");
                }
            });
        }
    }

    private int averageAbsPcm16(byte[] bytes) {
        int samples = bytes.length / 2;
        if (samples == 0) return 0;
        long total = 0;
        for (int i = 0; i + 1 < bytes.length; i += 2) {
            int sample = (short) ((bytes[i] & 0xff) | (bytes[i + 1] << 8));
            total += Math.abs(sample);
        }
        return (int) (total / samples);
    }

    private void enterAssistantSpeaking(int sampleRate) {
        markInitializationOpeningDeliveredFromRealtime();
        breatheScreenForAssistantReply();
        if (HALF_DUPLEX && (mic.running || inputAudioOpen)) {
            stopInputAudio("speaking");
        } else {
            setState("speaking");
        }
        if (sampleRate > 0) {
            player.begin(sampleRate);
        }
    }

    private void startContinuousListening() {
        if (!CONTINUOUS_CONVERSATION) return;
        if (hasActiveToolTtsPlayback()) return;
        if (mic.running || inputAudioOpen || !realtime.isOpen()) return;
        if (!isBoundHeadsetConnected()) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        startInputAudio();
    }

    private void scheduleContinuousListening(long delayMs) {
        if (!CONTINUOUS_CONVERSATION) return;
        main.postDelayed(() -> {
            if (hasActiveToolTtsPlayback()) return;
            if ("ready".equals(state) && !mic.running && !inputAudioOpen) {
                startContinuousListening();
            }
        }, delayMs);
    }

    private void onRealtimeReady() {
        if (hasActiveToolTtsPlayback()) {
            setState("speaking");
            return;
        }
        if (pendingVoiceWakeIntent) {
            pendingVoiceWakeIntent = false;
            if (isBoundHeadsetConnected()) {
                toggleMic();
                return;
            }
        }
        if (initPromptPending) {
            initPromptPending = false;
            updateInitializationContext();
            setState("ready");
            if (pendingMicStart) {
                pendingMicStart = false;
                startInputAudio();
            }
            return;
        }
        if (pendingWeatherBroadcastPrompt != null) {
            schedulePendingWeatherBroadcast(400);
            return;
        }
        if (pendingNewsToolAfterAck) {
            interruptRealtimePlayback("news_tool_ack", false);
            realtime.sendInputText(NewsSkill.LOOKUP_ACK_PROMPT);
            setState("news_ack");
            return;
        }
        if (pendingText != null) {
            String text = pendingText;
            pendingText = null;
            realtime.sendInputText(text);
            setState("processing");
        }
        if (pendingMicStart) {
            pendingMicStart = false;
            toggleMic();
        }
    }

    private void onAssistantDelta(String text) {
        markInitializationOpeningDeliveredFromRealtime();
        if (activeAssistantId == null) {
            breatheScreenForAssistantReply();
            activeAssistantId = newId("assistant");
            messages.add(new Message(activeAssistantId, "assistant", ""));
        }
        for (Message message : messages) {
            if (message.id.equals(activeAssistantId)) {
                message.text += text;
                break;
            }
        }
        updateVoiceHome();
        renderMessages();
    }

    private void markInitializationOpeningDeliveredFromRealtime() {
        if (!initializing || initOpeningDelivered) return;
        initOpeningDelivered = true;
        Log.d(TAG, "init realtime opening delivered");
    }

    private void breatheScreenForAssistantReply() {
        boolean interactive = isScreenInteractive();
        markConversationInteraction(!interactive);
        if (interactive) return;
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        acquireReplyWakeLock();
        animateReplyScreenBrightness();
    }

    private boolean isScreenInteractive() {
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power == null) return true;
        if (Build.VERSION.SDK_INT >= 20) return power.isInteractive();
        return power.isScreenOn();
    }

    private void acquireReplyWakeLock() {
        releaseReplyWakeLock();
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power == null) return;
        replyWakeLock = power.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                TAG + ":assistant_reply");
        replyWakeLock.setReferenceCounted(false);
        replyWakeLock.acquire(6500);
        main.postDelayed(this::releaseReplyWakeLock, 6800);
    }

    private void animateReplyScreenBrightness() {
        restoreReplyScreenBrightness();
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        replyOriginalBrightness = params.screenBrightness;
        replyBrightnessAnimator = ValueAnimator.ofFloat(0.05f, 0.88f, 0.24f, 0.72f);
        replyBrightnessAnimator.setDuration(3200);
        replyBrightnessAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        replyBrightnessAnimator.addUpdateListener(animation -> {
            WindowManager.LayoutParams next = window.getAttributes();
            next.screenBrightness = (float) animation.getAnimatedValue();
            window.setAttributes(next);
        });
        replyBrightnessAnimator.start();
        main.postDelayed(this::restoreReplyScreenBrightness, 6200);
    }

    private void restoreReplyScreenBrightness() {
        if (replyBrightnessAnimator != null) {
            replyBrightnessAnimator.cancel();
            replyBrightnessAnimator = null;
        }
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = replyOriginalBrightness;
        window.setAttributes(params);
        if (!shouldKeepScreenOnForCurrentInteraction()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void releaseReplyWakeLock() {
        if (replyWakeLock != null && replyWakeLock.isHeld()) {
            replyWakeLock.release();
        }
        replyWakeLock = null;
    }

    private void finishInitializationWithSummary() {
        if (summaryInProgress) return;
        summaryInProgress = true;
        initSummaryPending = true;
        Message writing = new Message("writing-user-md", "assistant", "我收到了。现在由潜意识模型写入 user.md 和 Agent.md。");
        messages.add(writing);
        renderMessages();
        updateInitProgress();
        startInitProgressAnimation();
        stopInputAudio("summarizing");
        realtime.close();
        summarizeInitialization();
    }

    private String initProgressText() {
        if (summaryInProgress) return "潜意识模型正在写入 user.md / Agent.md";
        int step = Math.min(initUserTurns + 1, INIT_TARGET_USER_TURNS);
        String label;
        if (step == 1) {
            label = "名字和称呼";
        } else if (step == 2) {
            label = "你希望的关系";
        } else {
            label = "你的故事";
        }
        return "初始化 " + step + "/" + INIT_TARGET_USER_TURNS + " · " + label;
    }

    private void startInitProgressAnimation() {
        summaryStartedAt = SystemClock.uptimeMillis();
        if (initProgressTicker != null) main.removeCallbacks(initProgressTicker);
        initProgressTicker = new Runnable() {
            @Override public void run() {
                if (!summaryInProgress) return;
                if (audioLevelView != null) {
                    long elapsed = SystemClock.uptimeMillis() - summaryStartedAt;
                    int progress = Math.min(92, 8 + (int) (elapsed / 85));
                    audioLevelView.setLevel(progress);
                }
                if (initProgressView != null) initProgressView.setText(initProgressText());
                main.postDelayed(this, 90);
            }
        };
        initProgressTicker.run();
    }

    private void stopInitProgressAnimation() {
        if (initProgressTicker != null) main.removeCallbacks(initProgressTicker);
        initProgressTicker = null;
        if (audioLevelView != null) audioLevelView.setLevel(100);
    }

    private void showInitializationCompleteThenHome() {
        showInitializationHome();
        if (initProgressView != null) initProgressView.setText("初始化完成");
        if (initLastTurnView != null) initLastTurnView.setText("我记住啦。我们从这里重新开始。");
        if (audioLevelView != null) audioLevelView.setLevel(100);
        main.postDelayed(this::showHome, 5000);
    }

    private void updateInitProgress() {
        if (initProgressView != null) initProgressView.setText(initProgressText());
        updateInitializationLastTurn();
    }

    private void scheduleInitializationContextUpdate() {
        updateInitializationContext();
    }

    private void updateInitializationContext() {
        if (!initializing || !realtime.isOpen()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("instructions", buildInstructions());
        } catch (JSONException ignored) { }
        realtime.sendEvent("context.update", payload);
    }

    private void setState(String next) {
        state = next;
        updateInteractionKeepScreenOn();
        updateResponsePendingTimeout(next);
        if (stateLabel != null) stateLabel.setText(stateLabelText());
        if (micButton != null) micButton.setText(next.equals("listening") ? "●" : voiceButtonText());
        if (voiceOrbView != null) voiceOrbView.setConversationState(next);
        if ("ready".equals(next) || "idle".equals(next)) {
            applyContextUpdateForNextTurn(false);
        }
    }

    private void updateInteractionKeepScreenOn() {
        if (shouldKeepScreenOnForCurrentInteraction()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            clearInteractionKeepScreenOn();
        }
    }

    private void clearInteractionKeepScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private boolean shouldKeepScreenOnForCurrentInteraction() {
        if (initializing || summaryInProgress) return true;
        if (mic.running || inputAudioOpen) return true;
        if (hasActiveToolTtsPlayback()) return true;
        return "connecting".equals(state) ||
                "listening".equals(state) ||
                isResponsePendingState(state) ||
                "speaking".equals(state) ||
                "news_ack".equals(state) ||
                "news_tool".equals(state) ||
                "summarizing".equals(state);
    }

    private void updateResponsePendingTimeout(String next) {
        if (responsePendingTimeout != null) {
            main.removeCallbacks(responsePendingTimeout);
            responsePendingTimeout = null;
        }
        if (!isResponsePendingState(next)) return;
        responsePendingTimeout = () -> {
            responsePendingTimeout = null;
            if (!isResponsePendingState(state)) return;
            if (mic.running || inputAudioOpen || summaryInProgress) return;
            if (realtime.isOpen()) {
                setState("ready");
                scheduleContinuousListening(80);
            } else {
                setState("idle");
            }
        };
        main.postDelayed(responsePendingTimeout, RESPONSE_PENDING_VISIBLE_TIMEOUT_MS);
    }

    private boolean isResponsePendingState(String value) {
        return "thinking".equals(value) || "processing".equals(value);
    }

    private String stateLabelText() {
        if (summaryInProgress) return "Summarizing";
        if (isResponsePendingState(state)) return "Processing";
        if ("news_ack".equals(state)) return "Checking news";
        if ("news_tool".equals(state)) return "Reading agentNews";
        if (!isBoundHeadsetConnected()) {
            if (headsets != null && headsets.hasBoundHeadset()) return "Headset disconnected";
            if (headsets != null && !headsets.connectedHeadsets().isEmpty()) return "Tap headset to bind";
            return "Text only · connect headphones";
        }
        return capitalize(state);
    }

    private String voiceButtonText() {
        if ((ttsPlayer != null && ttsPlayer.isPlaying()) || hasPendingToolTtsPlayback()) return "■";
        if ("news_tool".equals(state) || pendingNewsBroadcastPrompt != null ||
                pendingRealtimeNewsAnswer || latestVoiceNews != null) return "■";
        return "♩";
    }

    private boolean isBoundHeadsetConnected() {
        if (demoMode) return true;
        return headsets != null && headsets.isBoundConnected();
    }

    private void onHeadsetDevicesChanged() {
        if (!isBoundHeadsetConnected() && (mic.running || inputAudioOpen)) {
            stopInputAudio("text_only");
            toastError("耳机已断开，语音已暂停。");
        }
        if (stateLabel != null) stateLabel.setText(stateLabelText());
        if (micButton != null) micButton.setText(mic.running || inputAudioOpen ? "●" : voiceButtonText());
    }

    private void showHeadsetPrompt(boolean startVoiceAfterBind) {
        if (headsetDialogShowing || isFinishing()) return;
        if (headsets == null) return;
        List<HeadsetBindingManager.Device> devices = headsets.connectedHeadsets();
        headsetDialogShowing = true;
        if (devices.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("连接耳机")
                    .setMessage("没有检测到已连接耳机。现在只能用文字聊天；连接蓝牙或有线耳机后，再点耳机图标就可以绑定。")
                    .setPositiveButton("打开蓝牙设置", (dialog, which) -> {
                        headsetDialogShowing = false;
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    })
                    .setNegativeButton("文字聊天", (dialog, which) -> {
                        headsetDialogShowing = false;
                        showChat();
                    })
                    .setOnCancelListener(dialog -> headsetDialogShowing = false)
                    .show();
            return;
        }

        CharSequence[] labels = new CharSequence[devices.size()];
        for (int i = 0; i < devices.size(); i++) labels[i] = devices.get(i).label;
        new AlertDialog.Builder(this)
                .setTitle("绑定耳机")
                .setItems(labels, (dialog, which) -> {
                    headsetDialogShowing = false;
                    HeadsetBindingManager.Device device = devices.get(which);
                    headsets.bind(device);
                    toastError("已绑定 " + device.label + "。");
                    if (startVoiceAfterBind) main.postDelayed(this::toggleMic, 180);
                })
                .setNegativeButton("继续文字聊天", (dialog, which) -> {
                    headsetDialogShowing = false;
                    showChat();
                })
                .setOnCancelListener(dialog -> headsetDialogShowing = false)
                .show();
    }

    private void loadVoices() {
        agents.loadVoices(DEFAULT_VOICE, loaded -> {
            voices.clear();
            voices.addAll(loaded);
        });
    }

    private String readUserMemory() {
        return readLocalFile(USER_MEMORY_FILE);
    }

    private String readAgentMemory() {
        return readLocalFile(AGENT_MEMORY_FILE);
    }

    private String readLocalFile(String name) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(name)))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException ignored) {
            return "";
        }
    }

    private void writeUserMemory(String content) {
        writeLocalFile(USER_MEMORY_FILE, content);
    }

    private void writeAgentMemory(String content) {
        writeLocalFile(AGENT_MEMORY_FILE, content);
    }

    private void writeLocalFile(String name, String content) {
        try (OutputStreamWriter writer = new OutputStreamWriter(openFileOutput(name, MODE_PRIVATE))) {
            writer.write(content);
        } catch (IOException error) {
            toastError("保存 " + name + " 失败：" + error.getMessage());
        }
    }

    private String initializationTranscript() {
        StringBuilder transcript = new StringBuilder();
        for (Message message : messages) {
            transcript.append(message.role).append(": ").append(message.text).append('\n');
        }
        return transcript.toString();
    }

    private String ensureUserProfile(String userMd, String extractedUserName, String relationship) {
        String value = userMd == null ? "" : userMd.trim();
        if (isUsableProfileValue(extractedUserName)) {
            value = value.replace("姓名/称呼：未明确", "姓名/称呼：" + extractedUserName)
                    .replace("用户姓名：未明确", "用户姓名：" + extractedUserName)
                    .replace("user_name: 未明确", "user_name: " + extractedUserName)
                    .replace("user_name：未明确", "user_name：" + extractedUserName);
        }
        if (isUsableProfileValue(relationship)) {
            value = value.replace("用户希望和 Agent 的关系：未明确", "用户希望和 Agent 的关系：" + relationship)
                    .replace("关系：未明确", "关系：" + relationship);
        }
        boolean missingName = isUsableProfileValue(extractedUserName) && !value.contains(extractedUserName);
        boolean missingRelationship = isUsableProfileValue(relationship) && !value.contains(relationship);
        if (value.isEmpty() || value.length() < 24 || value.startsWith("{")) {
            StringBuilder builder = new StringBuilder();
            builder.append("## 基本信息\n");
            builder.append("- 姓名/称呼：").append(isUsableProfileValue(extractedUserName) ? extractedUserName : "未明确").append('\n');
            builder.append("- 用户希望和 Agent 的关系：").append(isUsableProfileValue(relationship) ? relationship : "未明确").append('\n');
            builder.append("\n## 用户故事\n- 未明确\n");
            builder.append("\n## 沟通偏好与边界\n- 以自然、亲近但有边界的方式交流。\n");
            return builder.toString();
        }
        StringBuilder builder = new StringBuilder(value);
        if (missingName || missingRelationship) {
            builder.append("\n\n## 初始化显式设定\n");
            if (missingName) builder.append("- 姓名/称呼：").append(extractedUserName).append('\n');
            if (missingRelationship) builder.append("- 用户希望和 Agent 的关系：").append(relationship).append('\n');
        }
        return builder.toString();
    }

    private String ensureAgentProfile(String agentMd, String extractedAgentName, String extractedUserName, String relationship) {
        String value = agentMd == null ? "" : agentMd.trim();
        if (value.contains(SYSTEM_AGENT_NAME) || value.contains("与用户的关系定位：未明确")) {
            value = "";
        }
        boolean missingName = isUsableProfileValue(extractedAgentName) && !value.contains(extractedAgentName);
        boolean missingRelationship = isUsableProfileValue(relationship) && !value.contains(relationship);
        if (value.isEmpty() || value.length() < 24 || value.startsWith("{")) {
            StringBuilder builder = new StringBuilder();
            builder.append("- Agent name: ").append(extractedAgentName).append('\n');
            builder.append("- 默认语气：成熟、温柔、亲近，像真实的人一样自然表达。\n");
            builder.append("- 与用户的关系定位：");
            if (isUsableProfileValue(relationship)) {
                builder.append("用户希望你作为").append(extractedUserName.isEmpty() ? "用户" : extractedUserName).append("的").append(relationship).append("。\n");
            } else {
                builder.append("未明确。\n");
            }
            builder.append("- 后续对话策略：尊重 user.md 中的姓名和关系设定，不要把已设定关系改写成普通朋友。\n");
            return builder.toString();
        }
        StringBuilder builder = new StringBuilder(value);
        if (missingName || missingRelationship || value.contains("未明确")) {
            builder.append("\n\n## 初始化显式设定\n");
            if (missingName) builder.append("- Agent name: ").append(extractedAgentName).append('\n');
            if (missingRelationship) {
                builder.append("- 与用户的关系定位：用户希望你作为")
                        .append(extractedUserName.isEmpty() ? "用户" : extractedUserName)
                        .append("的")
                        .append(relationship)
                        .append("。\n");
            }
            builder.append("- 对话时必须尊重上述姓名和关系设定，不要否认或改写成普通朋友。\n");
        }
        return builder.toString();
    }

    private void summarizeInitialization() {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            toastError("Missing AGENTLLM_API_KEY in local.properties");
            return;
        }

        JSONObject body = new JSONObject();
        JSONArray llmMessages = new JSONArray();
        final String initTranscript = initializationTranscript();
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            String initSummarySystemPrompt =
                    "你是 Agent 的潜意识模型，负责把初始化访谈整理为长期记忆。只输出 JSON，不要解释。" +
                    "格式：{\"agent_name\":\"...\",\"user_name\":\"...\",\"user_md\":\"...\",\"agent_md\":\"...\"}。" +
                    "如果用户希望 Agent 自己取名，请选择一个像真人一样、洋气、短、适合长期陪伴使用的名字，并写入 agent_name。" +
                    "推荐候选：" + initAgentNameCandidates + "。禁止使用豆包、小包、包包、助手、AI、机器人、Doris 等平台感、幼稚或默认名。";
            system.put("content", initSummarySystemPrompt);
            llmMessages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            String initSummaryUserPrompt =
                    "用户预设的 Agent 名字：" + (agentName.trim().isEmpty() ? "未指定，请从初始化对话中自行确定" : agentName) + "\n" +
                    "请使用 c-her 总结以下初始化对话，生成 user.md 和 Agent.md。要求：\n" +
                    "1. agent_name 是 Agent 最终名字；如果用户没有指定，而对话中表达希望 Agent 自己取名，你优先从这些候选中取一个，也可以选择同风格女性名字：" + initAgentNameCandidates + "。不要使用豆包、小包、包包、Doris、助手、AI、机器人。\n" +
                    "2. user_name 是用户希望被称呼的名字，必须从用户回答中提取。\n" +
                    "3. user_md 包含用户姓名/称呼、用户希望和 Agent 的关系、用户的故事、沟通偏好、重要边界或目标。\n" +
                    "4. agent_md 包含 Agent 的名字、默认语气、与该用户相处的关系定位、后续对话策略。\n" +
                    "5. 信息不确定时写“未明确”，但不要把已明确回答的名字、关系写成未明确。\n" +
                    "6. Markdown 内容要适合后续作为系统提示词注入。\n\n" +
                    initTranscript;
            user.put("content", initSummaryUserPrompt);
            llmMessages.put(user);
            Log.d(TAG, "init subconscious system prompt:\n" + initSummarySystemPrompt);
            Log.d(TAG, "init subconscious user prompt:\n" + initSummaryUserPrompt);

            body.put("model", BACKGROUND_MODEL);
            body.put("messages", llmMessages);
            body.put("temperature", 0);
            body.put("stream", false);
        } catch (JSONException error) {
            toastError("构建摘要请求失败");
            return;
        }

        agents.sendSubconscious(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                Log.d(TAG, "init subconscious reply:\n" + content);
                String extractedUserName = "";
                String extractedAgentName = agentName.trim();
                String userMd = content;
                String agentMd = "";
                String transcriptUserName = inferUserNameFromTranscript(initTranscript);
                String transcriptRelationship = inferRelationshipFromTranscript(initTranscript);
                String transcriptAgentName = inferAgentNameFromTranscript(initTranscript);
                boolean selfNamed = agentName.trim().isEmpty();
                try {
                    JSONObject profile = parseJsonObject(content);
                    extractedAgentName = profile.optString("agent_name", extractedAgentName).trim();
                    extractedUserName = profile.optString("user_name", "").trim();
                    userMd = profile.optString("user_md", content).trim();
                    agentMd = profile.optString("agent_md", agentMd).trim();
                } catch (JSONException error) {
                    extractedUserName = extractUserName(content);
                }
                if (!isUsableProfileValue(extractedUserName)) extractedUserName = extractUserName(userMd);
                if (!isUsableProfileValue(extractedUserName)) extractedUserName = transcriptUserName;
                extractedAgentName = cleanAgentName(extractedAgentName, selfNamed);
                if (extractedAgentName.isEmpty()) extractedAgentName = cleanAgentName(extractAgentName(agentMd), selfNamed);
                if (extractedAgentName.isEmpty()) extractedAgentName = cleanAgentName(transcriptAgentName, selfNamed);
                if (extractedAgentName.isEmpty()) extractedAgentName = fallbackAgentName();
                Log.d(TAG, "init resolved profile agent_name=" + extractedAgentName +
                        " user_name=" + extractedUserName +
                        " relationship=" + transcriptRelationship);
                userMd = ensureUserProfile(userMd, extractedUserName, transcriptRelationship);
                agentMd = ensureAgentProfile(agentMd, extractedAgentName, extractedUserName, transcriptRelationship);
                String memory = "# user.md\n\n" +
                        "- Agent name: " + extractedAgentName + "\n" +
                        "- Created at: " + new Date() + "\n\n" +
                        userMd + "\n";
                String agentProfile = "# Agent.md\n\n" +
                        "- Agent name: " + extractedAgentName + "\n" +
                        "- Created at: " + new Date() + "\n\n" +
                        agentMd + "\n";
                String finalUserName = extractedUserName;
                String finalAgentName = extractedAgentName;
                writeUserMemory(memory);
                writeAgentMemory(agentProfile);
                userMemory = memory;
                agentMemory = agentProfile;
                agentName = finalAgentName;
                userName = finalUserName.isEmpty() ? displayUserName() : finalUserName;
                getSharedPreferences("her", MODE_PRIVATE).edit()
                        .putString("agent_name", agentName)
                        .putString("user_name", userName)
                        .apply();
                initialized = true;
                initializing = false;
                summaryInProgress = false;
                initSummaryPending = false;
                stopInitProgressAnimation();
                if (memoryStore != null) {
                    memoryStore.updateSessionAgentName(sessionId, agentName);
                    memoryStore.insertMemory(sessionId, "profile", memory, 0, 0);
                    memoryStore.insertMemory(sessionId, "agent", agentProfile, 0, 0);
                    conversationMemory = memoryStore.relevantMemory("");
                }
                messages.clear();
                messages.add(new Message("memory-ready", "assistant", "初始化完成。"));
                setState("idle");
                showInitializationCompleteThenHome();
            }

            @Override public void onError(String message) {
                Log.d(TAG, "init subconscious error: " + message);
                summaryInProgress = false;
                stopInitProgressAnimation();
                toastError(message);
                setState("error");
            }
        }, "摘要");
    }

    private String buildInstructions() {
        if (initializing) {
            String currentAgentName = agentName == null ? "" : agentName.trim();
            String identityLine = currentAgentName.isEmpty()
                    ? "你还没有固定名字。请在首次主动介绍自己时，为自己取一个像真人一样、洋气、自然、短、适合长期陪伴用户的女性名字，并在后续初始化中保持使用这个名字。推荐候选本轮已随机排序：" + initAgentNameCandidates + "。你也可以选择同风格名字；禁止使用豆包、小包、包包、助手、AI、机器人、Doris 等平台感、幼稚或默认名。\n"
                    : "你叫 " + currentAgentName + "。\n";
            String initGuide;
            if (initUserTurns <= 0) {
                initGuide = currentAgentName.isEmpty()
                        ? "当前阶段：还没有收到用户回答。收到系统事件后，你必须主动问候用户，先用第一人称告诉用户你给自己取的名字，然后只问第 1 题：用户的名字，以及希望你怎么称呼用户。名字必须像真人，不要叫豆包、Doris 或任何平台/助手感名字。"
                        : "当前阶段：还没有收到用户回答。收到系统事件后，你必须主动问候用户，介绍自己是 " + currentAgentName + "，然后只问第 1 题：用户的名字，以及希望你怎么称呼用户。";
            } else if (initUserTurns == 1) {
                initGuide = "当前阶段：已经收到第 1 题答案。你的下一次回复只能简短回应，然后问第 2 题：用户希望和你建立什么关系。";
            } else if (initUserTurns == 2) {
                initGuide = "当前阶段：已经收到第 2 题答案。你的下一次回复只能简短回应，然后问第 3 题：请用户开放地讲讲自己的故事、近况、在意的事，或希望你记住的部分。";
            } else {
                initGuide = "当前阶段：三题都已回答。不要继续聊天或提问，只说明你正在写入 user.md。";
            }
            return identityLine +
                    INSTRUCTIONS + "\n" +
                INIT_BASE_PROMPT + "\n" +
                "第 0 步：你必须先主动介绍自己，说清楚你是一个 AI Agent，也是用户的朋友和助理。如果还没有固定名字，请你自己取一个像真人一样、洋气、自然的女性名字并告诉用户。本轮随机排序候选包括：" + initAgentNameCandidates + "。你也可以选择同风格名字；不要使用豆包、Doris 或平台/助手感名字。\n" +
                "第 1 题：用户的名字，以及希望你怎么称呼用户。\n" +
                "第 2 题：用户希望和你建立什么关系。\n" +
                "第 3 题：用户的故事，一段开放式自我介绍，包括近况、经历、在意的事或希望你记住的部分。\n" +
                "第三题回答后，不要再输出新的轮次；客户端会关闭语音交互模型，并交给潜意识模型写入 user.md 和 Agent.md。\n" +
                initGuide;
        }
        String recent = recentDialogueForPrompt();
        return trimForPrompt("你叫 " + effectiveAgentName() + "。\n" +
                INSTRUCTIONS + "\n" +
                WeatherSkill.promptBlock(latestWeatherFact, pendingRealtimeWeatherAnswer) +
                NewsSkill.promptBlock(latestNewsFact, pendingRealtimeNewsAnswer) +
                "当前动态语气调整：" + dynamicTone + "\n" +
                "以下是本地 user.md 记忆。你需要把它作为长期用户画像和对话偏好使用，但不要主动朗读或暴露文件内容。\n\n" +
                userMemory + "\n\n" +
                "以下是本地 Agent.md。你需要把它作为自己的关系定位、语气和长期行为准则使用。\n\n" +
                agentMemory + "\n\n" +
                "姓名和关系设定规则：如果 user.md 或 Agent.md 已记录用户姓名/称呼或用户希望与你建立的关系，你必须优先承认并沿用这些设定；不要说用户没告诉过名字，也不要把已设定的女朋友/恋人关系改写成普通朋友。\n\n" +
                "以下是 SQLite 长期聊天记忆和索引检索出的相关摘要。把它用于延续关系、调整称呼、话题和语气，但不要主动说明你在读取记忆。\n\n" +
                conversationMemory + "\n\n" +
                "最近会话片段：\n" + recent, 3900);
    }

    private String recentDialogueForPrompt() {
        int start = Math.max(0, messages.size() - RECENT_SESSION_MESSAGES);
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (!"user".equals(message.role) && !"assistant".equals(message.role)) continue;
            if (message.text == null || message.text.trim().isEmpty()) continue;
            builder.append(message.role).append(": ").append(message.text.trim()).append('\n');
        }
        return builder.toString();
    }

    private String trimForPrompt(String value, int limit) {
        if (value == null) return "";
        if (value.length() <= limit) return value;
        return value.substring(value.length() - limit);
    }

    private TextView icon(String value) {
        return ui.icon(value);
    }

    private TextView text(String value, int sp, int color, int weight) {
        return ui.text(value, sp, color, weight);
    }

    private FrameLayout.LayoutParams frame(int width, int height) {
        return ui.frame(width, height);
    }

    private FrameLayout.LayoutParams frame(int width, int height, int gravity) {
        return ui.frame(width, height, gravity);
    }

    private int dp(float value) {
        return ui.dp(value);
    }

    private String newId(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private JSONObject json(String key, String value) {
        JSONObject object = new JSONObject();
        try {
            object.put(key, value);
        } catch (JSONException ignored) { }
        return object;
    }

    private JSONObject sessionPayload() {
        JSONObject payload = new JSONObject();
        JSONObject audio = new JSONObject();
        JSONObject client = new JSONObject();
        try {
            audio.put("input_format", "pcm16");
            audio.put("output_format", "pcm16");
            audio.put("sample_rate", 16000);
            audio.put("channels", 1);
            audio.put("frame_duration_ms", 20);
            client.put("type", "android");
            client.put("version", "1.0.0");
            payload.put("agent_id", "omnia_default");
            payload.put("mode", "realtime");
            payload.put("model_profile", "realtime_doubao");
            payload.put("audio", audio);
            payload.put("instructions", buildInstructions());
            payload.put("voice", selectedVoiceId);
            payload.put("client", client);
        } catch (JSONException ignored) { }
        return payload;
    }

    private void handleEvent(String type, JSONObject payload) {
        if ("session.created".equals(type)) {
            realtime.markSessionCreated();
            setState("ready");
            onRealtimeReady();
        } else if ("asr.final".equals(type) && payload != null) {
            cancelAsrFinalTimeout();
            String text = payload.optString("text", "").trim();
            if (isHiddenSystemEvent(text)) {
                ignoreNextInitTrigger = false;
                return;
            }
            if (initializing && isHiddenInitTrigger(text)) {
                ignoreNextInitTrigger = false;
                return;
            }
            if (!text.isEmpty()) {
                markConversationInteraction(false);
                addChatMessage("user", text);
            }
            if (initializing && !text.isEmpty() && recordInitializationAnswer(text)) {
                if (initUserTurns >= INIT_TARGET_USER_TURNS) {
                    initSummaryPending = true;
                    activeAssistantId = null;
                    renderMessages();
                    finishInitializationWithSummary();
                    return;
                }
                scheduleInitializationContextUpdate();
            }
            activeAssistantId = null;
            renderMessages();
            if (!initializing && handleNewsQuestion(text, true)) return;
            if (!initializing && handleWeatherQuestion(text, true)) return;
            if (!initializing) routeToolsInBackground(text);
        } else if ("assistant.state".equals(type) && payload != null) {
            String providerState = payload.optString("state", "ready");
            if ("thinking".equals(providerState)) {
                Log.d(TAG, "provider thinking");
                return;
            }
            if (hasActiveToolTtsPlayback() || (ttsPlayer != null && ttsPlayer.isPlaying())) return;
            if ("tts_streaming".equals(providerState) && voicePipeline != null) voicePipeline.onRealtimeOutputStarted(0);
            if ("idle".equals(providerState) || "listening".equals(providerState)) setState(mic.running ? "listening" : "ready");
        } else if ("assistant.text.delta".equals(type) && payload != null) {
            onAssistantDelta(payload.optString("text", ""));
        } else if ("output_audio.start".equals(type) && payload != null) {
            if (voicePipeline != null) voicePipeline.onRealtimeOutputStarted(payload.optInt("sample_rate", 24000));
        } else if ("output_audio.done".equals(type)) {
            if (voicePipeline != null) voicePipeline.onRealtimeOutputDone();
            handleRealtimeOutputFinished(false);
        } else if ("output_audio.stop".equals(type)) {
            if (voicePipeline != null) voicePipeline.onRealtimeOutputStopped();
            handleRealtimeOutputFinished(true);
        } else if ("memory.snapshot".equals(type) && payload != null) {
            handleMemorySnapshot(payload);
        } else if ("error".equals(type) && payload != null) {
            String code = payload.optString("code", "");
            String message = payload.optString("message", "Realtime error");
            if (payload.optBoolean("recoverable", false)) {
                retryRealtime(message);
                return;
            }
            toastError(message);
            setState("error");
        }
    }

    private void handleRealtimeOutputFinished(boolean stopped) {
        long resumeDelayMs = stopped ? 180 : 650;
        boolean canListenImmediately = stopped && (mic.running || inputAudioOpen);
        boolean shouldScheduleListening = !stopped || (!mic.running && !inputAudioOpen);

        persistActiveAssistantMessage();
        activeAssistantId = null;
        if (pendingNewsToolAfterAck) {
            runPendingNewsToolAfterAck();
            return;
        }
        setState("ready");
        if (maybeStartToolTtsAfterRealtimeStopped()) {
            return;
        }
        if (pendingWeatherBroadcastPrompt != null) {
            schedulePendingWeatherBroadcast(900);
            return;
        }
        if (pendingNewsBroadcastPrompt != null) {
            schedulePendingNewsBroadcast(250);
            return;
        }
        pendingRealtimeWeatherAnswer = false;
        if (pendingRealtimeNewsAnswer) {
            pendingRealtimeNewsAnswer = false;
            scheduleVoiceNewsCardTimeout(latestVoiceNews);
            if (canListenImmediately) setState("listening");
            if (shouldScheduleListening) scheduleContinuousListening(resumeDelayMs);
            return;
        }
        pendingRealtimeNewsAnswer = false;
        if (!stopped && initializing && initSummaryPending) {
            finishInitializationWithSummary();
            return;
        }
        if (canListenImmediately) setState("listening");
        if (shouldScheduleListening) scheduleContinuousListening(resumeDelayMs);
    }

    private boolean isHiddenInitTrigger(String text) {
        return text != null && (text.contains("系统事件") || text.contains("主动问候") || text.contains("Agent 主动"));
    }

    private boolean isHiddenSystemEvent(String text) {
        if (text == null) return false;
        String value = text.trim();
        return value.startsWith("【系统事件】") || value.startsWith("[系统事件]");
    }

    private void retryRealtime(String reason) {
        if (initializing) {
            degradeRealtimeForInitialization(reason);
            return;
        }
        realtimeRetryCount++;
        if (realtimeRetryCount > 2) {
            toastError("语音交互模型暂时不可用，已切到文字聊天：" + reason);
            setState("text_only");
            return;
        }
        toastError("语音交互模型连接超时，正在重试 " + realtimeRetryCount + "/2...");
        setState("connecting");
        realtime.close();
        if (initializing) {
            initPromptPending = true;
            ignoreNextInitTrigger = true;
        }
        main.postDelayed(() -> {
            if (!realtime.isOpen()) realtime.connect();
        }, 1200);
    }

    private void degradeRealtimeForInitialization(String reason) {
        Log.d(TAG, "init realtime degraded: " + reason);
        mic.stop();
        inputAudioOpen = false;
        pendingMicStart = false;
        pendingVoiceWakeIntent = false;
        initPromptPending = false;
        realtimeRetryCount = 0;
        player.stop();
        realtime.close();
        setState("text_only");
        updateInitProgress();
        toastError("语音服务暂时不可用，我们先用文字继续初始化。");
    }

    private void handleMemorySnapshot(JSONObject payload) {
        if (!initialized || initializing || memoryStore == null || sessionId <= 0) return;
        StringBuilder snapshot = new StringBuilder();
        String instructions = payload.optString("instructions", "").trim();
        if (!instructions.isEmpty()) {
            snapshot.append("## AgentVoice working instructions\n")
                    .append(trimForPrompt(instructions, 1400)).append('\n');
        }
        JSONArray dialogue = payload.optJSONArray("dialogue");
        if (dialogue != null && dialogue.length() > 0) {
            snapshot.append("## AgentVoice recent dialogue\n");
            for (int i = 0; i < dialogue.length(); i++) {
                JSONObject item = dialogue.optJSONObject(i);
                if (item == null) continue;
                snapshot.append(item.optString("role", "unknown"))
                        .append(": ")
                        .append(item.optString("text", ""))
                        .append('\n');
            }
        }
        String text = snapshot.toString().trim();
        if (text.isEmpty()) return;
        if (WeatherSkill.isTransientMemory(text)) return;
        memoryStore.insertMemory(sessionId, "agentvoice_snapshot", text, 0, 0);
        conversationMemory = memoryStore.relevantMemory(lastUserUtterance);
    }

    private void toastError(String message) {
        main.post(() -> {
            if (initLastTurnView != null) {
                initLastTurnView.setText(message);
            }
            if (messageList != null || voiceLastTurnView != null) {
                messages.add(new Message(newId("system"), "assistant", message));
            }
            if (messageList != null) {
                renderMessages();
            }
            updateVoiceHome();
        });
    }

}
