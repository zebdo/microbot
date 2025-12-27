# Microbot API Documentation

This directory contains the new Queryable API for interacting with game entities.

## 📚 Documentation

- **[QUERYABLE_API.md](QUERYABLE_API.md)** - Complete guide to using the Queryable API
  - Introduction and benefits
  - Getting started guide
  - API reference
  - Common patterns and examples
  - Performance tips
  - Migration guide from legacy API
  - Troubleshooting

## 🚀 Quick Start

### NPCs
```java
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcQueryable;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;

Rs2NpcModel banker = new Rs2NpcQueryable()
    .withName("Banker")
    .nearest();
```

### Ground Items
```java
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemQueryable;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;

Rs2TileItemModel coins = new Rs2TileItemQueryable()
    .withName("Coins")
    .nearest();
```

### Players
```java
import net.runelite.client.plugins.microbot.api.player.Rs2PlayerQueryable;
import net.runelite.client.plugins.microbot.api.player.models.Rs2PlayerModel;

Rs2PlayerModel player = new Rs2PlayerQueryable()
    .withName("PlayerName")
    .nearest();
```

### Tile Objects
```java
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectQueryable;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;

Rs2TileObjectModel tree = new Rs2TileObjectQueryable()
    .withName("Tree")
    .nearest();
```

## 📂 Directory Structure

```
api/
├── README.md                      # This file
├── QUERYABLE_API.md              # Complete API documentation
│
├── IEntityQueryable.java         # Generic queryable interface
├── AbstractEntityQueryable.java  # Base implementation
├── IEntity.java                  # Base entity interface
│
├── npc/                          # NPC queries
│   ├── Rs2NpcQueryable.java
│   ├── Rs2NpcCache.java
│   └── models/
│       └── Rs2NpcModel.java
│
├── tileitem/                     # Ground item queries
│   ├── Rs2TileItemQueryable.java
│   ├── Rs2TileItemCache.java
│   ├── TileItemApiExample.java
│   └── models/
│       └── Rs2TileItemModel.java
│
├── player/                       # Player queries
│   ├── Rs2PlayerQueryable.java
│   ├── Rs2PlayerCache.java
│   ├── PlayerApiExample.java
│   └── models/
│       └── Rs2PlayerModel.java
│
├── tileobject/                   # Tile object queries
│   ├── Rs2TileObjectQueryable.java
│   ├── Rs2TileObjectCache.java
│   └── models/
│       └── Rs2TileObjectModel.java
│
├── actor/                        # Actor utilities
└── playerstate/                  # Player state cache
```

## 🔥 Why Use Queryable API?

### Before (Legacy) ❌
```java
NPC banker = null;
for (NPC npc : client.getNpcs()) {
    if (npc.getName() != null && npc.getName().equals("Banker")) {
        if (banker == null || npc.getWorldLocation().distanceTo(player.getWorldLocation()) 
            < banker.getWorldLocation().distanceTo(player.getWorldLocation())) {
            banker = npc;
        }
    }
}
```

### After (Queryable) ✅
```java
Rs2NpcModel banker = new Rs2NpcQueryable()
    .withName("Banker")
    .nearest();
```

**Benefits:**
- 📖 More readable and maintainable
- 🚀 Faster development
- 🐛 Fewer bugs (type-safe)
- ⚡ Better performance (optimized internally)

## 📖 Examples

Check the `*ApiExample.java` files in each subdirectory for complete examples:

- `npc/NpcApiExample.java` - NPC query examples
- `tileitem/TileItemApiExample.java` - Ground item examples
- `player/PlayerApiExample.java` - Player query examples

## 🔗 Additional Resources

- **Main Documentation**: `../CLAUDE.md`
- **Discord**: https://discord.gg/zaGrfqFEWE
- **Website**: https://themicrobot.com

---

**Last Updated:** November 18, 2025  
**Microbot Version:** 2.1.0

