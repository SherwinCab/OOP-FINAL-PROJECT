import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Window version of BiyaheBudget (Java Swing). It uses the same classes as the console version.
public class BiyaheBudgetGUI extends JFrame {
    private static final Color BLUE = new Color(33, 120, 230);
    private static final Color PAGE = new Color(243, 245, 249);
    private static final Color CARD_LINE = new Color(215, 220, 228);
    private static final Color GREEN = new Color(30, 130, 60);
    private static final Color ORANGE = new Color(214, 130, 20);
    private static final Color RED = new Color(190, 40, 40);

    private final TripManager manager = new TripManager();

    // One drop-off stop that the user added: where, how far, and who gets off
    static class StopEntry {
        final String place;
        final double km;
        final int regular;
        final int student;

        StopEntry(String place, double km, int regular, int student) {
            this.place = place;
            this.km = km;
            this.regular = regular;
            this.student = student;
        }

        int count() {
            return regular + student;
        }
    }

    private final List<StopEntry> entries = new ArrayList<>();
    private Trip pendingTrip = null;   // computed but not saved yet

    // Compute trip tab
    private final JTextField originField = new JTextField();
    private final JRadioButton jeepneyRadio = new JRadioButton("Jeepney", true);
    private final JRadioButton tricycleRadio = new JRadioButton("Tricycle");
    private final SegButton individualBtn = new SegButton("Individual");
    private final SegButton duoBtn = new SegButton("Duo");
    private final SegButton groupBtn = new SegButton("Group");
    private final JSpinner groupSpinner = new JSpinner(new SpinnerNumberModel(3, 3, 50, 1));
    private final DefaultTableModel stopModel = new DefaultTableModel(new Object[]{"#", "Place", "Distance", "Passengers"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable stopTable = new JTable(stopModel);
    private final JLabel placedLabel = new JLabel();
    private final JTextArea receiptArea = new JTextArea();
    private final JLabel totalCaption = new JLabel("Total");
    private final JLabel totalLabel = new JLabel("PHP 0.00");
    private final PillButton saveButton = new PillButton("Save trip", true);

    // Trip history tab
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"No.", "Ride type", "Transport", "From", "Passengers", "Fare"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyModel);
    private final JLabel historyTotal = new JLabel("Total spent: PHP 0.00");
    private final JTextArea historyDetail = new JTextArea();

    // Weekly budget (bottom bar)
    private final JLabel budgetInfo = new JLabel();
    private final JLabel budgetStatus = new JLabel();
    private final BudgetBar budgetBar = new BudgetBar();

    public BiyaheBudgetGUI() {
        super("BiyaheBudget");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(PAGE);
        setLayout(new BorderLayout(0, 10));

        JLabel title = new JLabel("BiyaheBudget");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(BLUE);
        title.setBorder(new EmptyBorder(12, 18, 0, 18));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Compute trip", buildComputeTab());
        tabs.addTab("Trip history", buildHistoryTab());
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 18, 0, 18));
        center.add(tabs);
        add(center, BorderLayout.CENTER);
        add(buildBudgetBar(), BorderLayout.SOUTH);

        individualBtn.setSelected(true);
        onTotalChanged();
        invalidateResult();
        refreshHistory();
        refreshBudget();
        setSize(1020, 740);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
    }

    // ================= Compute trip tab =================

    private JPanel buildComputeTab() {
        JPanel tab = new JPanel(new GridBagLayout());
        tab.setBackground(PAGE);
        tab.setBorder(new EmptyBorder(12, 0, 12, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0; c.weightx = 0.62; c.insets = new Insets(0, 0, 0, 8);
        tab.add(buildTripDetailsCard(), c);
        c.gridx = 1; c.weightx = 0.38; c.insets = new Insets(0, 8, 0, 0);
        tab.add(buildReceiptCard(), c);
        return tab;
    }

    private JPanel card() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(CARD_LINE), new EmptyBorder(14, 16, 14, 16)));
        return p;
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        return l;
    }

    private JLabel small(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.GRAY);
        l.setFont(l.getFont().deriveFont(12f));
        return l;
    }

    private JPanel buildTripDetailsCard() {
        JPanel card = card();

        // ---- form ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 0, 3, 14);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; form.add(small("Starting point"), c);
        c.gridx = 1; form.add(small("Transport"), c);
        c.gridx = 0; c.gridy = 1; form.add(originField, c);

        JPanel transportRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        transportRow.setOpaque(false);
        jeepneyRadio.setOpaque(false);
        tricycleRadio.setOpaque(false);
        ButtonGroup transportGroup = new ButtonGroup();
        transportGroup.add(jeepneyRadio);
        transportGroup.add(tricycleRadio);
        transportRow.add(jeepneyRadio);
        transportRow.add(tricycleRadio);
        c.gridx = 1; form.add(transportRow, c);

        c.gridx = 0; c.gridy = 2; form.add(small("Ride type"), c);
        c.gridx = 1; form.add(small("Group size (3 or more)"), c);

        JPanel rideRow = new JPanel(new GridLayout(1, 3, 0, 0));
        rideRow.setOpaque(false);
        ButtonGroup rideGroup = new ButtonGroup();
        rideGroup.add(individualBtn);
        rideGroup.add(duoBtn);
        rideGroup.add(groupBtn);
        rideRow.add(individualBtn);
        rideRow.add(duoBtn);
        rideRow.add(groupBtn);
        c.gridx = 0; c.gridy = 3; form.add(rideRow, c);
        c.gridx = 1; form.add(groupSpinner, c);

        // ---- stops table ----
        stopTable.setRowHeight(28);
        stopTable.setShowGrid(false);
        stopTable.setIntercellSpacing(new Dimension(0, 1));
        stopTable.getColumnModel().getColumn(0).setMaxWidth(36);
        stopTable.getColumnModel().getColumn(2).setMaxWidth(90);
        stopTable.getColumnModel().getColumn(3).setPreferredWidth(220);
        JScrollPane tableScroll = new JScrollPane(stopTable);
        tableScroll.setBorder(new LineBorder(CARD_LINE));
        tableScroll.getViewport().setBackground(Color.WHITE);

        JPanel stopsPanel = new JPanel(new BorderLayout(0, 6));
        stopsPanel.setOpaque(false);
        stopsPanel.add(heading("Stops and passengers"), BorderLayout.NORTH);
        stopsPanel.add(tableScroll, BorderLayout.CENTER);

        // ---- buttons ----
        PillButton addStop = new PillButton("+ Add stop", false);
        PillButton removeStop = new PillButton("Remove stop", false);
        PillButton compute = new PillButton("Compute fare", true);
        addStop.addActionListener(e -> addStop());
        removeStop.addActionListener(e -> removeStop());
        compute.addActionListener(e -> computeFare());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(addStop);
        left.add(removeStop);
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(left, BorderLayout.WEST);
        buttons.add(compute, BorderLayout.EAST);

        placedLabel.setForeground(Color.GRAY);
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(placedLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);
        stopsPanel.add(south, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(heading("Trip details"), BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(stopsPanel, BorderLayout.CENTER);

        // ---- listeners ----
        originField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { invalidateResult(); }
            public void removeUpdate(DocumentEvent e) { invalidateResult(); }
            public void changedUpdate(DocumentEvent e) { invalidateResult(); }
        });
        jeepneyRadio.addActionListener(e -> invalidateResult());
        tricycleRadio.addActionListener(e -> invalidateResult());
        individualBtn.addActionListener(e -> onTotalChanged());
        duoBtn.addActionListener(e -> onTotalChanged());
        groupBtn.addActionListener(e -> onTotalChanged());
        groupSpinner.addChangeListener(e -> onTotalChanged());
        return card;
    }

    private JPanel buildReceiptCard() {
        JPanel card = card();
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        receiptArea.setOpaque(false);
        receiptArea.setBorder(new EmptyBorder(4, 0, 4, 0));
        JScrollPane scroll = new JScrollPane(receiptArea);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        totalCaption.setForeground(Color.GRAY);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 28f));
        totalLabel.setForeground(BLUE);
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.add(totalCaption, BorderLayout.WEST);
        totalRow.add(totalLabel, BorderLayout.EAST);

        saveButton.addActionListener(e -> saveTrip());
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(totalRow, BorderLayout.NORTH);
        south.add(saveButton, BorderLayout.SOUTH);

        card.add(heading("Fare receipt"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private int totalPassengers() {
        if (individualBtn.isSelected()) {
            return 1;
        }
        if (duoBtn.isSelected()) {
            return 2;
        }
        return (Integer) groupSpinner.getValue();
    }

    private int placed() {
        int n = 0;
        for (StopEntry s : entries) {
            n += s.count();
        }
        return n;
    }

    private void onTotalChanged() {
        groupSpinner.setEnabled(groupBtn.isSelected());
        if (placed() > totalPassengers()) {
            entries.clear();   // too many placed for the new size: start the stops again
        }
        refreshStops();
        invalidateResult();
    }

    private static String formatKm(double km) {
        return (km == Math.floor(km) ? String.valueOf((int) km) : String.valueOf(km)) + " km";
    }

    private void refreshStops() {
        stopModel.setRowCount(0);
        int no = 1;
        for (StopEntry s : entries) {
            List<String> types = new ArrayList<>();
            for (int i = 0; i < s.regular; i++) {
                types.add("Regular");
            }
            for (int i = 0; i < s.student; i++) {
                types.add("Student");
            }
            stopModel.addRow(new Object[]{no, s.place, formatKm(s.km), String.join(", ", types)});
            no++;
        }
        int total = totalPassengers();
        placedLabel.setText(placed() + " of " + total + " passengers placed");
        placedLabel.setForeground(placed() == total ? GREEN : Color.GRAY);
        totalCaption.setText("Total for " + total + " passenger" + (total == 1 ? "" : "s"));
    }

    // Opens a small form: where they get off, how far, and how many Regular / Student
    private void addStop() {
        int remaining = totalPassengers() - placed();
        if (remaining <= 0) {
            JOptionPane.showMessageDialog(this, "All passengers are already placed.", "Add stop", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTextField placeField = new JTextField(14);
        JTextField kmField = new JTextField(6);
        JSpinner regularSpin = new JSpinner(new SpinnerNumberModel(0, 0, remaining, 1));
        JSpinner studentSpin = new JSpinner(new SpinnerNumberModel(0, 0, remaining, 1));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Place where they get off:"));
        form.add(placeField);
        form.add(new JLabel("Distance from start (km):"));
        form.add(kmField);
        form.add(new JLabel("Regular passengers:"));
        form.add(regularSpin);
        form.add(new JLabel("Student passengers:"));
        form.add(studentSpin);

        while (true) {
            int answer = JOptionPane.showConfirmDialog(this, form,
                    "Add stop (" + remaining + " passenger" + (remaining == 1 ? "" : "s") + " left)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            String place = placeField.getText().trim();
            int regular = (Integer) regularSpin.getValue();
            int student = (Integer) studentSpin.getValue();
            double km = 0;
            String problem = null;

            if (place.isEmpty()) {
                problem = "Please enter where they get off.";
            } else {
                try {
                    km = Double.parseDouble(kmField.getText().trim());
                    if (km <= 0) {
                        problem = "Distance must be more than 0.";
                    }
                } catch (NumberFormatException e) {
                    problem = "Distance must be a number.";
                }
            }
            if (problem == null) {
                if (regular + student == 0) {
                    problem = "Choose at least 1 passenger who gets off here.";
                } else if (regular + student > remaining) {
                    problem = "Only " + remaining + " passenger(s) are left to place.";
                }
            }
            if (problem != null) {
                JOptionPane.showMessageDialog(this, problem, "Please check your input", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            entries.add(new StopEntry(place, km, regular, student));
            break;
        }
        refreshStops();
        invalidateResult();
    }

    private void removeStop() {
        int row = stopTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a stop in the table first.", "Remove stop", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        entries.remove(row);
        refreshStops();
        invalidateResult();
    }

    private void invalidateResult() {
        pendingTrip = null;
        saveButton.setEnabled(false);
        receiptArea.setText("Add the stops, then press \"Compute fare\".");
        totalLabel.setText("PHP 0.00");
    }

    private void computeFare() {
        String origin = originField.getText().trim();
        if (origin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the starting point.", "Please check your input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int total = totalPassengers();
        if (placed() != total) {
            JOptionPane.showMessageDialog(this,
                    "Place all " + total + " passenger(s) first (" + placed() + " of " + total + " placed).\nUse \"+ Add stop\".",
                    "Please check your input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Transport transport = jeepneyRadio.isSelected() ? new Jeepney() : new Tricycle();
        String rideType = individualBtn.isSelected() ? "Individual" : duoBtn.isSelected() ? "Duo" : "Group";
        Trip trip = new Trip(origin, transport, rideType);

        List<StopEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingDouble(s -> s.km));
        for (StopEntry e : sorted) {
            Stop stop = new Stop(e.place, e.km);
            for (int i = 0; i < e.regular; i++) {
                stop.addPassenger(new RegularPassenger());
            }
            for (int i = 0; i < e.student; i++) {
                stop.addPassenger(new StudentPassenger());
            }
            trip.addStop(stop);
        }

        pendingTrip = trip;
        receiptArea.setText(trip.getReceiptText());
        receiptArea.setCaretPosition(0);
        totalLabel.setText(Ui.money(trip.getTotalFare()));
        saveButton.setEnabled(true);
    }

    private void saveTrip() {
        if (pendingTrip == null) {
            return;
        }
        manager.addTrip(pendingTrip);
        pendingTrip = null;
        saveButton.setEnabled(false);
        receiptArea.setText(receiptArea.getText() + "\nSaved to Trip history.");
        entries.clear();
        refreshStops();
        refreshHistory();
        refreshBudget();
    }

    // ================= Trip history tab =================

    private JPanel buildHistoryTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(PAGE);
        tab.setBorder(new EmptyBorder(12, 0, 12, 0));
        JPanel card = card();

        historyTable.setRowHeight(28);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < manager.getTrips().size()) {
                historyDetail.setText(manager.getTrips().get(row).getReceiptText());
                historyDetail.setCaretPosition(0);
            }
        });
        JScrollPane tableScroll = new JScrollPane(historyTable);
        tableScroll.setBorder(new LineBorder(CARD_LINE));
        tableScroll.getViewport().setBackground(Color.WHITE);

        historyDetail.setEditable(false);
        historyDetail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        historyDetail.setOpaque(false);
        JScrollPane detailScroll = new JScrollPane(historyDetail);
        detailScroll.setBorder(new LineBorder(CARD_LINE));
        detailScroll.setPreferredSize(new Dimension(300, 190));
        detailScroll.getViewport().setOpaque(false);

        historyTotal.setFont(historyTotal.getFont().deriveFont(Font.BOLD, 15f));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(heading("Trip history"), BorderLayout.NORTH);
        top.add(tableScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(historyTotal, BorderLayout.NORTH);
        bottom.add(detailScroll, BorderLayout.CENTER);

        card.add(top, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        tab.add(card);
        return tab;
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        int no = 1;
        for (Trip t : manager.getTrips()) {
            historyModel.addRow(new Object[]{no, t.getRideType(), t.getTransportName(), t.getOrigin(),
                    t.getPassengerCount(), Ui.money(t.getTotalFare())});
            no++;
        }
        historyTotal.setText("Total spent: " + Ui.money(manager.computeTotal()));
        if (manager.getTrips().isEmpty()) {
            historyDetail.setText("No trips saved yet.");
        }
    }

    // ================= Weekly budget bar =================

    private JPanel buildBudgetBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(new LineBorder(CARD_LINE), new EmptyBorder(10, 18, 10, 18)));

        JLabel title = heading("Weekly budget");
        PillButton setButton = new PillButton("Set budget", false);
        setButton.addActionListener(e -> setBudget());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(title);
        left.add(budgetInfo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        budgetBar.setPreferredSize(new Dimension(240, 14));
        budgetStatus.setFont(budgetStatus.getFont().deriveFont(Font.BOLD, 13f));
        right.add(budgetBar);
        right.add(budgetStatus);
        right.add(setButton);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void setBudget() {
        String text = JOptionPane.showInputDialog(this, "Enter your weekly budget (PHP):", "Set budget", JOptionPane.PLAIN_MESSAGE);
        if (text == null) {
            return;
        }
        try {
            manager.setBudget(Double.parseDouble(text.trim()));
            refreshBudget();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter numbers only.", "Please check your input", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Please check your input", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refreshBudget() {
        double total = manager.computeTotal();
        double budget = manager.getBudget();
        if (budget == 0) {
            budgetInfo.setText("Spent: " + Ui.money(total) + "  -  no budget set yet");
            budgetBar.set(0, Color.LIGHT_GRAY);
            budgetStatus.setText("No budget set");
            budgetStatus.setForeground(Color.GRAY);
            return;
        }
        budgetInfo.setText("Budget: " + Ui.money(budget) + "   Spent: " + Ui.money(total)
                + "   Left: " + Ui.money(Math.max(budget - total, 0)));
        String status = manager.getBudgetStatus();
        if (status.startsWith("WARNING: Over")) {
            budgetStatus.setText("Over budget");
            budgetStatus.setForeground(RED);
            budgetBar.set(total / budget, RED);
        } else if (status.startsWith("WARNING")) {
            budgetStatus.setText("Almost used up");
            budgetStatus.setForeground(ORANGE);
            budgetBar.set(total / budget, ORANGE);
        } else {
            budgetStatus.setText("Within budget");
            budgetStatus.setForeground(GREEN);
            budgetBar.set(total / budget, GREEN);
        }
    }

    // ================= small custom components =================

    // A flat progress bar (draws the same way on every computer)
    static class BudgetBar extends JComponent {
        private double ratio = 0;
        private Color color = Color.LIGHT_GRAY;

        void set(double ratio, Color color) {
            this.ratio = Math.max(0, Math.min(ratio, 1));
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 12, y = (getHeight() - h) / 2;
            g2.setColor(new Color(228, 231, 236));
            g2.fillRoundRect(0, y, getWidth(), h, h, h);
            g2.setColor(color);
            g2.fillRoundRect(0, y, (int) (getWidth() * ratio), h, h, h);
            g2.dispose();
        }
    }

    // A rounded button: blue when it is the main button, white with a border otherwise
    static class PillButton extends JButton {
        private final boolean primary;

        PillButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill, line, text;
            if (!isEnabled()) {
                fill = new Color(225, 228, 233);
                line = fill;
                text = new Color(140, 145, 155);
            } else if (primary) {
                fill = getModel().isRollover() ? BLUE.brighter() : BLUE;
                line = fill;
                text = Color.WHITE;
            } else {
                fill = getModel().isRollover() ? new Color(238, 243, 252) : Color.WHITE;
                line = new Color(170, 178, 190);
                text = new Color(40, 45, 55);
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            setForeground(text);
            super.paintComponent(g);
        }
    }

    // One piece of the Individual / Duo / Group switch
    static class SegButton extends JToggleButton {
        SegButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(isSelected() ? BLUE : new Color(240, 242, 246));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(190, 197, 208));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
            setForeground(isSelected() ? Color.WHITE : new Color(60, 65, 75));
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Nimbus")) {
                    UIManager.setLookAndFeel(info.getClassName());
                }
            }
        } catch (Exception e) {
            // use the default look if Nimbus is not available
        }
        SwingUtilities.invokeLater(() -> new BiyaheBudgetGUI().setVisible(true));
    }
}
