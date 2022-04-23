package dev.dansplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.bukkit.entity.Arrow;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.HandlerList;
import org.bukkit.Sound;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Material;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.List;

public class GunType extends BukkitRunnable implements Listener {
    String gunName;
    String itemDisplayName;
    String description;
    List<String> aliases;
    Material gunMaterial;
    int cost;

    boolean fullauto;
    int fullautoChargeAmount;
    double projectileYBias;
    double velocity;

    double damage;
    int knockback;
    int pierce;
    boolean gravity;

    int shootInterval;
    int projectileCount;

    double baseAccruacy;
    double sneekAccruacy;
    double movementAccruacy;
    double heatAccruacyEffect;
    double heatAdd;
    double heatDecay;
    double heatMax;

    Sound shootSound;
    float shootSoundVolume;
    float shootSoundPitch;

    HashMap <Player, Double> heats;
    HashMap <Player, Integer> cooldowns;
    HashMap <Player, Integer> fullautoCharge;

    BukkitTask decayTask;

    Objective heatObjective;

    public GunType(
            Plugin plugin,
            FileConfiguration conf,
            String gunKey)
    {
        String p = "guns." + gunKey;
        this.gunName = conf.getString(p + ".name");
        this.itemDisplayName = ChatColor.translateAlternateColorCodes(
                '&', conf.getString(p + ".item-display-name"));
        this.description = conf.getString(p + ".description");
        this.aliases = conf.getStringList(p + ".aliases");
        this.gunMaterial = Material.valueOf(conf.getString(p + ".item-material"));
        this.cost = conf.getInt(p + ".cost");

        this.fullauto = conf.getBoolean(p + ".full-auto");
        this.fullautoChargeAmount = conf.getInt(p + ".full-auto-charge");
        this.shootInterval = conf.getInt(p + ".shoot-tick-interval");
        this.projectileCount = conf.getInt(p + ".projectile-count");

        this.projectileYBias = conf.getDouble(p + ".projectile-y-bias");
        this.velocity = conf.getDouble(p + ".velocity");
        this.damage = conf.getDouble(p + ".damage") / velocity;
        this.knockback = conf.getInt(p + ".knockback");
        this.pierce = conf.getInt(p + ".pierce");
        this.gravity = conf.getBoolean(p + ".gravity");

        this.baseAccruacy = conf.getDouble(p + ".base-accruacy");
        this.sneekAccruacy = conf.getDouble(p + ".sneek-accruacy-modifier");
        this.heatAccruacyEffect = conf.getDouble(p + ".accruacy-heat-effect");
        this.heatAdd = conf.getDouble(p + ".heat-add");
        this.heatDecay = conf.getDouble(p + ".heat-sub");
        this.heatMax = conf.getDouble(p + ".heat-max");

        this.shootSound = Sound.valueOf(conf.getString(p + ".sound"));
        this.shootSoundVolume = (float)conf.getDouble(p + ".sound-volume");
        this.shootSoundPitch = (float)conf.getDouble(p + ".sound-pitch");

        this.heats = new HashMap<Player, Double>();
        this.cooldowns = new HashMap<Player, Integer>();
        this.fullautoCharge = new HashMap<Player, Integer>();

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        heatObjective = scoreboard.getObjective("heat");
        if (heatObjective == null) {
            heatObjective = scoreboard.registerNewObjective("heat", "dummy", "heat");
        }

        decayTask = this.runTaskTimer(plugin, 1, 1);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void giveToPlayer(Player p) {
        ItemStack item = new ItemStack(gunMaterial);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(itemDisplayName);
        item.setItemMeta(meta);

        p.getInventory().addItem(item);
    }

    public void unregister(Plugin plugin) {
        HandlerList.unregisterAll(this);
        decayTask.cancel();
    }

    @Override
    public void run()
    {
        for(Player p : cooldowns.keySet()) {
            if(!p.isOnline()) {
                cooldowns.remove(p);
                continue;
            }
            Integer cooldown = cooldowns.get(p);
            if (cooldown > 0) {
                cooldowns.put(p, cooldown - 1);
            }
        }

        if (fullauto) {
            for(Player p : fullautoCharge.keySet()) {
                if(!p.isOnline()) {
                    fullautoCharge.remove(p);
                    continue;
                }
                Integer charge = fullautoCharge.get(p);
                if(charge > 0) {
                    if(cooldowns.getOrDefault(p, 0) == 0) {
                        shoot(p);
                    }
                    fullautoCharge.put(p, charge - 1);
                }
            }
        }

        for(Player p : heats.keySet()) {
            if(!p.isOnline()) {
                heats.remove(p);
                continue;
            }

            Double heat = heats.get(p);
            if (heat > heatDecay) {
                heat -= heatDecay;
            } else {
                heat = 0d;
            }
            heats.put(p, heat);

            ItemStack item = p.getInventory().getItemInMainHand();
            if(item != null) {
                ItemMeta meta = item.getItemMeta();
                if(meta != null) {
                    String displayName = ChatColor.stripColor(meta.getDisplayName());
                    String strippedItemName = ChatColor.stripColor(itemDisplayName);
                    if(displayName != null) {
                        if (displayName.equals(strippedItemName)) {
                            heatObjective.getScore(p.getName()).setScore((int)(100 * heat / heatMax));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event)
    {
        ItemStack item = event.getItem();
        if (item != null && event.getHand() == EquipmentSlot.HAND) {
            Player player = event.getPlayer();

            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            String strippedItemName = ChatColor.stripColor(itemDisplayName);
            if(displayName.equals(strippedItemName)) {
                if(fullauto) {
                    fullautoCharge.put(player, fullautoChargeAmount);
                }

                if(cooldowns.getOrDefault(player, 0) == 0) {
                    shoot(player);
                }
            }
        }
    }
    private void shoot(Player player)
    {

        if (shootSound != null) {
            player.getWorld().playSound(player.getLocation(), shootSound, shootSoundVolume, shootSoundPitch);
        }

        Double heat = heats.getOrDefault(player, 0d);

        for (int i = 0; i < projectileCount; i++) {
            Vector velocity = getProjectileVelocity(player);
            Arrow arrow = player.launchProjectile(Arrow.class, velocity);
            arrow.setDamage(damage);
            arrow.setGravity(gravity);
            arrow.setKnockbackStrength(knockback);
            arrow.setPierceLevel(pierce);
            arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
        }

        heat += heatAdd;
        if (heat > heatMax) {
            heat = heatMax;
        }
        heats.put(player, heat);

        int currentCooldown = cooldowns.getOrDefault(player, 0);
        cooldowns.put(player, currentCooldown + shootInterval);

    }

    private Vector getProjectileVelocity(Player player)
    {
        Vector perfectDirection = player.getEyeLocation().getDirection().clone().normalize().multiply(100);

        double accruacy = getPlayerAccruacy(player);
        double velocity = getPlayerVelocity(player);

        Vector horozontalVector
            = perfectDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector verticalVector
            = perfectDirection.clone().crossProduct(horozontalVector).normalize();

        Vector velocityVector = perfectDirection.clone()
            .add(horozontalVector.multiply((Math.random() - 0.5) * accruacy))
            .add(verticalVector.multiply((Math.random() - 0.5) * accruacy - projectileYBias))
            .normalize()
            .multiply(velocity);

        return velocityVector;
    }

    private double getPlayerAccruacy(Player player) {
        Vector playerVelocity = player.getVelocity().clone();
        playerVelocity.setY(0);
        double acc
            = baseAccruacy
            + heats.getOrDefault(player, 0d) * heatAccruacyEffect
            + playerVelocity.length() * movementAccruacy;
        if(player.isSneaking()) {
            acc += sneekAccruacy;
        }
        return Double.max(0, acc);
    }

    private double getPlayerVelocity(Player player) {
        return velocity;
    }
}
