package com.linkyun.her;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class AgentApiClient {
    private static final String TAG = "HerRealtime";
    private static final String[] UNSUPPORTED_REALTIME_VOICES = {
            "zh_female_roumeinvyou_emo_v2_mars_bigtts",
            "zh_female_tianxinxiaomei_emo_v2_mars_bigtts",
            "zh_female_gaolengyujie_emo_v2_mars_bigtts",
            "zh_male_aojiaobazong_emo_v2_mars_bigtts",
            "zh_male_junlangnanyou_emo_v2_mars_bigtts",
            "zh_male_ruyayichen_emo_v2_mars_bigtts",
            "zh_male_jingqiangkanye_emo_mars_bigtts",
            "zh_male_guangzhoudege_emo_mars_bigtts",
            "zh_female_linjuayi_emo_v2_mars_bigtts",
            "zh_male_yourougongzi_emo_v2_mars_bigtts",
            "zh_male_zhoujielun_emo_v2_mars_bigtts"
    };

    interface ReplyCallback {
        void onSuccess(String content);
        void onError(String message);
    }

    interface VoicesCallback {
        void onSuccess(List<Voice> voices);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http;
    private final Handler main;

    AgentApiClient(OkHttpClient http, Handler main) {
        this.http = http;
        this.main = main;
    }

    void sendChat(String model, String instructions, String text, ReplyCallback callback) throws JSONException {
        JSONObject body = new JSONObject();
        JSONArray llmMessages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", instructions);
        llmMessages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", text);
        llmMessages.put(user);

        body.put("model", model);
        body.put("messages", llmMessages);
        body.put("temperature", 0.7);
        body.put("stream", false);
        enqueueChat(body, callback, "文本聊天");
    }

    void sendSubconscious(JSONObject body, ReplyCallback callback, String label) {
        enqueueChat(body, callback, label);
    }

    void loadVoices(String defaultVoice, VoicesCallback callback) {
        if (BuildConfig.AGENTVOICE_API_KEY.isEmpty()) return;
        Request request = new Request.Builder()
                .url(BuildConfig.AGENTVOICE_BASE_URL + "/v1/voices?model=doubao-realtime")
                .header("Authorization", "Bearer " + BuildConfig.AGENTVOICE_API_KEY)
                .build();
        http.newCall(request).enqueue(new Callback() {
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
                        if (!isRealtimeSupportedVoiceId(id)) continue;
                        String label = id.equals(defaultVoice) ? "Doris Clone" : item.optString("label", id);
                        loaded.add(new Voice(id, label, item.optString("gender", "voice")));
                    }
                    loaded.sort((a, b) -> a.id.equals(defaultVoice) ? -1 : b.id.equals(defaultVoice) ? 1 : 0);
                    main.post(() -> callback.onSuccess(loaded));
                } catch (JSONException ignored) { }
            }
        });
    }

    static boolean isRealtimeSupportedVoiceId(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        for (String unsupported : UNSUPPORTED_REALTIME_VOICES) {
            if (unsupported.equals(id)) return false;
        }
        return true;
    }

    private void enqueueChat(JSONObject body, ReplyCallback callback, String label) {
        long startedAtMs = SystemClock.elapsedRealtime();
        String model = body.optString("model", "");
        Request request = new Request.Builder()
                .url(BuildConfig.AGENTLLM_BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.AGENTLLM_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                Log.i(TAG, "agent chat failed label=" + label
                        + " model=" + model
                        + " elapsedMs=" + elapsedSince(startedAtMs)
                        + " message=" + error.getMessage());
                main.post(() -> callback.onError(label + "失败：" + error.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body() == null ? "" : response.body().string();
                long elapsedMs = elapsedSince(startedAtMs);
                if (!response.isSuccessful()) {
                    Log.i(TAG, "agent chat http failed label=" + label
                            + " model=" + model
                            + " code=" + response.code()
                            + " elapsedMs=" + elapsedMs
                            + " bytes=" + responseText.length());
                    main.post(() -> callback.onError(label + "接口失败：" + response.code()));
                    return;
                }
                try {
                    String reply = extractAssistantContent(responseText).trim();
                    Log.i(TAG, "agent chat completed label=" + label
                            + " model=" + model
                            + " elapsedMs=" + elapsedMs
                            + " replyChars=" + reply.length());
                    main.post(() -> callback.onSuccess(reply));
                } catch (JSONException error) {
                    Log.i(TAG, "agent chat parse failed label=" + label
                            + " model=" + model
                            + " elapsedMs=" + elapsedMs
                            + " bytes=" + responseText.length());
                    main.post(() -> callback.onError("解析" + label + "回复失败"));
                }
            }
        });
    }

    private static long elapsedSince(long startedAtMs) {
        return SystemClock.elapsedRealtime() - startedAtMs;
    }

    static String extractAssistantContent(String responseText) throws JSONException {
        return new JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "");
    }
}
