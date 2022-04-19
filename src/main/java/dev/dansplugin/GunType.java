package dev.dansplugin;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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

import java.util.HashMap;

public class GunType extends BukkitRunnable implements Listener {
    String gunName;

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
            String name,
            boolean fullauto,
            int fullautoChargeAmount,
            int shootInterval,
            int projectileCount,
            double projectileYBias,
            double velocity,
            double damage,
            int knockback,
            int pierce,
            boolean gravity,
            double baseAccruacy,
            double heatAccruacyEffect,
            double heatAdd,
            double heatDecay,
            double heatMax,
            Sound shootSound,
            double shootSoundVolume,
            double shootSoundPitch)
            {
                this.gunName = name;

                this.fullauto = fullauto;
                this.fullautoChargeAmount = fullautoChargeAmount;
                this.shootInterval = shootInterval;
                this.projectileCount = projectileCount;

                this.projectileYBias = projectileYBias;
                this.velocity = velocity;
                this.damage = damage;
                this.knockback = knockback;
                this.pierce = pierce;
                this.gravity = gravity;

                this.baseAccruacy = baseAccruacy;
                this.heatAccruacyEffect = heatAccruacyEffect;
                this.heatAdd = heatAdd;
                this.heatDecay = heatDecay;
                this.heatMax = heatMax;

                this.shootSound = shootSound;
                this.shootSoundVolume = (float)shootSoundVolume;
                this.shootSoundPitch = (float)shootSoundPitch;

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

    public void unregister(Plugin plugin) {
        HandlerList.unregisterAll(this);
        decayTask.cancel();
    }

    @Override
    public void run()
    {
        for(Player p : cooldowns.keySet()) {
            Integer cooldown = cooldowns.get(p);
            if (cooldown > 0) {
                cooldowns.put(p, cooldown - 1);
            }
        }

        if (fullauto) {
            for(Player p : fullautoCharge.keySet()) {
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
                    String displayName = meta.getDisplayName();
                    if(displayName != null) {
                        if (displayName.equals(gunName)) {
                            heatObjective.getScore(p.getName()).setScore((int)(100 * heat / heatMax));
                        }
                    }
                }
            }
        }
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

    @EventHandler
    public void onInteract(PlayerInteractEvent event)
    {
        ItemStack item = event.getItem();
        if (item != null && event.getHand() == EquipmentSlot.HAND) {
            String itemName = item.getItemMeta().getDisplayName();
            Player player = event.getPlayer();
            if(itemName.equals(gunName)) {
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
        return baseAccruacy + heats.getOrDefault(player, 0d) * heatAccruacyEffect;
    }

    private double getPlayerVelocity(Player player) {
        return velocity;
    }
}
