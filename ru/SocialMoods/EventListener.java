package ru.SocialMoods;

import ru.SocialMoods.Storage.Areas;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventListener implements Listener {

    private int radius;
    private int maximumprotections;
    private List<Integer> protectionBlocks;
    private NeoProtect plugin;

    public EventListener(NeoProtect plugin) {
        this.plugin = plugin;
        this.radius = plugin.config.getInt("protection-radius");
        this.maximumprotections = plugin.config.getInt("maximum-protections");
        this.protectionBlocks = plugin.config.getIntegerList("protection-blockids");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onExplode(EntityExplodeEvent event) {
        List<Block> list = event.getBlockList();

        for (Iterator<Block> i = list.iterator(); i.hasNext();) {
            Block block = i.next();
            if(blockInProtection(block.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (playerInRangeProtection(block.getLocation(), player)) {
            player.sendMessage(getMessage("block-use-denied").replace("%block_name%", block.getName()));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (protectionBlocks.contains(block.getId())) {
            if(playerRemoveProtection(block.getLocation(), player)) {
                player.sendMessage(getMessage("block-break-own"));
            } else {
                if (playerInRangeProtection(block.getLocation(), player)) {
                    event.setCancelled(true);
                }
            }
        } else if (playerInRangeProtection(block.getLocation(), player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (protectionBlocks.contains(block.getId())) {
            int numPlayerProtections = getNumPlayerProtections(player);
            if (numPlayerProtections >= maximumprotections) {
                player.sendMessage(getMessage("max-protections-reached"));
                event.setCancelled(true);
            } else {
                if (playerInRangeProtection(block.getLocation(), player)) {
                    event.setCancelled(true);
                } else {
                    List<Location> protectionarea = ProtectionArea(block.getLocation());
                    boolean isProtected = false;
                    for (Iterator<Location> i = protectionarea.iterator(); i.hasNext();) {
                        if(blockInProtection(i.next())) {
                            isProtected = true;
                            break;
                        }
                    }

                    if(isProtected) {
                        player.sendMessage(getMessage("protection-overlap"));
                        event.setCancelled(true);
                    } else {
                        int remaining = ((maximumprotections - numPlayerProtections)-1);
                        playerPlaceProtection(block.getLocation(), player);
                        player.sendMessage(getMessage("protection-placed"));
                        player.sendMessage(getMessage("protections-remaining").replace("%remaining%", String.valueOf(remaining)));
                        player.sendMessage(getMessage("protection-radius-created").replace("%radius%", String.valueOf(radius)));
                    }
                }
            }
        } else if (playerInRangeProtection(block.getLocation(), player)) {
            event.setCancelled(true);
        }
    }

    public int getNumPlayerProtections(Player player) {
        List list;
        try {
            list = (List) plugin.map.get(player.getName());
            return list.size();
        } catch (NullPointerException e) {
            plugin.map.put(player.getName(), new ArrayList<>());
            return 0;
        }
    }

    public void playerPlaceProtection(Location loc, Player player) {
        List list = (List) plugin.map.get(player.getName());
        list.add(new Areas(loc));
        plugin.map.put(player.getName(), list);
    }

    public boolean playerRemoveProtection(Location loc, Player player) {
        Set set = plugin.map.entrySet();
        Iterator i = set.iterator();

        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            List list = (List) me.getValue();
            for (int index = 0, d = list.size(); index < d; index++) {
                Areas area = (Areas) list.get(index);
                Location x = area.getLocation();
                if ((x.equals(loc)) && (me.getKey().equals(player.getName()))) {
                    list.remove(area);
                    plugin.map.put(player.getName(), list);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean playerInRangeProtection(Location loc, Player player) {
        Set set = plugin.map.entrySet();
        Iterator i = set.iterator();

        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            List list = (List) me.getValue();
            for (int index = 0, d = list.size(); index < d; index++) {
                Areas area = (Areas)list.get(index);
                Location x = area.getLocation();
                if (loc.distance(x) < radius) {
                    if(me.getKey().equals(player.getName())) {
                        return false;
                    } else {
                        player.sendPopup(getMessage("area-owned-popup").replace("%owner%", me.getKey().toString()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean blockInProtection(Location loc) {
        Set set = plugin.map.entrySet();
        Iterator i = set.iterator();

        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            List list = (List) me.getValue();
            for (int index = 0, d = list.size(); index < d; index++) {
                Areas area = (Areas)list.get(index);
                Location x = area.getLocation();
                if(loc.distance(x) < radius) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Location> ProtectionArea(Location loc) {
        List<Location> list = new ArrayList<>();

        double x;
        double y = loc.y;
        double z;

        for (double i = 0.0; i < 360.0; i += 0.1) {
            double angle = i * Math.PI / 180;
            x = (loc.getX() + radius * Math.cos(angle));
            z = (loc.getZ() + radius * Math.sin(angle));
            list.add(new Location(x,y,z));
        }

        return list;
    }

    private String getMessage(String key) {
        return plugin.config.getString("messages." + key, "Сообщение не найдено: " + key);
    }
}