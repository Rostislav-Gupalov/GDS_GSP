import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DrillingProgram extends JFrame {
    private JTable drillingTable;
    private DefaultTableModel drillingModel;
    public static DefaultTableModel materialsModel;
    private JButton addButton;
    public static List<String> drillingStages = new ArrayList<>();
    private static DrillingProgram instance;

    public DrillingProgram() {
        instance = this;
        initComponents();
    }

    private void initComponents() {
        setTitle("Программа бурения");
        setSize(1000, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Инициализация модели (если она ещё не создана)
        if (drillingModel == null) {
            String[] columns = {"Наименование этапа", "Начало этапа", "Конец этапа", "Длительность этапа"};
            drillingModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 3; // Запрещаем редактирование столбца "Длительность"
                }
            };
            drillingTable = new JTable(drillingModel);
            JScrollPane scrollPane = new JScrollPane(drillingTable);
            add(scrollPane, BorderLayout.CENTER);

            //Настройка редакторов для столбцов с датой
            drillingTable.getColumnModel().getColumn(1).setCellEditor(new DateTimeEditor()); // Для "Начало этапа"
            drillingTable.getColumnModel().getColumn(2).setCellEditor(new DateTimeEditor()); // Для "Конец этапа"

            //Инициализация списка этапов
            updateDrillingStages();

            // Кнопка добавления этапа
            addButton = new JButton("Добавить этап");
            addButton.addActionListener(this::addNewRow);

            // Кнопка перехода к таблице "Материалы
            JButton nextButton = new JButton("Перейти к таблице \"Материалы\"");
            nextButton.addActionListener(e -> openMaterialsTable());

            // Панель с двумя кнопками
            JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
            buttonPanel.add(addButton);
            buttonPanel.add(nextButton);
            add(buttonPanel, BorderLayout.SOUTH);

            // Валидация данных при редактировании
            drillingModel.addTableModelListener(e ->
            {
                if (e.getColumn() == 1 || e.getColumn() == 2) {
                    validateDates(e.getFirstRow());
                }
            });
        }
    }

    private void addNewRow(ActionEvent e) {
        int selectedRow = drillingTable.getSelectedRow();
        int insertRow = (selectedRow == -1) ? drillingModel.getRowCount() : selectedRow + 1;

        // 1. Автозаполнение даты начала из предыдущей строки
        String startDate = "";
        if (insertRow > 0) {  // Если есть предыдущая строка
            Object prevEndDate = drillingModel.getValueAt(insertRow - 1, 2);  // Берем "Конец этапа" из предыдущей строки
            if (prevEndDate != null) {
                startDate = prevEndDate.toString();
            }
        }

        // 2. Вставляем новую строку с предзаполненным началом
        drillingModel.insertRow(insertRow, new Object[]{
                "Новый этап",
                startDate,  // Начало = концу предыдущего этапа
                "",         // Конец пока пустой
                ""          // Длительность пустая
        });

        // 3. Обновляем список этапов
        updateDrillingStages();

        // 4. Выделяем новую строку
        drillingTable.setRowSelectionInterval(insertRow, insertRow);

        // 5. Автоматически открываем редактор для даты окончания
        if (!startDate.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                drillingTable.editCellAt(insertRow, 2);  // Фокус на столбец "Конец этапа"
                Component editor = drillingTable.getEditorComponent();
                if (editor != null) {
                    editor.requestFocusInWindow();
                }
            });
        }
    }

    private void updateDrillingStages() {
        drillingStages.clear();
        for (int i = 0; i < drillingModel.getRowCount(); i++) {
            drillingStages.add(drillingModel.getValueAt(i, 0).toString());
        }
    }

    private void openMaterialsTable() {
        if (drillingStages.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Добавьте хотя бы один этап бурения!",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.setVisible(false);
        new MaterialsTable(drillingStages).setVisible(true);
        //this.dispose(); // Закрываем текущее окно
    }
    /*private void openMaterialsTable() {
        List<String> stages = new ArrayList<>();
        for (int i = 0; i < drillingModel.getRowCount(); i++) {
            stages.add(drillingModel.getValueAt(i, 0).toString());
        }

        this.setVisible(false); // Скрываем текущее окно вместо dispose()
        new MaterialsTable(stages).setVisible(true);
    }*/

    public static void returnToDrillingTable() {
        if (instance != null) {
            instance.setVisible(true);
        } else {
            new DrillingProgram().setVisible(true);
        }
    }

    private void validateDates(int row) {
        //Date endDate = null;
        //Date startDate = null;
        try {
            /*// 1. Получаем значения из модели (уже в формате "HH:00 dd/MM/yyyy")
            Object startValue = drillingModel.getValueAt(row, 1);
            Object endValue = drillingModel.getValueAt(row, 2);

            // 2. Проверяем, что ячейки не пустые
            if (startValue == null || endValue == null ||
                    startValue.toString().isEmpty() || endValue.toString().isEmpty()) {
                return;
            }*/

            // 1. Парсим даты (формат остался прежним)
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            Date startDate = sdf.parse(drillingModel.getValueAt(row, 1).toString());
            Date endDate = sdf.parse(drillingModel.getValueAt(row, 2).toString());

            // 2. Существующие проверки (без изменений)
            if (startDate.after(endDate)) {
                JOptionPane.showMessageDialog(this, "Ошибка: Начало этапа не может быть позже конца этапа!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                drillingModel.setValueAt("", row, 2);
                return;
            }

            // 3.Расчет длительности с минутами
            long durationMillis = endDate.getTime() - startDate.getTime();
            long days = durationMillis / (1000 * 60 * 60 * 24);
            long hours = (durationMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
            long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
            drillingModel.setValueAt(days + " суток " + hours + " часов " + minutes + " минут", row, 3);

            // 4. Проверка пересечения с другими этапами (без изменений)
            if (row < drillingModel.getRowCount() - 1) {
                Date nextStartDate = sdf.parse(drillingModel.getValueAt(row + 1, 1).toString());
                if (endDate.after(nextStartDate)) {
                    JOptionPane.showMessageDialog(this, "Ошибка: Следующий этап не может начинаться раньше текущего!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    drillingModel.setValueAt("", row, 2);
                }
            }
        } catch (Exception ex) {
            // Игнорируем ошибки парсинга
        }
    }
}
