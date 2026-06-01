package com.linkyun.her;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.SystemClock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends Activity {
    private static final String TAG = "HerRealtime";
    private static final int REQ_AUDIO = 71;
    private static final int REQ_NOTIFY = 72;
    private static final boolean HALF_DUPLEX = true;
    private static final boolean CONTINUOUS_CONVERSATION = true;
    private static final int VAD_SPEECH_THRESHOLD = 520;
    private static final int VAD_SILENCE_FRAMES_TO_END = 45;
    private static final int VAD_MIN_FRAMES_BEFORE_END = 24;
    private static final int INIT_TARGET_USER_TURNS = 3;
    private static final int COMPACT_MESSAGE_THRESHOLD = 10;
    private static final int COMPACT_CHAR_THRESHOLD = 3000;
    private static final int RECENT_SESSION_MESSAGES = 16;
    private static final String USER_MEMORY_FILE = "user.md";
    private static final String AGENT_MEMORY_FILE = "Agent.md";
    private static final String SYSTEM_AGENT_NAME = "Doris";
    private static final String TEXT_CHAT_MODEL = "c-her";
    private static final String SUBCONSCIOUS_MODEL = "mimo-v2.5";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_VOICE = BuildConfig.AGENTVOICE_CLONED_VOICE;
    private static final String INSTRUCTIONS =
            "你是一个像 Her 里那样亲密、聪明、有温度的中文陪伴式语音助手。\n" +
            "你默认使用温柔大姐姐的语气：成熟、关照、轻轻调侃，但不要油腻或过度亲密。\n" +
            "你正在和用户进行实时语音或文字对话。回复要自然、短一些，有情绪感，但不要装腔作势。\n" +
            "当用户焦虑、孤独、疲惫或犹豫时，先共情，再给一个轻柔可执行的下一步。";
    private static final String INIT_BASE_PROMPT =
            "你是 Doris，一个 AI Agent，也是用户的朋友和助理。\n" +
            "你是语音交互模型，负责自然说话和倾听；mimo-v2.5 是潜意识模型，负责后台判断与写入长期记忆。\n" +
            "你正在进行首次初始化，不是普通聊天。目标是温柔、自然地收集三类信息：用户姓名/希望被如何称呼、用户希望和 Doris 的关系、用户的故事。\n" +
            "用户的故事是一段开放式自我介绍，可以包括近况、经历、在意的事、期待、边界或希望你记住的内容。\n" +
            "每次只问一个问题，回复要短，不要展开闲聊，不要一次列清单。";
    private static final String INIT_WAKE_EVENT =
            "【系统事件】用户刚打开应用，正在等待 Doris 主动问候。请不要复述本事件；请直接用第一人称主动介绍自己，并问第一个初始化问题。";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Message> messages = new ArrayList<>();
    private final List<Voice> voices = new ArrayList<>();
    private final RealtimeClient realtime = new RealtimeClient();
    private final OkHttpClient llmHttp = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build();
    private final MicStreamer mic = new MicStreamer();
    private final PcmPlayer player = new PcmPlayer();
    private MemoryStore memoryStore;

    private FrameLayout root;
    private long sessionId = -1;
    private String selectedVoiceId = DEFAULT_VOICE;
    private String selectedVoiceLabel = "Doris Clone";
    private String agentName = SYSTEM_AGENT_NAME;
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
    private boolean summaryInProgress = false;
    private boolean ignoreNextInitTrigger = false;
    private boolean initContextUpdatePending = false;
    private int realtimeRetryCount = 0;
    private int initUserTurns = 0;
    private boolean pendingMicStart = false;
    private boolean compactInProgress = false;
    private boolean memoryDirtyForRealtime = false;
    private boolean inputAudioOpen = false;
    private boolean vadSpeechStarted = false;
    private int vadSilenceFrames = 0;
    private int vadFrames = 0;
    private String pendingText = null;
    private String activeAssistantId = null;
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

        voices.add(new Voice(DEFAULT_VOICE, "Doris Clone", "female"));
        voices.add(new Voice("zh_female_roumeinvyou_emo_v2_mars_bigtts", "柔美女友（多情感）", "female"));
        voices.add(new Voice("zh_female_gaolengyujie_emo_v2_mars_bigtts", "高冷御姐（多情感）", "female"));
        voices.add(new Voice("zh_male_ruyayichen_emo_v2_mars_bigtts", "儒雅男友（多情感）", "male"));
        SharedPreferences prefs = getSharedPreferences("her", MODE_PRIVATE);
        memoryStore = new MemoryStore(this);
        agentName = SYSTEM_AGENT_NAME;
        prefs.edit().putString("agent_name", SYSTEM_AGENT_NAME).apply();
        userName = prefs.getString("user_name", "");
        userMemory = readUserMemory();
        agentMemory = readAgentMemory();
        initialized = !userMemory.trim().isEmpty();
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
            beginInitialization(agentName);
        }
        loadVoices();
    }

    @Override
    protected void onDestroy() {
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        if (initProgressTicker != null) main.removeCallbacks(initProgressTicker);
        mic.stop();
        player.stop();
        realtime.close();
        if (memoryStore != null) memoryStore.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_AUDIO && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            toggleMic();
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
        root = baseRoot();
        messageList = null;
        messageScroll = null;
        composer = null;
        homeTimeView = null;
        if (homeClockTicker != null) main.removeCallbacks(homeClockTicker);
        LinearLayout top = topBar("☰", "", "Aa", this::showSettings, this::showChat);
        root.addView(top);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(30), 0, dp(30), dp(86));
        root.addView(center, frame(-1, -1));

        voiceOrbView = new VoiceOrbView(this);
        voiceOrbView.setOnClickListener(v -> toggleMic());
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(178), dp(178));
        center.addView(voiceOrbView, markParams);

        voiceLastTurnView = text(lastConversationLine(), 22, Color.WHITE, 0);
        voiceLastTurnView.setGravity(Gravity.CENTER);
        voiceLastTurnView.setLineSpacing(dp(4), 1.0f);
        voiceLastTurnView.setPadding(dp(8), dp(34), dp(8), 0);
        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(-1, -2);
        lastParams.topMargin = dp(6);
        center.addView(voiceLastTurnView, lastParams);

        stateLabel = text(capitalize(state), 12, 0xA8FFE0E0, 0);
        stateLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.topMargin = dp(16);
        center.addView(stateLabel, stateParams);

        audioLevelView = new AudioLevelView(this);
        FrameLayout.LayoutParams levelParams = frame(dp(190), dp(12), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        levelParams.bottomMargin = dp(94);
        root.addView(audioLevelView, levelParams);

        micButton = text("♩", 36, 0xFFFF6377, 0);
        micButton.setGravity(Gravity.CENTER);
        micButton.setOnClickListener(v -> toggleMic());
        FrameLayout.LayoutParams micParams = frame(dp(78), dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        micParams.bottomMargin = dp(18);
        root.addView(micButton, micParams);

        top.bringToFront();
        setContentView(root);
        updateVoiceHome();
        if (!realtime.isOpen()) realtime.connect();
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
                    String value = trimmed.substring(colon + 1).replace("*", "").replace("-", "").trim();
                    if (!value.isEmpty() && !value.contains("未明确")) return value;
                }
            }
        }
        return "";
    }

    private void animateAgentName() {
        final String value = agentName == null || agentName.trim().isEmpty() ? SYSTEM_AGENT_NAME : agentName.trim();
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
        deleteFile(USER_MEMORY_FILE);
        deleteFile(AGENT_MEMORY_FILE);
        if (memoryStore != null) memoryStore.resetAll();
        sessionId = memoryStore == null ? -1 : memoryStore.startSession(agentName);
        persistedMessageIds.clear();
        getSharedPreferences("her", MODE_PRIVATE).edit().remove("user_name").apply();
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
        initContextUpdatePending = false;
        realtimeRetryCount = 0;
        initUserTurns = 0;
        inputAudioOpen = false;
        pendingMicStart = false;
        pendingText = null;
        activeAssistantId = null;
        messages.clear();
        setState("idle");
        beginInitialization(agentName);
    }

    private void showInitialize() {
        root = baseRoot();
        homeTimeView = null;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(34), 0, dp(34), 0);
        root.addView(content, frame(-1, -1));

        InitOrbView mark = new InitOrbView(this);
        content.addView(mark, new LinearLayout.LayoutParams(dp(130), dp(130)));

        TextView title = text("Create your Agent", 28, Color.WHITE, 0);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(34);
        content.addView(title, titleParams);

        TextView subtitle = text("先给她一个名字。接下来她会主动介绍自己，然后了解你的称呼、你希望彼此是什么关系，以及你的生活习惯。", 15, 0xB8FFE0E0, 0);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(14);
        content.addView(subtitle, subtitleParams);

        EditText nameInput = new EditText(this);
        nameInput.setText(agentName);
        nameInput.setHint("Agent name");
        nameInput.setHintTextColor(0x80FFE0E0);
        nameInput.setTextColor(Color.WHITE);
        nameInput.setTextSize(21);
        nameInput.setSingleLine(true);
        nameInput.setGravity(Gravity.CENTER);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setBackgroundColor(0x22FFFFFF);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(58));
        inputParams.topMargin = dp(28);
        content.addView(nameInput, inputParams);

        TextView start = text("Initialize", 19, Color.WHITE, 600);
        start.setGravity(Gravity.CENTER);
        start.setBackground(new BubbleDrawable(true));
        start.setOnClickListener(v -> {
            String value = nameInput.getText().toString().trim();
            if (value.isEmpty()) value = SYSTEM_AGENT_NAME;
            beginInitialization(value);
        });
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(58));
        startParams.topMargin = dp(18);
        content.addView(start, startParams);

        setContentView(root);
    }

    private void beginInitialization(String name) {
        agentName = SYSTEM_AGENT_NAME;
        getSharedPreferences("her", MODE_PRIVATE).edit().putString("agent_name", SYSTEM_AGENT_NAME).apply();
        if (memoryStore != null) {
            sessionId = memoryStore.startSession(agentName);
            persistedMessageIds.clear();
        }
        initializing = true;
        initialized = false;
        initPromptPending = true;
        initSummaryPending = false;
        summaryInProgress = false;
        ignoreNextInitTrigger = true;
        initContextUpdatePending = false;
        realtimeRetryCount = 0;
        initUserTurns = 0;
        activeAssistantId = null;
        messages.clear();
        showInitializationHome();
        realtime.close();
        realtime.connect();
    }

    private void showInitializationHome() {
        root = baseRoot();
        messageList = null;
        messageScroll = null;
        composer = null;
        homeTimeView = null;

        root.addView(topBar("☰", "", "", this::showSettings, null));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(28), 0, dp(28), dp(96));
        root.addView(center, frame(-1, -1));

        InitOrbView mark = new InitOrbView(this);
        center.addView(mark, new LinearLayout.LayoutParams(dp(154), dp(154)));

        initProgressView = text(initProgressText(), 14, 0xCCFFE0E0, 0);
        initProgressView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, -2);
        progressParams.topMargin = dp(30);
        center.addView(initProgressView, progressParams);

        initLastTurnView = text(lastInitializationLine(), 21, Color.WHITE, 0);
        initLastTurnView.setGravity(Gravity.CENTER);
        initLastTurnView.setLineSpacing(dp(4), 1.0f);
        initLastTurnView.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(-1, -2);
        lastParams.topMargin = dp(22);
        center.addView(initLastTurnView, lastParams);

        stateLabel = text(capitalize(state), 12, 0x99FFE0E0, 0);
        stateLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.topMargin = dp(8);
        center.addView(stateLabel, stateParams);

        audioLevelView = new AudioLevelView(this);
        FrameLayout.LayoutParams levelParams = frame(dp(190), dp(12), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        levelParams.bottomMargin = dp(100);
        root.addView(audioLevelView, levelParams);

        micButton = text("♩", 34, 0xFFFF6377, 0);
        micButton.setGravity(Gravity.CENTER);
        micButton.setOnClickListener(v -> toggleMic());
        FrameLayout.LayoutParams micParams = frame(dp(78), dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        micParams.bottomMargin = dp(22);
        root.addView(micButton, micParams);

        setContentView(root);
    }

    private void showChat() {
        if (mic.running || inputAudioOpen) stopInputAudio("ready");
        root = baseRoot();
        homeTimeView = null;
        voiceLastTurnView = null;
        voiceOrbView = null;
        audioLevelView = null;
        micButton = null;
        LinearLayout top = topBar("‹", agentName, "♩", initialized ? this::showHome : this::showInitialize, initialized ? this::showHome : this::showSettings);
        root.addView(top);

        stateLabel = text(capitalize(state), 11, 0x99FFE0E0, 0);
        stateLabel.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams stateParams = frame(-1, dp(22), Gravity.TOP);
        stateParams.topMargin = dp(54);
        root.addView(stateLabel, stateParams);

        if (initializing) {
            initProgressView = text(initProgressText(), 13, 0xCCFFE0E0, 0);
            initProgressView.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams initParams = frame(-1, dp(26), Gravity.TOP);
            initParams.topMargin = dp(76);
            root.addView(initProgressView, initParams);
        } else {
            initProgressView = null;
        }

        messageScroll = new ScrollView(this);
        messageScroll.setFillViewport(true);
        messageScroll.setPadding(0, initializing ? dp(108) : dp(80), 0, dp(98));
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(dp(26), dp(10), dp(26), dp(28));
        messageScroll.addView(messageList, new ScrollView.LayoutParams(-1, -2));
        root.addView(messageScroll, frame(-1, -1));
        renderMessages();

        LinearLayout input = new LinearLayout(this);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setPadding(dp(22), dp(12), dp(18), dp(12));
        input.setBackgroundColor(0x88523B48);

        composer = new EditText(this);
        composer.setHint("Type a message...");
        composer.setHintTextColor(0x80FFE0E0);
        composer.setTextColor(Color.WHITE);
        composer.setTextSize(18);
        composer.setSingleLine(true);
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.setBackgroundColor(Color.TRANSPARENT);
        input.addView(composer, new LinearLayout.LayoutParams(0, dp(56), 1));

        TextView send = text("➤", 26, 0xFFFF6377, 0);
        send.setGravity(Gravity.CENTER);
        send.setOnClickListener(v -> {
            String value = composer.getText().toString().trim();
            composer.setText("");
            sendText(value);
        });
        input.addView(send, new LinearLayout.LayoutParams(dp(50), dp(56)));

        root.addView(input, frame(-1, dp(92), Gravity.BOTTOM));
        top.bringToFront();
        setContentView(root);
    }

    private void showVoices() {
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
        root = baseRoot();
        Runnable back = initializing ? this::showInitializationHome : (initialized ? this::showHome : () -> beginInitialization(agentName));
        root.addView(topBar("‹", "Settings", "", back, null));
        LinearLayout list = screenList();
        list.addView(navRow("↺", "Reinitialize", "Reset memory", this::resetInitialization));
        list.addView(navRow("⌫", "Clear Session", "Keep memory", this::clearCurrentSession));
        list.addView(navRow("≋", "Voice", selectedVoiceLabel, this::showVoices));
        list.addView(navRow("♬", "Sound", "76%", null));
        list.addView(navRow("♧", "Notifications", "On", null));
        list.addView(navRow("▢", "Privacy", "", null));
        list.addView(navRow("◎", "Language", "中文 / English", null));
        list.addView(navRow("ⓘ", "About Her", "", this::showAbout));
        list.addView(navRow("≡", "Realtime", "Doubao · PCM16", null));
        setContentView(root);
    }

    private void clearCurrentSession() {
        mic.stop();
        player.stop();
        realtime.close();
        inputAudioOpen = false;
        pendingMicStart = false;
        pendingText = null;
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
        TextView version = text("Doubao model · Custom cloned voice · mimo-v2.5 memory", 13, 0x88FFE0E0, 0);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams v = new LinearLayout.LayoutParams(-1, -2);
        v.topMargin = dp(12);
        content.addView(version, v);
        setContentView(root);
    }

    private FrameLayout baseRoot() {
        FrameLayout frame = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(getResources().getIdentifier("her_background", "drawable", getPackageName()));
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bg, frame(-1, -1));
        moodVeil = new MoodVeil(this);
        moodVeil.setMood(moodForText(lastConversationLine()));
        frame.addView(moodVeil, frame(-1, -1));
        return frame;
    }

    private LinearLayout topBar(String left, String title, String right, Runnable leftAction, Runnable rightAction) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(22), dp(20), dp(22), 0);
        TextView l = icon(left);
        l.setOnClickListener(v -> {
            if (leftAction != null) leftAction.run();
        });
        bar.addView(l, new LinearLayout.LayoutParams(dp(54), dp(58)));
        TextView middle = text(title, 18, Color.WHITE, 500);
        middle.setGravity(Gravity.CENTER);
        bar.addView(middle, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView r = icon(right);
        r.setOnClickListener(v -> {
            if (rightAction != null) rightAction.run();
        });
        bar.addView(r, new LinearLayout.LayoutParams(dp(54), dp(58)));
        FrameLayout.LayoutParams params = frame(-1, dp(86), Gravity.TOP);
        bar.setLayoutParams(params);
        return bar;
    }

    private LinearLayout screenList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(28), dp(92), dp(28), dp(28));
        root.addView(list, frame(-1, -1));
        return list;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setMinimumHeight(dp(72));
        row.setBackground(new BottomLineDrawable());
        return row;
    }

    private View navRow(String symbol, String label, String value, Runnable action) {
        LinearLayout row = row();
        TextView s = text(symbol, 23, 0xCCFFFFFF, 0);
        s.setGravity(Gravity.CENTER);
        row.addView(s, new LinearLayout.LayoutParams(dp(48), -1));
        row.addView(text(label, 16, Color.WHITE, 0), new LinearLayout.LayoutParams(0, -1, 1));
        TextView val = text(value, 13, 0x99FFE0E0, 0);
        val.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(val, new LinearLayout.LayoutParams(dp(150), -1));
        TextView chevron = text(action == null ? "" : "›", 28, 0xCCFFFFFF, 0);
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(28), -1));
        if (action != null) row.setOnClickListener(v -> action.run());
        return row;
    }

    private void renderMessages() {
        updateInitializationLastTurn();
        updateVoiceHome();
        if (messageList == null) return;
        messageList.removeAllViews();
        for (Message message : messages) {
            messageList.addView(bubble(message));
            Space gap = new Space(this);
            messageList.addView(gap, new LinearLayout.LayoutParams(1, dp(18)));
        }
        main.postDelayed(() -> messageScroll.fullScroll(View.FOCUS_DOWN), 80);
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
        return "我在这里，马上开始认识你。";
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
            applyContextUpdateIfSafe(false);
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

            body.put("model", SUBCONSCIOUS_MODEL);
            body.put("messages", llmMessages);
            body.put("temperature", 0.2);
            body.put("stream", false);
        } catch (JSONException error) {
            compactInProgress = false;
            return;
        }

        Request request = new Request.Builder()
                .url(BuildConfig.AGENTLLM_BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.AGENTLLM_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        llmHttp.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> compactInProgress = false);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String text = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    main.post(() -> compactInProgress = false);
                    return;
                }
                String memory = "";
                String tone = "";
                try {
                    String content = new JSONObject(text)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .optString("content", "")
                            .trim();
                    JSONObject compact = parseJsonObject(content);
                    memory = compact.optString("memory_md", content).trim();
                    tone = compact.optString("tone_guidance", "").trim();
                } catch (JSONException error) {
                    memory = text;
                }
                String finalMemory = memory;
                String finalTone = tone;
                main.post(() -> {
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
                    applyContextUpdateIfSafe(true);
                });
            }
        });
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

    private String extractAssistantContent(String responseText) throws JSONException {
        return new JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "");
    }

    private void applyContextUpdateIfSafe(boolean includeFact) {
        if (!memoryDirtyForRealtime || !realtime.isOpen()) return;
        if ("speaking".equals(state) || "thinking".equals(state) || "listening".equals(state) || initializing) return;
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

    private View bubble(Message message) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setGravity(message.role.equals("user") ? Gravity.RIGHT : Gravity.LEFT);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView body = text(message.text.isEmpty() ? "..." : message.text, 19, Color.WHITE, 0);
        body.setLineSpacing(dp(2), 1.0f);
        body.setPadding(dp(16), dp(13), dp(16), dp(13));
        body.setBackground(new BubbleDrawable(message.role.equals("user")));
        column.addView(body, new LinearLayout.LayoutParams(-2, -2));
        TextView time = text(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(new Date(message.timestamp)), 12, 0x80FFE0E0, 0);
        time.setPadding(dp(8), dp(6), dp(8), 0);
        time.setGravity(message.role.equals("user") ? Gravity.RIGHT : Gravity.LEFT);
        column.addView(time, new LinearLayout.LayoutParams(-1, -2));
        int width = getResources().getDisplayMetrics().widthPixels - dp(110);
        wrap.addView(column, new LinearLayout.LayoutParams(Math.max(dp(190), width), -2));
        return wrap;
    }

    private void sendText(String text) {
        if (summaryInProgress) return;
        if (text == null || text.isEmpty()) return;
        addChatMessage("user", text);
        if (initializing) {
            initUserTurns++;
            updateInitProgress();
            if (initUserTurns >= INIT_TARGET_USER_TURNS) {
                renderMessages();
                finishInitializationWithSummary();
                return;
            }
            scheduleInitializationContextUpdate();
        }
        activeAssistantId = null;
        renderMessages();
        sendTextWithAgentLLM(text);
    }

    private void sendTextWithAgentLLM(String text) {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            toastError("Missing AGENTLLM_API_KEY in local.properties");
            setState("error");
            return;
        }
        setState("thinking");
        JSONObject body = new JSONObject();
        JSONArray llmMessages = new JSONArray();
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                    buildInstructions() + "\n\n" +
                    "当前是文本聊天通道。请只输出适合聊天气泡展示的文字，不要描述语音、音频或工具过程。");
            llmMessages.put(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", text);
            llmMessages.put(user);

            body.put("model", TEXT_CHAT_MODEL);
            body.put("messages", llmMessages);
            body.put("temperature", 0.7);
            body.put("stream", false);
        } catch (JSONException error) {
            toastError("构建文本聊天请求失败");
            setState("error");
            return;
        }

        Request request = new Request.Builder()
                .url(BuildConfig.AGENTLLM_BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.AGENTLLM_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        llmHttp.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> {
                    toastError("文本聊天失败：" + error.getMessage());
                    setState("error");
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    main.post(() -> {
                        toastError("文本聊天接口失败：" + response.code());
                        setState("error");
                    });
                    return;
                }
                String reply;
                try {
                    reply = extractAssistantContent(responseText);
                } catch (JSONException error) {
                    main.post(() -> {
                        toastError("解析文本聊天回复失败");
                        setState("error");
                    });
                    return;
                }
                String finalReply = reply.trim();
                main.post(() -> {
                    if (finalReply.isEmpty()) {
                        toastError("文本聊天返回为空");
                        setState("error");
                        return;
                    }
                    addChatMessage("assistant", finalReply);
                    renderMessages();
                    if (initializing) {
                        updateInitProgress();
                    }
                    setState("ready");
                });
            }
        });
    }

    private void toggleMic() {
        if (summaryInProgress) return;
        if (mic.running || inputAudioOpen) {
            stopInputAudio("thinking");
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
        player.stop();
        realtime.sendEvent("input_audio.interrupt", json("reason", "user_speech_detected"));
        startInputAudio();
    }

    private void startInputAudio() {
        if (mic.running || inputAudioOpen) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
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
        if (inputAudioOpen) {
            realtime.sendEvent("input_audio.end", null);
            inputAudioOpen = false;
        }
        startHerForegroundService(false);
        setState(nextState);
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
                    stopInputAudio("thinking");
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
        if (mic.running || inputAudioOpen || !realtime.isOpen()) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        startInputAudio();
    }

    private void scheduleContinuousListening(long delayMs) {
        if (!CONTINUOUS_CONVERSATION) return;
        main.postDelayed(() -> {
            if ("ready".equals(state) && !mic.running && !inputAudioOpen) {
                startContinuousListening();
            }
        }, delayMs);
    }

    private void onRealtimeReady() {
        if (initPromptPending) {
            initPromptPending = false;
            realtime.sendInputText(INIT_WAKE_EVENT);
            setState("thinking");
            return;
        }
        if (pendingText != null) {
            String text = pendingText;
            pendingText = null;
            realtime.sendInputText(text);
            setState("thinking");
        }
        if (pendingMicStart) {
            pendingMicStart = false;
            toggleMic();
        }
    }

    private void onAssistantDelta(String text) {
        if (activeAssistantId == null) {
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
        initContextUpdatePending = true;
    }

    private void updateInitializationContext() {
        if (!initializing || !realtime.isOpen()) return;
        if ("speaking".equals(state) || "thinking".equals(state) || "listening".equals(state)) {
            initContextUpdatePending = true;
            return;
        }
        JSONObject payload = new JSONObject();
        try {
            payload.put("instructions", buildInstructions());
        } catch (JSONException ignored) { }
        realtime.sendEvent("context.update", payload);
        initContextUpdatePending = false;
    }

    private void setState(String next) {
        state = next;
        if (stateLabel != null) stateLabel.setText(capitalize(next));
        if (micButton != null) micButton.setText(next.equals("listening") ? "●" : "♩");
        if (voiceOrbView != null) voiceOrbView.setConversationState(next);
        if ("ready".equals(next) || "idle".equals(next)) {
            applyContextUpdateIfSafe(false);
        }
    }

    private void loadVoices() {
        if (BuildConfig.AGENTVOICE_API_KEY.isEmpty()) return;
        Request request = new Request.Builder()
                .url(BuildConfig.AGENTVOICE_BASE_URL + "/v1/voices?model=doubao-realtime")
                .header("Authorization", "Bearer " + BuildConfig.AGENTVOICE_API_KEY)
                .build();
        llmHttp.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) return;
                try {
                    JSONArray array = new JSONObject(body).getJSONArray("voices");
                    List<Voice> loaded = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        String id = item.optString("id");
                        String label = id.equals(DEFAULT_VOICE) ? "Doris Clone" : item.optString("label", id);
                        loaded.add(new Voice(id, label, item.optString("gender", "voice")));
                    }
                    loaded.sort((a, b) -> a.id.equals(DEFAULT_VOICE) ? -1 : b.id.equals(DEFAULT_VOICE) ? 1 : 0);
                    main.post(() -> {
                        voices.clear();
                        voices.addAll(loaded);
                    });
                } catch (JSONException ignored) { }
            }
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

    private void summarizeInitialization() {
        if (BuildConfig.AGENTLLM_API_KEY.isEmpty()) {
            toastError("Missing AGENTLLM_API_KEY in local.properties");
            return;
        }

        JSONObject body = new JSONObject();
        JSONArray llmMessages = new JSONArray();
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", "你是 Doris 的潜意识模型，负责把初始化访谈整理为长期记忆。只输出 JSON，不要解释。格式：{\"user_name\":\"...\",\"user_md\":\"...\",\"agent_md\":\"...\"}");
            llmMessages.put(system);

            StringBuilder transcript = new StringBuilder();
            for (Message message : messages) {
                transcript.append(message.role).append(": ").append(message.text).append('\n');
            }
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content",
                    "Agent 名字：" + agentName + "\n" +
                    "请使用 mimo-v2.5 总结以下初始化对话，生成 user.md 和 Agent.md。要求：\n" +
                    "1. user_name 是用户希望被称呼的名字。\n" +
                    "2. user_md 包含用户姓名/称呼、用户希望和 AI 的关系、用户的故事、沟通偏好、重要边界或目标。\n" +
                    "3. agent_md 包含 Agent 的名字、默认语气、与该用户相处的关系定位、后续对话策略。\n" +
                    "2. 信息不确定时写“未明确”。\n" +
                    "4. Markdown 内容要适合后续作为系统提示词注入。\n\n" +
                    transcript);
            llmMessages.put(user);

            body.put("model", SUBCONSCIOUS_MODEL);
            body.put("messages", llmMessages);
            body.put("temperature", 0);
            body.put("stream", false);
        } catch (JSONException error) {
            toastError("构建摘要请求失败");
            return;
        }

        Request request = new Request.Builder()
                .url(BuildConfig.AGENTLLM_BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.AGENTLLM_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        realtime.http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> {
                    summaryInProgress = false;
                    stopInitProgressAnimation();
                    toastError("摘要失败：" + error.getMessage());
                    setState("error");
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String text = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    main.post(() -> {
                        summaryInProgress = false;
                        stopInitProgressAnimation();
                        toastError("摘要接口失败：" + response.code());
                        setState("error");
                    });
                    return;
                }
                try {
                    JSONObject json = new JSONObject(text);
                    String content = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .optString("content", "")
                            .trim();
                    String extractedUserName = "";
                    String userMd = content;
                    String agentMd = "- Agent name: " + agentName + "\n- 默认语气：温柔大姐姐。\n- 与用户的关系定位：未明确。";
                    try {
                        JSONObject profile = parseJsonObject(content);
                        extractedUserName = profile.optString("user_name", "").trim();
                        userMd = profile.optString("user_md", content).trim();
                        agentMd = profile.optString("agent_md", agentMd).trim();
                    } catch (JSONException ignored) {
                        extractedUserName = extractUserName(content);
                    }
                    String memory = "# user.md\n\n" +
                            "- Agent name: " + agentName + "\n" +
                            "- Created at: " + new Date() + "\n\n" +
                            userMd + "\n";
                    String agentProfile = "# Agent.md\n\n" +
                            "- Agent name: " + agentName + "\n" +
                            "- Created at: " + new Date() + "\n\n" +
                            agentMd + "\n";
                    String finalUserName = extractedUserName;
                    main.post(() -> {
                        writeUserMemory(memory);
                        writeAgentMemory(agentProfile);
                        userMemory = memory;
                        agentMemory = agentProfile;
                        userName = finalUserName.isEmpty() ? displayUserName() : finalUserName;
                        getSharedPreferences("her", MODE_PRIVATE).edit().putString("user_name", userName).apply();
                        initialized = true;
                        initializing = false;
                        summaryInProgress = false;
                        initSummaryPending = false;
                        stopInitProgressAnimation();
                        if (memoryStore != null) {
                            memoryStore.insertMemory(sessionId, "profile", memory, 0, 0);
                            memoryStore.insertMemory(sessionId, "agent", agentProfile, 0, 0);
                            conversationMemory = memoryStore.relevantMemory("");
                        }
                        messages.clear();
                        messages.add(new Message("memory-ready", "assistant", "初始化完成。"));
                        setState("idle");
                        showInitializationCompleteThenHome();
                    });
                } catch (JSONException error) {
                    main.post(() -> {
                        summaryInProgress = false;
                        stopInitProgressAnimation();
                        toastError("解析摘要失败");
                        setState("error");
                    });
                }
            }
        });
    }

    private String buildInstructions() {
        if (initializing) {
            String initGuide;
            if (initUserTurns <= 0) {
                initGuide = "当前阶段：还没有收到用户回答。收到系统事件后，你必须主动问候用户，介绍自己是 Doris，然后只问第 1 题：用户的名字，以及希望你怎么称呼用户。";
            } else if (initUserTurns == 1) {
                initGuide = "当前阶段：已经收到第 1 题答案。你的下一次回复只能简短回应，然后问第 2 题：用户希望和你建立什么关系。";
            } else if (initUserTurns == 2) {
                initGuide = "当前阶段：已经收到第 2 题答案。你的下一次回复只能简短回应，然后问第 3 题：请用户开放地讲讲自己的故事、近况、在意的事，或希望你记住的部分。";
            } else {
                initGuide = "当前阶段：三题都已回答。不要继续聊天或提问，只说明你正在写入 user.md。";
            }
            return "你叫 " + SYSTEM_AGENT_NAME + "。\n" +
                    INSTRUCTIONS + "\n" +
                INIT_BASE_PROMPT + "\n" +
                "第 0 步：你必须先主动介绍自己，说清楚你是 Doris，一个 AI Agent，也是用户的朋友和助理。\n" +
                "第 1 题：用户的名字，以及希望你怎么称呼用户。\n" +
                "第 2 题：用户希望和 Doris 建立什么关系。\n" +
                "第 3 题：用户的故事，一段开放式自我介绍，包括近况、经历、在意的事或希望你记住的部分。\n" +
                "第三题回答后，不要再输出新的轮次；客户端会关闭语音交互模型，并交给潜意识模型写入 user.md 和 Agent.md。\n" +
                initGuide;
        }
        String recent = recentDialogueForPrompt();
        return trimForPrompt("你叫 " + SYSTEM_AGENT_NAME + "。\n" +
                INSTRUCTIONS + "\n" +
                "当前动态语气调整：" + dynamicTone + "\n" +
                "以下是本地 user.md 记忆。你需要把它作为长期用户画像和对话偏好使用，但不要主动朗读或暴露文件内容。\n\n" +
                userMemory + "\n\n" +
                "以下是本地 Agent.md。你需要把它作为自己的关系定位、语气和长期行为准则使用。\n\n" +
                agentMemory + "\n\n" +
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
        TextView view = text(value, 29, Color.WHITE, 0);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        return view;
    }

    private TextView text(String value, int sp, int color, int weight) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (weight > 0) view.setTypeface(Typeface.DEFAULT, weight >= 700 ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private FrameLayout.LayoutParams frame(int width, int height) {
        return new FrameLayout.LayoutParams(width, height);
    }

    private FrameLayout.LayoutParams frame(int width, int height, int gravity) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = gravity;
        return params;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private final class RealtimeClient extends WebSocketListener {
        final OkHttpClient http = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
        WebSocket socket;
        boolean sessionCreated = false;

        boolean isOpen() {
            return socket != null && sessionCreated;
        }

        void connect() {
            if (BuildConfig.AGENTVOICE_API_KEY.isEmpty()) {
                toastError("Missing AGENTVOICE_API_KEY in gradle.properties");
                return;
            }
            if (socket != null) return;
            Log.d(TAG, "connect realtime");
            setState("connecting");
            String key;
            try {
                key = URLEncoder.encode(BuildConfig.AGENTVOICE_API_KEY, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                key = BuildConfig.AGENTVOICE_API_KEY;
            }
            Request request = new Request.Builder()
                    .url(BuildConfig.AGENTVOICE_REALTIME_URL + "?api_key=" + key)
                    .build();
            socket = http.newWebSocket(request, this);
        }

        @Override public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "websocket open");
            sendEvent("session.start", sessionPayload());
        }

        @Override public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject event = new JSONObject(text);
                JSONObject payload = event.optJSONObject("payload");
                String type = event.optString("type");
                Log.d(TAG, "event " + type + " payload=" + (payload == null ? "{}" : payload.toString()));
                main.post(() -> handleEvent(type, payload));
            } catch (JSONException e) {
                toastError("Bad realtime event");
            }
        }

        @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.d(TAG, "audio bytes " + bytes.size());
            player.play(bytes.toByteArray());
        }

        @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            socket = null;
            sessionCreated = false;
            Log.e(TAG, "websocket failure", t);
            main.post(() -> {
                setState("error");
                toastError(t.getMessage() == null ? "Realtime failed" : t.getMessage());
            });
        }

        @Override public void onClosed(WebSocket webSocket, int code, String reason) {
            socket = null;
            sessionCreated = false;
            Log.d(TAG, "websocket closed code=" + code + " reason=" + reason);
            main.post(() -> setState(summaryInProgress ? "summarizing" : "idle"));
        }

        void close() {
            sessionCreated = false;
            if (socket != null) {
                socket.close(1000, "client closing");
                socket = null;
            }
        }

        void sendInputText(String text) {
            Log.d(TAG, "send input_text " + text);
            JSONObject payload = new JSONObject();
            try {
                payload.put("text", text);
            } catch (JSONException ignored) { }
            sendEvent("input_text", payload);
        }

        void sendAudio(byte[] bytes) {
            if (socket != null) socket.send(ByteString.of(bytes));
        }

        void sendEvent(String type, JSONObject payload) {
            if (socket == null) return;
            Log.d(TAG, "send event " + type);
            JSONObject envelope = new JSONObject();
            try {
                envelope.put("type", type);
                if (payload != null) envelope.put("payload", payload);
            } catch (JSONException ignored) { }
            socket.send(envelope.toString());
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
    }

    private void handleEvent(String type, JSONObject payload) {
        if ("session.created".equals(type)) {
            realtime.sessionCreated = true;
            setState("ready");
            onRealtimeReady();
        } else if ("asr.final".equals(type) && payload != null) {
            String text = payload.optString("text", "").trim();
            if (initializing && isHiddenInitTrigger(text)) {
                ignoreNextInitTrigger = false;
                return;
            }
            if (!text.isEmpty()) addChatMessage("user", text);
            if (initializing && !text.isEmpty()) {
                initUserTurns++;
                updateInitProgress();
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
        } else if ("assistant.state".equals(type) && payload != null) {
            String providerState = payload.optString("state", "ready");
            if ("thinking".equals(providerState)) setState("thinking");
            if ("tts_streaming".equals(providerState)) enterAssistantSpeaking(0);
            if ("idle".equals(providerState) || "listening".equals(providerState)) setState(mic.running ? "listening" : "ready");
        } else if ("assistant.text.delta".equals(type) && payload != null) {
            onAssistantDelta(payload.optString("text", ""));
        } else if ("output_audio.start".equals(type) && payload != null) {
            enterAssistantSpeaking(payload.optInt("sample_rate", 24000));
        } else if ("output_audio.done".equals(type)) {
            persistActiveAssistantMessage();
            activeAssistantId = null;
            if (initializing && initSummaryPending) {
                finishInitializationWithSummary();
                return;
            }
            setState("ready");
            if (initializing && initContextUpdatePending) {
                updateInitializationContext();
            }
            scheduleContinuousListening(650);
        } else if ("output_audio.stop".equals(type)) {
            player.stop();
            persistActiveAssistantMessage();
            activeAssistantId = null;
            setState(mic.running ? "listening" : "ready");
            if (initializing && initContextUpdatePending && !mic.running && !inputAudioOpen) {
                updateInitializationContext();
            }
            if (!mic.running && !inputAudioOpen) scheduleContinuousListening(180);
        } else if ("memory.snapshot".equals(type) && payload != null) {
            handleMemorySnapshot(payload);
        } else if ("error".equals(type) && payload != null) {
            String code = payload.optString("code", "");
            String message = payload.optString("message", "Realtime error");
            if ("realtime_unavailable".equals(code) && payload.optBoolean("recoverable", false)) {
                retryRealtime(message);
                return;
            }
            toastError(message);
            setState("error");
        }
    }

    private boolean isHiddenInitTrigger(String text) {
        return text != null && (text.contains("系统事件") || text.contains("主动问候") || text.contains("Doris 主动"));
    }

    private void retryRealtime(String reason) {
        realtimeRetryCount++;
        if (realtimeRetryCount > 2) {
            toastError("语音交互模型暂时连接不上：" + reason);
            setState("error");
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

    interface AudioSink {
        void onAudio(byte[] bytes);
    }

    private final class MicStreamer {
        volatile boolean running = false;
        AudioRecord recorder;
        AcousticEchoCanceler echoCanceler;
        NoiseSuppressor noiseSuppressor;
        AutomaticGainControl gainControl;
        Thread thread;

        boolean start(AudioSink sink) {
            if (running) return true;
            int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(min, 16000);
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                recorder.release();
                recorder = null;
                return false;
            }
            enableEffects(recorder.getAudioSessionId());
            running = true;
            try {
                recorder.startRecording();
            } catch (IllegalStateException error) {
                running = false;
                releaseRecorder();
                return false;
            }
            thread = new Thread(() -> {
                byte[] buffer = new byte[640];
                while (running) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        byte[] out = new byte[read];
                        System.arraycopy(buffer, 0, out, 0, read);
                        sink.onAudio(out);
                    }
                }
            }, "her-mic");
            thread.start();
            return true;
        }

        void stop() {
            running = false;
            releaseRecorder();
        }

        private void enableEffects(int sessionId) {
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(sessionId);
                    if (echoCanceler != null) echoCanceler.setEnabled(true);
                }
            } catch (Throwable ignored) { }
            try {
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(sessionId);
                    if (noiseSuppressor != null) noiseSuppressor.setEnabled(true);
                }
            } catch (Throwable ignored) { }
            try {
                if (AutomaticGainControl.isAvailable()) {
                    gainControl = AutomaticGainControl.create(sessionId);
                    if (gainControl != null) gainControl.setEnabled(true);
                }
            } catch (Throwable ignored) { }
        }

        private void releaseRecorder() {
            if (echoCanceler != null) {
                echoCanceler.release();
                echoCanceler = null;
            }
            if (noiseSuppressor != null) {
                noiseSuppressor.release();
                noiseSuppressor = null;
            }
            if (gainControl != null) {
                gainControl.release();
                gainControl = null;
            }
            if (recorder != null) {
                try { recorder.stop(); } catch (Exception ignored) { }
                recorder.release();
                recorder = null;
            }
        }
    }

    private final class PcmPlayer {
        AudioTrack track;
        int sampleRate = 24000;

        synchronized void begin(int rate) {
            sampleRate = rate;
            Log.d(TAG, "player begin sampleRate=" + sampleRate);
            ensureTrack();
        }

        synchronized void play(byte[] bytes) {
            ensureTrack();
            int written = track.write(bytes, 0, bytes.length);
            Log.d(TAG, "player write bytes=" + bytes.length + " written=" + written + " state=" + track.getPlayState());
        }

        synchronized void stop() {
            if (track != null) {
                try { track.pause(); } catch (Exception ignored) { }
                try { track.flush(); } catch (Exception ignored) { }
                track.release();
                track = null;
            }
        }

        private void ensureTrack() {
            if (track != null) return;
            int min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            Log.d(TAG, "create AudioTrack min=" + min + " rate=" + sampleRate);
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(Math.max(min, sampleRate))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.play();
            Log.d(TAG, "AudioTrack playState=" + track.getPlayState());
        }
    }

    private static final class Message {
        final String id;
        final String role;
        String text;
        final long timestamp = System.currentTimeMillis();
        Message(String id, String role, String text) {
            this.id = id;
            this.role = role;
            this.text = text;
        }
    }

    private static final class Voice {
        final String id;
        final String label;
        final String gender;
        Voice(String id, String label, String gender) {
            this.id = id;
            this.label = label;
            this.gender = gender;
        }
    }

    private static final class MemoryChunk {
        final long firstId;
        final long lastId;
        final String transcript;
        MemoryChunk(long firstId, long lastId, String transcript) {
            this.firstId = firstId;
            this.lastId = lastId;
            this.transcript = transcript;
        }
    }

    private static final class MemoryStore extends SQLiteOpenHelper {
        MemoryStore(Activity activity) {
            super(activity, "her_memory.db", null, 1);
        }

        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "agent_name TEXT NOT NULL," +
                    "started_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "session_id INTEGER NOT NULL," +
                    "role TEXT NOT NULL," +
                    "content TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "compacted INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE memories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "session_id INTEGER NOT NULL," +
                    "kind TEXT NOT NULL," +
                    "content TEXT NOT NULL," +
                    "source_first_message_id INTEGER," +
                    "source_last_message_id INTEGER," +
                    "created_at INTEGER NOT NULL)");
            db.execSQL("CREATE VIRTUAL TABLE memory_fts USING fts4(content, kind)");
            db.execSQL("CREATE INDEX idx_messages_session_compacted ON messages(session_id, compacted, id)");
            db.execSQL("CREATE INDEX idx_memories_kind ON memories(kind, created_at)");
        }

        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS memory_fts");
            db.execSQL("DROP TABLE IF EXISTS memories");
            db.execSQL("DROP TABLE IF EXISTS messages");
            db.execSQL("DROP TABLE IF EXISTS sessions");
            onCreate(db);
        }

        long startSession(String agentName) {
            ContentValues values = new ContentValues();
            values.put("agent_name", agentName);
            values.put("started_at", System.currentTimeMillis());
            return getWritableDatabase().insert("sessions", null, values);
        }

        void insertMessage(long sessionId, String role, String content) {
            ContentValues values = new ContentValues();
            values.put("session_id", sessionId);
            values.put("role", role);
            values.put("content", content);
            values.put("created_at", System.currentTimeMillis());
            getWritableDatabase().insert("messages", null, values);
        }

        void insertMemory(long sessionId, String kind, String content, long firstId, long lastId) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("session_id", sessionId);
            values.put("kind", kind);
            values.put("content", content);
            values.put("source_first_message_id", firstId);
            values.put("source_last_message_id", lastId);
            values.put("created_at", System.currentTimeMillis());
            db.insert("memories", null, values);

            ContentValues fts = new ContentValues();
            fts.put("content", content);
            fts.put("kind", kind);
            db.insert("memory_fts", null, fts);
        }

        MemoryChunk unsummarizedChunk(long sessionId, int minCount, int minChars) {
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT id, role, content FROM messages WHERE session_id=? AND compacted=0 ORDER BY id ASC",
                    new String[]{String.valueOf(sessionId)});
            long first = 0;
            long last = 0;
            int count = 0;
            StringBuilder transcript = new StringBuilder();
            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    if (first == 0) first = id;
                    last = id;
                    count++;
                    transcript.append(cursor.getString(1)).append(": ")
                            .append(cursor.getString(2)).append('\n');
                }
            } finally {
                cursor.close();
            }
            if (count < minCount && transcript.length() < minChars) return null;
            return new MemoryChunk(first, last, transcript.toString());
        }

        void markCompacted(long lastId) {
            ContentValues values = new ContentValues();
            values.put("compacted", 1);
            getWritableDatabase().update("messages", values, "id<=?", new String[]{String.valueOf(lastId)});
        }

        String relevantMemory(String query) {
            StringBuilder builder = new StringBuilder();
            if (query != null && !query.trim().isEmpty()) {
                String match = sanitizeFts(query);
                if (!match.isEmpty()) {
                    Cursor cursor = getReadableDatabase().rawQuery(
                            "SELECT kind, content FROM memory_fts WHERE memory_fts MATCH ? LIMIT 4",
                            new String[]{match});
                    try {
                        while (cursor.moveToNext()) {
                            builder.append("- [").append(cursor.getString(0)).append("] ")
                                    .append(cursor.getString(1)).append('\n');
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            Cursor recent = getReadableDatabase().rawQuery(
                    "SELECT kind, content FROM memories ORDER BY id DESC LIMIT 6", null);
            try {
                while (recent.moveToNext()) {
                    builder.append("- [").append(recent.getString(0)).append("] ")
                            .append(recent.getString(1)).append('\n');
                }
            } finally {
                recent.close();
            }
            return builder.toString();
        }

        String latestTone() {
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT content FROM memories WHERE kind='tone' ORDER BY id DESC LIMIT 1", null);
            try {
                if (cursor.moveToFirst()) return cursor.getString(0);
            } finally {
                cursor.close();
            }
            return "保持温柔大姐姐语气：成熟、关照、亲近但有边界。";
        }

        void resetAll() {
            SQLiteDatabase db = getWritableDatabase();
            db.delete("memory_fts", null, null);
            db.delete("memories", null, null);
            db.delete("messages", null, null);
            db.delete("sessions", null, null);
        }

        void clearSession(long sessionId) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete("messages", "session_id=?", new String[]{String.valueOf(sessionId)});
            db.delete("sessions", "id=?", new String[]{String.valueOf(sessionId)});
        }

        private String sanitizeFts(String query) {
            String normalized = query.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
            if (normalized.isEmpty()) return "";
            String[] parts = normalized.split("\\s+");
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                if (part.length() < 2) continue;
                if (builder.length() > 0) builder.append(' ');
                builder.append(part);
            }
            return builder.toString();
        }
    }

    private class HerMarkView extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        HerMarkView(Activity activity) {
            super(activity);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setClickable(true);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.37f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xDDFFFFFF);
            paint.setStrokeWidth(dp(1));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setColor(0x44FFFFFF);
            canvas.drawCircle(cx, cy, r + dp(12), paint);
            canvas.drawCircle(cx, cy, r + dp(22), paint);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(dp(4));
            Path path = new Path();
            float left = cx - r * 0.55f;
            float right = cx + r * 0.55f;
            path.moveTo(left, cy);
            path.cubicTo(left, cy - r * 0.36f, cx - r * 0.12f, cy - r * 0.36f, cx, cy);
            path.cubicTo(cx + r * 0.12f, cy + r * 0.36f, right, cy + r * 0.36f, right, cy);
            path.cubicTo(right, cy - r * 0.36f, cx + r * 0.12f, cy - r * 0.36f, cx, cy);
            path.cubicTo(cx - r * 0.12f, cy + r * 0.36f, left, cy + r * 0.36f, left, cy);
            canvas.drawPath(path, paint);
        }
    }

    private final class InitOrbView extends HerMarkView {
        final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        long started = SystemClock.uptimeMillis();
        InitOrbView(Activity activity) {
            super(activity);
        }
        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            postInvalidateOnAnimation();
        }
        @Override protected void onDraw(Canvas canvas) {
            float t = ((SystemClock.uptimeMillis() - started) % 3600) / 3600f;
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            if (summaryInProgress) {
                float flip = (float) Math.cos(t * Math.PI * 8);
                canvas.save();
                canvas.scale(Math.max(0.18f, Math.abs(flip)), 1f, cx, cy);
                canvas.rotate(t * 360f, cx, cy);
            }
            float base = Math.min(w, h) * 0.38f;
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 4; i++) {
                float phase = (t + i * 0.18f) % 1f;
                int alpha = (int) (95 * (1f - phase));
                glow.setColor((alpha << 24) | 0x00FFFFFF);
                glow.setStrokeWidth(dp(1.2f + i * 0.3f));
                canvas.drawCircle(cx, cy, base + dp(8) + phase * dp(36), glow);
            }
            glow.setStyle(Paint.Style.FILL);
            glow.setShader(new LinearGradient(0, 0, w, h, 0x44FFFFFF, 0x11FF6377, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, base * (0.92f + 0.04f * (float) Math.sin(t * Math.PI * 2)), glow);
            glow.setShader(null);
            super.onDraw(canvas);
            if (summaryInProgress) canvas.restore();
            postInvalidateOnAnimation();
        }
    }

    private final class VoiceOrbView extends HerMarkView {
        final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        long started = SystemClock.uptimeMillis();
        int level = 0;
        String conversationState = "idle";

        VoiceOrbView(Activity activity) {
            super(activity);
        }

        void setLevel(int next) {
            level = Math.max(0, Math.min(100, next));
            invalidate();
        }

        void setConversationState(String next) {
            conversationState = next == null ? "idle" : next;
            invalidate();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            postInvalidateOnAnimation();
        }

        @Override protected void onDraw(Canvas canvas) {
            float t = ((SystemClock.uptimeMillis() - started) % 2400) / 2400f;
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float energy = level / 100f;
            if ("speaking".equals(conversationState)) {
                energy = Math.max(energy, 0.34f + 0.14f * (float) Math.sin(t * Math.PI * 4));
            } else if ("thinking".equals(conversationState) || "connecting".equals(conversationState)) {
                energy = Math.max(energy, 0.18f + 0.08f * (float) Math.sin(t * Math.PI * 2));
            } else if ("listening".equals(conversationState)) {
                energy = Math.max(energy, 0.12f);
            }

            float base = Math.min(w, h) * (0.35f + energy * 0.035f);
            glow.setStyle(Paint.Style.FILL);
            glow.setShader(new LinearGradient(0, 0, w, h,
                    0x33FFFFFF, "listening".equals(conversationState) ? 0x33FF6377 : 0x22B96A7C,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, base + dp(18) * energy, glow);
            glow.setShader(null);

            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 4; i++) {
                float phase = (t + i * 0.2f) % 1f;
                int alpha = (int) ((55 + 85 * energy) * (1f - phase));
                glow.setColor((Math.max(0, Math.min(160, alpha)) << 24) | 0x00FFFFFF);
                glow.setStrokeWidth(dp(1.2f + energy * 2.2f));
                canvas.drawCircle(cx, cy, base + dp(10) + phase * dp(44 + 18 * energy), glow);
            }

            canvas.save();
            float scale = 1f + energy * 0.05f;
            canvas.scale(scale, scale, cx, cy);
            super.onDraw(canvas);
            canvas.restore();
            postInvalidateOnAnimation();
        }
    }

    private final class SwatchView extends View {
        final int color;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        SwatchView(Activity activity, int color) {
            super(activity);
            this.color = color;
        }
        @Override protected void onDraw(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) * 0.45f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0xDDFFFFFF);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) * 0.45f, paint);
        }
    }

    private final class AudioLevelView extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int level = 0;
        AudioLevelView(Activity activity) {
            super(activity);
        }
        void setLevel(int next) {
            level = Math.max(0, Math.min(100, next));
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            float radius = getHeight() / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x26FFFFFF);
            canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()), radius, radius, paint);
            float width = Math.max(getHeight(), getWidth() * (level / 100f));
            paint.setColor(0xFFFF6377);
            canvas.drawRoundRect(new RectF(0, 0, width, getHeight()), radius, radius, paint);
        }
    }

    private final class MoodVeil extends View {
        final Paint paint = new Paint();
        int mood = 0;
        MoodVeil(Activity activity) {
            super(activity);
        }

        void setMood(int next) {
            if (mood == next) return;
            mood = next;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            int start;
            int end;
            int wash;
            if (mood == 1) {
                start = 0x806E6EA8;
                end = 0xAA221C34;
                wash = 0x262B2343;
            } else if (mood == 2) {
                start = 0x80D64C58;
                end = 0xAA3D2530;
                wash = 0x24FFB15C;
            } else if (mood == 3) {
                start = 0x8055798B;
                end = 0xAA202D35;
                wash = 0x24233345;
            } else {
                start = 0x80C93445;
                end = 0xAA281D2C;
                wash = 0x2230182A;
            }
            paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                    start, end, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
            paint.setColor(wash);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    private final class BubbleDrawable extends android.graphics.drawable.Drawable {
        final boolean user;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BubbleDrawable(boolean user) {
            this.user = user;
        }
        @Override public void draw(Canvas canvas) {
            paint.setColor(user ? 0xB08F3846 : 0xB0A93C4E);
            canvas.drawRoundRect(new RectF(getBounds()), dp(7), dp(7), paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    private final class BottomLineDrawable extends android.graphics.drawable.ColorDrawable {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas canvas) {
            super.draw(canvas);
            paint.setColor(0x18FFFFFF);
            paint.setStrokeWidth(1);
            canvas.drawLine(0, getBounds().bottom - 1, getBounds().right, getBounds().bottom - 1, paint);
        }
    }
}
