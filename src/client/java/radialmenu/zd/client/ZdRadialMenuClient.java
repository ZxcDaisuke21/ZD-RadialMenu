package radialmenu.zd.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Queue;

public class ZdRadialMenuClient implements ClientModInitializer {
    private static KeyBinding openMenu;
    private static final Queue<InputUtil.Key> pendingRelease = new ArrayDeque<>();

    public static KeyBinding openBinding() {
        return openMenu;
    }

    @Override
    public void onInitializeClient() {
        SectorConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(
                Identifier.of("zdradialmenu", "main"));

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zdradialmenu.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // отпускаем клавиши, «нажатые» из меню в прошлом тике
            while (!pendingRelease.isEmpty()) {
                KeyBinding.setKeyPressed(pendingRelease.poll(), false);
            }
            while (openMenu.wasPressed()) {
                if (client.currentScreen == null) {
                    RadialMenuScreen.open();
                }
            }
        });
    }

    // имитация нажатия игровой привязки: инвентарь, дроп и т. д.
    public static void pulseKey(KeyBinding binding) {
        if (binding == null) return;
        InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
        pendingRelease.add(key);
    }
}
