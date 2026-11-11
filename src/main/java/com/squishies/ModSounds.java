package com.squishies;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent NEEDOH_PRESS = reg("needoh_press");
    public static final SoundEvent NEEDOH_RELEASE = reg("needoh_release");
    public static final SoundEvent BUTTER_PRESS = reg("butter_press");
    public static final SoundEvent BUTTER_RELEASE = reg("butter_release");
    public static final SoundEvent CRUNCHY_PRESS = reg("crunchy_press");
    public static final SoundEvent CRUNCHY_RELEASE = reg("crunchy_release");
    public static final SoundEvent DUMPLING_SQUISH = reg("dumpling_squish");

    private static SoundEvent reg(String name) {
        Identifier id = new Identifier(SquishiesMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {}
}
