package dev.hydrogen1.excludeaccess;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

public final class LoginHandler implements Listener {
    private final ExcludeAccess plugin;
    private DatabaseReader reader;
    private final File dbFile;
    private boolean checkOnly;
    private final List<IPAddressString> masks = new ArrayList<>();
    LoginHandler(ExcludeAccess plugin) {
        this.plugin = plugin;
        dbFile = new File(plugin.getDataFolder(), "GeoLite2-Country.mmdb");
        load();
    }

    /**
     * プレイヤーがログインしようとしたときに呼び出され、ログインの可否を確認する。
     * ただし、ホワイトリストが有効な場合は何もしない。
     * 加えて、ホワイトリストに登録されたプレイヤーの場合も何もしない。
     * @param event イベント
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if(Bukkit.isWhitelistEnforced()) return;
        if(Bukkit.getWhitelistedPlayers().stream()
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .anyMatch(player -> player.equals(event.getName()))) return;
        val addr = event.getAddress();
        if(checkIfLocal(addr)) return;
        if(checkOnly) {
            val ip = new IPAddressString(addr.getHostAddress());
            for(val m : masks) {
                if(m.contains(ip)) {
                    plugin.getLogger().info("Allowed Player " + event.getName() + " from " + addr.getHostAddress());
                    return;
                }
            }
            kick(event, "You are not allowed to join.");
            plugin.getLogger().info("Unknown Player " + event.getName() + " from " + addr.getHostAddress());
            plugin.discordLog(event.getName() + " (" + addr.getHostAddress().substring(0,8) + ") のログインを拒否しました。");
            return;
        }
        try {
            val response = reader.country(addr);
            val country = response.getCountry();
            if(!country.getIsoCode().equals("JP")) {
                kick(event, "You are not allowed to join.");
                plugin.discordLog(event.getName() + " (" + country.getIsoCode() + ") のログインを拒否しました。");
            }
            plugin.getLogger().info("Player " + event.getName() + " from " + country.getName());
        } catch (IOException | GeoIp2Exception e) {
            kick(event, "Failed to check your country.");
            plugin.getLogger().warning("Failed to check country: " + e.getMessage());
            plugin.getLogger().info("Player " + event.getName() + " from unknown country");
            plugin.discordLog(event.getName() + " (不明) のログインを拒否しました。");
        }
    }

    /**
     * データベースを読み込む。ただし、データベースが存在しない場合や前回のダウンロードから1月以上経過している場合はダウンロードする。
     * なお、config.ymlのcheck-only-allowed-ipsがtrueの場合は、IPアドレスが指定されたリストに含まれているかのみを確認する。
     */
    void load() {
        checkOnly = plugin.getConfig().getBoolean("check-only-allowed-ips",false);
        if(checkOnly) {
            val iplist = plugin.getConfig().getStringList("allowed-ips");
            val c = iplist.stream()
                    .filter(Predicate.not(String::isEmpty))
                    .map(IPAddressString::new)
                    .toList();
            masks.clear();
            masks.addAll(c);
            return;
        }
        if(!dbFile.exists()) {
            downloadDatabase();
        }
        val diff = System.currentTimeMillis() - dbFile.lastModified();
        if(diff / 30 / 24 / 60 / 60 / 1000 > 30) {
            downloadDatabase();
        }
        try {
            if(reader != null) reader.close();
            reader = new DatabaseReader.Builder(dbFile).build();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load database: " + e.getMessage());
        }
    }

    /**
     * データベースをダウンロードする。
     */
    private void downloadDatabase() {
        var url = plugin.getConfig().getString("download-url", "");
        if(url.isEmpty()) {
            plugin.getLogger().warning("No download URL specified in config.yml");
            return;
        }
        val key = plugin.getConfig().getString("license-key", "");
        if(key.isEmpty()) {
            plugin.getLogger().warning("No download key specified in config.yml");
            return;
        }
        url = url.replace("{KEY}", key);
        try {
            val conn = new URI(url).toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.connect();
            val in = new TarInputStream(new GZIPInputStream(conn.getInputStream()));
            TarEntry entry;
            while((entry = in.getNextEntry()) != null) {
                // データベースファイルを見つけたら保存する
                if(!entry.isDirectory() && entry.getName().endsWith(".mmdb")) {
                    try(val out = new FileOutputStream(dbFile)) {
                        byte[] buffer = new byte[2048];
                        int len;
                        while((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                    break;
                }
            }
            in.close();
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("Failed to download database: " + e.getMessage());
        }
    }

    /**
     * プレイヤーをキックする。
     * このメゾットが存在するのは、処理が長引いている場合にプレイヤーを確実にキックするためである。
     *
     * @param event イベント
     * @param reason 理由
     */
    private void kick(AsyncPlayerPreLoginEvent event, String reason) {
        val msg = Component.text(reason);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, msg);
        val player = Bukkit.getPlayer(event.getName());
        if(player != null) {
            player.kick(msg);
        }
    }

    /**
     * ローカルアドレスかどうかを確認する。
     * @param address アドレス
     * @return ローカルアドレスかどうか
     */
    private boolean checkIfLocal(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            return true;
        }
        // インターフェースに割り当てられたアドレスかどうかを念のため確認する
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}
