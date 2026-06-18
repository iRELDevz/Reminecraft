package org.reminecraft.perms.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.reminecraft.perms.model.Group;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Storage {

    private final File groupsFile;
    private final File playersFile;
    private YamlConfiguration groupsYml;
    private YamlConfiguration playersYml;

    private final Map<String, Group>  groupCache  = new LinkedHashMap<>();
    private final Map<UUID, String>   playerGroup = new HashMap<>();
    private final Map<UUID, List<String>> playerExtra = new HashMap<>();

    public Storage(File dataFolder) {
        dataFolder.mkdirs();
        groupsFile  = new File(dataFolder, "groups.yml");
        playersFile = new File(dataFolder, "players.yml");
    }

    public void load(File defaultGroups) {
        if (!groupsFile.exists() && defaultGroups != null && defaultGroups.exists()) {
            try {
                Files.copy(defaultGroups.toPath(), groupsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }

        groupsYml  = YamlConfiguration.loadConfiguration(groupsFile);
        playersYml = playersFile.exists()
            ? YamlConfiguration.loadConfiguration(playersFile)
            : new YamlConfiguration();

        loadGroups();
        loadPlayers();
    }

    private void loadGroups() {
        groupCache.clear();
        if (!groupsYml.isConfigurationSection("groups")) return;

        for (String name : groupsYml.getConfigurationSection("groups").getKeys(false)) {
            String path = "groups." + name;
            int weight       = groupsYml.getInt(path + ".weight", 0);
            String prefix    = groupsYml.getString(path + ".prefix", "");
            List<String> perms = groupsYml.getStringList(path + ".permissions");
            groupCache.put(name.toLowerCase(), new Group(name, weight, prefix, perms));
        }

        if (!groupCache.containsKey("default")) {
            groupCache.put("default", new Group("default", 0, "§7", new ArrayList<>()));
            saveGroups();
        }
    }

    private void loadPlayers() {
        playerGroup.clear();
        playerExtra.clear();
        if (!playersYml.isConfigurationSection("players")) return;

        for (String uuidStr : playersYml.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String group = playersYml.getString("players." + uuidStr + ".group", "default");
                List<String> extra = playersYml.getStringList("players." + uuidStr + ".extra");
                playerGroup.put(uuid, group.toLowerCase());
                playerExtra.put(uuid, new ArrayList<>(extra));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveGroups() {
        groupsYml = new YamlConfiguration();
        for (Group g : groupCache.values()) {
            String path = "groups." + g.getName();
            groupsYml.set(path + ".weight",      g.getWeight());
            groupsYml.set(path + ".prefix",      g.getPrefix());
            groupsYml.set(path + ".permissions", g.getPermissions());
        }
        write(groupsYml, groupsFile);
    }

    public void savePlayer(UUID uuid, String username) {
        String path = "players." + uuid;
        playersYml.set(path + ".username", username);
        playersYml.set(path + ".group",    playerGroup.getOrDefault(uuid, "default"));
        playersYml.set(path + ".extra",    playerExtra.getOrDefault(uuid, new ArrayList<>()));
        write(playersYml, playersFile);
    }

    private void write(YamlConfiguration yml, File file) {
        try { yml.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Group getGroup(String name) {
        return groupCache.get(name == null ? "default" : name.toLowerCase());
    }

    public Collection<Group> getAllGroups() {
        return Collections.unmodifiableCollection(groupCache.values());
    }

    public void putGroup(Group group) {
        groupCache.put(group.getName().toLowerCase(), group);
        saveGroups();
    }

    public boolean removeGroup(String name) {
        if (name.equalsIgnoreCase("default")) return false;
        boolean removed = groupCache.remove(name.toLowerCase()) != null;
        if (removed) saveGroups();
        return removed;
    }

    public String getPlayerGroupName(UUID uuid) {
        return playerGroup.getOrDefault(uuid, "default");
    }

    public Group getPlayerGroup(UUID uuid) {
        String name = getPlayerGroupName(uuid);
        Group g = groupCache.get(name);
        return g != null ? g : groupCache.get("default");
    }

    public void setPlayerGroup(UUID uuid, String username, String groupName) {
        playerGroup.put(uuid, groupName.toLowerCase());
        savePlayer(uuid, username);
    }

    public List<String> getPlayerExtra(UUID uuid) {
        return playerExtra.getOrDefault(uuid, new ArrayList<>());
    }

    public void setPlayerExtra(UUID uuid, String username, List<String> extra) {
        playerExtra.put(uuid, new ArrayList<>(extra));
        savePlayer(uuid, username);
    }

    public void ensurePlayer(UUID uuid, String username) {
        if (!playerGroup.containsKey(uuid)) {
            playerGroup.put(uuid, "default");
            playerExtra.put(uuid, new ArrayList<>());
            savePlayer(uuid, username);
        }
    }
}
