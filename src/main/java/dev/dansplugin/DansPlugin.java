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

import java.lang.IllegalArgumentException;

public class DansPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileFire(ProjectileLaunchEvent event)
    {
        ProjectileSource source = event.getEntity().getShooter();
        if(source instanceof Player) {
            Player player = (Player) source;

            Entity projectile = event.getEntity();
            Vector perfectDirection = player.getEyeLocation().getDirection().clone().normalize();

            double accruacy = getPlayerAccruacy(player);
            double velocityMultiplier = getPlayerVelocity(player);

            updateProjectileVelocity(projectile, perfectDirection, accruacy, velocityMultiplier);
        }
    }

    private double getPlayerAccruacy(Player player) {
        try {
            return getPlayerScore("dansplugin_acc", player) / 100.0;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private double getPlayerVelocity(Player player) {
        try {
            return getPlayerScore("dansplugin_vel", player) / 100.0;
        } catch (IllegalArgumentException e) {
            return 1.0;
        }
    }

    private int getPlayerScore(String objectiveName, Player player) {
        String playerName = player.getName();

        ScoreboardManager scoreManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreManager.getMainScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            throw new IllegalArgumentException();
        }
        int score = objective.getScore(playerName).getScore();
        return score;
    }

    private void updateProjectileVelocity(
            Entity projectile,
            Vector perfectDirection,
            double accruacy,
            double velocityMultiplier)
    {
        double projectileVelocity = projectile.getVelocity().length();

        Vector horozontalVector
            = perfectDirection.clone().crossProduct(new Vector(0, 1, 0));
        Vector verticalVector
            = perfectDirection.clone().crossProduct(horozontalVector);

        Vector newDirection = perfectDirection.clone()
            .add(horozontalVector.multiply((Math.random() - 0.5) * accruacy))
            .add(verticalVector.multiply((Math.random() - 0.5) * accruacy))
            .normalize();

        Vector newVelocity = newDirection.clone().multiply(projectileVelocity * velocityMultiplier);
        projectile.setVelocity(newVelocity);
    }
}
