package net.runelite.client.plugins.microbot.util.item;

public class Rs2ItemSearchModel {
    private int id;
    private String name;

    public Rs2ItemSearchModel(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // No-argument constructor required for Gson deserialization
    public Rs2ItemSearchModel() {
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Optionally, add setters if needed
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Rs2ItemSearchModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
