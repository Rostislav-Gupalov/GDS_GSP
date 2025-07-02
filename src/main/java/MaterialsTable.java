import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MaterialsTable extends JFrame {
    private JTable materialsTable;
    private DefaultTableModel materialsModel;
    private List<String> drillingStages;

    public MaterialsTable(List<String> stages) {
        this.drillingStages = stages;
        setTitle("Таблица материалов");
        setSize(1500, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
    }
    private void initComponents() {
        // Столбцы таблицы
        if (DrillingProgram.materialsModel == null) {
            String[] columns = {
                    "Наименование этапа", "Наименование материала", "Вид материала",
                    "Требуемый объём материала", "Объём единицы груза", "Площадь единицы груза",
                    "Количество единиц груза", "Общий объём груза", "Общая площадь груза",
                    "Специальные требования", "Возможность хранения в одной ёмкости"
            };

            materialsModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // Блокируем вычисляемые столбцы
                    return column != 7 && column != 8;
                }
            };
        }


        materialsTable = new JTable(DrillingProgram.materialsModel);
        setupCellEditors();

        JScrollPane scrollPane = new JScrollPane(materialsTable);
        add(scrollPane, BorderLayout.CENTER);
        // Кнопка добавления материала
        JButton addButton = new JButton("Добавить материал");
        addButton.addActionListener(e -> addMaterialRow());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addButton);
        add(buttonPanel, BorderLayout.SOUTH);
        // Добавляем кнопку возврата
        JButton backButton = new JButton("Вернуться к таблице \"Программа бурения\"");
        backButton.addActionListener(e -> {
            this.setVisible(false);
            DrillingProgram.returnToDrillingTable();
        });

        /*JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);*/
    }

    private void setupCellEditors() {
        // 1. Комбобокс для этапов бурения
        materialsTable.getColumn("Наименование этапа").setCellEditor(
                new DefaultCellEditor(new JComboBox<>(drillingStages.toArray(new String[0]))));

        // 2. Комбобокс для вида материала
        materialsTable.getColumn("Вид материала").setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[]{"Сыпучий", "Жидкий", "В таре"})));
// 3. Комбобокс для спецтребований
        materialsTable.getColumn("Специальные требования").setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[]{"", "Радиоактивность", "Взрывоопасность", "Фонтанная арматура"})));

        // 4. Комбобокс для хранения
        materialsTable.getColumn("Возможность хранения в одной ёмкости").setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[]{"Да", "Нет"})));

        // Валидация для числовых полей
        materialsTable.getColumn("Требуемый объём материала").setCellEditor(new NumericCellEditor());
        materialsTable.getColumn("Объём единицы груза").setCellEditor(new NumericCellEditor());
        materialsTable.getColumn("Площадь единицы груза").setCellEditor(new NumericCellEditor());
        materialsTable.getColumn("Количество единиц груза").setCellEditor(new IntegerCellEditor());
        // Слушатель изменений для автоматических расчетов
        materialsModel.addTableModelListener(e -> {
            if (e.getColumn() == 4 || e.getColumn() == 5 || e.getColumn() == 6) {
                calculateTotals(e.getFirstRow());
            }
        });
    }

    private void calculateTotals(int row) {
        try {
            String materialType = (String) materialsModel.getValueAt(row, 2);
            if ("В таре".equals(materialType)) {
                double volume = Double.parseDouble(materialsModel.getValueAt(row, 4).toString());
                double area = Double.parseDouble(materialsModel.getValueAt(row, 5).toString());
                int count = Integer.parseInt(materialsModel.getValueAt(row, 6).toString());

                materialsModel.setValueAt(volume * count, row, 7);
                materialsModel.setValueAt(area * count, row, 8);
            }
        } catch (Exception ignored) {}
    }
    private void addMaterialRow() {
        materialsModel.addRow(new Object[]{
                drillingStages.isEmpty() ? "" : drillingStages.get(0), // Первый этап по умолчанию
                "", "", 0.0, 0.0, 0.0, 0, 0.0, 0.0, "", "Да"
        });
    }

    // Кастомный редактор для чисел
    static class NumericCellEditor extends DefaultCellEditor {
        public NumericCellEditor() {
            super(new JTextField());
            ((JTextField)getComponent()).setHorizontalAlignment(JTextField.RIGHT);
        }

        @Override
        public boolean stopCellEditing() {
            try {
                Double.parseDouble(getCellEditorValue().toString());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Введите число!");
                return false;
            }
            return super.stopCellEditing();
        }
    }
    // Кастомный редактор для целых чисел
    static class IntegerCellEditor extends NumericCellEditor {
        @Override
        public boolean stopCellEditing() {
            try {
                Integer.parseInt(getCellEditorValue().toString());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Введите целое число!");
                return false;
            }
            return super.stopCellEditing();
        }
    }
}