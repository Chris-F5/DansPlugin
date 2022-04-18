package dev.dansplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Objective;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.Sound;

import java.lang.IllegalArgumentException;

public class DansPlugin extends JavaPlugin {

    GunType glock;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        reloadGunConfig();
        getCommand("rlcf").setExecutor(this);
    }

    private void reloadGunConfig()
    {
        reloadConfig();
        if (glock != null) {
            glock.unregister(this);
        }
        FileConfiguration conf = this.getConfig();
        String gunPath = "guns.glock";
        glock = new GunType(
            this,
            conf.getString(gunPath + ".name"),
            conf.getDouble(gunPath + ".projYBias"),
            conf.getDouble(gunPath + ".velocity"),
            conf.getDouble(gunPath + ".accBase"),
            conf.getDouble(gunPath + ".accHeatEffect"),
            conf.getDouble(gunPath + ".heatAdd"),
            conf.getDouble(gunPath + ".heatDec"),
            conf.getDouble(gunPath + ".heatMax"),
            Sound.ENTITY_ARROW_SHOOT,
            1.0f,
            1.0f);
                
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args)
    {
        System.out.println("reloading config");
        reloadGunConfig();
        return true;
    }


}
