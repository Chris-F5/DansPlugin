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

import java.util.Set;
import java.util.HashSet;
import java.lang.IllegalArgumentException;

public class DansPlugin extends JavaPlugin {
    Set<GunType> guns;

    @Override
    public void onEnable()
    {
        guns = new HashSet<GunType>();
        saveDefaultConfig();
        reloadGunConfig();
        getCommand("rlcf").setExecutor(this);
    }

    private void reloadGunConfig()
    {
        reloadConfig();
        for(GunType gun : guns) {
            gun.unregister(this);
        }
        guns.clear();

        FileConfiguration conf = this.getConfig();
        Set<String> gunKeys = conf.getConfigurationSection("guns").getKeys(false);
        for(String gunKey : gunKeys) {
            String gunPath = "guns." + gunKey;
            guns.add(new GunType(
                    this,
                    conf.getString(gunPath + ".name"),
                    conf.getBoolean(gunPath + ".fullAuto"),
                    conf.getInt(gunPath + ".fullAutoCharge"),
                    conf.getInt(gunPath + ".shootInterval"),
                    conf.getInt(gunPath + ".projectileCount"),
                    conf.getDouble(gunPath + ".projYBias"),
                    conf.getDouble(gunPath + ".velocity"),
                    conf.getDouble(gunPath + ".damage"),
                    conf.getInt(gunPath + ".knockback"),
                    conf.getInt(gunPath + ".pierce"),
                    conf.getBoolean(gunPath + ".gravity"),
                    conf.getDouble(gunPath + ".accBase"),
                    conf.getDouble(gunPath + ".accHeatEffect"),
                    conf.getDouble(gunPath + ".heatAdd"),
                    conf.getDouble(gunPath + ".heatDec"),
                    conf.getDouble(gunPath + ".heatMax"),
                    Sound.ENTITY_ARROW_SHOOT,
                    conf.getDouble(gunPath + ".soundVol"),
                    conf.getDouble(gunPath + ".soundPitch")
                    ));
        }
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
