import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Searcher {

    private String key;

    public Searcher(String key) {

        this.key = key;
    }

    public Result search(String query) {

        try {
            String key = "AIzaSyBTBQAadm6FkGtmL-vu5hy1upMetwpK_ks";
            StringBuilder sb = new StringBuilder();
            query = query.replace(" ", "+");
            sb.append("https://www.googleapis.com/youtube/v3/search?part=snippet&q=")
                    .append(query)
                    .append("&key=")
                    .append(key);
            URL url = new URL(sb.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonMap = mapper.readTree(in);
                JsonNode items = jsonMap.get("items");
                List<JsonNode> nodes = new LinkedList<>();
                Iterator<JsonNode> jNodes = items.elements();
                while (jNodes.hasNext()) {
                    nodes.add(jNodes.next());
                    jNodes.remove();
                }
                List<JsonNode> elements = new LinkedList<>();
                for (JsonNode node : nodes) {
                    elements.add(node.get("id"));
                }
                List<JsonNode> list = new LinkedList<>();
                for (JsonNode node : elements) {
                    jNodes = node.elements();
                    while (jNodes.hasNext()) {
                        list.add(jNodes.next());
                        jNodes.remove();
                    }
                    for (JsonNode json : list) {
                        String string = json.toString().replace("\"", "");
                        if (string.equals("youtube#video")) {
                            return new Result(list.get(1).toString().replace("\"", ""), true);
                        }
                    }
                }
                return new Result(null, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Result(null, false);
    }
}
