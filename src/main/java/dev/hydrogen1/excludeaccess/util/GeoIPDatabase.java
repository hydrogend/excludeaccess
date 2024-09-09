package dev.hydrogen1.excludeaccess.util;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.record.Country;
import lombok.Getter;
import lombok.val;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

public final class GeoIPDatabase {
    private static Path dbFile;
    @Getter
    private static DatabaseReader reader = null;
    public static void load(Plugin plugin) {
        // Load the GeoIP database
        dbFile = plugin.getDataPath().resolve("GeoLite2-Country.mmdb");
        if(Files.exists(dbFile)) {
            downloadDatabase(plugin);
        }
        val diff = Instant.now().toEpochMilli() - dbFile.toFile().lastModified();
        if(diff > 30L * 24 * 60 * 60 * 1000) {
            downloadDatabase(plugin);
        }
        try {
            if(reader != null) reader.close();
            reader = new DatabaseReader.Builder(dbFile.toFile()).build();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load database: " + e.getMessage());
        }
    }
    private static void downloadDatabase(Plugin plugin) {
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
                // Find the database file
                if(!entry.isDirectory() && entry.getName().endsWith(".mmdb")) {
                    try(val out = Files.newOutputStream(dbFile)) {
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
            plugin.getLogger().warning("Failed to download database");
            plugin.getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }
    public static Country getCountry(InetAddress ip) throws IOException, GeoIp2Exception {
        return reader.country(ip).getCountry();
    }

    private GeoIPDatabase() {}
}
