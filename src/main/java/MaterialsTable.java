import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MaterialsTable extends JFrame {

    public MaterialsTable(List<String> stages) {
        //Проверяем, есть ли доступные этапы
        if (DrillingProgram.getDrillingStages().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Нет доступных этапов бурения. Сначала добавьте этапы в программу бурения.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
        initComponents(stages);
    }

    private void initComponents(List<String> stages) {
        setTitle("Таблица материалов");
        setSize(1500, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //  1. Создаём модель со столбцами
        if (DrillingProgram.materialsModel == null) {
            String[] columns = {
                    "Наименование этапа",                   //0
                    "Наименование материала",               //1
                    "Вид материала",                        //2
                    "Требуемый объём материала",            //3
                    "Объём единицы груза",                  //4
                    "Площадь единицы груза",                //5
                    "Количество единиц груза",              //6
                    "Требуемая площадь груза",              //7
                    "Специальные требования",               //8
                    "Возможность хранения в одной ёмкости"  //9
            };

            // 2. Инициализируем модель (если она еще не создана)
            DrillingProgram.materialsModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // Блокируем вычисляемые столбцы
                    return column != 7;
                }
            };
        }

        // 3. Создаем таблицу с моделью
        JTable materialsTable = new JTable(DrillingProgram.materialsModel);

        //Добавим рендерер для неактивных ячеек
        materialsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String materialType = (String) table.getValueAt(row, 2);
                boolean isInContainer = "В таре".equals(materialType);

                if ((column == 4 || column == 5 || column == 6 || column == 7) && !isInContainer) {
                    c.setBackground(Color.LIGHT_GRAY);
                    c.setForeground(Color.GRAY);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        });

        //Установка слушателя изменений модели
        DrillingProgram.materialsModel.addTableModelListener(e -> {
            // 1. При изменении вида материала (столбец 2)
            if (e.getColumn() == 2) {
                updateColumnEditableState(materialsTable);
            }
            // 2. При изменении объема/площади/количества (столбцы 4-6)
            if (e.getColumn() >= 4 && e.getColumn() <= 6) {
                calculateTotals(e.getFirstRow());
            }
        });

        //Проверка перед редактированием
        materialsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = materialsTable.columnAtPoint(e.getPoint());
                if (col == 0) { // Для столбца "Наименование этапа"
                    if (DrillingProgram.getDrillingStages().isEmpty()) {
                        JOptionPane.showMessageDialog(MaterialsTable.this,
                                "Сначала добавьте этапы в программу бурения!",
                                "Ошибка", JOptionPane.WARNING_MESSAGE);
                        e.consume(); // Отменяем событие
                    }
                }
            }
        });

        setupCellEditors(materialsTable);

        // 4. Настройка интерфейса
        JScrollPane scrollPane = new JScrollPane(materialsTable);
        add(scrollPane, BorderLayout.CENTER);

        // 5. Кнопка добавления материала
        JButton addButton = new JButton("Добавить материал");
        addButton.addActionListener(e -> addMaterialRow());


        // 6. Добавляем кнопку возврата
        JButton backButton = new JButton("Вернуться к таблице \"Программа бурения\"");
        backButton.addActionListener(e -> {
            this.setVisible(false);
            DrillingProgram.returnToDrillingTable();
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupCellEditors(JTable materialsTable) {
        //1. Создаём специальный редактор для этапов
        class StageEditor extends AbstractCellEditor implements TableCellEditor {
            private JComboBox<String> comboBox;

            public StageEditor() {
                comboBox = new JComboBox<>(DrillingProgram.getDrillingStagesVector());
                comboBox.setEditable(false);

                // Обновляем список при каждом открытии
                comboBox.addPopupMenuListener(new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        comboBox.removeAllItems();
                        Vector<String> stages = DrillingProgram.getDrillingStagesVector();
                        for (String stage : stages) {
                            comboBox.addItem(stage);
                        }
                    }

                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    }

                    @Override
                    public void popupMenuCanceled(PopupMenuEvent e) {
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                         boolean isSelected, int row, int column) {
                comboBox.setSelectedItem(value);
                return comboBox;
            }

            @Override
            public Object getCellEditorValue() {
                return comboBox.getSelectedItem();
            }
        }

        materialsTable.getColumnModel().getColumn(0).setCellEditor(new StageEditor());
        //new DefaultCellEditor(new JComboBox<>(drillingStages.toArray(new String[0]))));

        // 2. Комбобокс для вида материала (по индексу)
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Сыпучий", "Жидкий", "В таре"});
        typeCombo.addActionListener(e -> updateColumnEditableState(materialsTable));
        materialsTable.getColumnModel().getColumn(2).setCellEditor(
                new DefaultCellEditor(typeCombo));

        //3.Числовые редакторы
        materialsTable.getColumnModel().getColumn(3).setCellEditor(new NumericCellEditor()); // Требуемый объём
        materialsTable.getColumnModel().getColumn(4).setCellEditor(new NumericCellEditor()); // Объём единицы
        materialsTable.getColumnModel().getColumn(5).setCellEditor(new NumericCellEditor()); // Площадь единицы
        materialsTable.getColumnModel().getColumn(6).setCellEditor(new IntegerCellEditor()); // Количество

        // 4. Комбобокс для спецтребований (по индексу)
        JComboBox<String> specCombo = new JComboBox<>(new String[]{"", "Радиоактивность", "Взрывоопасность", "Фонтанная арматура"});
        materialsTable.getColumnModel().getColumn(8).setCellEditor(
                new DefaultCellEditor(specCombo));

        // 5. Комбобокс для хранения (по индексу)
        JComboBox<String> storageCombo = new JComboBox<>(new String[]{"Да", "Нет"});
        materialsTable.getColumnModel().getColumn(9).setCellEditor(
                new DefaultCellEditor(storageCombo));

        // Слушатель изменений для автоматических расчетов
        /*DrillingProgram.materialsModel.addTableModelListener(e -> {
            if (e.getColumn() == 4 || e.getColumn() == 5 || e.getColumn() == 6) {
                calculateTotals(e.getFirstRow());
            }
        });*/
    } /*catch (Exception e) {
            System.err.println("Ошибка настройки редакторов: " + e.getMessage());
            e.printStackTrace();*/

    private void calculateTotals(int row) {
        try {
            String materialType = (String) DrillingProgram.materialsModel.getValueAt(row, 2);
            if ("В таре".equals(materialType)) {
                //double volume = Double.parseDouble(DrillingProgram.materialsModel.getValueAt(row, 4).toString());
                double area = Double.parseDouble(DrillingProgram.materialsModel.getValueAt(row, 5).toString());
                int count = Integer.parseInt(DrillingProgram.materialsModel.getValueAt(row, 6).toString());

                //DrillingProgram.materialsModel.setValueAt(volume * count, row, 7); //Общий объём
                DrillingProgram.materialsModel.setValueAt(area * count, row, 7); // Общая площадь
            } else {
                DrillingProgram.materialsModel.setValueAt(null, row, 7);
            }
        } catch (Exception ignored) {
            DrillingProgram.materialsModel.setValueAt(null, row, 7);
            //Игнорирование ошибок парсинга
        }
    }

    private void updateColumnEditableState(JTable table) {
        for (int row = 0; row < DrillingProgram.materialsModel.getRowCount(); row++) {
            String materialType = (String) DrillingProgram.materialsModel.getValueAt(row, 2);
            boolean isInContainer = "В таре".equals(materialType);

            if (!isInContainer) {
                // Очищаем ячейки для "Сыпучих"/"Жидких" материалов
                table.setValueAt(null, row, 4); // Объём единицы
                table.setValueAt(null, row, 5); // Площадь единицы
                table.setValueAt(null, row, 6); // Количество
                table.setValueAt(null, row, 7); // Общая площадь
            }
        }


        //Обновляем отображение
        DrillingProgram.materialsModel.fireTableDataChanged();
    }


    private void addMaterialRow() {
        //Добавляем строку с значениями по умолчанию
        DrillingProgram.materialsModel.addRow(new Object[]{
                DrillingProgram.drillingStages.isEmpty() ? "" : DrillingProgram.drillingStages.get(0), // !!! Сделал drillingStages публичным и статичным. Первый этап по умолчанию
                "",         //Наименование материала
                "Сыпучий",  //Вид материала
                0.0,        //Требуемый объём
                null,        //Объём единицы
                null,        //Площадь единицы
                null,          //Количество
                //0.0,        //Общий объём (авторасчёт)
                null,        //Требуемая площадь (авторасчёт)
                "",         //Специальные требования
                "Да"        //Возможность хранения в одной таре с другими материалами
        });
    }

    // Классы для числовых редакторов
    static class NumericCellEditor extends DefaultCellEditor {
        public NumericCellEditor() {
            super(new JTextField());
            ((JTextField) getComponent()).setHorizontalAlignment(JTextField.RIGHT);
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