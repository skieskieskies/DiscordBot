import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class TrackScheduler implements AudioLoadResultHandler {

    private final AudioPlayer player;
    private MessageCreateEvent event;

    public TrackScheduler(final AudioPlayer player) {

        this.player = player;
    }

    public void setEvent(MessageCreateEvent event) {

        this.event = event;
    }

    @Override
    public void trackLoaded(final AudioTrack audioTrack) {

        player.playTrack(audioTrack);
        try {
            long duration = audioTrack.getDuration() / 1000;
            long min = duration / 60;
            long sec = duration - min * 60;
            String seconds = sec < 10 ? "0" + sec : "" + sec;
            MusicBot.print(event, "Playing: " + audioTrack.getInfo().title + "\nDuration: " + min + ":" + seconds);
            Thread.sleep(duration * 1000 + 3000);
            System.out.println(audioTrack.getInfo().title + " " + min + ":" + seconds);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void playlistLoaded(final AudioPlaylist audioPlaylist) {

    }

    @Override
    public void noMatches() {

    }

    @Override
    public void loadFailed(final FriendlyException e) {

    }
}
