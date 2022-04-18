package dev.dansplugin;

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

import java.util.HashMap;
import java.util.UUID;

public class GunType extends BukkitRunnable implements Listener {
    String gunName;
    
    double projectileYBias;
    double velocity;

    double baseAccruacy;
    double heatAccruacyEffect;
    double heatAdd;
    double heatDecay;
    double heatMax;

    Sound shootSound;
    float shootSoundVolume;
    float shootSoundPitch;

    HashMap <UUID, Double> heats;
    BukkitTask decayTask;

    public GunType(
            Plugin plugin,
            String name,
            double projectileYBias,
            double velocity,
            double baseAccruacy,
            double heatAccruacyEffect,
            double heatAdd,
            double heatDecay,
            double heatMax,
            Sound shootSound,
            float shootSoundVolume,
            float shootSoundPitch)
    {
        this.gunName = name;
        this.heats = new HashMap<UUID, Double>();

        this.projectileYBias = projectileYBias;
        this.velocity = velocity;
        this.baseAccruacy = baseAccruacy;
        this.heatAccruacyEffect = heatAccruacyEffect;
        this.heatAdd = heatAdd;
        this.heatDecay = heatDecay;
        this.heatMax = heatMax;
        
        this.shootSound = shootSound;
        this.shootSoundVolume = shootSoundVolume;
        this.shootSoundPitch = shootSoundPitch;

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
        for(UUID p : heats.keySet()) {
            Double heat = heats.get(p);
            if (heat > heatDecay) {
                heat -= heatDecay;
            } else {
                heat = 0d;
            }
            heats.put(p, heat);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event)
    {
        ItemStack item = event.getItem();
        String itemName = item.getItemMeta().getDisplayName();
        if(itemName.equals(gunName)) {
            shoot(event.getPlayer());
        }
    }
    private void shoot(Player player)
    {
        if (shootSound != null) {
            player.getWorld().playSound(player.getLocation(), shootSound, shootSoundVolume, shootSoundPitch);
        }

        Double heat = getHeat(player);
        Vector velocity = getProjectileVelocity(player);

        player.launchProjectile(Arrow.class, velocity);

        heat += heatAdd;
        if (heat > heatMax) {
            heat = heatMax;
        }
        heats.put(player.getUniqueId(), heat);

    }

    private Double getHeat(Player player)
    {
        Double heat = heats.get(player.getUniqueId());
        if(heat == null) {
            heats.put(player.getUniqueId(), Double.valueOf(0));
            return heats.get(player.getUniqueId());
        } else{
            return heat;
        }
    }

    private Vector getProjectileVelocity(Player player)
    {
        Vector perfectDirection = player.getEyeLocation().getDirection().clone();

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
        return baseAccruacy + getHeat(player) * heatAccruacyEffect;
    }

    private double getPlayerVelocity(Player player) {
        return velocity;
    }
}
