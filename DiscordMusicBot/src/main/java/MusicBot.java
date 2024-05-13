import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicBot {

    private final static Map<String, Command> commands = new HashMap<>();
    static String directory = "./src/main/resources/playlists/";
    static String reactionsFolder = "./src/main/resources/reactions/";
    static File library = new File(directory);
    static InsultGenerator insultGenerator = new InsultGenerator();

    //say hello
    static {
        commands.put("hello", event -> print(event, "Hi!"));
    }

    //say insult
    static {
        commands.put("insult", event -> print(event, insultGenerator.generate()));
    }

    static {
        commands.put("lists", event -> print(event, readFile(library)));
    }

    static {
        commands.put("list", event -> {
            String name = event.getMessage().getContent().split(" ")[1];
            print(event, readFile(new File(directory + name + ".txt")));
        });
    }

    //create playlist
    //format: >cpl name: song, song, song
    //playlist name shouldn't contain any splitters
    static {
        commands.put("cpl", event -> {
            String[] query = event.getMessage().getContent().split(": ");
            String name = query[0].split(" ")[1];
            while (!search(name)) {
                name = name + "1";
            }
            String format = ".txt";
            String[] songs = new String[query[1].split(", ").length];
            System.arraycopy(query[1].split(", "), 0, songs, 0, songs.length);
            File playlist = new File(directory + name + format);
            String[] lost = new String[1];
            lost[0] = songs[0];
            writeToFile(playlist, songs);
            writeToFile(playlist, lost);
            print(event, "Playlist " + name + " created");
        });
    }

    //add connection reaction
    static {
        commands.put("acr", event -> {
            String folder = "connect";
            String query = event.getMessage().getContent();
            String[] reactions = new String[query.split(" ").length - 1];
            System.arraycopy(query.split(" "), 1, reactions, 0, reactions.length);
            writeToFile(new File(reactionsFolder + folder + ".txt"), reactions);
        });
    }

    //add disconnection reaction
    static {
        commands.put("adr", event -> {
            String folder = "disconnect";
            String query = event.getMessage().getContent();
            String[] reactions = new String[query.split(" ").length - 1];
            System.arraycopy(query.split(" "), 1, reactions, 0, reactions.length);
            writeToFile(new File(reactionsFolder + folder + ".txt"), reactions);
        });
    }

    //show connection reactions
    static {
        commands.put("scr", event -> print(event, readFile(new File(reactionsFolder + "connect.txt"))));
    }

    //show disconnection reactions
    static {
        commands.put("sdr", event -> print(event, readFile(new File(reactionsFolder + "disconnect.txt"))));
    }

    public static void writeToFile(File file, String[] lines) {

        try {
            FileWriter writer = new FileWriter(file, true);
            for (String line : lines) {
                writer.write(line + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(File file) {

        StringBuilder content = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String link;
            while ((link = reader.readLine()) != null) {
                content.append(link).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content.toString();
    }

    public static void main(String[] args) throws LoginException {

        String token = args[0];
        String key = args[1];

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);

        final AudioPlayer player = playerManager.createPlayer();
        AudioProvider provider = new LavaPlayerAudioProvider(player);
        Searcher searcher = new Searcher(key);
        JDA jda = JDABuilder.createDefault(token).build();
        final TrackScheduler scheduler = new TrackScheduler(player);

        commands.put("join", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {
                        channel.join(spec -> spec.setProvider(provider)).block();
                        jda.addEventListener(new MyConnectEvent(playerManager, scheduler));
                        jda.addEventListener(new MyDisconnectEvent(playerManager, scheduler));
                        scheduler.setEvent(event);
                    }
                }
            }
        });

        commands.put("play", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split(" "));
            if (command.get(1).startsWith("http")) {
                executorService.submit(() -> {
                            playerManager.loadItem(command.get(1), scheduler);
                            //print(event, "https://www.youtube.com/watch?v=" + command.get(0));
                        }
                );
            } else {
                List<String> request = new LinkedList<>();
                for (String str : command) {
                    if (!str.equals(">play")) {
                        request.add(str);
                    }
                }
                StringBuilder track = new StringBuilder();
                for (String word : request) {
                    if (!word.equals("play")) {
                        track.append(word);
                    }
                }
                Result result = searcher.search(track.toString());
                if (result.getStatus()) {
                    playerManager.loadItem(result.toString(), scheduler);
                    executorService.submit(() -> {
                                playerManager.loadItem(result.toString(), scheduler);
                                //print(event, "https://www.youtube.com/watch?v=" + result.toString());
                            }
                    );
                }
            }
        });

        commands.put("ppl", event -> {
            String[] query = event.getMessage().getContent().split(" ");
            String pName = query[1] + ".txt";
            File playlist = null;
            if (library.listFiles() != null) {
                for (File plist : Objects.requireNonNull(library.listFiles())) {
                    if (pName.equals(plist.getName()))
                        playlist = plist;
                }
            }
            if (playlist != null) {
                String content = readFile(playlist);
                List<String> tracks = new LinkedList<>(Arrays.asList(content.split("\n")));
                for (String track : tracks) {
                    if (track.startsWith("http")) {
                        executorService.submit(() -> {
                            playerManager.loadItem(track, scheduler);
                        });
                    } else {
                        Result result = searcher.search(track);
                        if (result.getStatus()) {
                            executorService.submit(() -> {
                                        playerManager.loadItem(result.toString(), scheduler);
                                    }
                            );
                        }
                    }
                }
            } else {
                print(event, "Playlist not found");
            }
        });


        DiscordClientBuilder.create(token)
                .build()
                .withGateway(client -> {
                    client.getEventDispatcher().on(ReadyEvent.class)
                            .subscribe(ready -> System.out.println("Logged in as " + ready.getSelf().getUsername()));

                    client.getEventDispatcher().on(MessageCreateEvent.class)
                            .subscribe(event -> {
                                final String content = event.getMessage().getContent();
                                for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                                    if (content.startsWith('>' + entry.getKey())) {
                                        entry.getValue().execute(event);
                                        break;
                                    }
                                }
                            });

                    client.getEventDispatcher().on(Event.class).subscribe(
                            event -> {

                            });
                    return client.onDisconnect();
                })
                .block();
    }

    public static boolean search(String name) {

        for (File file : (Objects.requireNonNull(new File(directory).listFiles())))
            if (file.getName().equals(name + ".txt")) {
                return false;
            }

        return true;
    }

    public static void print(MessageCreateEvent event, String content) {

        Objects.requireNonNull(event.getMessage().getChannel().block()).createMessage(content).block();
    }
}