import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class MyDisconnectEvent extends ListenerAdapter {

    AudioPlayerManager playerManager;
    TrackScheduler scheduler;

    public MyDisconnectEvent(AudioPlayerManager playerManager, TrackScheduler scheduler) {

        this.playerManager = playerManager;
        this.scheduler = scheduler;
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {

        List<String> reactions = new LinkedList<>();
        String directory = "C:/Users/densh/botContent/reactions/";
        File lib = new File(directory + "disconnect.txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(lib));
            String link;
            while ((link = reader.readLine()) != null) {
                reactions.add(link);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String reaction = reactions.get((int) (Math.random() * reactions.size()));
        playerManager.loadItem(reaction, scheduler);
    }
}