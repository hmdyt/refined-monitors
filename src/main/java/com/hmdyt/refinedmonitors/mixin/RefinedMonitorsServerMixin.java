package com.hmdyt.refinedmonitors.mixin;

import com.hmdyt.refinedmonitors.RefinedMonitorsMod;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.TickTask;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Example Mixin to demonstrate that Mixins should be written in Java.
// It is _technically_ possible to write them in Kotlin but only if you understand
// how the Kotlin compiler works internally and what bytecode it produces and are fully
// aware of that at all times. The general advice is: JUST USE JAVA FOR MIXINS

// Marked as abstract so all the extends and implements clauses don't need to be "followed".
// Those clauses are added to get easy access to things without needing to @Shadow them.
@Mixin(MinecraftServer.class)
public abstract class RefinedMonitorsServerMixin extends ReentrantBlockableEventLoop<TickTask> implements ServerInfo, ChunkIOErrorReporter, CommandSource, AutoCloseable {
    // Constructor of a Mixin gets ignored
    public RefinedMonitorsServerMixin(String pName) {
        super(pName);
    }

    @Inject(method = "loadLevel", at = @At("TAIL"))
    public void refinedmonitors$loadLevel(CallbackInfo ci) {
        System.out.println("RefinedMonitors Mixin ran from server startup (modid: " + RefinedMonitorsMod.MODID + ")");
    }
}
