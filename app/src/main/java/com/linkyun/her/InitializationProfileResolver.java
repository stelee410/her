package com.linkyun.her;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;

final class InitializationProfileResolver {
    private InitializationProfileResolver() {
    }

    static Profile resolve(String content, String transcript, String currentAgentName,
            String fallbackAgentName, String systemAgentName) {
        String extractedUserName = "";
        String extractedAgentName = clean(currentAgentName);
        String userMd = clean(content);
        String agentMd = "";
        String transcriptUserName = inferUserNameFromTranscript(transcript);
        String transcriptRelationship = inferRelationshipFromTranscript(transcript);
        String transcriptAgentName = inferAgentNameFromTranscript(transcript, systemAgentName);
        boolean selfNamed = extractedAgentName.isEmpty();
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
        extractedAgentName = cleanAgentName(extractedAgentName, selfNamed, systemAgentName);
        if (extractedAgentName.isEmpty()) {
            extractedAgentName = cleanAgentName(extractAgentName(agentMd), selfNamed, systemAgentName);
        }
        if (extractedAgentName.isEmpty()) {
            extractedAgentName = cleanAgentName(transcriptAgentName, selfNamed, systemAgentName);
        }
        if (extractedAgentName.isEmpty()) extractedAgentName = clean(fallbackAgentName);
        if (extractedAgentName.isEmpty()) extractedAgentName = systemAgentName;
        userMd = ensureUserProfile(userMd, extractedUserName, transcriptRelationship);
        agentMd = ensureAgentProfile(agentMd, extractedAgentName, extractedUserName, transcriptRelationship, systemAgentName);
        return new Profile(extractedAgentName, extractedUserName, transcriptRelationship, userMd, agentMd);
    }

    static String extractUserName(String markdown) {
        if (markdown == null) return "";
        String[] lines = markdown.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.US).contains("\"user_name\"")) {
                try {
                    String value = normalizeProfileCandidate(parseJsonObject(trimmed).optString("user_name", ""));
                    if (isUsableProfileValue(value)) return value;
                } catch (JSONException ignored) {
                    // Raw JSON-like rows are not markdown profile rows.
                }
                continue;
            }
            int colon = userNameLabelColon(trimmed);
            if (colon >= 0 && colon + 1 < trimmed.length()) {
                String value = normalizeProfileCandidate(trimmed.substring(colon + 1));
                if (isUsableProfileValue(value)) return value;
            }
        }
        return "";
    }

    static String extractAgentName(String markdown) {
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

    private static String inferUserNameFromTranscript(String transcript) {
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

    private static String inferAgentNameFromTranscript(String transcript, String systemAgentName) {
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
                value = cleanAgentName(value, true, systemAgentName);
                if (!value.isEmpty()) return value;
            }
        }
        return "";
    }

    private static String inferRelationshipFromTranscript(String transcript) {
        if (transcript == null) return "";
        String[] lines = transcript.split("\\n");
        for (String line : lines) {
            if (!line.startsWith("user:")) continue;
            String relationship = relationshipFromText(line.substring(5).trim());
            if (isUsableProfileValue(relationship)) return relationship;
        }
        return "";
    }

    private static String ensureUserProfile(String userMd, String extractedUserName, String relationship) {
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

    private static String ensureAgentProfile(String agentMd, String extractedAgentName,
            String extractedUserName, String relationship, String systemAgentName) {
        String value = agentMd == null ? "" : agentMd.trim();
        if (value.contains(systemAgentName) || value.contains("与用户的关系定位：未明确")) {
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
                builder.append("用户希望你作为")
                        .append(extractedUserName.isEmpty() ? "用户" : extractedUserName)
                        .append("的")
                        .append(relationship)
                        .append("。\n");
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

    private static JSONObject parseJsonObject(String content) throws JSONException {
        String trimmed = content == null ? "" : content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return new JSONObject(trimmed);
    }

    private static boolean asksForUserName(String text) {
        if (text == null) return false;
        return text.contains("叫什么") || text.contains("怎么称呼") ||
                text.contains("称呼你") || text.contains("你的名字") ||
                text.contains("你叫什么");
    }

    private static String relationshipFromText(String text) {
        if (text == null) return "";
        if (text.contains("女朋友") || text.contains("恋人") || text.contains("情侣")) return "女朋友";
        if (text.contains("男朋友")) return "男朋友";
        if (text.contains("助理") || text.contains("助手")) return "朋友和助理";
        if (text.contains("朋友")) return "朋友";
        return "";
    }

    private static String normalizeProfileCandidate(String value) {
        if (value == null) return "";
        String result = value.replace("*", "")
                .replace("-", "")
                .replace("\"", "")
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

    private static int userNameLabelColon(String value) {
        String lower = value.toLowerCase(Locale.US);
        String[] labels = {"姓名/称呼", "用户姓名", "称呼", "user_name"};
        for (String label : labels) {
            int index = lower.indexOf(label.toLowerCase(Locale.US));
            if (index < 0) continue;
            int colon = firstProfileColonAfter(value, index + label.length());
            if (colon >= 0) return colon;
        }
        return -1;
    }

    private static int firstProfileColonAfter(String value, int start) {
        for (int i = start; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == ':' || current == '：') return i;
            if (!Character.isWhitespace(current) && current != '`' && current != '"' && current != '*') {
                return -1;
            }
        }
        return -1;
    }

    private static String cleanAgentName(String value, boolean selfNamed, String systemAgentName) {
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
        if (!isAcceptableAgentName(name, selfNamed, systemAgentName)) return "";
        return name;
    }

    private static boolean isAcceptableAgentName(String name, boolean selfNamed, String systemAgentName) {
        if (name == null || name.trim().isEmpty() || name.length() > 16) return false;
        String lower = name.toLowerCase(Locale.US);
        if (name.contains("豆包") || name.contains("小包") || name.contains("包包") ||
                name.contains("助手") || name.contains("助理") || name.contains("智能") ||
                name.contains("机器人") || name.contains("AI") || lower.contains("bot") ||
                lower.contains("assistant")) return false;
        return !selfNamed || !systemAgentName.equals(name);
    }

    private static boolean isUsableProfileValue(String value) {
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class Profile {
        final String agentName;
        final String userName;
        final String relationship;
        final String userMd;
        final String agentMd;

        Profile(String agentName, String userName, String relationship, String userMd, String agentMd) {
            this.agentName = agentName;
            this.userName = userName;
            this.relationship = relationship;
            this.userMd = userMd;
            this.agentMd = agentMd;
        }

        String userMemory(Date createdAt) {
            return "# user.md\n\n" +
                    "- Agent name: " + agentName + "\n" +
                    "- Created at: " + createdAt + "\n\n" +
                    userMd + "\n";
        }

        String agentMemory(Date createdAt) {
            return "# Agent.md\n\n" +
                    "- Agent name: " + agentName + "\n" +
                    "- Created at: " + createdAt + "\n\n" +
                    agentMd + "\n";
        }

        String displayUserName(String fallback) {
            return userName.isEmpty() ? fallback : userName;
        }
    }
}
