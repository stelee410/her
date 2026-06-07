package com.linkyun.her;

final class OneShotCallbackGuard {
    private boolean completed;

    synchronized boolean tryComplete() {
        if (completed) return false;
        completed = true;
        return true;
    }
}
