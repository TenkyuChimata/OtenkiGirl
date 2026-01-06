package jp.wolfx.otenkigirl;

import java.util.Objects;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class OtenkiGirl extends JavaPlugin {
    private Economy econ = null;
    private double price;
    private String pMsg;
    private String bMsg;
    private String moneyMsg;
    private String noStorm;
    private final boolean folia = isFolia();

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
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
            if (!folia) {
                player.getWorld().setStorm(false);
            } else {
                Bukkit.getGlobalRegionScheduler().run(this, _ -> player.getWorld().setStorm(false));
            }
            this.getServer().broadcast(LegacyComponentSerializer.legacySection().deserialize(String.format(bMsg, player.getName())));
            player.sendMessage(pMsg);
            if (!folia) {
                this.getServer().getScheduler().runTaskLater(this, () -> {
                    if (player.isValid()) {
                        player.setHealth(player.getHealth() * 0.5);
                        player.addPotionEffect(PotionEffectType.SLOWNESS.createEffect(120 * 20, 2));
                        player.addPotionEffect(PotionEffectType.LEVITATION.createEffect(10 * 20, 0));
                    }
                }, 100L);
            } else {
                player.getScheduler().runDelayed(this, _ -> {
                    if (!player.isValid()) return;
                    player.setHealth(player.getHealth() * 0.5);
                    player.addPotionEffect(PotionEffectType.SLOWNESS.createEffect(120 * 20, 2));
                    player.addPotionEffect(PotionEffectType.LEVITATION.createEffect(10 * 20, 0));
                }, null, 100L);
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
