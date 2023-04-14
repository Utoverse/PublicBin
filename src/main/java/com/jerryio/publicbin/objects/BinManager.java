package com.jerryio.publicbin.objects;

import com.jerryio.publicbin.PublicBinPlugin;
import com.jerryio.publicbin.disk.PluginSetting;
import com.jerryio.publicbin.enums.ModeEnum;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class BinManager {

    private BukkitTask scheduledTask;
    private List<Strategy> allClearStractegies;
    private CollectDespawnStrategy cdStrategy;

    public static BinManager load(PublicBinPlugin plugin) {
        BinManager rtn = plugin.setting.getMode() == ModeEnum.ShareMode ? new PublicBinManager() : new PrivateBinManager();

        for (Player p : Bukkit.getServer().getOnlinePlayers())
            rtn.onPlayerJoin(p);

        return rtn;
    }

    public BinManager() {
        PublicBinPlugin plugin = PublicBinPlugin.getInstance();
        PluginSetting setting = PublicBinPlugin.getPluginSetting();

        allClearStractegies = new ArrayList<>();

        if (setting.isAutoDespawnEnabled())
            allClearStractegies.add(new CountdownDespawnStrategy(this));

        if (setting.isClearIntervalsEnabled())
            allClearStractegies.add(new ClearIntervalsStrategy(this));

        if (this instanceof PublicBinManager && setting.isCollectDespawnEnabled())
            allClearStractegies.add((cdStrategy = new CollectDespawnStrategy(this)));

        scheduledTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> doTickCheck(), 20, 20);

    }

    public void close() {
        for (Bin bin : getAllBin()) {
            bin.close();
        }

        if (scheduledTask != null)
            Bukkit.getScheduler().cancelTask(scheduledTask.getTaskId());

        scheduledTask = null;
    }

    private void doTickCheck() {
        for (Strategy s : allClearStractegies)
            s.tickCheck();
    }

    public void trackDroppedItem(Item item) {
        if (cdStrategy != null) {
            cdStrategy.track(item);
        }
    }

    public void untrackDroppedItem(Item item) {
        if (cdStrategy != null) cdStrategy.untrack(item);
    }

    public abstract Bin getUsableBin(Player p);

    public abstract Collection<Bin> getAllBin();

    public abstract void onPlayerJoin(Player p);

    public abstract void onPlayerQuit(Player p);

}
