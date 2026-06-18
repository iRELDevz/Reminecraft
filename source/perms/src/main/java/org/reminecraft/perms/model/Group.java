package org.reminecraft.perms.model;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    private int weight;
    private String prefix;
    private final List<String> permissions;

    public Group(String name, int weight, String prefix, List<String> permissions) {
        this.name        = name;
        this.weight      = weight;
        this.prefix      = prefix;
        this.permissions = new ArrayList<>(permissions);
    }

    public String getName()            { return name; }
    public int    getWeight()          { return weight; }
    public String getPrefix()          { return prefix; }
    public List<String> getPermissions() { return permissions; }

    public void setWeight(int weight)   { this.weight = weight; }
    public void setPrefix(String prefix){ this.prefix = prefix; }

    public void addPermission(String node) {
        if (!permissions.contains(node)) permissions.add(node);
    }

    public void removePermission(String node) {
        permissions.remove(node);
    }

    public boolean hasPermission(String node) {
        return permissions.contains("*") || permissions.contains(node);
    }
}
