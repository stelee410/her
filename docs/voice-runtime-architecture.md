# Voice Runtime Architecture

这份文档描述当前 Android 端的语音运行时边界。目标是把 `RealtimeClient`、`AgentApiClient`、`GatewayTtsPlayer`、麦克风、播放、工具、fact update 和记忆落盘的职责拆清楚，方便后续继续把 `MainActivity` 里的编排逻辑抽成更小的 controller。

## 模块边界

| 模块 | 当前职责 | 主要调用方 |
| --- | --- | --- |
| `MainActivity` | 运行时编排、UI 状态机、工具路由、记忆写入入口 | Android lifecycle / UI / headset |
| `RealtimeClient` | AgentVoice realtime WebSocket，负责 session、事件、PCM 输入输出 | `MainActivity` |
| `AgentApiClient` | c-her HTTP chat/completions，负责文本聊天、潜意识总结、工具路由、记忆压缩 | `MainActivity` |
| `GatewayTtsPlayer` | AgentLLM TTS HTTP 生成 MP3，并用 `MediaPlayer` 播放 | `MainActivity` |
| `MicStreamer` | `AudioRecord` 采集 16 kHz PCM，带 AEC/NS/AGC | `MainActivity` |
| `PcmPlayer` | 播放 realtime 返回的 PCM16 音频流 | `MainActivity` |
| `WeatherTool` / `NewsTool` | 客户端直接调用天气/新闻数据源 | `MainActivity` |
| `MemoryStore` | SQLite messages/memories/FTS，保存会话、摘要、profile 索引 | `MainActivity` |

## 总体调用管理

```mermaid
flowchart LR
    UI["UI / Headset / Lifecycle"] --> A["MainActivity\nRuntime Orchestrator"]

    A --> RT["RealtimeClient\nAgentVoice WebSocket"]
    RT -->|events / PCM audio| A
    A -->|PCM chunks| RT

    A --> LLM["AgentApiClient\nc-her HTTP"]
    LLM -->|reply / JSON summary| A

    A --> TTS["GatewayTtsPlayer\nAgentLLM TTS"]
    TTS -->|started / completed / error| A

    A --> MIC["MicStreamer\nAudioRecord PCM16"]
    MIC -->|audio frames| A

    A --> PCM["PcmPlayer\nRealtime PCM playback"]
    A --> MEM["MemoryStore\nSQLite + FTS"]
    A --> TOOLS["WeatherTool / NewsTool"]
    TOOLS -->|tool result| A
```

`MainActivity` 现在仍然是编排中心。底层调用已经分别由 `RealtimeClient`、`AgentApiClient`、`GatewayTtsPlayer` 承担；后续最值得抽离的是 `MainActivity` 中的 runtime orchestration，而不是再改底层 client。

## 1. Initial 阶段

初始化目标是生成长期 profile：`user.md`、`Agent.md`、SQLite profile/agent memory，并把 Agent 名字、用户名字和关系设定保存下来。

```mermaid
sequenceDiagram
    participant App as MainActivity
    participant TTS as GatewayTtsPlayer
    participant RT as RealtimeClient
    participant LLM as AgentApiClient c-her
    participant FS as user.md / Agent.md
    participant DB as MemoryStore

    App->>App: beginInitialization("")
    App->>App: random 12 candidate female names
    App->>TTS: speak opening prompt
    TTS-->>App: onStarted -> show first subtitle
    TTS-->>App: onCompleted
    App->>RT: connect + input_audio.start
    RT-->>App: asr.final for init answer
    App->>App: recordInitializationAnswer()
    App->>RT: context.update(buildInstructions)
    Note over App,RT: Repeat for 3 init user turns
    App->>RT: close()
    App->>LLM: summarizeInitialization()
    LLM-->>App: JSON {agent_name,user_name,user_md,agent_md}
    App->>FS: writeUserMemory / writeAgentMemory
    App->>DB: insertMemory(profile), insertMemory(agent)
    App->>App: initialized=true, show home
```

关键点：

- 开场主动语音由 `GatewayTtsPlayer` 播放，不依赖 realtime 先发言。
- 初始化对话仍使用 realtime 收音和 ASR。
- 第三个用户回答后，客户端关闭 realtime，把 transcript 交给 c-her 潜意识模型整理。
- 写入顺序是文件和 SharedPreferences，再落 SQLite profile/agent memory。

## 2. 正常语音对话阶段

### 麦克风管线

```mermaid
flowchart TD
    Start["voice surface / headset / autoStart"] --> Perm["check RECORD_AUDIO + headset/demo mode"]
    Perm --> RTReady{"Realtime session open?"}
    RTReady -- no --> Connect["RealtimeClient.connect()"]
    Connect --> Session["session.created"]
    Session --> InputStart["input_audio.start"]
    RTReady -- yes --> InputStart
    InputStart --> Mic["MicStreamer.start()"]
    Mic --> Frame["640-byte PCM16 frames"]
    Frame --> Send["RealtimeClient.sendAudio(bytes)"]
    Frame --> VAD["processVad()"]
    VAD --> End{"silence after speech?"}
    End -- yes --> Stop["stopInputAudio(processing)"]
    Stop --> InputEnd["input_audio.end"]
    End -- no --> Frame
```

当前 UI 中 `processing` 表示“用户说完，等待 ASR/final 或模型回复”，不再暴露为 `Thinking`。

### Realtime 回复与播放管线

```mermaid
sequenceDiagram
    participant RT as RealtimeClient
    participant App as MainActivity
    participant PCM as PcmPlayer
    participant MEM as MemoryStore

    RT-->>App: assistant.text.delta
    App->>App: append active assistant message
    RT-->>App: output_audio.start(sample_rate)
    App->>App: realtimeOutputActive=true
    App->>PCM: begin(sample_rate)
    RT-->>App: binary PCM audio
    App->>PCM: play(bytes) unless discardRealtimeAudioUntilDone
    RT-->>App: output_audio.done / output_audio.stop
    App->>MEM: persistActiveAssistantMessage()
    App->>App: state=ready
    App->>App: scheduleContinuousListening()
```

### 语音播放与打断管线

```mermaid
flowchart TD
    NewAudio["Need to play TTS/tool result/user interrupt"] --> Interrupt["interruptRealtimePlayback(reason)"]
    Interrupt --> Discard["discardRealtimeAudioUntilDone=true"]
    Interrupt --> StopPCM["PcmPlayer.stop()"]
    Interrupt --> RTInterrupt["send input_audio.interrupt if realtime open"]
    StopPCM --> Wait{"Realtime output stopped?"}
    Wait -- done/stop callback --> PlayTTS["GatewayTtsPlayer.play()"]
    Wait -- stale/no callback --> Force["force start after short delay"]
    Force --> PlayTTS
    PlayTTS --> TTSDone{"completed / error / user stop"}
    TTSDone --> Ready["state=ready"]
    Ready --> Resume["resumeListeningAfterToolTts()"]
```

设计原则：

- realtime PCM 和 TTS 不能重叠。
- 工具 TTS 请求开始时就算 active，不等 `MediaPlayer.start()`。
- 如果 realtime 的 stop/done 回调丢失或来自 stale socket，短延迟后强制启动工具 TTS，并继续丢弃后续 realtime audio。

## 3. 包含调用工具的对话

天气和新闻都走“客户端工具结果 + TTS 播报”，避免 realtime 模型编造结果。

```mermaid
sequenceDiagram
    participant User
    participant App as MainActivity
    participant RT as RealtimeClient
    participant Tool as WeatherTool / NewsTool
    participant TTS as GatewayTtsPlayer

    User->>App: voice utterance
    App->>RT: input_audio.end
    RT-->>App: asr.final(text)
    App->>App: handleWeatherQuestion / handleNewsQuestion
    App->>RT: interruptRealtimePlayback(tool reason)
    App->>App: remove/discard realtime assistant draft
    App->>Tool: query/fetch
    Tool-->>App: success/error result
    App->>App: add card + assistant text
    App->>TTS: queueToolTtsPlayback(answer)
    TTS-->>App: started
    TTS-->>App: completed/error
    App->>App: resumeListeningAfterToolTts()
```

### Fact update 管线

```mermaid
flowchart TD
    ToolResult["Weather/News result"] --> Fact["latestWeatherFact / latestNewsFact"]
    Fact --> Prompt["buildInstructions(): WeatherSkill.promptBlock / NewsSkill.promptBlock"]
    Fact --> Context["applyContextUpdateForNextTurn(includeFact)"]
    Context --> RealtimeOpen{"Realtime open?"}
    RealtimeOpen -- yes --> Update["RealtimeClient.sendEvent(context.update)"]
    RealtimeOpen -- no --> NextSession["included in next session.start payload"]

    ToolResult --> DirectTTS["Direct tool TTS answer"]
    DirectTTS --> Note["Current preferred path for user-facing tool result"]
```

当前用户可听见的工具结果优先走 Direct TTS；fact update 主要用于保持 realtime 后续上下文一致，避免它在下一轮否认或编造工具结果。

### 新闻工具特殊链路

```mermaid
flowchart TD
    NewsIntent["news intent hit"] --> Ack["optional realtime ack: 请稍等..."]
    Ack --> AckDone["output_audio.done/stop"]
    AckDone --> RunTool["runNewsTool()"]
    RunTool --> Fetch["NewsTool.fetchDaily(agentnews)"]
    Fetch --> Queue["queueToolTtsPlayback(news, shortAnswer)"]
    Queue --> Interrupt["interrupt realtime playback"]
    Interrupt --> TTS["GatewayTtsPlayer.play(news answer)"]
    TTS --> Resume["resume listening"]
```

## 记忆落盘管线

```mermaid
flowchart TD
    Msg["addChatMessage / persistActiveAssistantMessage"] --> Persist["persistMessage()"]
    Persist --> DBMsg["MemoryStore.insertMessage(session_id, role, content)"]
    DBMsg --> Retrieve["conversationMemory = relevantMemory(lastUserUtterance)"]
    DBMsg --> CompactCheck{"unsummarized count/chars over threshold?"}
    CompactCheck -- no --> Done["done"]
    CompactCheck -- yes --> Subconscious["AgentApiClient.sendSubconscious(memory compression)"]
    Subconscious --> CompactJSON["memory_md + tone_guidance"]
    CompactJSON --> DBMem["insertMemory(compact/tone)"]
    DBMem --> Mark["markCompacted(lastId)"]
    Mark --> ContextDirty["memoryDirtyForRealtime=true"]
    ContextDirty --> ContextUpdate["applyContextUpdateForNextTurn(includeFact=true)"]

    InitSummary["Initialization summary"] --> Files["write user.md / Agent.md"]
    InitSummary --> ProfileMem["insertMemory(profile/agent)"]
    Snapshot["memory.snapshot from realtime"] --> SnapshotMem["insertMemory(agentvoice_snapshot)"]
```

注意：

- 普通消息只有初始化完成后才进入 SQLite。
- 工具 fact 是 transient memory，`MemoryStore` 检索时会过滤，避免天气/新闻旧结果污染长期记忆。
- `memory.snapshot` 会写入 SQLite，但在 prompt 检索中排除 `agentvoice_snapshot`，避免把完整 realtime prompt 反复注入。

## 后续代码整理建议

建议按风险从低到高拆分：

1. `ToolTtsCoordinator`
   管理 `pendingToolTtsId/text`、`toolTtsPlaybackActive`、force-start、resume listening 回调。这个边界最清楚，也最能减少 TTS/realtime overlap bug。

2. `VoiceInputController`
   管理 `MicStreamer`、VAD、`input_audio.start/end`、foreground service、`processing/listening` 状态。

3. `RealtimeTurnController`
   管理 realtime output lifecycle：`activeAssistantId`、`realtimeOutputActive`、`discardRealtimeAudioUntilDone`、PCM playback、interrupt。

4. `ToolRouter`
   管理 news/weather intent、工具执行、卡片、fact update、TTS answer。

5. `MemoryCoordinator`
   管理 `persistMessage`、`maybeCompactMemory`、初始化写档、SQLite memory/fact filtering。

拆分顺序不要反过来。先抽 TTS 和输入/输出管线，最后再抽记忆，因为记忆依赖 profile、prompt、消息和工具过滤，耦合面最大。
