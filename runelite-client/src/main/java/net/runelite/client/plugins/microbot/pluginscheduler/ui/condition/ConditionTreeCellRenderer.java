package net.runelite.client.plugins.microbot.pluginscheduler.ui.condition;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.NotCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.LootItemCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.skill.SkillCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.skill.SkillLevelCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.skill.SkillXpCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.ui.ColorScheme;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.Icon;

// Condition tree cell renderer
public class ConditionTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final Color PLUGIN_CONDITION_COLOR = new Color(0, 128, 255); // Blue for plugin conditions
    private static final Color USER_CONDITION_COLOR = Color.WHITE; // White for user conditions
    private static final Color SATISFIED_COLOR = new Color(0, 180, 0); // Bright green for satisfied conditions
    private static final Color NOT_SATISFIED_COLOR = new Color(220, 60, 60); // Bright red for unsatisfied conditions
    
    private final SchedulerPlugin schedulerPlugin;
    private PluginScheduleEntry scheduledPlugin;
   


    public ConditionTreeCellRenderer(SchedulerPlugin schedulerPlugin) {
        this.schedulerPlugin = schedulerPlugin;
          // Load icons (you should create these resources)
       
    }
    /**
     * Sets the scheduled plugin for this renderer
     */
    public void setScheduledPlugin(PluginScheduleEntry scheduledPlugin) {
        this.scheduledPlugin = scheduledPlugin;
    }
    /**
     * Creates a simple text-based icon as a fallback when image resources aren't available
     */
    private ImageIcon createTextIcon(String text) {
        java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 9);
        java.awt.FontMetrics metrics = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                .createGraphics().getFontMetrics(font);
        int width = metrics.stringWidth(text) + 4;
        int height = metrics.getHeight() + 4;
        
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, 
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, 
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(font);
        g2d.drawString(text, 2, metrics.getAscent() + 1);
        g2d.dispose();
        
        return new ImageIcon(image);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, 
                                            boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        // Check if node is null first
        if (value == null) {
            setText("null");
            return this;
        }
        
        // Safely cast to DefaultMutableTreeNode
        if (!(value instanceof DefaultMutableTreeNode)) {
            return this;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        
        // Default styling
        setFont(getFont().deriveFont(Font.PLAIN));
        
        if (userObject instanceof LogicalCondition) {
            LogicalCondition logicalCondition = (LogicalCondition) userObject;
            
            // Show condition counts and progress percentage
            int total = logicalCondition.getTotalConditionCount();
            int met = logicalCondition.getMetConditionCount();
            double progress = logicalCondition.getProgressPercentage();
            
            // Check if this is a plugin-defined logical condition
            boolean isPluginDefined = scheduledPlugin != null && 
                                    scheduledPlugin.getStopConditionManager().isPluginDefinedCondition(logicalCondition);
            
            // Determine icon based on logical type
            if (logicalCondition instanceof AndCondition) {
                setIcon(getConditionTypeIcon(logicalCondition));                
                setFont(getFont().deriveFont(isPluginDefined ? Font.BOLD | Font.ITALIC : Font.BOLD));
            } else if (logicalCondition instanceof OrCondition) {
                setIcon(getConditionTypeIcon(logicalCondition));                                
                setFont(getFont().deriveFont(isPluginDefined ? Font.BOLD | Font.ITALIC : Font.BOLD));
            }else{
                // Use appropriate icon based on condition type
                
            }
            // Color based on condition status - ALWAYS use these colors regardless of selection
            if (logicalCondition.isSatisfied()) {
                setForeground(SATISFIED_COLOR);  // Always green for satisfied conditions
            } else {
                setForeground(NOT_SATISFIED_COLOR);  // Always red for unsatisfied conditions
            }
            
            // Show progress info
            String text = logicalCondition.getDescription();
            if (total > 0) {
                text += String.format(" [%d/%d met, %.0f%%]", met, total, progress);
            }
            
            if (isPluginDefined) {
                text = "📌 " + text;
            }
            
            setText(text);
        } else if (userObject instanceof NotCondition) {
            NotCondition notCondition = (NotCondition) userObject;
            setIcon(getResourceIcon("not_icon.png"));
            setForeground(new Color(210, 40, 40));  // Red for NOT
            setText(notCondition.getDescription());
        } else if (userObject instanceof Condition) {
            Condition condition = (Condition) userObject;
            
            // Show progress for the condition
            String text = condition.getDescription();
            double progress = condition.getProgressPercentage();
            
            if (progress > 0 && progress < 100) {
                text += String.format(" (%.0f%%)", progress);
            }
            
            // Use appropriate icon based on condition type
            setIcon(getConditionTypeIcon(condition));
            
            // Color based on condition status - ALWAYS use these colors regardless of selection
            if (condition.isSatisfied()) {
                setForeground(SATISFIED_COLOR);  // Always green for satisfied conditions
            } else {
                setForeground(NOT_SATISFIED_COLOR);  // Always red for unsatisfied conditions
            }
            
            setText(text);
            
            // Visual indicator for plugin-defined conditions            
            if (scheduledPlugin !=null && scheduledPlugin.getStopConditionManager().isPluginDefinedCondition(condition)) {
                setFont(getFont().deriveFont(Font.ITALIC));
                text = "📌 " + text;
                setText(text);
            }
        } else if (userObject instanceof String) {
            // Section headers (Plugin/User Conditions)
            setFont(getFont().deriveFont(Font.BOLD));
            setIcon(null);
            setForeground(Color.YELLOW);
        }
        
        // If selected, keep our custom foreground color but change background
        if (sel) {
            // Keep the foreground color we've set above, but use a selection background that works with it
            setBackground(new Color(60, 60, 60)); // Dark gray selection background
            setBorderSelectionColor(new Color(100, 100, 100)); // Darker border for selection
        }
        
        return this;
    }

    /**
     * Gets an appropriate icon for the condition type
     */
    private Icon getConditionTypeIcon(Condition condition) {
        if (condition instanceof SkillCondition) {
            SkillCondition skillCondition = (SkillCondition) condition;
            Icon skillIcon = skillCondition.getSkillIcon();
            
            if (skillIcon != null) {
                // Ensure icon is properly sized to 24x24
                if (skillIcon instanceof ImageIcon) {
                    ImageIcon imageIcon = (ImageIcon) skillIcon;
                    if (imageIcon.getIconWidth() != 24 || imageIcon.getIconHeight() != 24) {
                        // Rescale if not already the right size
                        Image img = imageIcon.getImage();
                        Image scaledImg = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaledImg);
                    }
                }
                return skillIcon;
            }
            return getResourceIcon("skill_icon.png", 24, 24); // Fallback with proper sizing
        } else if (condition instanceof TimeCondition) {
            return getResourceIcon("old-watch.png", 24, 24);
        } else if (condition instanceof LootItemCondition) {
            return getResourceIcon("item_icon.png", 24, 24);
        } else if (condition instanceof TimeWindowCondition) {
            return getResourceIcon("calendar_icon.png", 24, 24);
        } else if (condition instanceof DayOfWeekCondition) {
            return getResourceIcon("day_icon.png", 24, 24);
        } else if (condition instanceof AndCondition) {
            return getResourceIcon("logic-gate-and.png", 24, 24);
        } else if (condition instanceof OrCondition) {
            return getResourceIcon("logic-gate-or.png", 24, 24);
        } 
        
        return UIManager.getIcon("Tree.leafIcon");
    }

    /**
     * Loads and scales an icon from resources
     */
    private Icon getResourceIcon(String name, int width, int height) {
        try {
            BufferedImage originalImage = ImageIO.read(getClass().getResource("/net/runelite/client/plugins/microbot/pluginscheduler/" + name));
            if (originalImage == null) {
                return UIManager.getIcon("Tree.leafIcon");
            }
            
            if (originalImage.getWidth() == width && originalImage.getHeight() == height) {
                return new ImageIcon(originalImage);
            }
            
            // Scale the image to desired size
            BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            
            // Use high quality scaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.drawImage(originalImage, 0, 0, width, height, null);
            g2d.dispose();
            
            return new ImageIcon(scaledImage);
        } catch (Exception e) {
            // Fallback to default icon
            return UIManager.getIcon("Tree.leafIcon");
        }
    }

    /**
     * Convenience method for default 24x24 icons
     */
    private Icon getResourceIcon(String name) {
        return getResourceIcon(name, 24, 24);
    }
}
