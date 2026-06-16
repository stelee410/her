package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TabletDemoVoiceCommandTest {
    @Test
    public void detectsGreetingReplayCommands() {
        assertTrue(TabletDemoVoiceCommand.shouldReplayGreeting(
                TabletDemoVoiceCommand.normalize("打个招呼。")));
        assertTrue(TabletDemoVoiceCommand.shouldReplayGreeting(
                TabletDemoVoiceCommand.normalize("再打个招呼")));
        assertTrue(TabletDemoVoiceCommand.shouldReplayGreeting(
                TabletDemoVoiceCommand.normalize("重新打招呼")));
        assertFalse(TabletDemoVoiceCommand.shouldReplayGreeting(
                TabletDemoVoiceCommand.normalize("你好")));
    }

    @Test
    public void detectsAvatarChangeCommands() {
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("换个形象")));
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("换一个角色。")));
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("换角色")));
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("切换角色")));
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("下一个角色")));
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("换一个人")));
        assertTrue(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("换套衣服！")));
        assertFalse(TabletDemoVoiceCommand.shouldChangeAvatar(
                TabletDemoVoiceCommand.normalize("打个招呼")));
    }

    @Test
    public void detectsHiddenJessCommands() {
        assertTrue(TabletDemoVoiceCommand.shouldShowHiddenJess(
                TabletDemoVoiceCommand.normalize("叫 Jess 出来")));
        assertTrue(TabletDemoVoiceCommand.shouldShowHiddenJess(
                TabletDemoVoiceCommand.normalize("切到杰西卡")));
        assertTrue(TabletDemoVoiceCommand.shouldShowHiddenJess(
                TabletDemoVoiceCommand.normalize("隐藏人物")));
        assertFalse(TabletDemoVoiceCommand.shouldShowHiddenJess(
                TabletDemoVoiceCommand.normalize("换一个人")));
    }
}
