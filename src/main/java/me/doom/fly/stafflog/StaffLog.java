package me.doom.fly.stafflog;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StaffLog extends JavaPlugin implements Listener {

    public static HashMap<String, Integer> players = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPermission("staff.log")) { return; }
        players.put(event.getPlayer().getUniqueId().toString(), (int) (System.currentTimeMillis() / 1000L));
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        if (!event.getPlayer().hasPermission("staff.log")) { return; }
        sendWebHookMessage(event.getPlayer(), (int) (System.currentTimeMillis() / 1000L), players.get(event.getPlayer().getUniqueId().toString()));
    }

    public void sendWebHookMessage(Player player, int logout, int login)  {

        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = new FormBody.Builder()
                    .add("username", player.getName())
                    .add("content", "Time Joined: <t:" + login + ":F>\n" +
                            "Time Left: <t:" + logout + ":F>" +
                            "\nTime Played: " + convertSeconds("${d- Day%, -s}${H- Hour%, -s}${M- Minute%, and -s}${S- Second%-s}", logout - login))
                    .add("avatar_url", "https://crafthead.net/cube/" + player.getUniqueId().toString().toLowerCase(Locale.ROOT))
                    .build();
            Request request = new Request.Builder()
                    //
                    // CHANGE ME \/ \/ \/
                    // Make a discord webhook, grab the url and put it here.
                    .url("https://discord.com/api/webhooks/884189025857056798/ZF6Nxyknq4DZIa6Tz7vjaZXaYzodemhqOCg6IUTuZLg1pj28YBF8TRwRnroWXEOE4exC")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String convertSeconds(String format, int rawSeconds) {

        boolean usesMinutes = false;
        boolean usesHours = false;
        boolean usesDays = false;

        final Pattern pattern = Pattern.compile("\\$\\{([dDhHmMsS])-([a-zA-Z0-9%, ]+)(-(s))?}");
        Matcher matcher = pattern.matcher(format);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            switch (matcher.group(1).toLowerCase()) {
                case "d":
                    usesDays = true;
                    break;
                case "h":
                    usesHours = true;
                    break;
                case "m":
                    usesMinutes = true;
                    break;
            }
        }

        int rawMinutes = rawSeconds / 60;
        int rawHours = rawMinutes / 60;
        int rawDays = rawHours / 24;

        int seconds = (usesMinutes ? rawSeconds % 60 : (int)Math.floor(rawSeconds));
        int minutes = (usesHours ? rawMinutes % 60 : (int)Math.floor(rawMinutes));
        int hours = (usesDays ? rawHours % 24 : (int)Math.floor(rawHours));
        int days = (int)Math.floor(rawDays);

        matcher.reset();
        while (matcher.find()) {
            String unit = matcher.group(1);
            String suffix = matcher.group(2);
            String plural = (matcher.groupCount() == 4) ? matcher.group(4) : "";
            boolean doPlural = false;
            String replace = "";
            switch (unit) {
                case "d":
                    if (days == 0) break;
                case "D":
                    doPlural = days != 1;
                    replace = days + suffix;
                    break;
                case "h":
                    if (hours == 0 && days == 0) break;
                case "H":
                    doPlural = hours != 1;
                    replace = hours + suffix;
                    break;
                case "m":
                    if (minutes == 0 && hours == 0 && days == 0) break;
                case "M":
                    doPlural = minutes != 1;
                    replace = minutes + suffix;
                    break;
                case "s":
                case "S":
                    doPlural = seconds != 1;
                    replace = seconds + suffix;
                    break;
            }
            replace = replace.replaceAll("%", (doPlural ? plural : ""));
            matcher.appendReplacement(buffer, replace);
        }

        return matcher.appendTail(buffer).toString();

    }

}
