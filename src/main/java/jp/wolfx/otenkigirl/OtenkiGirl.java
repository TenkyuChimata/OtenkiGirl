package jp.wolfx.otenkigirl;

import java.util.Objects;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class OtenkiGirl extends JavaPlugin {
    private static Economy econ = null;
    private static double price;
    private static String pMsg;
    private static String bMsg;
    private static String moneyMsg;
    private static String noStorm;

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static ScheduledTask scheduleSyncDelayedTask(Plugin plugin, Runnable task, long delay){
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (t) -> task.run(), delay != 0 ? delay : 1 );
    }

    @Override
    public void onEnable() {
        if (!this.setupEconomy()) {
            this.getLogger().severe("未检测到Vault, 插件启动失败!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            this.rel();
        }
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        } else if (this.getServer().getServicesManager().getRegistration(Economy.class) == null) {
            return false;
        } else {
            econ = (Economy) ((RegisteredServiceProvider<?>) Objects.requireNonNull(this.getServer().getServicesManager().getRegistration(Economy.class))).getProvider();
            return true;
        }
    }

    private void work(Player player) {
        if (!player.getWorld().hasStorm()) {
            player.sendMessage(noStorm);
        } else if (!econ.withdrawPlayer(player, price).transactionSuccess()) {
            player.sendMessage(String.format(moneyMsg, price));
        } else {
            if (!isFolia()) {
                player.getWorld().setStorm(false);
            } else {
                Bukkit.getGlobalRegionScheduler().run(this, task -> player.getWorld().setStorm(false));
            }
            this.getServer().broadcastMessage(String.format(bMsg, player.getDisplayName()));
            player.sendMessage(pMsg);
            if (!isFolia()) {
                this.getServer().getScheduler().runTaskLater(this, () -> {
                    if (player.isValid()) {
                        player.setHealth(player.getHealth() * 0.5);
                        player.addPotionEffect(PotionEffectType.SLOW.createEffect(2400, 0));
                        player.addPotionEffect(PotionEffectType.LEVITATION.createEffect(100, 0));
                    }
                }, 100L);
            } else {
                scheduleSyncDelayedTask(this, () -> {
                    if (player.isValid()) {
                        player.setHealth(player.getHealth() * 0.5);
                        player.addPotionEffect(PotionEffectType.SLOW.createEffect(2400, 0));
                        player.addPotionEffect(PotionEffectType.LEVITATION.createEffect(100, 0));
                    }
                }, 100L);
            }
        }
    }

    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.isOp()) {
            this.rel();
            sender.sendMessage("重载完成!");
            return true;
        } else {
            if (sender instanceof Player) {
                this.work((Player) sender);
            }
            return false;
        }
    }

    private void rel() {
        this.saveDefaultConfig();
        this.reloadConfig();
        price = this.getConfig().getDouble("price");
        pMsg = this.getConfig().getString("player-msg");
        bMsg = this.getConfig().getString("broadcast-msg");
        moneyMsg = this.getConfig().getString("more-money");
        noStorm = this.getConfig().getString("no-storm");
    }

    @Override
    public void onDisable() {
    }
}
