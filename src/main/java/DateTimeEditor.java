import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeEditor extends DefaultCellEditor {
    private JPanel panel;
    private JXDatePicker datePicker;
    private JSpinner timeSpinner;

    public DateTimeEditor() {
        super(new JTextField());
        panel = new JPanel(new BorderLayout());

        // Настройка выбора даты
        datePicker = new JXDatePicker(new Date());
        datePicker.setFormats(new SimpleDateFormat("dd/MM/yyyy"));

        // Настройка выбора времени (только часы, с шагом 1 час)
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeModel.setCalendarField(Calendar.HOUR_OF_DAY);
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:00");
        timeSpinner.setEditor(timeEditor);

        panel.add(datePicker, BorderLayout.WEST);
        panel.add(timeSpinner, BorderLayout.EAST);

        // Фиксируем размер
        panel.setPreferredSize(new Dimension(300, 30));
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:00 dd/MM/yyyy");
                Date date = sdf.parse(value.toString());
                datePicker.setDate(date);
                timeSpinner.setValue(date);
            } catch (ParseException e) {
                datePicker.setDate(new Date());
                timeSpinner.setValue(new Date());
            }
        }
        return panel;

    }
    @Override
    public Object getCellEditorValue() {
        Date date = datePicker.getDate();
        Date time = (Date) timeSpinner.getValue();

        // Объединяем дату и время
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);

        Calendar calTime = Calendar.getInstance();
        calTime.setTime(time);

        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:00 dd/MM/yyyy");
        return sdf.format(calDate.getTime());
    }
}
