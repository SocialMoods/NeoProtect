package ru.SocialMoods;

import cn.nukkit.event.EventHandler;
import ru.SocialMoods.Storage.Areas;
import ru.SocialMoods.Storage.Stream;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeoProtect extends PluginBase {

    public HashMap<String, List<Areas>> map;
    public Stream stream;
    public Config config;

    @EventHandler
    public void onEnable() {

        getLogger().info("NeoProtect включен!");
        getLogger().info("Переработано SkyStudio");

        getDataFolder().mkdirs();
        config = new Config(
                new File(this.getDataFolder(), "config.yml"),
                Config.YAML,
                new LinkedHashMap<String, Object>() {
                    {
                        // Заменим ключи на строки
                        put("protection-blocks", new LinkedHashMap<String, Integer>() {
                            {
                                put("22", 10); // Пример: блок ID 22 с радиусом 10
                                put("57", 15); // Пример: блок ID 57 с радиусом 15
                            }
                        });
                        put("maximum-protections", 5);
                        put("messages", new LinkedHashMap<String, String>() {
                            {
                                put("block-use-denied", "Вы не можете использовать: %block_name% в этой области.");
                                put("block-break-own", "Вы сломали свой защитный блок!");
                                put("block-break-denied", "Вы не можете сломать этот защитный блок!");
                                put("max-protections-reached", "Вы уже установили слишком много защитных блоков! Удалите некоторые, чтобы установить новые.");
                                put("protection-overlap", "Ваша защита пересекается с другой защищенной областью, отойдите подальше.");
                                put("protection-placed", "Вы установили защитный блок!");
                                put("protections-remaining", "У вас осталось %remaining% защитных областей.");
                                put("protection-radius-created", "Радиус защиты %radius% блоков создан вокруг вашего защитного блока.");
                                put("area-protected-popup", "Область внутри стеклянного круга защищена.");
                                put("area-owned-popup", "Вы находитесь в защищенной области, принадлежащей: %owner%");
                            }
                        });
                    }
                });
        config.save();

        map = new HashMap<>();

        stream = new Stream(this);
        stream.init();

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getScheduler().scheduleRepeatingTask(new SaveTask(this), 2400, true);
    }

    @Override
    public void onDisable() {
        stream.save();
        getLogger().info("NeoProtect выключен!");
    }

    public int getProtectionRadius(int blockId) {
        Map<Integer, Integer> protectionBlocks = (Map<Integer, Integer>) config.get("protection-blocks");
        return protectionBlocks.getOrDefault(blockId, 0);
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "Сообщение не найдено: " + key);
    }
}