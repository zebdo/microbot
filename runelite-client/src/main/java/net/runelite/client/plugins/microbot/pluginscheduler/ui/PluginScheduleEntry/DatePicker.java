package net.runelite.client.plugins.microbot.pluginscheduler.ui.PluginScheduleEntry;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.util.Calendar;

/**
 * A simple date picker component that wraps a JSpinner with a SpinnerDateModel.
 */
public class DatePicker extends JSpinner {
    
    /**
     * Creates a new DatePicker with the current date selected
     */
    public DatePicker() {
        super(new SpinnerDateModel(
            new Date(), null, null, Calendar.DAY_OF_MONTH));
        
        // Format the spinner to show only the date
        JSpinner.DateEditor editor = new JSpinner.DateEditor(this, "yyyy-MM-dd");
        setEditor(editor);
    }
    
    /**
     * Sets the date of this date picker
     * 
     * @param localDate The local date to set
     */
    public void setDate(LocalDate localDate) {
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        setValue(date);
    }
    
    /**
     * Gets the selected date as a LocalDate
     * 
     * @return The selected LocalDate
     */
    public LocalDate getDate() {
        Date date = (Date) getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}