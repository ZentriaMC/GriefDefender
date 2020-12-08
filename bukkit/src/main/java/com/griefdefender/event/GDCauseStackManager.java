/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.event;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.event.EventCause;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.internal.util.NMSUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public final class GDCauseStackManager implements Listener {

    private int tick_stored;

    private static GDCauseStackManager instance;

    private final CopyOnWriteArrayList<Object> cause = new CopyOnWriteArrayList<>();

    @Nullable private EventCause cached_cause;

    public EventCause getCurrentCause() {
        /* TODO: broken
        if (NMSUtil.getInstance().getRunningServerTicks() != tick_stored) {
            clearCause();
        }
        */
        if (this.cached_cause == null) {
            if (this.cause.isEmpty()) {
                this.cached_cause = EventCause.of(GriefDefenderPlugin.getInstance());
            } else {
                this.cached_cause = EventCause.of(this.cause);
            }
        }
        return this.cached_cause;
    }

    public GDCauseStackManager pushCause(Object obj) {
        checkNotNull(obj, "obj");
        if (obj instanceof OfflinePlayer) {
            obj = PermissionHolderCache.getInstance().getOrCreateUser((OfflinePlayer) obj);
        }
        /*if (tick_stored == NMSUtil.getInstance().getRunningServerTicks()) {
            this.cause.push(obj);
            return this;
        }*/

        tick_stored = NMSUtil.getInstance().getRunningServerTicks();
        this.cached_cause = null;
        this.cause.add(0, obj);
        return this;
    }

    public CauseStack doPushCause(Object cause) {
        pushCause(cause);
        return CauseStack.INSTANCE;
    }

    public <T> T withCause(Object cause, Supplier<T> supplier) {
        try (CauseStack unused = doPushCause(cause)) {
            return supplier.get();
        }
    }

    public void withCause(Object cause, Runnable runnable) {
        try (CauseStack unused = doPushCause(cause)) {
            runnable.run();
        }
    }

    public Object popCause() {
        this.cached_cause = null;
        if (this.cause.isEmpty()) {
            return null;
        }
        return this.cause.remove(0);
    }

    public Object peekCause() {
        return this.cause.get(0);
    }

    private void clearCause() {
        cached_cause = null;
        cause.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTick(ServerTickStartEvent event) {
        clearCause();
    }

    public static GDCauseStackManager getInstance() {
        return instance;
    }

    static {
        instance = new GDCauseStackManager();
    }

    public static class CauseStack implements AutoCloseable {
        private static final CauseStack INSTANCE = new CauseStack();

        private CauseStack() {}

        @Override
        public void close() {
            instance.popCause();
        }
    }
}
