package dev.dansplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class DansPlugin extends JavaPlugin {
    List<GunType> guns;
    Objective cashObjective;

    String purchaseMessage;
    String afterPurchaseCashMessage;
    String cantAffordMessage;
    String gunNotExistMessage;
    String gunListBeforeMessage;
    String gunListFormat;
    String gunListAfterMessage;
    String printCashMessage;

    @Override
    public void onEnable()
    {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        cashObjective = scoreboard.getObjective("cash");
        if (cashObjective == null) {
            cashObjective = scoreboard.registerNewObjective("cash", "dummy", "cash");
        }

        guns = new ArrayList<GunType>();
        saveDefaultConfig();
        reloadDansPluginConfig();
        getCommand("rlcf").setExecutor(this);
        getCommand("rlcf").setTabCompleter(this);
        getCommand("buy").setExecutor(this);
        getCommand("buy").setTabCompleter(this);
        getCommand("cash").setExecutor(this);
        getCommand("cash").setTabCompleter(this);
    }

    private void reloadDansPluginConfig()
    {
        reloadConfig();
        FileConfiguration conf = this.getConfig();

        // MESSAGES
        purchaseMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.purchase"));
        afterPurchaseCashMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.after-purchase-cash"));
        cantAffordMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.cant-afford"));
        gunNotExistMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.gun-not-exist"));
        gunListBeforeMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.gun-list-start"));
        gunListFormat = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.gun-list-format"));
        gunListAfterMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.gun-list-end"));
        printCashMessage = ChatColor.translateAlternateColorCodes(
                '&', conf.getString("messages.cash"));

        // GUNS
        for(GunType gun : guns) {
            gun.unregister(this);
        }
        guns.clear();

        Set<String> gunKeys = conf.getConfigurationSection("guns").getKeys(false);
        for(String gunKey : gunKeys) {
            System.out.println("add gun: " + gunKey);
            guns.add(new GunType(this, conf, gunKey));
        }
    }

    private int getPlayerCash(Player p) {
        Score score = cashObjective.getScore(p.getName());
        return score.getScore();
    }
    private void setPlayerCash(Player p, int cash) {
        Score score = cashObjective.getScore(p.getName());
        score.setScore(cash);
    }

    private boolean tryPurchaceGun(Player p, String gunName) {
        for(GunType g : guns) {
            for(String a: g.aliases) {
                if (gunName.equalsIgnoreCase(a)) {
                    int cash = getPlayerCash(p);
                    if (cash >= g.cost) {
                        setPlayerCash(p, cash - g.cost);
                        p.sendMessage(String.format(purchaseMessage, g.gunName, g.cost));
                        g.giveToPlayer(p);
                        return true;
                    } else {
                        p.sendMessage(String.format(cantAffordMessage, g.gunName, cash, g.cost));
                        return false;
                    }
                }
            }
        }
        p.sendMessage(String.format(gunNotExistMessage, gunName));
        return false;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String label,
            String[] args)
    {
        if (command.getName().equalsIgnoreCase("rlcf")) {
            return new ArrayList<String>();
        } else if(command.getName().equalsIgnoreCase("buy")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<String>();
                list.add("list");
                for(GunType g : guns) {
                    for(String a : g.aliases) {
                        list.add(a);
                    }
                }
                return list;
            }
        } else if(command.getName().equalsIgnoreCase("cash")) {
            return new ArrayList<String>();
        }
        return null;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args)
    {
        if (command.getName().equalsIgnoreCase("rlcf")) {
            reloadDansPluginConfig();
            if(sender instanceof Player) {
                ((Player)sender).sendMessage("reloading config");
            }
        } else if(command.getName().equalsIgnoreCase("buy")) {
            if(sender instanceof Player) {
                Player p = (Player)sender;
                if(args.length == 0 || (args.length == 1
                            && args[0].equalsIgnoreCase("list"))) {
                    int cash = getPlayerCash(p);
                    p.sendMessage(String.format(gunListBeforeMessage, cash));
                    for(GunType g : guns) {
                        p.sendMessage(String.format(gunListFormat, g.cost, g.gunName, g.description));
                    }
                    p.sendMessage(String.format(gunListAfterMessage, cash));
                } else {
                    boolean purchased = false;
                    for (String g : args) {
                        if(tryPurchaceGun(p, g)) {
                            purchased = true;
                        }
                    }
                    if(purchased) {
                        p.sendMessage(String.format(afterPurchaseCashMessage, getPlayerCash(p)));
                    }
                }
            }
        } else if(command.getName().equalsIgnoreCase("cash")) {
            if(sender instanceof Player) {
                Player p = (Player)sender;
                p.sendMessage(String.format(printCashMessage, getPlayerCash(p)));
            }
        }
        return true;
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity d = (Entity) e.getDamager();
        if (d instanceof Arrow) {
            Entity hit = (Entity) e.getEntity();
            if (hit instanceof LivingEntity) {
                ((LivingEntity) hit).setMaximumNoDamageTicks(0);
                ((LivingEntity) hit).setNoDamageTicks(0);
            }
        }
    }
}
