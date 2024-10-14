package fr.reloaded.vultr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class VultrStop extends JavaPlugin {

    private final static String LOG_PREFIX = "[Reloaded-MC - Vultr-Stop] ";
    private String apiKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.apiKey = this.getConfig().getString("VULTR_API_KEY");
    }

    @Override
    public void onDisable() {
        try {
            Map<String, String> serverList = getInstancesList();
            for (Map.Entry<String, String> entry : serverList.entrySet()) {
                if (entry.getValue().equals(this.getPublicAddress())) {
                    stopServer(entry.getKey());
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the server by send a DELETE request to the Vultr REST API
     * @param idServer the id of the server to stop
     * @throws IOException if an error occurs while stopping the server
     */
    private void stopServer(String idServer) throws IOException {
        Request.delete("https://api.vultr.com/v2/instances/" + idServer)
                .addHeader("Authorization", "Bearer " + apiKey)
                .execute()
                .handleResponse(response -> {
                    String body = EntityUtils.toString(response.getEntity());
                    if (response.getCode() != 204) {
                        System.err.println(LOG_PREFIX + "Failed to stop server: " + body);
                    }
                    System.out.println(LOG_PREFIX + "Server " + idServer + " stopped.");
                    return null;
                });
    }

    /**
     * Get instances list
     * @return a map of instances id and their public IP
     * @throws IOException if an error occurs while getting the instances list
     */
    private Map<String, String> getInstancesList() throws IOException {
        Map<String, String> serverList = new HashMap<>();
        return Request.get("https://api.vultr.com/v2/instances")
                .addHeader("Authorization", "Bearer " + apiKey)
                .execute()
                .handleResponse(response -> {
                    String body = EntityUtils.toString(response.getEntity());
                    if (response.getCode() == 200) {
                        JsonNode json = new ObjectMapper().readTree(body);
                        for (JsonNode instance : json.get("instances")) {
                            String id = instance.get("id").asText();
                            String main_ip = instance.get("main_ip").asText();
                            serverList.put(id, main_ip);
                        }
                        return serverList;
                    } else {
                        System.err.println(LOG_PREFIX + "Failed to retrieve server list: " + body);
                    }
                    return null;
                });
    }


    /**
     * Get the public IP of the minecraft server
     *
     * @return the public IP of the minecraft server
     * @throws IOException if an error occurs while getting the public IP
     */
    private String getPublicAddress() throws IOException {
        try {
            Process process = Runtime.getRuntime().exec("curl ifconfig.me");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String publicIp = reader.readLine();
            process.waitFor();
            return publicIp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
