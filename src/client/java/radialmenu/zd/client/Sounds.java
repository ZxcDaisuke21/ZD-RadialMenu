package radialmenu.zd.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public final class Sounds {
    private Sounds() {
    }

    // лёгкий щелчок при наведении на сектор
    public static void hover() {
        if (!SectorConfig.soundsEnabled) return;
        play(SoundEvents.UI_BUTTON_CLICK, 0.15f, 1.8f);
    }

    // подтверждающий клик при выборе сектора
    public static void select() {
        if (!SectorConfig.soundsEnabled) return;
        play(SoundEvents.UI_BUTTON_CLICK, 0.8f, 1.0f);
    }

    // открытие подменю — чуть ниже тоном
    public static void openSubmenu() {
        if (!SectorConfig.soundsEnabled) return;
        play(SoundEvents.UI_BUTTON_CLICK, 0.8f, 0.9f);
    }

    // закрытие подменю — ещё ниже
    public static void closeSubmenu() {
        if (!SectorConfig.soundsEnabled) return;
        play(SoundEvents.UI_BUTTON_CLICK, 0.8f, 0.7f);
    }

    // в новых маппингах метод master() переименован в ui() — категория звука там UI
    private static void play(RegistryEntry.Reference<SoundEvent> entry, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.ui(entry.value(), pitch, volume));
    }
}
