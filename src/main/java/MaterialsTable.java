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
        //  1. Создаём модель со столбцами
        //if (DrillingProgram.materialsModel == null) {
            String[] columns = {
                    "Наименование этапа",                   //0
                    "Наименование материала",               //1
                    "Вид материала",                        //2
                    "Требуемый объём материала",            //3
                    "Объём единицы груза",                  //4
                    "Площадь единицы груза",                //5
                    "Количество единиц груза",              //6
                    "Общий объём груза",                    //7
                    "Общая площадь груза",                  //8
                    "Специальные требования",               //9
                    "Возможность хранения в одной ёмкости"  //10
            };

            // 2. Инициализируем модель (если она еще не создана)
            materialsModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // Блокируем вычисляемые столбцы
                    return column != 7 && column != 8;
                }
            };

        // 3. Создаем таблицу с моделью
        materialsTable = new JTable(materialsModel);
        setupCellEditors();

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

    private void setupCellEditors() {
        // 1. Проверяем, что столбцы существуют
        if (materialsTable.getColumnCount() == 0) {
            System.err.println("Ошибка: таблица не содержит столбцов!");
            return;
        }

        // 2. Настраиваем редакторы ТОЛЬКО если столбцы есть
        try {
            // Получаем индексы столбцов по их модельным именам
            /*int stageCol = 0;    // "Наименование этапа"
            int typeCol = 2;     // "Вид материала"
            int specCol = 9;     // "Специальные требования"
            int storageCol = 10; // "Возможность хранения"*/


            // 1. Комбобокс для этапов бурения (столбец 0)
            //if (stageCol < materialsTable.getColumnCount()) {
            JComboBox<String> stageCombo = new JComboBox<>(drillingStages.toArray(new String[0]));
            materialsTable.getColumnModel().getColumn(0).setCellEditor(
                    //new DefaultCellEditor(new JComboBox<>(drillingStages.toArray(new String[0]))));
                    new DefaultCellEditor(stageCombo));
            // 2. Комбобокс для вида материала (по индексу)
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Сыпучий", "Жидкий", "В таре"});
            materialsTable.getColumnModel().getColumn(2).setCellEditor(
                    new DefaultCellEditor(typeCombo));

            //3.Числовые редакторы
            materialsTable.getColumnModel().getColumn(3).setCellEditor(new NumericCellEditor()); // Требуемый объём
            materialsTable.getColumnModel().getColumn(4).setCellEditor(new NumericCellEditor()); // Объём единицы
            materialsTable.getColumnModel().getColumn(5).setCellEditor(new NumericCellEditor()); // Площадь единицы
            materialsTable.getColumnModel().getColumn(6).setCellEditor(new IntegerCellEditor()); // Количество

            // 4. Комбобокс для спецтребований (по индексу)
            JComboBox<String> specCombo = new JComboBox<>(new String[]{"", "Радиоактивность", "Взрывоопасность", "Фонтанная арматура"});
            materialsTable.getColumnModel().getColumn(9).setCellEditor(
                    new DefaultCellEditor(specCombo));

            // 5. Комбобокс для хранения (по индексу)
            JComboBox<String> storageCombo = new JComboBox<>(new String[]{"Да", "Нет"});
            materialsTable.getColumnModel().getColumn(10).setCellEditor(
                    new DefaultCellEditor(storageCombo));

            // Слушатель изменений для автоматических расчетов
            materialsModel.addTableModelListener(e -> {
                if (e.getColumn() == 4 || e.getColumn() == 5 || e.getColumn() == 6) {
                    calculateTotals(e.getFirstRow());
                }
            });
        } catch (Exception e) {
            System.err.println("Ошибка настройки редакторов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateTotals(int row) {
        try {
            String materialType = (String) materialsModel.getValueAt(row, 2);
            if ("В таре".equals(materialType)) {
                double volume = Double.parseDouble(materialsModel.getValueAt(row, 4).toString());
                double area = Double.parseDouble(materialsModel.getValueAt(row, 5).toString());
                int count = Integer.parseInt(materialsModel.getValueAt(row, 6).toString());

                materialsModel.setValueAt(volume * count, row, 7); //Общий объём
                materialsModel.setValueAt(area * count, row, 8); // Общая площадь
            }
        } catch (Exception ignored) {
            //Игнорирование ошибок парсинга
        }
    }

    private void addMaterialRow() {
        //Добавляем строку с значениями по умолчанию
        materialsModel.addRow(new Object[]{
                drillingStages.isEmpty() ? "" : drillingStages.get(0), // Первый этап по умолчанию
                "",         //Наименование материала
                "Сыпучий",  //Вид материала
                0.0,        //Требуемый объём
                0.0,        //Объём единицы
                0.0,        //Площадь единицы
                0,          //Количество
                0.0,        //Общий объём (авторасчёт)
                0.0,        //Общая площадь (авторасчёт)
                "",         //Специальные требования
                "Да"        //Возможность хранения в одной таре с другими материалами
        });
    }

    // Классы для числовых редакторов
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