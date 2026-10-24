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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

public final class GeoIPDatabase {
    private static Path dbFile;
    @Getter
    private static DatabaseReader reader = null;
    public static void load(Plugin plugin) {
        // Load the GeoIP database
        dbFile = plugin.getDataPath().resolve("GeoLite2-Country.mmdb");
        val uri = plugin.getConfig().getString("download-url", "");
        val username = plugin.getConfig().getString("license-username", "");
        val password = plugin.getConfig().getString("license-key", "");
        val encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        if(!isDatabaseDownloadable(uri, encodedCredentials)) {
            plugin.getLogger().warning("GeoIP database is not downloadable. Please check your license key in config.yml.");
            return;
        }
        if(!Files.exists(dbFile)) {
            downloadDatabase(plugin, uri, encodedCredentials);
        }
        val diff = Instant.now().toEpochMilli() - dbFile.toFile().lastModified();
        if(diff > 30L * 24 * 60 * 60 * 1000) {
            downloadDatabase(plugin, uri, encodedCredentials);
        }
        try {
            if(reader != null) reader.close();
            reader = new DatabaseReader.Builder(dbFile.toFile()).build();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load database: " + e.getMessage());
        }
    }
    private static void downloadDatabase(Plugin plugin, String uri, String encodedCredentials) {
        val httpreq = HttpRequest.newBuilder(URI.create(uri))
                .GET()
                .header("Authorization", "Basic " + encodedCredentials)
                .build();
        try(val client = HttpClient.newHttpClient();
            val in = new TarInputStream(new GZIPInputStream(
                    client.send(httpreq, HttpResponse.BodyHandlers.ofInputStream()).body()
            ))) {
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
        } catch (IOException | InterruptedException e) {
            plugin.getLogger().warning("Failed to download database");
            plugin.getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }
    private static boolean isDatabaseDownloadable(String url, String encodedCredentials) {
        val httpreq = HttpRequest.newBuilder(URI.create(url))
                .HEAD()
                .header("Authorization", "Basic " + encodedCredentials)
                .build();
        try (val client = HttpClient.newHttpClient()) {
            val res = client.send(httpreq, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    public static Country getCountry(InetAddress ip) throws IOException, GeoIp2Exception {
        return reader.country(ip).getCountry();
    }

    private GeoIPDatabase() {}
}
