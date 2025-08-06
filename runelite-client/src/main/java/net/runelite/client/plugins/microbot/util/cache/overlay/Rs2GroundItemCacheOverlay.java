package net.runelite.client.plugins.microbot.util.cache.overlay;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.cache.Rs2GroundItemCache;
import net.runelite.client.plugins.microbot.util.cache.util.Rs2GroundItemCacheUtils;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItemModel;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.QuantityFormatter;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Overlay for rendering cached ground items with various highlight options.
 * Based on RuneLite's GroundItemsOverlay patterns but using the cache system.
 * 
 * @author Vox
 * @version 1.0
 */
public class Rs2GroundItemCacheOverlay extends Rs2BaseCacheOverlay {
    
    /**
     * Price calculation modes for value display.
     */
    public enum PriceMode {
        OFF("Off"),
        GE("Grand Exchange"),
        HA("High Alchemy"),
        STORE("Store Price"),
        BOTH("Both GE & HA");
        
        private final String displayName;
        
        PriceMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Ground item-specific colors (Green theme)
    private static final Color GROUND_ITEM_BORDER_COLOR = Color.GREEN;
    private static final Color GROUND_ITEM_FILL_COLOR = new Color(0, 255, 0, 50); // Green with alpha
    
    // Text rendering constants
    private static final int TEXT_OFFSET_Z = 20;
    private static final int STRING_GAP = 15; // Gap between multiple items on same tile
    
    // Rendering options
    private boolean renderTile = true;
    private boolean renderText = true;
    private boolean renderItemInfo = true; // Show item ID
    private boolean renderWorldCoordinates = false; // Show world coordinates
    private boolean onlyShowTextOnHover = true; // Only show text when mouse is hovering
    private Predicate<Rs2GroundItemModel> renderFilter = groundItem -> true; // Default to no filter
    
    // Advanced rendering options
    private boolean renderQuantity = true; // Show quantity for stackable items
    private PriceMode priceMode = PriceMode.OFF; // Price display mode
    private boolean renderDespawnTimer = false; // Show despawn countdown
    private boolean renderOwnershipIndicator = false; // Show ownership status
    
    // Value thresholds for color coding
    private int lowValueThreshold = 1000;
    private int mediumValueThreshold = 10000;
    private int highValueThreshold = 100000;
    
    // Map to track text offset for multiple items on same tile
    private final Map<WorldPoint, Integer> offsetMap = new HashMap<>();
    
    public Rs2GroundItemCacheOverlay(Client client, ModelOutlineRenderer modelOutlineRenderer) {
        super(client, modelOutlineRenderer);
    }
    
    @Override
    protected Color getDefaultBorderColor() {
        return GROUND_ITEM_BORDER_COLOR;
    }
    
    @Override
    protected Color getDefaultFillColor() {
        return GROUND_ITEM_FILL_COLOR;
    }
    
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!isClientReady()) {
            return null;
        }
        
        // Clear offset map for new frame
        offsetMap.clear();
        
        // Group ground items by tile location to handle multiple items on same tile
        Map<WorldPoint, List<Rs2GroundItemModel>> itemsByLocation = Rs2GroundItemCache.getInstance().stream()
                .filter(item -> renderFilter == null || renderFilter.test(item))
                .filter(Rs2GroundItemCacheUtils::isVisibleInViewport)
                .collect(Collectors.groupingBy(Rs2GroundItemModel::getLocation));
        
        // Render each tile
        for (Map.Entry<WorldPoint, List<Rs2GroundItemModel>> entry : itemsByLocation.entrySet()) {
            WorldPoint location = entry.getKey();
            List<Rs2GroundItemModel> itemsAtLocation = entry.getValue();
            
            renderItemsAtTile(graphics, location, itemsAtLocation);
        }
        
        return null;
    }
    
    /**
     * Renders all ground items at a specific tile location.
     * Handles multiple items by spacing them vertically.
     * 
     * @param graphics The graphics context
     * @param location The tile location
     * @param itemsAtLocation List of items at this location
     */
    private void renderItemsAtTile(Graphics2D graphics, WorldPoint location, List<Rs2GroundItemModel> itemsAtLocation) {
        if (itemsAtLocation.isEmpty()) {
            return;
        }
        
        // Check if we should only show text on hover for this tile
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), location);
        if (localPoint == null) {
            return;
        }
        
        boolean shouldShowText = !onlyShowTextOnHover || isMouseHoveringOver(localPoint);
        
        // Sort items by value (highest first) for better display order
        itemsAtLocation.sort((a, b) -> Integer.compare(getItemDisplayValue(b), getItemDisplayValue(a)));
        
        // Render tile highlight if any item should be highlighted
        if (renderTile) {
            for (Rs2GroundItemModel item : itemsAtLocation) {
                Color borderColor = getBorderColorForItem(item);
                Color fillColor = getFillColorForItem(item);
                renderItemTile(graphics, item, borderColor, fillColor, DEFAULT_BORDER_WIDTH);
                break; // Only render tile once per location
            }
        }
        
        // Render text for each item with vertical offset
        if (shouldShowText && (renderText || renderItemInfo || renderWorldCoordinates || 
            renderQuantity || priceMode != PriceMode.OFF || renderDespawnTimer || renderOwnershipIndicator)) {
            
            for (int i = 0; i < itemsAtLocation.size(); i++) {
                Rs2GroundItemModel item = itemsAtLocation.get(i);
                renderItemTextWithOffset(graphics, item, i);
            }
        }
    }
    
    /**
     * Gets the display value for an item based on the current price mode.
     * 
     * @param item The ground item model
     * @return The display value for sorting and comparison
     */
    private int getItemDisplayValue(Rs2GroundItemModel item) {
        switch (priceMode) {
            case GE:
                return item.getTotalGeValue();
            case HA:
                return item.getTotalHaValue();
            case STORE:
                return item.getTotalValue();
            case BOTH:
                return Math.max(item.getTotalGeValue(), item.getTotalHaValue());
            case OFF:
            default:
                return item.getTotalValue(); // Default to store value
        }
    }
    
    /**
     * Renders text for a ground item with vertical offset for multiple items.
     * 
     * @param graphics The graphics context
     * @param itemModel The ground item model
     * @param offset The vertical offset index (0-based)
     */
    private void renderItemTextWithOffset(Graphics2D graphics, Rs2GroundItemModel itemModel, int offset) {
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), itemModel.getLocation());
        if (localPoint == null) {
            return;
        }
        
        // Build text information for this item
        String itemText = buildItemText(itemModel);
        if (itemText.isEmpty()) {
            return;
        }
        
        // Get canvas point with Z offset for text
        net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPoint, 
                client.getTopLevelWorldView().getPlane(), TEXT_OFFSET_Z);
        
        if (canvasPoint != null) {
            // Apply vertical offset for multiple items on same tile
            int adjustedY = canvasPoint.getY() - (STRING_GAP * offset);
            net.runelite.api.Point adjustedPoint = new net.runelite.api.Point(canvasPoint.getX(), adjustedY);
            
            Color textColor = getBorderColorForItem(itemModel);
            renderTextWithBackground(graphics, itemText, adjustedPoint, textColor);
        }
    }

    /**
     * Renders a single ground item with the configured options (Legacy method for compatibility).
     * 
     * @param graphics The graphics context
     * @param itemModel The ground item model to render
     */
    private void renderGroundItemOverlay(Graphics2D graphics, Rs2GroundItemModel itemModel) {
        try {
            Color borderColor = getBorderColorForItem(itemModel);
            Color fillColor = getFillColorForItem(itemModel);
            float borderWidth = DEFAULT_BORDER_WIDTH;
            
            // Render tile highlight
            if (renderTile) {
                renderItemTile(graphics, itemModel, borderColor, fillColor, borderWidth);
            }
            
            // Render text information if enabled
            if (renderText || renderItemInfo || renderWorldCoordinates || 
                renderQuantity || priceMode != PriceMode.OFF || renderDespawnTimer || renderOwnershipIndicator) {
                renderItemText(graphics, itemModel, borderColor);
            }
            
        } catch (Exception e) {
            // Silent fail to avoid spam
        }
    }
    
    /**
     * Renders a tile highlight for the ground item.
     * 
     * @param graphics The graphics context
     * @param itemModel The ground item model
     * @param borderColor The border color
     * @param fillColor The fill color
     * @param borderWidth The border width
     */
    private void renderItemTile(Graphics2D graphics, Rs2GroundItemModel itemModel, 
                               Color borderColor, Color fillColor, float borderWidth) {
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), itemModel.getLocation());
        if (localPoint == null) {
            return;
        }
        
        Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
        if (tilePoly != null) {
            Stroke stroke = new BasicStroke(borderWidth);
            renderPolygon(graphics, tilePoly, borderColor, fillColor, stroke);
        }
    }
    
    /**
     * Renders text information for the ground item with hover detection and background.
     * 
     * @param graphics The graphics context
     * @param itemModel The ground item model
     * @param color The text color
     */
    private void renderItemText(Graphics2D graphics, Rs2GroundItemModel itemModel, Color color) {
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), itemModel.getLocation());
        if (localPoint == null) {
            return;
        }

        // Check if we should only show text on hover
        if (onlyShowTextOnHover && !isMouseHoveringOver(localPoint)) {
            return;
        }

        // Build single line of information
        StringBuilder infoText = new StringBuilder();
        
        // Main item name and quantity
        if (renderText) {
            infoText.append(buildItemText(itemModel));
        }
        
        // Add item ID and coordinates if enabled
        if (renderItemInfo || renderWorldCoordinates) {
            if (infoText.length() > 0) {
                infoText.append(" | ");
            }
            
            if (renderItemInfo) {
                infoText.append("ID:").append(itemModel.getId());
            }
            
            if (renderWorldCoordinates) {
                if (renderItemInfo) {
                    infoText.append(" ");
                }
                WorldPoint wp = itemModel.getLocation();
                infoText.append("(").append(wp.getX()).append(",").append(wp.getY()).append(")");
            }
        }
        
        // Add additional information based on settings
        if (renderQuantity && itemModel.getQuantity() > 1) {
            if (infoText.length() > 0) {
                infoText.append(" ");
            }
            infoText.append("x").append(itemModel.getQuantity());
        }
        
        if (priceMode != PriceMode.OFF) {
            if (infoText.length() > 0) {
                infoText.append(" ");
            }
            
            switch (priceMode) {
                case GE:
                    infoText.append("(GE: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalGeValue())).append(" gp)");
                    break;
                case HA:
                    infoText.append("(HA: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalHaValue())).append(" gp)");
                    break;
                case STORE:
                    infoText.append("(Store: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalValue())).append(" gp)");
                    break;
                case BOTH:
                    if (itemModel.getTotalGeValue() > 0) {
                        infoText.append("(GE: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalGeValue())).append(" gp)");
                    }
                    if (itemModel.getTotalHaValue() > 0) {
                        infoText.append(" (HA: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalHaValue())).append(" gp)");
                    }
                    break;
                case OFF:
                default:
                    // No price display
                    break;
            }
        }
        
        if (renderDespawnTimer && !itemModel.isDespawned()) {
            if (infoText.length() > 0) {
                infoText.append(" ");
            }
            infoText.append("⏰").append(formatDespawnTime(itemModel.getSecondsUntilDespawn()));
        }
        
        if (renderOwnershipIndicator) {
            if (infoText.length() > 0) {
                infoText.append(" ");
            }
            infoText.append(itemModel.isOwned() ? "👤" : "🌐");
        }

        // Get canvas point with Z offset for text
        net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPoint, 
                client.getTopLevelWorldView().getPlane(), TEXT_OFFSET_Z);

        if (canvasPoint != null && infoText.length() > 0) {
            renderTextWithBackground(graphics, infoText.toString(), canvasPoint, color);
        }
    }
    
    /**
     * Checks if the mouse is hovering over a ground item location.
     * 
     * @param localPoint The ground item location
     * @return true if mouse is hovering over the location
     */
    private boolean isMouseHoveringOver(LocalPoint localPoint) {
        net.runelite.api.Point mousePos = client.getMouseCanvasPosition();
        if (mousePos == null) {
            return false;
        }
        
        // Check if mouse is over the tile
        Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
        return tilePoly != null && tilePoly.contains(mousePos.getX(), mousePos.getY());
    }
    
    /**
     * Renders text with a semi-transparent background for better readability.
     * 
     * @param graphics The graphics context
     * @param text The text to render
     * @param location The location to render at
     * @param color The text color
     */
    private void renderTextWithBackground(Graphics2D graphics, String text, 
                                        net.runelite.api.Point location, Color color) {
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        // Create background rectangle
        int padding = 4;
        int backgroundX = location.getX() - padding;
        int backgroundY = location.getY() - textHeight - padding;
        int backgroundWidth = textWidth + (padding * 2);
        int backgroundHeight = textHeight + (padding * 2);
        
        // Draw semi-transparent background
        Color backgroundColor = new Color(0, 0, 0, 128); // Semi-transparent black
        graphics.setColor(backgroundColor);
        graphics.fillRect(backgroundX, backgroundY, backgroundWidth, backgroundHeight);
        
        // Draw text
        renderText(graphics, text, location, color);
    }
    
    /**
     * Builds the text to display for a ground item.
     * 
     * @param itemModel The ground item model
     * @return The text to display
     */
    private String buildItemText(Rs2GroundItemModel itemModel) {
        StringBuilder text = new StringBuilder();
        
        // Main item name
        if (renderText) {
            text.append(itemModel.getName());
        }
        
        // Add quantity if more than 1
        if (renderQuantity && itemModel.getQuantity() > 1) {
            if (text.length() > 0) {
                text.append(" ");
            }
            text.append("(").append(QuantityFormatter.quantityToStackSize(itemModel.getQuantity())).append(")");
        }
        
        // Add item ID and coordinates if enabled
        if (renderItemInfo || renderWorldCoordinates) {
            if (text.length() > 0) {
                text.append(" | ");
            }
            
            if (renderItemInfo) {
                text.append("ID:").append(itemModel.getId());
            }
            
            if (renderWorldCoordinates) {
                if (renderItemInfo) {
                    text.append(" ");
                }
                WorldPoint wp = itemModel.getLocation();
                text.append("(").append(wp.getX()).append(",").append(wp.getY()).append(")");
            }
        }
        
        // Add price information based on price mode
        if (priceMode != PriceMode.OFF) {
            if (text.length() > 0) {
                text.append(" ");
            }
            
            switch (priceMode) {
                case GE:
                    if (itemModel.getTotalGeValue() > 0) {
                        text.append("[GE: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalGeValue())).append(" gp]");
                    }
                    break;
                case HA:
                    if (itemModel.getTotalHaValue() > 0) {
                        text.append("[HA: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalHaValue())).append(" gp]");
                    }
                    break;
                case STORE:
                    if (itemModel.getTotalValue() > 0) {
                        text.append("[Store: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalValue())).append(" gp]");
                    }
                    break;
                case BOTH:
                    boolean hasGe = itemModel.getTotalGeValue() > 0;
                    boolean hasHa = itemModel.getTotalHaValue() > 0;
                    if (hasGe || hasHa) {
                        text.append("[");
                        if (hasGe) {
                            text.append("GE: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalGeValue()));
                        }
                        if (hasGe && hasHa) {
                            text.append(" | ");
                        }
                        if (hasHa) {
                            text.append("HA: ").append(QuantityFormatter.quantityToStackSize(itemModel.getTotalHaValue()));
                        }
                        text.append(" gp]");
                    }
                    break;
                case OFF:
                default:
                    // No price display
                    break;
            }
        }
        
        // Add despawn timer if enabled
        if (renderDespawnTimer && !itemModel.isDespawned()) {
            if (text.length() > 0) {
                text.append(" ");
            }
            text.append("⏰").append(formatDespawnTime(itemModel.getSecondsUntilDespawn()));
        }
        
        // Add ownership indicator if enabled
        if (renderOwnershipIndicator) {
            if (text.length() > 0) {
                text.append(" ");
            }
            text.append(itemModel.isOwned() ? "👤" : "🌐");
        }
        
        return text.toString();
    }
    
    /**
     * Formats the item value for display.
     * 
     * @param value The item value
     * @return The formatted value string
     */
    private String formatValue(int value) {
        // Simple color formatting without ColorUtils dependency
        if (value < lowValueThreshold) {
            return String.format("%d gp", value);
        } else if (value < mediumValueThreshold) {
            return String.format("%d gp", value);
        } else if (value < highValueThreshold) {
            return String.format("%d gp", value);
        } else {
            return String.format("%d gp", value);
        }
    }
    
    /**
     * Formats the despawn time for display.
     * 
     * @param despawnSeconds The despawn time in seconds
     * @return The formatted time string
     */
    private String formatDespawnTime(long despawnSeconds) {
        if (despawnSeconds <= 0) {
            return "";
        }
        
        long minutes = despawnSeconds / 60;
        long seconds = despawnSeconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    // ============================================
    // Configuration Methods
    // ============================================
    
    public Rs2GroundItemCacheOverlay setRenderTile(boolean renderTile) {
        this.renderTile = renderTile;
        return this;
    }
    
    public Rs2GroundItemCacheOverlay setRenderText(boolean renderText) {
        this.renderText = renderText;
        return this;
    }
    
    public Rs2GroundItemCacheOverlay setRenderItemInfo(boolean renderItemInfo) {
        this.renderItemInfo = renderItemInfo;
        return this;
    }
    
    public Rs2GroundItemCacheOverlay setRenderWorldCoordinates(boolean renderWorldCoordinates) {
        this.renderWorldCoordinates = renderWorldCoordinates;
        return this;
    }
    
    public Rs2GroundItemCacheOverlay setRenderFilter(Predicate<Rs2GroundItemModel> renderFilter) {
        this.renderFilter = renderFilter;
        return this;
    }
    
    // Configuration methods for advanced rendering options
    public void setRenderQuantity(boolean renderQuantity) {
        this.renderQuantity = renderQuantity;
    }
    
    public void setPriceMode(PriceMode priceMode) {
        this.priceMode = priceMode;
    }
    
    public void setRenderDespawnTimer(boolean renderDespawnTimer) {
        this.renderDespawnTimer = renderDespawnTimer;
    }
    
    public void setRenderOwnershipIndicator(boolean renderOwnershipIndicator) {
        this.renderOwnershipIndicator = renderOwnershipIndicator;
    }
    
    public void setValueThresholds(int low, int medium, int high) {
        this.lowValueThreshold = low;
        this.mediumValueThreshold = medium;
        this.highValueThreshold = high;
    }
    
    /**
     * Gets the border color for a specific ground item.
     * Can be overridden by subclasses to provide per-item coloring.
     * 
     * @param itemModel The ground item model
     * @return The border color for this item
     */
    protected Color getBorderColorForItem(Rs2GroundItemModel itemModel) {
        return getDefaultBorderColor();
    }
    
    /**
     * Gets the fill color for a specific ground item.
     * Can be overridden by subclasses to provide per-item coloring.
     * 
     * @param itemModel The ground item model
     * @return The fill color for this item
     */
    protected Color getFillColorForItem(Rs2GroundItemModel itemModel) {
        return getDefaultFillColor();
    }
    
    public Rs2GroundItemCacheOverlay setOnlyShowTextOnHover(boolean onlyShowTextOnHover) {
        this.onlyShowTextOnHover = onlyShowTextOnHover;
        return this;
    }
}
