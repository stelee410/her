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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MainActivity extends Activity {
    private static final String TAG = "HerRealtime";
    private static final int REQ_AUDIO = 71;
    private static final int REQ_NOTIFY = 72;
    private static final int REQ_LOCATION = 73;
    private static final boolean HALF_DUPLEX = true;
    private static final boolean CONTINUOUS_CONVERSATION = true;
    private static final int INIT_TARGET_USER_TURNS = 3;
    private static final int COMPACT_MESSAGE_THRESHOLD = 10;
    private static final int COMPACT_CHAR_THRESHOLD = 3000;
    private static final int RECENT_SESSION_MESSAGES = 16;
    private static final long CHAT_ACTIVE_UNLOCK_MS = 60_000;
    private static final long RESPONSE_PENDING_VISIBLE_TIMEOUT_MS = 10_000;
    private static final long ASR_FINAL_TIMEOUT_MS = 2_800;
    private static final int TEXT_ASR_SLIDE_THRESHOLD_DP = 82;
    private static final int TEXT_ASR_PREBUFFER_MAX_BYTES = 96_000;
    private static final String USER_MEMORY_FILE = "user.md";
    private static final String AGENT_MEMORY_FILE = "Agent.md";
    private static final String SYSTEM_AGENT_NAME = "Doris";
    private static final String TEXT_CHAT_MODEL = "c-her";
    private static final String BACKGROUND_MODEL = "c-her";
    private static final String PREF_DEMO_MODE = "demo_mode";
    private static final String PREF_DIGITAL_AVATAR_ENABLED = "digital_avatar_enabled";
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
            "当用户焦虑、孤独、疲惫或犹豫时，先共情，再给一个轻柔可执行的下一步。\n" +
            "如果用户说“换个形象”“换装”“换套衣服”等数字形象控制口令，客户端会执行本地换装视频；你只回复“稍等”。\n" +
            "如果用户说“想看看你的宠物”“看看你的宠物”“小猫”“猫”等宠物展示口令，客户端会执行本地宠物视频；你只回复“稍等”。";
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
            setState(RealtimeConnectingDecision.nextState(isTextModeActive()));
        }

        @Override public void onRealtimeEvent(String type, JSONObject payload) {
            handleEvent(type, payload);
        }

        @Override public void onRealtimeAudio(byte[] bytes) {
            if (!RealtimeAudioPlaybackDecision.shouldPlay(
                    isTextModeActive(), voiceInputSurfaceActive, shouldDiscardRealtimeAudio())) return;
            player.play(bytes);
        }

        @Override public void onRealtimeError(String message) {
            handleRealtimeTransportError(message);
        }

        @Override public void onRealtimeClosed() {
            handleRealtimeClosed();
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
    private HeadsetController headsetController;
    private MemoryStore memoryStore;
    private MemoryCoordinator memoryCoordinator;
    private AgentApiClient agents;
    private ChatController chatController;
    private TextInputController textInputController;
    private TextModeAsrClient textModeAsr;
    private WeatherInteractionHandler weatherHandler;
    private NewsInteractionHandler newsHandler;
    private VoicePipelineManager voicePipeline;
    private PendingBroadcastCoordinator pendingBroadcasts;
    private ToolInteractionCoordinator toolInteractions;
    private ToolRouter toolRouter;
    private BackgroundToolRouteController backgroundToolRoutes;
    private ToolResultPresenter toolResultPresenter;
    private WeatherRequestCoordinator weatherRequests;
    private VoiceCardController voiceCards;
    private VoiceSessionOrchestrator voiceSession;
    private RealtimeErrorRecoveryController realtimeRecovery;
    private RealtimeClosedHandler realtimeClosedHandler;
    private RealtimeAsrFinalController realtimeAsrFinalController;
    private VoiceInputCoordinator voiceInput;
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
    private String avatarEmotion = AvatarVideoCatalog.EMOTION_NEUTRAL;
    private String lastUserUtterance = "";
    private VoiceSessionState voiceState = VoiceSessionState.initial();
    private String state = voiceState.legacyValue();
    private boolean initialized = false;
    private boolean initializing = false;
    private boolean initPromptPending = false;
    private boolean initSummaryPending = false;
    private boolean initOpeningDelivered = false;
    private boolean summaryInProgress = false;
    private boolean ignoreNextInitTrigger = false;
    private int initUserTurns = 0;
    private boolean headsetDialogShowing = false;
    private boolean demoMode = false;
    private boolean digitalAvatarEnabled = false;
    private boolean voiceInputSurfaceActive = false;
    private boolean compactInProgress = false;
    private boolean memoryDirtyForRealtime = false;
    private boolean inputAudioOpen = false;
    private boolean textModeAsrStarting = false;
    private boolean textModeAsrRecording = false;
    private boolean pendingTextModeAsrAfterPermission = false;
    private boolean textModeAsrTaskReady = false;
    private boolean textModeAsrFinishAfterStart = false;
    private boolean textModeAsrTouchActive = false;
    private boolean textModeAsrDiscardResult = false;
    private float textModeAsrTouchStartX = 0;
    private int textModeAsrGestureState = TextModeAsrGesture.NEUTRAL;
    private boolean textReplyPlaceholderVisible = false;
    private int textReplyPlaceholderFrame = 0;
    private int weatherIntentSeq = 0;
    private String pendingText = null;
    private String latestWeatherFact = "";
    private String latestNewsFact = "";
    private final AssistantMessageAccumulator assistantAccumulator =
            new AssistantMessageAccumulator(messages, role -> newId(role));
    private final VoiceActivityDetector voiceActivityDetector = new VoiceActivityDetector();
    private final InitializationSummaryRequestGate initializationSummaryGate =
            new InitializationSummaryRequestGate();
    private final MemoryCompactionRequestGate memoryCompactionGate =
            new MemoryCompactionRequestGate();
    private LinearLayout messageList;
    private ScrollView messageScroll;
    private EditText composer;
    private TextView stateLabel;
    private TextView initProgressView;
    private ImageButton textAsrButton;
    private TextView textAsrHint;
    private TextView initLastTurnView;
    private TextView voiceLastTurnView;
    private TextView micButton;
    private TextView homeTimeView;
    private TextView handwrittenNameView;
    private AudioLevelView audioLevelView;
    private VoiceOrbView voiceOrbView;
    private DigitalAvatarView digitalAvatarView;
    private MoodVeil moodVeil;
    private Runnable homeClockTicker;
    private Runnable initProgressTicker;
    private Runnable conversationLockTimeout;
    private Runnable responsePendingTimeout;
    private Runnable asrFinalTimeout;
    private Runnable textModeAsrPulse;
    private Runnable textReplyPlaceholderTicker;
    private boolean conversationOverLockscreen = false;
    private PowerManager.WakeLock replyWakeLock;
    private ValueAnimator replyBrightnessAnimator;
    private float replyOriginalBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    private long summaryStartedAt = 0;
    private final Object textModeAsrAudioLock = new Object();
    private final List<byte[]> textModeAsrBufferedAudio = new ArrayList<>();
    private int textModeAsrBufferedBytes = 0;

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
        textModeAsr = new TextModeAsrClient(TAG, main,
                BuildConfig.AGENTLLM_BASE_URL, BuildConfig.AGENTLLM_API_KEY);
        chatController = createChatController();
        textInputController = createTextInputController();
        weatherHandler = new WeatherInteractionHandler(new WeatherTool(llmHttp, main));
        newsHandler = new NewsInteractionHandler(new NewsTool(llmHttp, main));
        ttsPlayer = new GatewayTtsPlayer(TAG, llmHttp, main, getCacheDir(),
                BuildConfig.AGENTLLM_BASE_URL, BuildConfig.AGENTLLM_API_KEY,
                "doubao-tts", DEFAULT_VOICE);
        realtimeRecovery = createRealtimeErrorRecoveryController();
        realtimeClosedHandler = createRealtimeClosedHandler();
        realtimeAsrFinalController = createRealtimeAsrFinalController();
        voiceInput = createVoiceInputCoordinator();
        voicePipeline = createVoicePipelineManager();
        pendingBroadcasts = createPendingBroadcastCoordinator();
        toolInteractions = createToolInteractionCoordinator();
        toolRouter = createToolRouter();
        backgroundToolRoutes = createBackgroundToolRouteController();
        toolResultPresenter = createToolResultPresenter();
        weatherRequests = createWeatherRequestCoordinator();
        voiceCards = createVoiceCardController();
        voiceSession = createVoiceSessionOrchestrator();
        voices.add(new Voice(DEFAULT_VOICE, "Doris Clone", "female"));
        voices.add(new Voice("zh_female_roumeinvyou_emo_v2_mars_bigtts", "柔美女友（多情感）", "female"));
        voices.add(new Voice("zh_female_gaolengyujie_emo_v2_mars_bigtts", "高冷御姐（多情感）", "female"));
        voices.add(new Voice("zh_male_ruyayichen_emo_v2_mars_bigtts", "儒雅男友（多情感）", "male"));
        SharedPreferences prefs = getSharedPreferences("her", MODE_PRIVATE);
        headsets = new HeadsetBindingManager(this, prefs, this::onHeadsetDevicesChanged);
        headsets.start();
        demoMode = prefs.getBoolean(PREF_DEMO_MODE, false);
        digitalAvatarEnabled = prefs.getBoolean(PREF_DIGITAL_AVATAR_ENABLED, false);
        headsetController = createHeadsetController();
        setupHeadsetMediaSession();
        memoryStore = new MemoryStore(this);
        memoryCoordinator = createMemoryCoordinator();
        String persistedAgentName = prefs.getString("agent_name", SYSTEM_AGENT_NAME);
        String persistedUserName = prefs.getString("user_name", "");
        userMemory = readUserMemory();
        agentMemory = readAgentMemory();
        initialized = !userMemory.trim().isEmpty();
        agentName = StartupProfileNames.startupAgentName(
                persistedAgentName, initialized, agentMemory, SYSTEM_AGENT_NAME);
        userName = StartupProfileNames.startupUserName(persistedUserName, userMemory);
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

    private VoiceInputCoordinator createVoiceInputCoordinator() {
        return new VoiceInputCoordinator((runnable, delayMs) -> main.postDelayed(runnable, delayMs),
                new VoiceInputCoordinator.Host() {
                    @Override public boolean isTextModeActive() {
                        return MainActivity.this.isTextModeActive();
                    }

                    @Override public boolean isInputActive() {
                        return mic.running || inputAudioOpen;
                    }

                    @Override public boolean isRealtimeOpen() {
                        return realtime.isOpen();
                    }

                    @Override public boolean isBoundHeadsetConnected() {
                        return MainActivity.this.isBoundHeadsetConnected();
                    }

                    @Override public boolean hasRecordPermission() {
                        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    }

                    @Override public boolean hasActiveToolTtsPlayback() {
                        return MainActivity.this.hasActiveToolTtsPlayback() ||
                                (ttsPlayer != null && ttsPlayer.isPlaying());
                    }

                    @Override public boolean isReadyForContinuousListening() {
                        return voiceState.isReady();
                    }

                    @Override public boolean isVoiceSurfaceActive() {
                        return voiceInputSurfaceActive;
                    }

                    @Override public void requestRecordPermission() {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
                    }

                    @Override public void connectRealtime() {
                        realtime.connect();
                    }

                    @Override public void prepareInputStart(String interruptReason) {
                        interruptRealtimePlayback(interruptReason);
                    }

                    @Override public void startInputAudio() {
                        MainActivity.this.startInputAudio();
                    }

                    @Override public void stopInputAudio(String nextState) {
                        MainActivity.this.stopInputAudio(nextState);
                    }

                    @Override public void setState(String nextState) {
                        MainActivity.this.setState(nextState);
                    }

                    @Override public void showHeadsetPrompt() {
                        MainActivity.this.showHeadsetPrompt(true);
                    }

                    @Override public void logVoiceInput(String message) {
                        Log.d(TAG, "voice input " + message + " state=" + state);
                    }
                }, CONTINUOUS_CONVERSATION);
    }

    private VoicePipelineManager createVoicePipelineManager() {
        return new VoicePipelineManager((runnable, delayMs) -> main.postDelayed(runnable, delayMs),
                new VoicePipelineManager.Host() {
                    @Override public boolean isRealtimePlaybackActive() {
                        return isRealtimeOutputActive();
                    }

                    @Override public boolean isTextModeActive() {
                        return MainActivity.this.isTextModeActive();
                    }

                    @Override public boolean shouldDeferStart() {
                        return voiceState.isSpeaking() || voiceState.isResponsePending();
                    }

                    @Override public boolean isSpeakingState() {
                        return voiceState.isSpeaking();
                    }

                    @Override public void logToolTts(String message) {
                        Log.d(TAG, "tool tts " + message);
                    }

                    @Override public void interruptRealtimePlayback(String reason, boolean discardUntilDone) {
                        MainActivity.this.interruptRealtimePlayback(reason, discardUntilDone);
                    }

                    @Override public void prepareToolTtsPlayback() {
                        clearVoiceInputRequests();
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
                        if (isTextModeActive()) {
                            setState("text_only");
                            return;
                        }
                        setState("speaking");
                        updateVoiceHome();
                    }

                    @Override public void onToolTtsFinished(String id) {
                        if (id != null) Log.d(TAG, "tool tts finished id=" + id);
                        setState(isTextModeActive() ? "text_only" : "ready");
                    }

                    @Override public void resumeListeningAfterToolTts(long delayMs) {
                        MainActivity.this.resumeListeningAfterToolTts(delayMs);
                    }

                    @Override public boolean isExternalTtsPlaying() {
                        return ttsPlayer != null && ttsPlayer.isPlaying();
                    }

                    @Override public boolean isVoiceSurfaceActive() {
                        return voiceInputSurfaceActive;
                    }

                    @Override public void enterRealtimeSpeaking(int sampleRate) {
                        MainActivity.this.enterAssistantSpeaking(sampleRate);
                    }

                    @Override public void stopRealtimeOutput() {
                        player.stop();
                    }
                });
    }

    private PendingBroadcastCoordinator createPendingBroadcastCoordinator() {
        return new PendingBroadcastCoordinator((runnable, delayMs) -> main.postDelayed(runnable, delayMs),
                new PendingBroadcastCoordinator.Host() {
                    @Override public boolean isTextModeActive() {
                        return MainActivity.this.isTextModeActive();
                    }

                    @Override public boolean canSendWeatherNow() {
                        return !voiceState.isSpeaking() && !voiceState.isResponsePending();
                    }

                    @Override public boolean canSendNewsNow() {
                        return !voiceState.isSpeaking() && !voiceState.isResponsePending();
                    }

                    @Override public boolean isRealtimeOpen() {
                        return realtime.isOpen();
                    }

                    @Override public void connectRealtime() {
                        realtime.connect();
                    }

                    @Override public void pushWeatherFact() {
                        MainActivity.this.pushRealtimeWeatherFact();
                    }

                    @Override public void pushNewsFact() {
                        MainActivity.this.pushRealtimeNewsFact();
                    }

                    @Override public void sendRealtimeText(String text) {
                        realtime.sendInputText(text);
                    }

                    @Override public void onBroadcastSent() {
                        setState("processing");
                    }

                    @Override public void logBroadcast(String message) {
                        Log.d(TAG, message + " state=" + state);
                    }
                });
    }

    private ToolInteractionCoordinator createToolInteractionCoordinator() {
        return new ToolInteractionCoordinator(new ToolInteractionCoordinator.Host() {
            @Override public void onNewsStarted(String question, boolean realtimeMode) {
                latestNewsFact = "";
                clearPendingNewsBroadcast();
                latestWeatherFact = "";
                clearWeatherInteraction();
                if (!realtimeMode) {
                    addChatMessage("assistant", "稍等，我看一下新闻热点。");
                    renderMessages();
                }
            }

            @Override public void invalidateBackgroundToolRoute() {
                MainActivity.this.invalidateBackgroundToolRoute();
            }

            @Override public boolean startNewsAck(String question) {
                invalidateBackgroundToolRoute();
                interruptRealtimePlayback("news_tool_ack", false);
                assistantAccumulator.clearActive();
                setState("news_ack");
                if (!realtime.isOpen()) {
                    realtime.connect();
                    return false;
                }
                realtime.sendInputText(NewsSkill.LOOKUP_ACK_PROMPT);
                return true;
            }

            @Override public void startNewsFetch(String question, boolean realtimeMode, int token) {
                if (realtimeMode) {
                    invalidateBackgroundToolRoute();
                    if (realtime.isOpen()) realtime.close();
                    resetRealtimeOutput();
                    player.stop();
                    setState("news_tool");
                }
                runNewsTool(question, realtimeMode, token);
            }

            @Override public void onNewsCompleted(String question, boolean realtimeMode) {
                Log.d(TAG, "news interaction completed realtime=" + realtimeMode + " question=" + question);
            }

            @Override public void onNewsInterrupted(String question, boolean realtimeMode) {
                Log.d(TAG, "news interaction interrupted realtime=" + realtimeMode + " question=" + question);
            }

            @Override public void onWeatherStarted(String question, boolean realtimeMode) {
                latestWeatherFact = "";
                clearPendingWeatherBroadcast();
                clearNewsInteraction();
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
            }

            @Override public void startWeatherFetch(String question, boolean realtimeMode, int token) {
                resolveWeatherIntentAndRun(question, realtimeMode, token);
            }

            @Override public void onWeatherCompleted(String question, boolean realtimeMode) {
                Log.d(TAG, "weather interaction completed realtime=" + realtimeMode + " question=" + question);
            }

            @Override public void onWeatherInterrupted(String question, boolean realtimeMode) {
                Log.d(TAG, "weather interaction interrupted realtime=" + realtimeMode + " question=" + question);
            }

            @Override public void logToolInteraction(String message) {
                Log.d(TAG, message + " state=" + state);
            }
        });
    }

    private RealtimeErrorRecoveryController createRealtimeErrorRecoveryController() {
        return new RealtimeErrorRecoveryController(new RealtimeErrorRecoveryController.Host() {
            @Override public boolean isInitializing() {
                return initializing;
            }

            @Override public boolean isTextModeActive() {
                return MainActivity.this.isTextModeActive();
            }

            @Override public void logInitializationDegraded(String reason) {
                Log.d(TAG, "init realtime degraded: " + reason);
            }

            @Override public void stopMic() {
                mic.stop();
            }

            @Override public void markInputAudioClosed() {
                inputAudioOpen = false;
            }

            @Override public void clearVoiceInputRequests() {
                MainActivity.this.clearVoiceInputRequests();
            }

            @Override public void clearInitPromptPending() {
                initPromptPending = false;
            }

            @Override public void stopRealtimeAudio() {
                player.stop();
            }

            @Override public void closeRealtime() {
                realtime.close();
            }

            @Override public boolean isRealtimeOpen() {
                return realtime.isOpen();
            }

            @Override public void connectRealtime() {
                realtime.connect();
            }

            @Override public void postDelayed(Runnable runnable, long delayMs) {
                main.postDelayed(runnable, delayMs);
            }

            @Override public void setState(String nextState) {
                MainActivity.this.setState(nextState);
            }

            @Override public void updateInitProgress() {
                MainActivity.this.updateInitProgress();
            }

            @Override public void toastError(String message) {
                MainActivity.this.toastError(message);
            }
        });
    }

    private RealtimeClosedHandler createRealtimeClosedHandler() {
        return new RealtimeClosedHandler(new RealtimeClosedHandler.Host() {
            @Override public void resetRealtimeOutput() {
                MainActivity.this.resetRealtimeOutput();
            }

            @Override public boolean hasPendingToolTtsPlayback() {
                return MainActivity.this.hasPendingToolTtsPlayback();
            }

            @Override public boolean maybeStartToolTtsAfterRealtimeStopped() {
                return MainActivity.this.maybeStartToolTtsAfterRealtimeStopped();
            }

            @Override public boolean hasActiveToolTtsPlayback() {
                return MainActivity.this.hasActiveToolTtsPlayback();
            }

            @Override public boolean isGatewayTtsPlaying() {
                return ttsPlayer != null && ttsPlayer.isPlaying();
            }

            @Override public boolean isTextModeActive() {
                return MainActivity.this.isTextModeActive();
            }

            @Override public boolean isInitializing() {
                return initializing;
            }

            @Override public boolean isSummaryInProgress() {
                return summaryInProgress;
            }

            @Override public void setState(String nextState) {
                MainActivity.this.setState(nextState);
            }
        });
    }

    private RealtimeAsrFinalController createRealtimeAsrFinalController() {
        return new RealtimeAsrFinalController(new RealtimeAsrFinalController.Host() {
            @Override public boolean isInitializing() {
                return initializing;
            }

            @Override public boolean isTextModeActive() {
                return MainActivity.this.isTextModeActive();
            }

            @Override public void cancelAsrFinalTimeout() {
                MainActivity.this.cancelAsrFinalTimeout();
            }

            @Override public void clearIgnoreNextInitTrigger() {
                ignoreNextInitTrigger = false;
            }

            @Override public void markConversationInteraction() {
                MainActivity.this.markConversationInteraction(false);
            }

            @Override public void addUserMessage(String text) {
                MainActivity.this.addChatMessage("user", text);
                MainActivity.this.handleAvatarVoiceCommand(text);
            }

            @Override public boolean recordInitializationAnswer(String text) {
                return MainActivity.this.recordInitializationAnswer(text);
            }

            @Override public boolean hasReachedInitializationTarget() {
                return initUserTurns >= INIT_TARGET_USER_TURNS;
            }

            @Override public void markInitSummaryPending() {
                initSummaryPending = true;
            }

            @Override public void scheduleInitializationContextUpdate() {
                MainActivity.this.scheduleInitializationContextUpdate();
            }

            @Override public void clearActiveAssistant() {
                assistantAccumulator.clearActive();
            }

            @Override public void renderMessages() {
                MainActivity.this.renderMessages();
            }

            @Override public void finishInitializationWithSummary() {
                MainActivity.this.finishInitializationWithSummary();
            }

            @Override public boolean routeToolQuestion(String text, boolean realtimeMode) {
                return MainActivity.this.routeToolQuestion(text, realtimeMode);
            }

            @Override public void routeToolsInBackground(String text) {
                MainActivity.this.routeToolsInBackground(text);
            }

            @Override public void setState(String nextState) {
                MainActivity.this.setState(nextState);
            }
        });
    }

    private VoiceSessionOrchestrator createVoiceSessionOrchestrator() {
        return new VoiceSessionOrchestrator(new VoiceSessionOrchestrator.Host() {
            @Override public boolean isTextModeActive() {
                return MainActivity.this.isTextModeActive();
            }

            @Override public boolean isVoiceSurfaceActive() {
                return voiceInputSurfaceActive;
            }

            @Override public boolean isSummaryInProgress() {
                return summaryInProgress;
            }

            @Override public boolean hasActiveToolTtsPlayback() {
                return MainActivity.this.hasActiveToolTtsPlayback();
            }

            @Override public boolean isGatewayTtsPlaying() {
                return ttsPlayer != null && ttsPlayer.isPlaying();
            }

            @Override public boolean hasNewsInterruptionAvailable() {
                return VoiceInterruptionAvailability.hasNewsInterruption(
                        voiceState,
                        hasPendingNewsBroadcast(),
                        isNewsInteractionActive(),
                        voiceCards != null && voiceCards.hasNewsCard());
            }

            @Override public boolean hasWeatherInterruptionAvailable() {
                return VoiceInterruptionAvailability.hasWeatherInterruption(
                        voiceState,
                        hasPendingWeatherBroadcast(),
                        isWeatherInteractionActive(),
                        voiceCards != null && voiceCards.hasWeatherCard());
            }

            @Override public boolean isInputActive() {
                return mic.running || inputAudioOpen;
            }

            @Override public void markConversationInteraction() {
                MainActivity.this.markConversationInteraction(false);
            }

            @Override public void stopToolTtsPlayback(boolean interrupt) {
                MainActivity.this.stopToolTtsPlayback(interrupt);
            }

            @Override public void setState(String nextState) {
                MainActivity.this.setState(nextState);
            }

            @Override public void clearPendingNewsBroadcast() {
                MainActivity.this.clearPendingNewsBroadcast();
            }

            @Override public void clearPendingWeatherBroadcast() {
                MainActivity.this.clearPendingWeatherBroadcast();
            }

            @Override public void interruptNewsInteraction() {
                if (toolInteractions != null) toolInteractions.interruptNews();
            }

            @Override public void interruptWeatherInteraction() {
                if (toolInteractions != null) toolInteractions.interruptWeather();
            }

            @Override public void invalidateBackgroundToolRoute() {
                MainActivity.this.invalidateBackgroundToolRoute();
            }

            @Override public void invalidateWeatherIntentAndPendingRequest() {
                weatherIntentSeq++;
                if (weatherRequests != null) weatherRequests.clearPending();
            }

            @Override public boolean isRealtimeOpen() {
                return realtime.isOpen();
            }

            @Override public void interruptRealtimePlayback(String reason) {
                MainActivity.this.interruptRealtimePlayback(reason);
            }

            @Override public void closeRealtime() {
                realtime.close();
            }

            @Override public void connectRealtime() {
                realtime.connect();
            }

            @Override public void resetRealtimeOutput() {
                MainActivity.this.resetRealtimeOutput();
            }

            @Override public void stopRealtimePlayback() {
                player.stop();
            }

            @Override public void clearVoiceNewsCard(boolean refreshVoice) {
                MainActivity.this.clearVoiceNewsCard(refreshVoice);
            }

            @Override public void clearVoiceWeatherCard(boolean refreshVoice) {
                MainActivity.this.clearVoiceWeatherCard(refreshVoice);
            }

            @Override public void stopInputAudio(String nextState) {
                MainActivity.this.stopInputAudio(nextState);
            }

            @Override public void requestVoiceInputStart(boolean requestPermission, String interruptReason, boolean showHeadsetPrompt) {
                MainActivity.this.requestVoiceInputStart(requestPermission, interruptReason, showHeadsetPrompt);
            }

            @Override public boolean consumeInitPromptPending() {
                if (!initPromptPending) return false;
                initPromptPending = false;
                return true;
            }

            @Override public void updateInitializationContext() {
                MainActivity.this.updateInitializationContext();
            }

            @Override public boolean hasPendingWeatherBroadcast() {
                return MainActivity.this.hasPendingWeatherBroadcast();
            }

            @Override public void schedulePendingWeatherBroadcast(long delayMs) {
                MainActivity.this.schedulePendingWeatherBroadcast(delayMs);
            }

            @Override public boolean onToolRealtimeReady() {
                return toolInteractions != null && toolInteractions.onRealtimeReady();
            }

            @Override public String consumePendingText() {
                String text = pendingText;
                pendingText = null;
                return text;
            }

            @Override public void sendRealtimeText(String text) {
                realtime.sendInputText(text);
            }

            @Override public void onVoiceInputRealtimeReady() {
                if (voiceInput != null) voiceInput.onRealtimeReady();
            }

            @Override public boolean isInitializing() {
                return initializing;
            }

            @Override public boolean hasInitSummaryPending() {
                return initSummaryPending;
            }

            @Override public void persistAndClearActiveAssistantMessage() {
                persistActiveAssistantMessage();
                assistantAccumulator.clearActive();
            }

            @Override public boolean onToolRealtimeOutputFinished() {
                return toolInteractions != null && toolInteractions.onRealtimeOutputFinished();
            }

            @Override public boolean maybeStartToolTtsAfterRealtimeStopped() {
                return MainActivity.this.maybeStartToolTtsAfterRealtimeStopped();
            }

            @Override public boolean hasPendingNewsBroadcast() {
                return MainActivity.this.hasPendingNewsBroadcast();
            }

            @Override public void schedulePendingNewsBroadcast(long delayMs) {
                MainActivity.this.schedulePendingNewsBroadcast(delayMs);
            }

            @Override public void finishInitializationWithSummary() {
                MainActivity.this.finishInitializationWithSummary();
            }

            @Override public void scheduleContinuousListening(long delayMs) {
                MainActivity.this.scheduleContinuousListening(delayMs);
            }
        });
    }

    private ToolRouter createToolRouter() {
        return new ToolRouter(ToolRegistry.defaults(), new ToolRouter.Host() {
            @Override public boolean hasLatestWeatherFact() {
                return !latestWeatherFact.trim().isEmpty();
            }

            @Override public void reuseLatestWeatherFact(String question) {
                clearWeatherInteraction();
                pushRealtimeWeatherFact();
                setState("processing");
            }

            @Override public void startNews(String question, boolean realtimeMode) {
                if (toolInteractions != null) toolInteractions.startNews(question, realtimeMode);
            }

            @Override public void startNewsFromBackground(String question) {
                MainActivity.this.startNewsToolFromBackground(question);
            }

            @Override public void startWeather(String question, boolean realtimeMode) {
                if (toolInteractions != null) toolInteractions.startWeather(question, realtimeMode);
            }

            @Override public void logToolRoute(String message) {
                Log.d(TAG, message);
            }
        });
    }

    private BackgroundToolRouteController createBackgroundToolRouteController() {
        return new BackgroundToolRouteController(BACKGROUND_MODEL,
                (body, callback) -> agents.sendSubconscious(body, callback, "工具路由"),
                new BackgroundToolRouteController.Host() {
                    @Override public boolean hasApiKey() {
                        return !BuildConfig.AGENTLLM_API_KEY.isEmpty();
                    }

                    @Override public String effectiveAgentName() {
                        return MainActivity.this.effectiveAgentName();
                    }

                    @Override public void routeBackgroundDecision(String toolId, double confidence, String text) {
                        if (toolRouter != null) {
                            toolRouter.routeBackgroundDecision(toolId, confidence, text);
                        }
                    }

                    @Override public void logToolRoute(String message) {
                        Log.d(TAG, message);
                    }
                });
    }

    private ToolResultPresenter createToolResultPresenter() {
        return new ToolResultPresenter(new ToolResultPresenter.Host() {
            @Override public void cacheToolFact(String toolId, String fact) {
                if (NewsToolDefinition.ID.equals(toolId)) {
                    latestNewsFact = fact;
                } else if (WeatherToolDefinition.ID.equals(toolId)) {
                    latestWeatherFact = fact;
                }
            }

            @Override public void showNewsCard(NewsTool.NewsResult result) {
                addNewsCard(result);
            }

            @Override public void showWeatherCard(WeatherTool.WeatherResult result) {
                addWeatherCard(result);
            }

            @Override public void addAssistantMessage(String text) {
                MainActivity.this.addChatMessage("assistant", text);
            }

            @Override public void renderMessages() {
                MainActivity.this.renderMessages();
            }

            @Override public boolean isVoiceSurfaceActive() {
                return voiceInputSurfaceActive;
            }

            @Override public void queueToolTtsPlayback(String source, String text) {
                MainActivity.this.queueToolTtsPlayback(source, text);
            }

            @Override public void setState(String nextState) {
                MainActivity.this.setState(nextState);
            }

            @Override public void logToolResult(String message) {
                Log.d(TAG, message);
            }
        });
    }

    private WeatherRequestCoordinator createWeatherRequestCoordinator() {
        return new WeatherRequestCoordinator(new WeatherRequestCoordinator.Host() {
            @Override public boolean hasLocationPermission() {
                return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }

            @Override public void requestLocationPermission() {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQ_LOCATION);
            }

            @Override public boolean hasLocationManager() {
                return getSystemService(LOCATION_SERVICE) instanceof LocationManager;
            }

            @Override public Location bestLastLocation() {
                Object service = getSystemService(LOCATION_SERVICE);
                if (!(service instanceof LocationManager)) return null;
                return WeatherSkill.bestLastLocation((LocationManager) service);
            }

            @Override public void requestSingleLocation(WeatherSkill.LocationCallback success, WeatherSkill.ErrorCallback error) {
                Object service = getSystemService(LOCATION_SERVICE);
                if (!(service instanceof LocationManager)) {
                    error.onError("无法读取当前位置");
                    return;
                }
                WeatherSkill.requestSingleLocation((LocationManager) service, main, success, error);
            }

            @Override public void queryLocation(String question, Location location, boolean realtimeMode, int token) {
                if (weatherHandler != null) {
                    weatherHandler.queryLocation(question, location,
                            result -> onWeatherToolResult(result, realtimeMode, token));
                }
            }

            @Override public void failWeather(String question, String message, boolean realtimeMode, int token) {
                if (weatherHandler != null) {
                    weatherHandler.fail(question, message,
                            result -> onWeatherToolResult(result, realtimeMode, token));
                }
            }

            @Override public void logWeatherRequest(String message) {
                Log.d(TAG, message);
            }
        });
    }

    private VoiceCardController createVoiceCardController() {
        return new VoiceCardController(new VoiceCardController.Scheduler() {
            @Override public void postDelayed(Runnable runnable, long delayMs) {
                main.postDelayed(runnable, delayMs);
            }

            @Override public void removeCallbacks(Runnable runnable) {
                main.removeCallbacks(runnable);
            }
        }, new VoiceCardController.Host() {
            @Override public boolean isVoiceSurfaceActive() {
                return voiceLastTurnView != null;
            }

            @Override public void refreshVoiceHome() {
                showVoiceHome();
            }
        });
    }

    private HeadsetController createHeadsetController() {
        return new HeadsetController(new HeadsetController.Host() {
            @Override public void markConversationInteraction() {
                MainActivity.this.markConversationInteraction(false);
            }

            @Override public void interruptRealtimePlayback(String reason) {
                MainActivity.this.interruptRealtimePlayback(reason);
            }

            @Override public void stopToolTtsPlayback(boolean resumeListening) {
                MainActivity.this.stopToolTtsPlayback(resumeListening);
            }

            @Override public void persistAndClearActiveAssistantMessage() {
                MainActivity.this.persistActiveAssistantMessage();
                assistantAccumulator.clearActive();
            }

            @Override public void clearNewsInteraction() {
                MainActivity.this.clearNewsInteraction();
            }

            @Override public void clearWeatherInteraction() {
                MainActivity.this.clearWeatherInteraction();
            }

            @Override public boolean isTextModeActive() {
                return MainActivity.this.isTextModeActive();
            }

            @Override public boolean isInputActive() {
                return mic.running || inputAudioOpen;
            }

            @Override public void stopInputAudio(String nextState) {
                MainActivity.this.stopInputAudio(nextState);
            }

            @Override public void setState(String nextState) {
                MainActivity.this.setState(nextState);
            }

            @Override public void toast(String message) {
                MainActivity.this.toastError(message);
            }

            @Override public void refreshVoiceControls() {
                refreshHeadsetDependentControls();
            }

            @Override public void connectRealtime() {
                realtime.connect();
            }
        });
    }

    private ChatController createChatController() {
        return new ChatController(TEXT_CHAT_MODEL,
                (model, instructions, text, callback) -> agents.sendChat(model, instructions, text, callback),
                new ChatController.Host() {
                    @Override public boolean hasApiKey() {
                        return !BuildConfig.AGENTLLM_API_KEY.isEmpty();
                    }

                    @Override public String buildTextChatInstructions() throws JSONException {
                        return MainActivity.this.buildTextChatInstructions();
                    }

                    @Override public void setState(String nextState) {
                        MainActivity.this.setState(nextState);
                    }

                    @Override public void toastError(String message) {
                        MainActivity.this.toastError(message);
                    }

                    @Override public void showReplyPlaceholder() {
                        MainActivity.this.showTextReplyPlaceholder();
                    }

                    @Override public void hideReplyPlaceholder() {
                        MainActivity.this.hideTextReplyPlaceholder();
                    }

                    @Override public void breatheScreenForAssistantReply() {
                        MainActivity.this.breatheScreenForAssistantReply();
                    }

                    @Override public void addAssistantMessage(String text) {
                        MainActivity.this.addChatMessage("assistant", text);
                    }

                    @Override public void renderMessages() {
                        MainActivity.this.renderMessages();
                    }

                    @Override public boolean isInitializing() {
                        return initializing;
                    }

                    @Override public void updateInitProgress() {
                        MainActivity.this.updateInitProgress();
                    }
                });
    }

    private TextInputController createTextInputController() {
        return new TextInputController(new TextInputController.Host() {
            @Override public boolean isSummaryInProgress() {
                return summaryInProgress;
            }

            @Override public boolean isInitializing() {
                return initializing;
            }

            @Override public void markConversationInteraction() {
                MainActivity.this.markConversationInteraction(false);
            }

            @Override public void addUserMessage(String text) {
                MainActivity.this.addChatMessage("user", text);
                MainActivity.this.handleAvatarVoiceCommand(text);
            }

            @Override public boolean recordInitializationAnswer(String text) {
                return MainActivity.this.recordInitializationAnswer(text);
            }

            @Override public boolean hasReachedInitializationTarget() {
                return initUserTurns >= INIT_TARGET_USER_TURNS;
            }

            @Override public void scheduleInitializationContextUpdate() {
                MainActivity.this.scheduleInitializationContextUpdate();
            }

            @Override public void clearActiveAssistant() {
                assistantAccumulator.clearActive();
            }

            @Override public void renderMessages() {
                MainActivity.this.renderMessages();
            }

            @Override public void finishInitializationWithSummary() {
                MainActivity.this.finishInitializationWithSummary();
            }

            @Override public boolean routeToolQuestion(String text, boolean realtimeMode) {
                return MainActivity.this.routeToolQuestion(text, realtimeMode);
            }

            @Override public void sendTextWithAgentLLM(String text) {
                MainActivity.this.sendTextWithAgentLLM(text);
            }
        });
    }

    private MemoryCoordinator createMemoryCoordinator() {
        return new MemoryCoordinator(COMPACT_MESSAGE_THRESHOLD, COMPACT_CHAR_THRESHOLD,
                new MemoryCoordinator.Host() {
                    @Override public boolean isInitialized() {
                        return initialized;
                    }

                    @Override public boolean isInitializing() {
                        return initializing;
                    }

                    @Override public boolean isSummaryInProgress() {
                        return summaryInProgress;
                    }

                    @Override public boolean isCompactInProgress() {
                        return compactInProgress;
                    }

                    @Override public long sessionId() {
                        return sessionId;
                    }

                    @Override public MemoryCoordinator.Store memoryStore() {
                        if (memoryStore == null) return null;
                        return new MemoryCoordinator.Store() {
                            @Override public void insertMessage(long sessionId, String role, String content) {
                                memoryStore.insertMessage(sessionId, role, content);
                            }

                            @Override public String relevantMemory(String query) {
                                return memoryStore.relevantMemory(query);
                            }

                            @Override public MemoryChunk unsummarizedChunk(long sessionId, int minCount, int minChars) {
                                return memoryStore.unsummarizedChunk(sessionId, minCount, minChars);
                            }
                        };
                    }

                    @Override public void onUserMessagePersisted(String text, String relevantMemory) {
                        lastUserUtterance = text;
                        conversationMemory = relevantMemory;
                        memoryDirtyForRealtime = true;
                    }

                    @Override public void applyContextUpdateForNextTurn() {
                        MainActivity.this.applyContextUpdateForNextTurn(false);
                    }

                    @Override public void markCompactInProgress() {
                        compactInProgress = true;
                    }

                    @Override public void compactConversation(MemoryChunk chunk) {
                        MainActivity.this.compactConversation(chunk);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        VoiceRuntimeShutdownCleanupDecision shutdown =
                VoiceRuntimeShutdownCleanupDecision.shutdown();
        if (shutdown.cancelVoiceInputRequests) clearVoiceInputRequests();
        if (shutdown.clearToolInteractions) {
            clearNewsInteraction();
            clearWeatherInteraction();
        }
        if (shutdown.invalidateBackgroundToolRoute) invalidateBackgroundToolRoute();
        if (shutdown.cancelMemoryCompaction) cancelMemoryCompaction();
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        if (initProgressTicker != null) main.removeCallbacks(initProgressTicker);
        if (shutdown.cancelVoiceCardTimeouts && voiceCards != null) voiceCards.cancelTimeouts();
        if (conversationLockTimeout != null) main.removeCallbacks(conversationLockTimeout);
        if (responsePendingTimeout != null) main.removeCallbacks(responsePendingTimeout);
        if (asrFinalTimeout != null) main.removeCallbacks(asrFinalTimeout);
        stopTextReplyPlaceholderTicker();
        clearConversationOverLockscreen();
        restoreReplyScreenBrightness();
        releaseReplyWakeLock();
        releaseHeadsetMediaSession();
        if (headsets != null) headsets.stop();
        mic.stop();
        player.stop();
        if (shutdown.stopToolTtsPlayback) stopToolTtsPlayback(false);
        else stopOpeningTts();
        cancelTextModeAsr();
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
        AssistantLaunchDecision.ScreenAction screenAction =
                AssistantLaunchDecision.screenAction(initialized, initializing);
        if (screenAction == AssistantLaunchDecision.ScreenAction.SHOW_VOICE_HOME) {
            showVoiceHome();
        } else if (screenAction == AssistantLaunchDecision.ScreenAction.SHOW_INITIALIZATION_HOME) {
            showInitializationHome();
        } else {
            beginInitialization("");
        }
        main.postDelayed(() -> {
            AssistantLaunchDecision.VoiceAction voiceAction = AssistantLaunchDecision.voiceAction(
                    isBoundHeadsetConnected(),
                    voiceInputSurfaceActive,
                    summaryInProgress,
                    isAnyGatewayOrToolTtsPlaying(),
                    mic.running,
                    inputAudioOpen);
            if (voiceAction == AssistantLaunchDecision.VoiceAction.START) {
                startVoiceFromAssistantCommand();
            } else if (voiceAction == AssistantLaunchDecision.VoiceAction.PROMPT_HEADSET) {
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
        if (AssistantLaunchDecision.voiceAction(
                isBoundHeadsetConnected(),
                voiceInputSurfaceActive,
                summaryInProgress,
                isAnyGatewayOrToolTtsPlaying(),
                mic.running,
                inputAudioOpen)
                == AssistantLaunchDecision.VoiceAction.START) {
            requestVoiceInputStart(true, "assistant_launch", true);
        }
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
                if (headsetController != null) headsetController.interruptCurrentConversation();
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
        if (headsetController == null || event == null) return false;
        return headsetController.onMediaButton(mapHeadsetMediaButton(event.getKeyCode()),
                event.getAction() == KeyEvent.ACTION_DOWN,
                event.getRepeatCount(),
                SystemClock.uptimeMillis());
    }

    private void handleHeadsetTransportClick() {
        if (headsetController != null) headsetController.onTransportClick(SystemClock.uptimeMillis());
    }

    private HeadsetController.MediaButton mapHeadsetMediaButton(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) return HeadsetController.MediaButton.NEXT;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) return HeadsetController.MediaButton.HOOK;
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return HeadsetController.MediaButton.PLAY_PAUSE;
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) return HeadsetController.MediaButton.PLAY;
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) return HeadsetController.MediaButton.PAUSE;
        return HeadsetController.MediaButton.OTHER;
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
        PermissionResultDecision decision = PermissionResultDecision.decide(
                requestCode, grants, REQ_AUDIO, REQ_LOCATION, PackageManager.PERMISSION_GRANTED);
        if (decision.action == PermissionResultDecision.Action.RECORD_GRANTED) {
            if (pendingTextModeAsrAfterPermission) {
                pendingTextModeAsrAfterPermission = false;
                startTextModeAsr();
                return;
            }
            if (voiceInput != null) voiceInput.onRecordPermissionGranted();
        } else if (decision.action == PermissionResultDecision.Action.RECORD_DENIED) {
            if (pendingTextModeAsrAfterPermission) {
                pendingTextModeAsrAfterPermission = false;
                toastError("需要录音权限才能语音输入文字。");
                return;
            }
            if (voiceInput != null) voiceInput.onRecordPermissionDenied();
        } else if (decision.action == PermissionResultDecision.Action.LOCATION_RESULT) {
            if (weatherRequests != null) {
                weatherRequests.onLocationPermissionResult(decision.granted);
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
        cancelTextModeAsr();
        clearVoiceCards(false);
        leaveVoiceSurface();
        messageList = null;
        messageScroll = null;
        composer = null;
        textAsrButton = null;
        textAsrHint = null;
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
        digitalAvatarView = null;
        homeTimeView = views.homeTimeView;
        handwrittenNameView = views.handwrittenNameView;
        setContentView(root);
        animateAgentName();
        startHomeClock();
    }

    private void showVoiceHome() {
        cancelTextModeAsr();
        voiceInputSurfaceActive = true;
        messageList = null;
        messageScroll = null;
        composer = null;
        textAsrButton = null;
        textAsrHint = null;
        homeTimeView = null;
        handwrittenNameView = null;
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        HomePage.Views views = HomePage.renderVoice(this, ui,
                new HomePage.VoiceModel(lastConversationLine(),
                        stateLabelText(),
                        moodForText(lastConversationLine()),
                        voiceCards == null ? null : voiceCards.latestWeather(),
                        voiceCards == null ? null : voiceCards.latestNews(),
                        digitalAvatarEnabled,
                        voiceState.isSpeaking(),
                        avatarEmotion),
                new HomePage.Callbacks() {
                    @Override public void onSettings() { showSettings(); }
                    @Override public void onChat() { showChat(); }
                    @Override public void onVoiceHome() { showVoiceHome(); }
                    @Override public void onToggleMic() { toggleMic(); }
                });
        root = views.root;
        moodVeil = views.moodVeil;
        voiceOrbView = views.voiceOrbView;
        digitalAvatarView = views.digitalAvatarView;
        voiceLastTurnView = views.voiceLastTurnView;
        stateLabel = views.stateLabel;
        audioLevelView = views.audioLevelView;
        micButton = views.micButton;
        setContentView(root);
        updateVoiceHome();
        autoStartListeningOnVoiceSurface();
    }

    private void autoStartListeningOnVoiceSurface() {
        if (VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                initialized,
                initializing,
                summaryInProgress,
                hasActiveToolTtsPlayback(),
                ttsPlayer != null && ttsPlayer.isPlaying(),
                mic.running,
                inputAudioOpen,
                isBoundHeadsetConnected())) {
            requestVoiceInputStart(true);
        }
    }

    private String displayUserName() {
        return StartupProfileNames.displayUserName(userName);
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

    private String lastAssistantBeforeLatestUser() {
        return ConversationHistory.lastAssistantBeforeLatestUser(messages);
    }

    private boolean recordInitializationAnswer(String text) {
        String lastAssistant = lastAssistantBeforeLatestUser();
        int nextTurn = InitializationAnswerTracker.nextTurn(initUserTurns, lastAssistant, text);
        boolean advanced = nextTurn > initUserTurns;
        if (advanced) initUserTurns = nextTurn;
        if (advanced) updateInitProgress();
        return advanced;
    }

    private String fallbackAgentName() {
        return randomAgentNameCandidate();
    }

    private String effectiveAgentName() {
        return StartupProfileNames.effectiveAgentName(agentName, SYSTEM_AGENT_NAME);
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
        cancelMemoryCompaction();
        mic.stop();
        player.stop();
        realtime.close();
        stopToolTtsPlayback(false);
        deleteFile(USER_MEMORY_FILE);
        deleteFile(AGENT_MEMORY_FILE);
        if (memoryStore != null) memoryStore.resetAll();
        SessionClearState.ResetInitializationFields reset =
                SessionClearState.resetInitialization("保持温柔大姐姐语气：成熟、关照、亲近但有边界。");
        agentName = reset.agentName;
        sessionId = memoryStore == null ? -1 : memoryStore.startSession("initializing");
        if (memoryCoordinator != null) memoryCoordinator.clearPersistedMessageIds();
        getSharedPreferences("her", MODE_PRIVATE).edit()
                .remove("user_name")
                .remove("agent_name")
                .apply();
        applyResetInitializationFields(reset);
        if (realtimeRecovery != null) realtimeRecovery.resetRetryCount();
        clearVoiceInputRequests();
        clearWeatherInteraction();
        clearNewsInteraction();
        clearVoiceCards(false);
        assistantAccumulator.clearActive();
        messages.clear();
        setState("idle");
        beginInitialization("");
    }

    private void showInitialize() {
        clearVoiceCards(false);
        voiceInputSurfaceActive = false;
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
        initializationSummaryGate.invalidate();
        cancelMemoryCompaction();
        InitializationStartState.Result startState =
                InitializationStartState.from(name, isBoundHeadsetConnected());
        agentName = startState.agentName;
        initAgentNameCandidates = shuffledAgentNameCandidates();
        Log.d(TAG, "init agent name candidates: " + initAgentNameCandidates);
        SharedPreferences.Editor prefs = getSharedPreferences("her", MODE_PRIVATE).edit();
        if (!startState.shouldPersistAgentName) prefs.remove("agent_name");
        else prefs.putString("agent_name", agentName);
        prefs.apply();
        if (memoryStore != null) {
            sessionId = memoryStore.startSession(startState.sessionAgentName);
            if (memoryCoordinator != null) memoryCoordinator.clearPersistedMessageIds();
        }
        initializing = true;
        initialized = false;
        initPromptPending = true;
        initSummaryPending = false;
        initOpeningDelivered = false;
        summaryInProgress = false;
        ignoreNextInitTrigger = true;
        if (realtimeRecovery != null) realtimeRecovery.resetRetryCount();
        initUserTurns = 0;
        assistantAccumulator.clearActive();
        messages.clear();
        if (startState.shouldEnterTextOnly) {
            setState("text_only");
        }
        realtime.close();
        showInitializationHome();
        deliverInitializationOpening();
    }

    private void deliverInitializationOpening() {
        if (!InitializationOpening.shouldDeliver(initializing, initOpeningDelivered)) return;
        ensureInitializationAgentName();
        deliverTtsInitializationOpening();
    }

    private void ensureInitializationAgentName() {
        String fallback = InitializationAgentNameDecision.requiresFallback(agentName)
                ? randomAgentNameCandidate()
                : "";
        InitializationAgentNameDecision decision = InitializationAgentNameDecision.ensure(
                agentName,
                fallback,
                memoryStore != null && sessionId > 0);
        agentName = decision.agentName;
        if (!decision.selectedFallback) return;
        Log.d(TAG, "init fallback selected agent name: " + agentName);
        if (decision.persistAgentName) {
            getSharedPreferences("her", MODE_PRIVATE).edit()
                    .putString("agent_name", agentName)
                    .apply();
        }
        if (decision.updateSessionAgentName) {
            memoryStore.updateSessionAgentName(sessionId, agentName);
        }
    }

    private void deliverTtsInitializationOpening() {
        if (!InitializationOpening.shouldDeliverTts(
                initializing, initOpeningDelivered, isTextModeActive())) return;
        initOpeningDelivered = true;
        String name = InitializationOpening.cleanAgentName(agentName);
        if (name.isEmpty()) name = randomAgentNameCandidate();
        String opening = InitializationOpening.openingText(name);
        Log.d(TAG, "init tts opening: " + opening);
        updateInitProgress();
        updateVoiceHome();
        speakOpeningWithGatewayTts(opening);
    }

    private void speakOpeningWithGatewayTts(String text) {
        if (text == null || text.trim().isEmpty()) return;
        ttsPlayer.play("init-opening", text, new GatewayTtsPlayer.Listener() {
            @Override public void onStarted(String id, String spokenText) {
                if (!InitializationOpening.shouldHandleTtsCallback(
                        initializing, isTextModeActive(), voiceInputSurfaceActive)) return;
                showInitializationOpeningSubtitle(spokenText);
            }

            @Override public void onCompleted(String id) {
                if (!InitializationOpening.shouldHandleTtsCallback(
                        initializing, isTextModeActive(), voiceInputSurfaceActive)) return;
                startRealtimeListeningAfterOpening();
            }

            @Override public void onError(String id, String message) {
                Log.d(TAG, "init tts failed: " + message);
                if (!InitializationOpening.shouldHandleTtsCallback(
                        initializing, isTextModeActive(), voiceInputSurfaceActive)) return;
                showInitializationOpeningSubtitle(text);
                startRealtimeListeningAfterOpening();
            }
        });
    }

    private void showInitializationOpeningSubtitle(String opening) {
        if (InitializationOpening.shouldAddSubtitle(initializing, opening, hasMessage("init-opening"))) {
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
        VoiceAutoStartDecision.OpeningAction action =
                VoiceAutoStartDecision.afterInitializationOpening(
                        initializing, summaryInProgress, isBoundHeadsetConnected());
        if (action == VoiceAutoStartDecision.OpeningAction.START_LISTENING) {
            requestVoiceInputStart(true);
        } else if (action == VoiceAutoStartDecision.OpeningAction.PROMPT_HEADSET) {
            setState("text_only");
            showHeadsetPrompt(true);
        }
    }

    private void stopOpeningTts() {
        if (ttsPlayer != null) ttsPlayer.stop();
    }

    private void showInitializationHome() {
        clearVoiceCards(false);
        voiceInputSurfaceActive = true;
        messageList = null;
        messageScroll = null;
        composer = null;
        textAsrHint = null;
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
        clearVoiceCards(false);
        enterTextMode();
        homeTimeView = null;
        clearVoiceSurfaceViews();
        ChatPage.Views views = ChatPage.render(this, ui,
                new ChatPage.Model(agentName, currentStateLabelText(), initializing, initProgressText(),
                        textModeAsrStarting || textModeAsrRecording, textReplyPlaceholderText(),
                        moodForText(lastConversationLine()), messages),
                new ChatPage.Callbacks() {
                    @Override public void onBack() { if (initialized) openVoiceSurface(); else showInitializationHome(); }
                    @Override public void onAsrPressStart(float rawX) { startTextModeAsrPress(rawX); }
                    @Override public void onAsrPressMove(float rawX) { moveTextModeAsrPress(rawX); }
                    @Override public void onAsrPressEnd(float rawX) { endTextModeAsrPress(rawX); }
                    @Override public void onAsrPressCancel() { cancelTextModeAsrPress(); }
                    @Override public void onSend(String text) { sendText(text); }
                });
        root = views.root;
        moodVeil = views.moodVeil;
        messageList = views.messageList;
        messageScroll = views.messageScroll;
        composer = views.composer;
        stateLabel = views.stateLabel;
        initProgressView = views.initProgressView;
        textAsrButton = views.asrButton;
        textAsrHint = views.asrHint;
        setContentView(root);
    }

    private void enterTextMode() {
        voiceInputSurfaceActive = false;
        TextModeEntryCleanupDecision cleanup = TextModeEntryCleanupDecision.enterTextMode();
        if (cleanup.resetRealtimeRetries && realtimeRecovery != null) {
            realtimeRecovery.resetRetryCount();
        }
        if (cleanup.stopToolTtsPlayback) stopToolTtsPlayback(false);
        if (cleanup.stopRealtimeAudio) player.stop();
        if (cleanup.resetRealtimeOutput) resetRealtimeOutput();
        if (cleanup.clearVoiceInputRequests) clearVoiceInputRequests();
        if (cleanup.clearPendingBroadcasts) {
            clearPendingWeatherBroadcast();
            clearPendingNewsBroadcast();
        }
        if (voiceInput != null) {
            voiceInput.enterTextMode();
            return;
        }
        if (mic.running || inputAudioOpen) stopInputAudio("text_only");
        else setState("text_only");
    }

    private boolean isTextModeActive() {
        return composer != null;
    }

    private void leaveVoiceSurface() {
        voiceInputSurfaceActive = false;
        VoiceSurfaceExitCleanupDecision cleanup =
                VoiceSurfaceExitCleanupDecision.leaveVoiceSurface(mic.running || inputAudioOpen);
        if (cleanup.cancelVoiceInputRequests) clearVoiceInputRequests();
        if (cleanup.stopActiveInput) stopInputAudio(cleanup.nextInputState);
        if (cleanup.stopToolTtsPlayback) stopToolTtsPlayback(false);
        if (cleanup.stopRealtimeAudio) player.stop();
        if (cleanup.resetRealtimeOutput) resetRealtimeOutput();
        if (cleanup.clearVoiceSurfaceViews) clearVoiceSurfaceViews();
    }

    private void clearVoiceSurfaceViews() {
        voiceLastTurnView = null;
        voiceOrbView = null;
        digitalAvatarView = null;
        audioLevelView = null;
        micButton = null;
        textAsrButton = null;
        textAsrHint = null;
    }

    private void openVoiceSurface() {
        cancelTextModeAsr();
        VoiceSurfaceNavigationDecision.ScreenAction screenAction =
                VoiceSurfaceNavigationDecision.screenAction(initialized, initializing);
        if (screenAction == VoiceSurfaceNavigationDecision.ScreenAction.SHOW_VOICE_HOME) {
            showVoiceHome();
        } else if (screenAction == VoiceSurfaceNavigationDecision.ScreenAction.SHOW_INITIALIZATION_HOME) {
            showInitializationHome();
        }
        if (VoiceSurfaceNavigationDecision.shouldPromptHeadset(
                isBoundHeadsetConnected(), voiceInputSurfaceActive)) {
            main.postDelayed(() -> {
                if (VoiceSurfaceNavigationDecision.shouldPromptHeadset(
                        isBoundHeadsetConnected(), voiceInputSurfaceActive)) {
                    showHeadsetPrompt(true);
                }
            }, 160);
        }
    }

    private void showVoices() {
        clearVoiceCards(false);
        leaveVoiceSurface();
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
        clearVoiceCards(false);
        leaveVoiceSurface();
        root = baseRoot();
        Runnable back = initializing ? this::showInitializationHome : (initialized ? this::showHome : () -> beginInitialization(agentName));
        root.addView(topBar("‹", "Settings", "", back, null));
        LinearLayout list = screenList();
        list.addView(navRow("↺", "Reinitialize", "Reset memory", this::resetInitialization));
        list.addView(navRow("⌫", "Clear Session", "Keep memory", this::clearCurrentSession));
        list.addView(navRow("▣", "演示模式", demoMode ? "On · 本机麦克风与 Speaker" : "Off", this::showDemoModePrompt));
        list.addView(navRow("◌", "Enable 数字形象", digitalAvatarEnabled ? "On · Voice chat" : "Off",
                () -> setDigitalAvatarEnabled(!digitalAvatarEnabled)));
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
        return HeadsetSettingsLabel.build(
                demoMode,
                headsets != null && headsets.hasBoundHeadset(),
                headsets == null ? "" : headsets.boundLabel(),
                headsets != null && headsets.isBoundConnected());
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
        if (headsetController != null) {
            headsetController.onDemoModeChanged(demoMode,
                    initialized || initializing,
                    realtime.isOpen(),
                    isBoundHeadsetConnected());
        }
        showSettings();
    }

    private void setDigitalAvatarEnabled(boolean enabled) {
        if (digitalAvatarEnabled == enabled) return;
        digitalAvatarEnabled = enabled;
        getSharedPreferences("her", MODE_PRIVATE).edit()
                .putBoolean(PREF_DIGITAL_AVATAR_ENABLED, digitalAvatarEnabled)
                .apply();
        showSettings();
    }

    private void clearCurrentSession() {
        cancelMemoryCompaction();
        mic.stop();
        player.stop();
        realtime.close();
        stopToolTtsPlayback(false);
        applyRuntimeClearFields(SessionClearState.clearedRuntime());
        clearVoiceInputRequests();
        clearWeatherInteraction();
        clearNewsInteraction();
        clearVoiceCards(false);
        assistantAccumulator.clearActive();
        if (memoryCoordinator != null) memoryCoordinator.clearPersistedMessageIds();
        if (memoryStore != null && sessionId > 0) {
            memoryStore.clearSession(sessionId);
            sessionId = memoryStore.startSession(agentName);
            conversationMemory = memoryStore.relevantMemory("");
            dynamicTone = memoryStore.latestTone();
        }
        messages.clear();
        messages.add(SessionClearState.sessionClearedMessage());
        setState("idle");
        if (initialized) {
            showHome();
        } else {
            showInitializationHome();
        }
    }

    private void applyRuntimeClearFields(SessionClearState.RuntimeFields runtime) {
        inputAudioOpen = runtime.inputAudioOpen;
        pendingText = runtime.pendingText;
        latestWeatherFact = runtime.latestWeatherFact;
        latestNewsFact = runtime.latestNewsFact;
    }

    private void applyResetInitializationFields(SessionClearState.ResetInitializationFields reset) {
        userName = reset.userName;
        userMemory = reset.userMemory;
        agentMemory = reset.agentMemory;
        conversationMemory = reset.conversationMemory;
        dynamicTone = reset.dynamicTone;
        initialized = reset.initialized;
        initializing = reset.initializing;
        initPromptPending = reset.initPromptPending;
        initSummaryPending = reset.initSummaryPending;
        summaryInProgress = reset.summaryInProgress;
        ignoreNextInitTrigger = reset.ignoreNextInitTrigger;
        initUserTurns = reset.initUserTurns;
        applyRuntimeClearFields(reset.runtime);
    }

    private void showAbout() {
        clearVoiceCards(false);
        leaveVoiceSurface();
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
        ChatPage.renderMessages(this, ui, messageList, messageScroll, messages, textReplyPlaceholderText());
    }

    private void showTextReplyPlaceholder() {
        textReplyPlaceholderVisible = true;
        textReplyPlaceholderFrame = 0;
        renderMessages();
        startTextReplyPlaceholderTicker();
    }

    private void hideTextReplyPlaceholder() {
        if (!textReplyPlaceholderVisible && textReplyPlaceholderTicker == null) return;
        textReplyPlaceholderVisible = false;
        stopTextReplyPlaceholderTicker();
        renderMessages();
    }

    private String textReplyPlaceholderText() {
        if (!textReplyPlaceholderVisible) return "";
        int dots = (textReplyPlaceholderFrame % 3) + 1;
        if (dots == 1) return "•";
        if (dots == 2) return "• •";
        return "• • •";
    }

    private void startTextReplyPlaceholderTicker() {
        if (textReplyPlaceholderTicker != null) return;
        textReplyPlaceholderTicker = new Runnable() {
            @Override public void run() {
                if (!textReplyPlaceholderVisible) {
                    stopTextReplyPlaceholderTicker();
                    return;
                }
                textReplyPlaceholderFrame++;
                if (messageList != null) renderMessages();
                main.postDelayed(this, 320);
            }
        };
        main.postDelayed(textReplyPlaceholderTicker, 320);
    }

    private void stopTextReplyPlaceholderTicker() {
        if (textReplyPlaceholderTicker != null) {
            main.removeCallbacks(textReplyPlaceholderTicker);
            textReplyPlaceholderTicker = null;
        }
    }

    private String lastConversationLine() {
        return ConversationHistory.lastConversationLine(messages, "我在这里。轻轻点一下，说给我听。");
    }

    private void updateVoiceHome() {
        String line = lastConversationLine();
        if (voiceLastTurnView != null) voiceLastTurnView.setText(line);
        if (moodVeil != null) moodVeil.setMood(moodForText(line));
        if (voiceOrbView != null) voiceOrbView.setConversationState(voiceState);
        if (digitalAvatarView != null) digitalAvatarView.setAvatarState(avatarEmotion, voiceState.isSpeaking());
        if (micButton != null) micButton.setText(mic.running || inputAudioOpen ? "●" : voiceButtonText());
    }

    private int moodForText(String text) {
        return ConversationMood.forText(text);
    }

    private String lastInitializationLine() {
        return ConversationHistory.lastAnyLine(messages, "初始化");
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
        if (memoryCoordinator != null) memoryCoordinator.persistMessage(message);
    }

    private void persistActiveAssistantMessage() {
        Message message = assistantAccumulator.activeMessage();
        if (message != null) persistMessage(message);
    }

    private void discardActiveAssistantMessage() {
        if (!assistantAccumulator.discardActive()) return;
        renderMessages();
        updateVoiceHome();
    }

    private void removeAssistantReplyAfterLastUser() {
        assistantAccumulator.removeAssistantReplyAfterLastUser();
        renderMessages();
        updateVoiceHome();
    }

    private void compactConversation(MemoryChunk chunk) {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            compactInProgress = false;
            return;
        }
        JSONObject body;
        try {
            body = MemoryCompactor.requestBody(BACKGROUND_MODEL, agentName, userMemory, conversationMemory, chunk);
        } catch (JSONException error) {
            compactInProgress = false;
            return;
        }

        final long compactSessionId = sessionId;
        final int compactRequestId = memoryCompactionGate.start();
        agents.sendSubconscious(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                if (!isCurrentMemoryCompaction(compactRequestId, compactSessionId)) return;
                MemoryCompactor.Result result = MemoryCompactor.parseResult(content);
                String finalMemory = result.memory;
                String finalTone = result.tone;
                String finalAvatarEmotion = result.avatarEmotion;
                if (!finalMemory.isEmpty()) {
                    memoryStore.insertMemory(sessionId, "compact", finalMemory, chunk.firstId, chunk.lastId);
                    memoryStore.markCompacted(chunk.lastId);
                }
                if (!finalTone.isEmpty()) {
                    dynamicTone = finalTone;
                    memoryStore.insertMemory(sessionId, "tone", finalTone, chunk.firstId, chunk.lastId);
                }
                applyAvatarEmotion(finalAvatarEmotion);
                memoryStore.insertMemory(sessionId, "avatar_emotion", avatarEmotion, chunk.firstId, chunk.lastId);
                conversationMemory = memoryStore.relevantMemory(lastUserUtterance);
                compactInProgress = false;
                memoryDirtyForRealtime = true;
                applyContextUpdateForNextTurn(true);
            }

            @Override public void onError(String message) {
                if (!isCurrentMemoryCompaction(compactRequestId, compactSessionId)) return;
                compactInProgress = false;
            }
        }, "记忆压缩");
    }

    private void applyAvatarEmotion(String nextEmotion) {
        String normalized = AvatarVideoCatalog.normalizeEmotion(nextEmotion);
        if (AvatarVideoCatalog.EMOTION_NEUTRAL.equals(normalized)) {
            normalized = AvatarVideoCatalog.emotionFromText(lastConversationLine());
        }
        avatarEmotion = normalized;
        if (digitalAvatarView != null) {
            digitalAvatarView.setAvatarState(avatarEmotion, voiceState.isSpeaking());
        }
    }

    private void handleAvatarVoiceCommand(String text) {
        if (digitalAvatarView == null || text == null) return;
        String normalized = text.replace("，", "")
                .replace("。", "")
                .replace("！", "")
                .replace("？", "")
                .replace(",", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .trim();
        if (normalized.contains("想看看你的宠物") ||
                normalized.contains("看看你的宠物") ||
                normalized.contains("你的宠物") ||
                normalized.contains("小猫") ||
                normalized.contains("猫")) {
            digitalAvatarView.playOnce(AvatarVideoCatalog.petVideo());
            applyContextUpdateForNextTurn(true);
            return;
        }
        if (normalized.contains("换个形象") ||
                normalized.contains("换一个形象") ||
                normalized.contains("换套衣服") ||
                normalized.contains("换身衣服") ||
                normalized.contains("换装")) {
            digitalAvatarView.playOnce(AvatarVideoCatalog.randomImageChangeVideo(random.nextInt(4)));
            applyContextUpdateForNextTurn(true);
        }
    }

    private void cancelMemoryCompaction() {
        memoryCompactionGate.invalidate();
        compactInProgress = false;
    }

    private boolean isCurrentMemoryCompaction(int requestId, long compactSessionId) {
        if (!memoryCompactionGate.isCurrent(requestId)) return false;
        if (sessionId == compactSessionId && memoryStore != null) return true;
        cancelMemoryCompaction();
        return false;
    }

    private void applyContextUpdateForNextTurn(boolean includeFact) {
        if (!memoryDirtyForRealtime || !realtime.isOpen()) return;
        JSONObject payload;
        try {
            payload = RealtimePayloadBuilder.contextUpdate(buildInstructions(), conversationMemory, includeFact, 900);
        } catch (JSONException ignored) {
            return;
        }
        realtime.sendEvent("context.update", payload);
        memoryDirtyForRealtime = false;
    }

    private void sendText(String text) {
        if (textInputController != null) textInputController.sendText(text);
    }

    private boolean routeToolQuestion(String text, boolean realtimeMode) {
        return toolRouter != null && toolRouter.routeUserText(text, realtimeMode);
    }

    private void routeToolsInBackground(String text) {
        if (backgroundToolRoutes != null) backgroundToolRoutes.route(text);
    }

    private void invalidateBackgroundToolRoute() {
        if (backgroundToolRoutes != null) backgroundToolRoutes.invalidate();
    }

    private void startNewsToolFromBackground(String question) {
        interruptRealtimePlayback("background_tool_daily_news");
        discardActiveAssistantMessage();
        removeAssistantReplyAfterLastUser();
        if (realtime.isOpen()) {
            realtime.close();
        }
        if (toolInteractions != null) toolInteractions.startNewsFromBackground(question);
    }

    private void runNewsTool(String question, boolean realtimeMode, int token) {
        if (newsHandler == null) return;
        Log.d(TAG, "news tool fetch realtime=" + realtimeMode + " question=" + question);
        newsHandler.fetch(question, result -> onNewsToolResult(result, realtimeMode, token));
    }

    private void onNewsToolResult(ToolInteractionResult<NewsTool.NewsResult> result,
            boolean realtimeMode, int token) {
        if (toolInteractions != null && !toolInteractions.completeNewsFetch(token)) return;
        if (toolResultPresenter != null) toolResultPresenter.presentNews(result, realtimeMode);
    }

    private void resolveWeatherIntentAndRun(String question, boolean realtimeMode, int token) {
        if (agents == null || BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            Log.d(TAG, "weather intent resolver unavailable, fallback to location");
            runWeatherTool(question, "", realtimeMode, token);
            return;
        }
        int seq = ++weatherIntentSeq;
        JSONObject body;
        try {
            body = WeatherIntentResolver.requestBody(BACKGROUND_MODEL, effectiveAgentName(), question);
        } catch (JSONException error) {
            runWeatherTool(question, "", realtimeMode, token);
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
                        boolean completed = toolInteractions == null ||
                                toolInteractions.completeWeatherFetch(token);
                        WeatherIntentCompletionDecision decision =
                                WeatherIntentCompletionDecision.notWeatherQuery(
                                        completed,
                                        realtimeMode,
                                        isTextModeActive(),
                                        voiceInputSurfaceActive);
                        if (!decision.shouldUpdateState) return;
                        setState(decision.nextState);
                        if (decision.shouldScheduleListening) scheduleContinuousListening(180);
                        return;
                    }
                    runWeatherTool(question, intent.city, realtimeMode, token);
                } catch (JSONException error) {
                    Log.d(TAG, "weather intent parse failed content=" + content);
                    failWeatherIntent(question, realtimeMode, token);
                }
            }

            @Override public void onError(String message) {
                if (seq != weatherIntentSeq) return;
                Log.d(TAG, "weather intent failed " + message);
                failWeatherIntent(question, realtimeMode, token);
            }
        }, "天气意图解析");
    }

    private void failWeatherIntent(String question, boolean realtimeMode, int token) {
        if (weatherHandler == null) return;
        weatherHandler.fail(question, "天气意图解析失败，请再说一次城市名。",
                result -> onWeatherToolResult(result, realtimeMode, token));
    }

    private void runWeatherTool(String question, String city, boolean realtimeMode, int token) {
        if (weatherHandler == null) {
            onWeatherToolResult(ToolInteractionResult.failure(
                    "weather",
                    question,
                    WeatherSkill.failureFact("天气工具不可用"),
                    "暂时没查到天气，天气工具不可用。你可以稍后再试，或者告诉我具体城市。",
                    "天气工具不可用"), realtimeMode, token);
            return;
        }
        if (city != null && !city.trim().isEmpty()) {
            Log.d(TAG, "weather query city=" + city.trim() + " realtime=" + realtimeMode);
            weatherHandler.queryCity(question, city.trim(),
                    result -> onWeatherToolResult(result, realtimeMode, token));
            return;
        }
        Log.d(TAG, "weather query current location realtime=" + realtimeMode);
        requestWeatherForCurrentLocation(question, realtimeMode, token);
    }

    private void onWeatherToolResult(ToolInteractionResult<WeatherTool.WeatherResult> result,
            boolean realtimeMode, int token) {
        if (toolInteractions != null && !toolInteractions.completeWeatherFetch(token)) return;
        if (toolResultPresenter != null) toolResultPresenter.presentWeather(result, realtimeMode);
    }

    private void requestWeatherForCurrentLocation(String question, boolean realtimeMode, int token) {
        if (weatherRequests != null) weatherRequests.requestCurrentLocation(question, realtimeMode, token);
    }

    private void pushRealtimeWeatherFact() {
        if (!realtime.isOpen()) return;
        try {
            JSONObject payload = RealtimePayloadBuilder.factContextUpdate(latestWeatherFact, 1100);
            if (payload != null) realtime.sendEvent("context.update", payload);
        } catch (JSONException ignored) { }
    }

    private void queueRealtimeWeatherBroadcast(String prompt) {
        if (pendingBroadcasts != null) pendingBroadcasts.queueWeather(prompt);
    }

    private void schedulePendingWeatherBroadcast(long delayMs) {
        if (pendingBroadcasts != null) pendingBroadcasts.scheduleWeather(delayMs);
    }

    private void pushRealtimeNewsFact() {
        if (!realtime.isOpen()) return;
        try {
            JSONObject payload = RealtimePayloadBuilder.factContextUpdate(latestNewsFact, 1800);
            if (payload != null) realtime.sendEvent("context.update", payload);
        } catch (JSONException ignored) { }
    }

    private void queueRealtimeNewsBroadcast(String prompt) {
        invalidateBackgroundToolRoute();
        if (pendingBroadcasts != null) pendingBroadcasts.queueNews(prompt);
    }

    private void schedulePendingNewsBroadcast(long delayMs) {
        if (pendingBroadcasts != null) pendingBroadcasts.scheduleNews(delayMs);
    }

    private boolean hasPendingWeatherBroadcast() {
        return pendingBroadcasts != null && pendingBroadcasts.hasPendingWeather();
    }

    private boolean hasPendingNewsBroadcast() {
        return pendingBroadcasts != null && pendingBroadcasts.hasPendingNews();
    }

    private boolean isNewsInteractionActive() {
        return toolInteractions != null && toolInteractions.isNewsActive();
    }

    private boolean isAwaitingRealtimeNewsAnswer() {
        return toolInteractions != null && toolInteractions.isAwaitingRealtimeNewsAnswer();
    }

    private boolean isAwaitingRealtimeWeatherAnswer() {
        return toolInteractions != null && toolInteractions.isAwaitingRealtimeWeatherAnswer();
    }

    private boolean isWeatherInteractionActive() {
        return toolInteractions != null && toolInteractions.isWeatherActive();
    }

    private void clearPendingWeatherBroadcast() {
        if (pendingBroadcasts != null) pendingBroadcasts.clearWeather();
    }

    private void clearPendingNewsBroadcast() {
        if (pendingBroadcasts != null) pendingBroadcasts.clearNews();
    }

    private void clearNewsInteraction() {
        clearPendingNewsBroadcast();
        if (toolInteractions != null) toolInteractions.clearNews();
    }

    private void clearWeatherInteraction() {
        clearPendingWeatherBroadcast();
        if (weatherRequests != null) weatherRequests.clearPending();
        weatherIntentSeq++;
        if (toolInteractions != null) toolInteractions.clearWeather();
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

    private boolean isAnyGatewayOrToolTtsPlaying() {
        return hasActiveToolTtsPlayback() || (ttsPlayer != null && ttsPlayer.isPlaying());
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

    private void clearVoiceInputRequests() {
        VoiceInputCleanupDecision cleanup =
                VoiceInputCleanupDecision.clearRequests(voiceInput != null);
        if (cleanup.cancelAsrFinalTimeout) cancelAsrFinalTimeout();
        if (voiceInput == null) return;
        if (cleanup.clearPendingStart) voiceInput.clearPendingStart();
        if (cleanup.cancelContinuousListening) voiceInput.cancelContinuousListening();
    }

    private void resumeListeningAfterToolTts(long delayMs) {
        if (voiceInput != null) voiceInput.resumeAfterToolTts(delayMs);
    }

    private void requestVoiceInputStart(boolean requestPermission) {
        requestVoiceInputStart(requestPermission, null, false);
    }

    private void requestVoiceInputStart(boolean requestPermission, String interruptReason, boolean showHeadsetPrompt) {
        if (voiceInput != null) voiceInput.requestStart(requestPermission, interruptReason, showHeadsetPrompt);
    }

    private void stopToolTtsPlayback(boolean resumeListening) {
        if (ttsPlayer != null) ttsPlayer.stop();
        if (voicePipeline != null) voicePipeline.stopToolTts(resumeListening);
    }

    private void addWeatherCard(WeatherTool.WeatherResult result) {
        messages.add(new Message(newId("weather"), result));
        if (voiceLastTurnView != null) {
            if (voiceCards != null) voiceCards.showWeather(result);
        } else {
            renderMessages();
        }
    }

    private void addNewsCard(NewsTool.NewsResult result) {
        discardActiveAssistantMessage();
        removeAssistantReplyAfterLastUser();
        messages.add(new Message(newId("news"), result));
        if (voiceLastTurnView != null) {
            if (voiceCards != null) voiceCards.showNews(result, !isAwaitingRealtimeNewsAnswer());
        } else {
            renderMessages();
        }
    }

    private void clearVoiceWeatherCard(boolean refreshVoice) {
        if (voiceCards != null) voiceCards.clearWeather(refreshVoice);
    }

    private void clearVoiceNewsCard(boolean refreshVoice) {
        if (voiceCards != null) voiceCards.clearNews(refreshVoice);
    }

    private void clearVoiceCards(boolean refreshVoice) {
        if (voiceCards != null) voiceCards.clearAll(refreshVoice);
    }

    private void sendTextWithAgentLLM(String text) {
        if (chatController != null) chatController.sendText(text);
    }

    private String buildTextChatInstructions() throws JSONException {
        return buildInstructions() + "\n\n" +
                "当前是文本聊天通道。请只输出适合聊天气泡展示的文字，不要描述语音、音频或工具过程。";
    }

    private void startTextModeAsrPress(float rawX) {
        textModeAsrTouchActive = true;
        textModeAsrTouchStartX = rawX;
        textModeAsrGestureState = TextModeAsrGesture.NEUTRAL;
        startTextModeAsr();
    }

    private void moveTextModeAsrPress(float rawX) {
        if (!textModeAsrTouchActive) return;
        int next = TextModeAsrGesture.decide(
                rawX - textModeAsrTouchStartX,
                ui == null ? TEXT_ASR_SLIDE_THRESHOLD_DP : ui.dp(TEXT_ASR_SLIDE_THRESHOLD_DP));
        if (next == textModeAsrGestureState) return;
        textModeAsrGestureState = next;
        refreshTextModeAsrControls();
    }

    private void endTextModeAsrPress(float rawX) {
        if (!textModeAsrTouchActive) return;
        moveTextModeAsrPress(rawX);
        textModeAsrTouchActive = false;
        if (textModeAsrGestureState == TextModeAsrGesture.CANCEL) {
            cancelTextModeAsr();
        } else {
            finishTextModeAsrCapture();
        }
        textModeAsrGestureState = TextModeAsrGesture.NEUTRAL;
        refreshTextModeAsrControls();
    }

    private void cancelTextModeAsrPress() {
        textModeAsrTouchActive = false;
        textModeAsrGestureState = TextModeAsrGesture.NEUTRAL;
        cancelTextModeAsr();
    }

    private void startTextModeAsr() {
        if (!isTextModeActive()) return;
        if (summaryInProgress) return;
        if (textModeAsrStarting || textModeAsrRecording) return;
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            toastError("Missing AGENTLLM_API_KEY");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingTextModeAsrAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        clearVoiceInputRequests();
        stopToolTtsPlayback(false);
        player.stop();
        resetRealtimeOutput();
        if (mic.running || inputAudioOpen) stopInputAudio("text_only");
        textModeAsrStarting = true;
        textModeAsrRecording = true;
        textModeAsrTaskReady = false;
        textModeAsrFinishAfterStart = false;
        textModeAsrDiscardResult = false;
        clearTextModeAsrAudioBuffer();
        startHerForegroundService(true);
        boolean started = mic.start(this::onTextModeAsrAudio);
        if (!started) {
            startHerForegroundService(false);
            resetTextModeAsrState(false);
            toastError("无法启动麦克风，请检查录音权限或重试。");
            return;
        }
        markConversationInteraction(false);
        refreshTextModeAsrControls();
        textModeAsr.start(new TextModeAsrClient.Listener() {
            @Override public void onStarted() {
                onTextModeAsrStarted();
            }

            @Override public void onFinalText(String text) {
                onTextModeAsrFinalText(text);
            }

            @Override public void onError(String message) {
                onTextModeAsrError(message);
            }

            @Override public void onClosed() {
                onTextModeAsrClosed();
            }
        });
    }

    private void onTextModeAsrStarted() {
        if (!isTextModeActive()) {
            cancelTextModeAsr();
            return;
        }
        List<byte[]> buffered;
        synchronized (textModeAsrAudioLock) {
            textModeAsrTaskReady = true;
            buffered = new ArrayList<>(textModeAsrBufferedAudio);
            textModeAsrBufferedAudio.clear();
            textModeAsrBufferedBytes = 0;
        }
        textModeAsrStarting = false;
        for (byte[] bytes : buffered) {
            if (textModeAsr != null) textModeAsr.sendAudio(bytes);
        }
        refreshTextModeAsrControls();
        if (textModeAsrFinishAfterStart) {
            textModeAsrFinishAfterStart = false;
            if (textModeAsr != null) textModeAsr.finish();
        }
    }

    private void finishTextModeAsrCapture() {
        if (textModeAsrRecording) {
            mic.stop();
            startHerForegroundService(false);
        }
        textModeAsrRecording = false;
        if (textModeAsrStarting && !textModeAsrTaskReady) {
            textModeAsrFinishAfterStart = true;
        } else if (textModeAsr != null) {
            textModeAsr.finish();
        }
        refreshTextModeAsrControls();
    }

    private void onTextModeAsrFinalText(String text) {
        boolean discard = textModeAsrDiscardResult;
        if (textModeAsrRecording) {
            mic.stop();
            startHerForegroundService(false);
        }
        resetTextModeAsrState(false);
        refreshTextModeAsrControls();
        if (discard) return;
        String clean = text == null ? "" : text.trim();
        if (!isTextModeActive()) return;
        if (clean.isEmpty()) {
            setState("text_only");
            toastError("没有识别到可发送的文字。");
            return;
        }
        sendText(clean);
    }

    private void onTextModeAsrError(String message) {
        if (textModeAsrRecording) {
            mic.stop();
            startHerForegroundService(false);
        }
        resetTextModeAsrState(false);
        refreshTextModeAsrControls();
        if (isTextModeActive()) setState("text_only");
        toastError("语音识别失败：" + message);
    }

    private void onTextModeAsrClosed() {
        if (!textModeAsrStarting && !textModeAsrRecording) return;
        if (textModeAsrRecording) {
            mic.stop();
            startHerForegroundService(false);
        }
        resetTextModeAsrState(false);
        refreshTextModeAsrControls();
        if (isTextModeActive()) setState("text_only");
    }

    private void cancelTextModeAsr() {
        pendingTextModeAsrAfterPermission = false;
        if (textModeAsrRecording) {
            mic.stop();
            startHerForegroundService(false);
        }
        resetTextModeAsrState(true);
        if (textModeAsr != null) textModeAsr.cancel();
        refreshTextModeAsrControls();
    }

    private void refreshTextModeAsrControls() {
        if (textAsrButton != null) {
            textAsrButton.setImageResource(textModeAsrStarting || textModeAsrRecording
                    ? R.drawable.ic_stop_text_input
                    : R.drawable.ic_mic_text_input);
            int tint = 0xDDFFFFFF;
            if (textModeAsrGestureState == TextModeAsrGesture.CANCEL) tint = 0xFFFF6377;
            if (textModeAsrGestureState == TextModeAsrGesture.SEND) tint = 0xFF80FFB0;
            textAsrButton.setColorFilter(tint);
        }
        if (textAsrHint != null) {
            boolean active = textModeAsrStarting || textModeAsrRecording;
            textAsrHint.setVisibility(active ? View.VISIBLE : View.GONE);
            textAsrHint.setText(TextModeAsrGesture.label(textModeAsrGestureState));
            int color = 0xCCFFFFFF;
            if (textModeAsrGestureState == TextModeAsrGesture.CANCEL) color = 0xFFFF6377;
            if (textModeAsrGestureState == TextModeAsrGesture.SEND) color = 0xFF80FFB0;
            textAsrHint.setTextColor(color);
            if (active) startTextModeAsrPulse();
            else stopTextModeAsrPulse();
        }
        if (stateLabel != null && isTextModeActive()) {
            stateLabel.setText(currentStateLabelText());
        }
    }

    private void onTextModeAsrAudio(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        boolean sendNow;
        synchronized (textModeAsrAudioLock) {
            sendNow = textModeAsrTaskReady && textModeAsrRecording && !textModeAsrDiscardResult;
            if (!sendNow && textModeAsrRecording && !textModeAsrDiscardResult) {
                textModeAsrBufferedAudio.add(bytes);
                textModeAsrBufferedBytes += bytes.length;
                while (textModeAsrBufferedBytes > TEXT_ASR_PREBUFFER_MAX_BYTES &&
                        !textModeAsrBufferedAudio.isEmpty()) {
                    byte[] removed = textModeAsrBufferedAudio.remove(0);
                    textModeAsrBufferedBytes -= removed.length;
                }
            }
        }
        if (sendNow && textModeAsr != null) textModeAsr.sendAudio(bytes);
    }

    private void clearTextModeAsrAudioBuffer() {
        synchronized (textModeAsrAudioLock) {
            textModeAsrTaskReady = false;
            textModeAsrBufferedAudio.clear();
            textModeAsrBufferedBytes = 0;
        }
    }

    private void resetTextModeAsrState(boolean discardResult) {
        textModeAsrStarting = false;
        textModeAsrRecording = false;
        textModeAsrTaskReady = false;
        textModeAsrFinishAfterStart = false;
        textModeAsrDiscardResult = discardResult;
        textModeAsrTouchActive = false;
        textModeAsrGestureState = TextModeAsrGesture.NEUTRAL;
        clearTextModeAsrAudioBuffer();
        stopTextModeAsrPulse();
    }

    private void startTextModeAsrPulse() {
        if (textAsrHint == null || textModeAsrPulse != null) return;
        textModeAsrPulse = new Runnable() {
            @Override public void run() {
                if (textAsrHint == null || (!textModeAsrStarting && !textModeAsrRecording)) {
                    stopTextModeAsrPulse();
                    return;
                }
                textAsrHint.setAlpha(0.45f);
                textAsrHint.animate().alpha(1f).setDuration(220).start();
                main.postDelayed(this, 360);
            }
        };
        textModeAsrPulse.run();
    }

    private void stopTextModeAsrPulse() {
        if (textModeAsrPulse != null) {
            main.removeCallbacks(textModeAsrPulse);
            textModeAsrPulse = null;
        }
        if (textAsrHint != null) {
            textAsrHint.animate().cancel();
            textAsrHint.setAlpha(1f);
        }
    }

    private void toggleMic() {
        if (voiceSession != null) voiceSession.onMicToggle();
    }

    private void interruptNewsPlayback() {
        if (voiceSession != null) voiceSession.interruptNewsPlayback();
    }

    private void interruptWeatherPlayback() {
        if (voiceSession != null) voiceSession.interruptWeatherPlayback();
    }

    private void startInputAudio() {
        boolean hasRecordPermission =
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (!VoiceInputAudioDecision.canStart(isTextModeActive(), voiceInputSurfaceActive,
                voiceState.isTextOnly(), mic.running, inputAudioOpen, hasRecordPermission)) {
            return;
        }
        cancelScheduledContinuousListening();
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
        VoiceInputAudioDecision.StopDecision decision =
                VoiceInputAudioDecision.stop(inputAudioOpen, nextState);
        if (decision.sendInputEnd) {
            realtime.sendEvent("input_audio.end", null);
            inputAudioOpen = false;
        }
        startHerForegroundService(false);
        setState(nextState);
        if (decision.scheduleAsrFinalTimeout) {
            scheduleAsrFinalTimeout();
        }
    }

    private void scheduleAsrFinalTimeout() {
        cancelAsrFinalTimeout();
        asrFinalTimeout = () -> {
            asrFinalTimeout = null;
            AsrFinalTimeoutDecision decision = AsrFinalTimeoutDecision.decide(
                    voiceState.isProcessing(),
                    mic.running,
                    inputAudioOpen,
                    isRealtimeOutputActive(),
                    summaryInProgress,
                    isTextModeActive());
            if (decision == null) return;
            Log.d(TAG, "asr final timeout, resume listening");
            setState("ready");
            if (decision.resumeListening) scheduleContinuousListening(80);
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
        voiceActivityDetector.reset();
    }

    private void processVad(byte[] bytes) {
        VoiceActivityDetector.Result result = voiceActivityDetector.process(bytes);
        if (audioLevelView != null) {
            main.post(() -> {
                AudioLevelView levelView = audioLevelView;
                VoiceOrbView orbView = voiceOrbView;
                if (levelView != null) levelView.setLevel(result.visualLevel);
                if (orbView != null) orbView.setLevel(result.visualLevel);
            });
        }
        if (result.shouldEndInput) {
            main.post(() -> {
                if (inputAudioOpen && mic.running) {
                    stopInputAudio("processing");
                }
            });
        }
    }

    private void enterAssistantSpeaking(int sampleRate) {
        cancelScheduledContinuousListening();
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
        if (voiceInput != null) voiceInput.startContinuousListening();
    }

    private void scheduleContinuousListening(long delayMs) {
        if (voiceInput != null) voiceInput.scheduleContinuousListening(delayMs);
    }

    private void cancelScheduledContinuousListening() {
        if (voiceInput != null) voiceInput.cancelContinuousListening();
    }

    private void onRealtimeReady() {
        if (voiceSession != null) voiceSession.onRealtimeReady();
    }

    private void onAssistantDelta(String text) {
        RealtimeAssistantDeltaDecision decision = RealtimeAssistantDeltaDecision.decide(
                isTextModeActive(), assistantAccumulator.activeMessage() != null);
        if (!decision.appendDelta) {
            if (decision.discardActiveDraft) assistantAccumulator.discardActive();
            if (decision.refreshAfterDiscard) {
                updateVoiceHome();
                renderMessages();
            }
            return;
        }
        markInitializationOpeningDeliveredFromRealtime();
        boolean hadActiveMessage = assistantAccumulator.activeMessage() != null;
        if (!hadActiveMessage) {
            breatheScreenForAssistantReply();
        }
        assistantAccumulator.appendDelta(text);
        updateVoiceHome();
        renderMessages();
    }

    private void markInitializationOpeningDeliveredFromRealtime() {
        if (!InitializationOpening.shouldMarkRealtimeDelivered(initializing, initOpeningDelivered)) return;
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
        if (!InitializationSummaryLifecycle.shouldStart(summaryInProgress)) return;
        applyInitializationSummaryFlags(InitializationSummaryLifecycle.active());
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
        return InitializationProgressText.build(summaryInProgress, initUserTurns, INIT_TARGET_USER_TURNS);
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

    private void applyInitializationSummaryFlags(InitializationSummaryLifecycle.Flags flags) {
        summaryInProgress = flags.summaryInProgress;
        initSummaryPending = flags.initSummaryPending;
    }

    private void clearInitializationSummaryProgress() {
        initializationSummaryGate.invalidate();
        applyInitializationSummaryFlags(InitializationSummaryLifecycle.cleared());
        stopInitProgressAnimation();
    }

    private void failInitializationSummary(String message) {
        clearInitializationSummaryProgress();
        toastError(message);
        setState("error");
    }

    private void showInitializationCompleteThenHome() {
        InitializationCompletionDisplay.State completion = InitializationCompletionDisplay.completed();
        showInitializationHome();
        if (initProgressView != null) initProgressView.setText(completion.progressText);
        if (initLastTurnView != null) initLastTurnView.setText(completion.lastTurnText);
        if (audioLevelView != null) audioLevelView.setLevel(completion.audioLevel);
        main.postDelayed(this::showHome, completion.homeDelayMs);
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
        try {
            JSONObject payload = RealtimePayloadBuilder.contextUpdate(buildInstructions(), "", false, 0);
            realtime.sendEvent("context.update", payload);
        } catch (JSONException ignored) { }
    }

    private void setState(String next) {
        voiceState = VoiceSessionStateReducer.reduce(voiceState, next);
        state = voiceState.legacyValue();
        updateInteractionKeepScreenOn();
        updateResponsePendingTimeout(voiceState);
        if (stateLabel != null) stateLabel.setText(currentStateLabelText());
        if (micButton != null) micButton.setText(voiceState.isListening() ? "●" : voiceButtonText());
        refreshTextModeAsrControls();
        if (voiceOrbView != null) voiceOrbView.setConversationState(voiceState);
        if (digitalAvatarView != null) digitalAvatarView.setAvatarState(avatarEmotion, voiceState.isSpeaking());
        if (voiceState.shouldApplyContextUpdateForNextTurn()) {
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
        return InteractionKeepScreenOnDecision.shouldKeepScreenOn(
                initializing,
                summaryInProgress,
                mic.running,
                inputAudioOpen,
                hasActiveToolTtsPlayback(),
                isWeatherInteractionActive(),
                isNewsInteractionActive(),
                voiceState);
    }

    private void updateResponsePendingTimeout(VoiceSessionState next) {
        if (responsePendingTimeout != null) {
            main.removeCallbacks(responsePendingTimeout);
            responsePendingTimeout = null;
        }
        if (!next.isResponsePending()) return;
        responsePendingTimeout = () -> {
            responsePendingTimeout = null;
            ResponsePendingTimeoutDecision decision = ResponsePendingTimeoutDecision.decide(
                    voiceState.isResponsePending(),
                    mic.running,
                    inputAudioOpen,
                    summaryInProgress,
                    isTextModeActive(),
                    realtime.isOpen());
            if (decision == null) return;
            setState(decision.nextState);
            if (decision.scheduleContinuousListening) {
                scheduleContinuousListening(80);
            }
        };
        main.postDelayed(responsePendingTimeout, RESPONSE_PENDING_VISIBLE_TIMEOUT_MS);
    }

    private String stateLabelText() {
        return voiceState.labelText(summaryInProgress,
                isWeatherInteractionActive(),
                isBoundHeadsetConnected(),
                headsets != null && headsets.hasBoundHeadset(),
                headsets != null && !headsets.connectedHeadsets().isEmpty());
    }

    private String currentStateLabelText() {
        if (isTextModeActive()) {
            if (textModeAsrStarting || textModeAsrRecording) return "Listening";
        }
        return stateLabelText();
    }

    private String voiceButtonText() {
        return voiceState.voiceButtonText(
                (ttsPlayer != null && ttsPlayer.isPlaying()) || hasPendingToolTtsPlayback(),
                hasPendingWeatherBroadcast() || isWeatherInteractionActive() ||
                        (voiceCards != null && voiceCards.hasWeatherCard()),
                hasPendingNewsBroadcast() || isNewsInteractionActive() ||
                        (voiceCards != null && voiceCards.hasNewsCard()));
    }

    private boolean isBoundHeadsetConnected() {
        if (demoMode) return true;
        return headsets != null && headsets.isBoundConnected();
    }

    private void onHeadsetDevicesChanged() {
        if (headsetController != null) headsetController.onHeadsetDevicesChanged(isBoundHeadsetConnected());
    }

    private void refreshHeadsetDependentControls() {
        if (stateLabel != null) stateLabel.setText(currentStateLabelText());
        if (micButton != null) micButton.setText(mic.running || inputAudioOpen ? "●" : voiceButtonText());
        refreshTextModeAsrControls();
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
                    if (VoiceSurfaceNavigationDecision.shouldStartAfterHeadsetBind(
                            startVoiceAfterBind, voiceInputSurfaceActive)) {
                        main.postDelayed(() -> {
                            if (VoiceSurfaceNavigationDecision.shouldStartAfterHeadsetBind(
                                    startVoiceAfterBind, voiceInputSurfaceActive)) {
                                requestVoiceInputStart(true, "user_speech_detected", false);
                            }
                        }, 180);
                    }
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
        return ConversationHistory.initializationTranscript(messages);
    }

    private void summarizeInitialization() {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            failInitializationSummary("Missing AGENTLLM_API_KEY in local.properties");
            return;
        }

        final String initTranscript = initializationTranscript();
        InitializationSummaryRequestBuilder.Request request;
        try {
            request = InitializationSummaryRequestBuilder.build(
                    BACKGROUND_MODEL, agentName, initAgentNameCandidates, initTranscript);
            Log.d(TAG, "init subconscious system prompt:\n" + request.systemPrompt);
            Log.d(TAG, "init subconscious user prompt:\n" + request.userPrompt);
        } catch (JSONException error) {
            failInitializationSummary("构建摘要请求失败");
            return;
        }

        final int summaryRequestId = initializationSummaryGate.start();
        agents.sendSubconscious(request.body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                if (!initializationSummaryGate.isCurrent(summaryRequestId)) return;
                Log.d(TAG, "init subconscious reply:\n" + content);
                InitializationSummaryCompletion.Result result = InitializationSummaryCompletion.complete(
                        content, initTranscript, agentName, fallbackAgentName(), SYSTEM_AGENT_NAME,
                        displayUserName(), new Date());
                Log.d(TAG, "init resolved profile agent_name=" + result.profile.agentName +
                        " user_name=" + result.profile.userName +
                        " relationship=" + result.profile.relationship);
                String memory = result.userMemory;
                String agentProfile = result.agentMemory;
                String finalAgentName = result.agentName;
                writeUserMemory(memory);
                writeAgentMemory(agentProfile);
                userMemory = memory;
                agentMemory = agentProfile;
                agentName = finalAgentName;
                userName = result.userName;
                getSharedPreferences("her", MODE_PRIVATE).edit()
                        .putString("agent_name", agentName)
                        .putString("user_name", userName)
                        .apply();
                initialized = true;
                initializing = false;
                clearInitializationSummaryProgress();
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
                if (!initializationSummaryGate.isCurrent(summaryRequestId)) return;
                Log.d(TAG, "init subconscious error: " + message);
                clearInitializationSummaryProgress();
                toastError(message);
                setState("error");
            }
        }, "摘要");
    }

    private String buildInstructions() {
        if (initializing) {
            return ConversationInstructionsComposer.initialization(
                    agentName, initAgentNameCandidates, initUserTurns, INSTRUCTIONS, INIT_BASE_PROMPT);
        }
        String recent = recentDialogueForPrompt();
        return ConversationInstructionsComposer.normal(
                effectiveAgentName(),
                userName,
                userMemory,
                agentMemory,
                dynamicTone,
                conversationMemory,
                recent,
                latestWeatherFact,
                isAwaitingRealtimeWeatherAnswer(),
                latestNewsFact,
                isAwaitingRealtimeNewsAnswer(),
                INSTRUCTIONS,
                3900);
    }

    private String recentDialogueForPrompt() {
        return ConversationHistory.recentDialogue(messages, RECENT_SESSION_MESSAGES);
    }

    private String trimForPrompt(String value, int limit) {
        return RealtimePayloadBuilder.trimTail(value, limit);
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
        try {
            return RealtimePayloadBuilder.sessionPayload(buildInstructions(), selectedVoiceId);
        } catch (JSONException ignored) { }
        return new JSONObject();
    }

    private void handleEvent(String type, JSONObject payload) {
        switch (RealtimeEventType.classify(type, payload != null)) {
        case SESSION_CREATED:
            realtime.markSessionCreated();
            setState(isTextModeActive() ? "text_only" : "ready");
            onRealtimeReady();
            break;
        case ASR_FINAL:
            if (realtimeAsrFinalController != null) {
                realtimeAsrFinalController.onFinalText(RealtimeEventPayload.asrFinalText(payload));
            }
            break;
        case ASSISTANT_STATE:
            RealtimeProviderStateDecision decision = RealtimeProviderStateDecision.decide(
                    RealtimeEventPayload.assistantState(payload),
                    hasActiveToolTtsPlayback(),
                    ttsPlayer != null && ttsPlayer.isPlaying(),
                    mic.running,
                    isTextModeActive(),
                    voiceInputSurfaceActive);
            if (decision.logThinking) {
                Log.d(TAG, "provider thinking");
                return;
            }
            if (decision.outputStarted && voicePipeline != null) voicePipeline.onRealtimeOutputStarted(0);
            if (decision.nextState != null) setState(decision.nextState);
            break;
        case ASSISTANT_TEXT_DELTA:
            onAssistantDelta(RealtimeEventPayload.assistantTextDelta(payload));
            break;
        case OUTPUT_AUDIO_START:
            RealtimeOutputStartDecision outputStart =
                    RealtimeOutputStartDecision.decide(isTextModeActive());
            if (outputStart.startRealtimeOutput && voicePipeline != null) {
                voicePipeline.onRealtimeOutputStarted(RealtimeEventPayload.outputSampleRate(payload));
            }
            if (outputStart.nextState != null) setState(outputStart.nextState);
            break;
        case OUTPUT_AUDIO_DONE:
            if (voicePipeline != null) voicePipeline.onRealtimeOutputDone();
            handleRealtimeOutputFinished(false);
            break;
        case OUTPUT_AUDIO_STOP:
            if (voicePipeline != null) voicePipeline.onRealtimeOutputStopped();
            handleRealtimeOutputFinished(true);
            break;
        case MEMORY_SNAPSHOT:
            handleMemorySnapshot(payload);
            break;
        case ERROR:
            RealtimeErrorEventDecision errorDecision = RealtimeErrorEventDecision.fromPayload(payload);
            if (errorDecision.action == RealtimeErrorEventDecision.Action.RETRY) {
                retryRealtime(errorDecision.message);
                return;
            }
            toastError(errorDecision.message);
            setState("error");
            break;
        case IGNORE:
            break;
        }
    }

    private void handleRealtimeOutputFinished(boolean stopped) {
        if (voiceSession != null) voiceSession.onRealtimeOutputFinished(stopped);
    }

    private void handleRealtimeClosed() {
        if (realtimeClosedHandler != null) realtimeClosedHandler.onClosed();
    }

    private void retryRealtime(String reason) {
        if (realtimeRecovery != null) realtimeRecovery.retry(reason);
    }

    private void handleRealtimeTransportError(String message) {
        if (realtimeRecovery != null) realtimeRecovery.onTransportError(message);
    }

    private void handleMemorySnapshot(JSONObject payload) {
        if (!initialized || initializing || memoryStore == null || sessionId <= 0) return;
        String text = RealtimeMemorySnapshot.fromPayload(payload, 1400);
        if (!RealtimeMemorySnapshot.shouldPersist(text)) return;
        memoryStore.insertMemory(sessionId, "agentvoice_snapshot", text, 0, 0);
        conversationMemory = memoryStore.relevantMemory(lastUserUtterance);
    }

    private void toastError(String message) {
        main.post(() -> {
            ErrorDisplayDecision decision = ErrorDisplayDecision.decide(
                    message,
                    initLastTurnView != null,
                    messageList != null,
                    voiceLastTurnView != null);
            if (decision.updateInitializationLastTurn) {
                initLastTurnView.setText(decision.message);
            }
            if (decision.appendAssistantMessage) {
                messages.add(new Message(newId("system"), "assistant", decision.message));
            }
            if (decision.renderMessages) {
                renderMessages();
            }
            if (decision.updateVoiceHome) updateVoiceHome();
        });
    }

}
