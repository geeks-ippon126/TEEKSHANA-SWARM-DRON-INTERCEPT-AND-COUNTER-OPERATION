package ui;

import core.Drone;
import core.MissionSimulator;
import core.Target;
import dao.DroneDAO;
import dao.MissionDAO;
import dao.ReinforcementDAO;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class MissionControl extends JFrame {

    // ── Simulation
    private MissionSimulator sim;
    private TacticalDisplay tacticalDisplay;
    private boolean running = false;
    private boolean timeUp = false;
    private boolean finalSweepDone = false;
    private Thread simThread;
    private long startTime;
    private int currentMissionId = -1;
    private String currentMissionName = "";
    private static final int SIM_DURATION_MS = 60 * 1000;

    // ── DAO
    private MissionDAO missionDAO = new MissionDAO();
    private DroneDAO droneDAO = new DroneDAO();
    private ReinforcementDAO reinforcementDAO = new ReinforcementDAO();

    // ── Colors
    private static final Color BG_DARK    = new Color(10, 10, 10);
    private static final Color BG_PANEL   = new Color(18, 28, 18);
    private static final Color GREEN_MAT  = new Color(0, 255, 65);
    private static final Color GREEN_DIM  = new Color(0, 160, 40);
    private static final Color RED_ALERT  = new Color(255, 30, 30);
    private static final Color YELLOW_W   = new Color(255, 220, 0);
    private static final Color BORDER_COL = new Color(0, 120, 40);

    // ── UI components
    private JLabel missionNameLabel;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JLabel killsLabel;
    private JLabel lossesLabel;
    private JLabel accuracyLabel;
    private JLabel costLabel;
    private JLabel reinforcementLabel;
    private JLabel waveLabel;

    private DefaultTableModel droneTableModel;
    private DefaultTableModel targetTableModel;
    private DefaultTableModel historyTableModel;
    private JTextArea logArea;
    private JProgressBar missionTimerBar;
    private JProgressBar fuelBar;

    private JButton launchBtn;
    private JButton abortBtn;
    private JButton raidBtn;
    private JButton exportBtn;
    private JButton refreshHistoryBtn;

    private JSpinner droneCountSpinner;
    private JSpinner targetCountSpinner;

    private Random rand = new Random();
    private Timer swingTimer;

    public MissionControl() {
        super("◈ TEEKSHANA — DRONE SWARM MISSION CONTROL ◈");

        sim = new MissionSimulator(this::appendLog);

        setLookAndFeel();
        buildUI();

        setSize(1280, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });

        // Swing repaint timer (30fps)
        swingTimer = new Timer(33, e -> {
            tacticalDisplay.repaint();
            refreshDroneTable();
            refreshTargetTable();
            updateStats();
            updateTimerBar();
        });

        setVisible(true);
        appendLog("[System] Teekshana Sky Guardian ready. Configure mission and click LAUNCH.");
    }

    // ── Look & Feel ────────────────────────────────────────────────────────
    private void setLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("Label.foreground", GREEN_MAT);
        UIManager.put("TextArea.background", BG_DARK);
        UIManager.put("TextArea.foreground", GREEN_MAT);
    }

    // ── Build UI ───────────────────────────────────────────────────────────
    private void buildUI() {
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(4, 4));

        add(buildTopBar(),      BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomBar(),   BorderLayout.SOUTH);
    }

    // ── Top Bar ────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = darkPanel(new BorderLayout(10, 0));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COL));
        bar.setPreferredSize(new Dimension(0, 52));

        // Left: title
        JLabel title = new JLabel("  ◈ TEEKSHANA SWARM CONTROL ◈");
        title.setFont(new Font("Courier New", Font.BOLD, 18));
        title.setForeground(GREEN_MAT);
        bar.add(title, BorderLayout.WEST);

        // Center: mission name + status
        JPanel mid = darkPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        missionNameLabel = greenLabel("MISSION: ---", Font.BOLD, 13);
        statusLabel = greenLabel("STATUS: STANDBY", Font.BOLD, 13);
        timerLabel  = greenLabel("T+00:00", Font.BOLD, 13);
        mid.add(missionNameLabel);
        mid.add(statusLabel);
        mid.add(timerLabel);
        bar.add(mid, BorderLayout.CENTER);

        // Right: date/time live
        JLabel dtLabel = new JLabel();
        dtLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        dtLabel.setForeground(GREEN_DIM);
        Timer dtTimer = new Timer(1000, e -> {
            dtLabel.setText(new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss  ").format(new Date()));
        });
        dtTimer.start();
        bar.add(dtLabel, BorderLayout.EAST);

        return bar;
    }

    // ── Center Panel ───────────────────────────────────────────────────────
    private JPanel buildCenterPanel() {
        JPanel center = darkPanel(new BorderLayout(4, 4));

        // Left column: tables
        JPanel leftCol = darkPanel(new GridLayout(2, 1, 4, 4));
        leftCol.setPreferredSize(new Dimension(280, 0));
        leftCol.add(buildDroneTable());
        leftCol.add(buildTargetTable());
        center.add(leftCol, BorderLayout.WEST);

        // Center: tactical display
        tacticalDisplay = new TacticalDisplay(sim);
        JPanel tacWrapper = darkPanel(new BorderLayout());
        tacWrapper.setBorder(titledBorder("◈ TACTICAL GRID"));
        tacWrapper.add(tacticalDisplay, BorderLayout.CENTER);
        center.add(tacWrapper, BorderLayout.CENTER);

        // Right column: stats + log
        JPanel rightCol = darkPanel(new BorderLayout(4, 4));
        rightCol.setPreferredSize(new Dimension(280, 0));
        rightCol.add(buildStatsPanel(),   BorderLayout.NORTH);
        rightCol.add(buildLogPanel(),     BorderLayout.CENTER);
        rightCol.add(buildHistoryPanel(), BorderLayout.SOUTH);
        center.add(rightCol, BorderLayout.EAST);

        return center;
    }

    // ── Drone Table ────────────────────────────────────────────────────────
    private JScrollPane buildDroneTable() {
        String[] cols = {"ID", "STATUS", "AMMO"};
        droneTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = styledTable(droneTableModel);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(titledBorder("◈ DRONE FLEET"));
        sp.setBackground(BG_DARK);
        return sp;
    }

    // ── Target Table ───────────────────────────────────────────────────────
    private JScrollPane buildTargetTable() {
        String[] cols = {"ID", "STATUS"};
        targetTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = styledTable(targetTableModel);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(titledBorder("◈ TARGET BOARD"));
        sp.setBackground(BG_DARK);
        return sp;
    }

    // ── Stats Panel ────────────────────────────────────────────────────────
    private JPanel buildStatsPanel() {
        JPanel p = darkPanel(new GridLayout(8, 1, 2, 2));
        p.setBorder(titledBorder("◈ MISSION STATS"));

        killsLabel        = greenLabel("KILLS    : 0", Font.BOLD, 12);
        lossesLabel       = greenLabel("LOSSES   : 0", Font.BOLD, 12);
        accuracyLabel     = greenLabel("ACCURACY : 0.0%", Font.BOLD, 12);
        costLabel         = greenLabel("COST     : $0", Font.BOLD, 12);
        waveLabel         = greenLabel("WAVE     : 0", Font.BOLD, 12);
        waveLabel.setForeground(YELLOW_W);
        reinforcementLabel = new JLabel("REINFORCE: ---");
        reinforcementLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        reinforcementLabel.setForeground(YELLOW_W);

        missionTimerBar = new JProgressBar(0, SIM_DURATION_MS);
        missionTimerBar.setForeground(GREEN_MAT);
        missionTimerBar.setBackground(BG_PANEL);
        missionTimerBar.setStringPainted(true);
        missionTimerBar.setString("MISSION TIMER");

        fuelBar = new JProgressBar(0, 100);
        fuelBar.setForeground(new Color(0, 200, 255));
        fuelBar.setBackground(BG_PANEL);
        fuelBar.setStringPainted(true);
        fuelBar.setString("AVG FUEL: 100%");
        fuelBar.setValue(100);

        p.add(killsLabel);
        p.add(lossesLabel);
        p.add(accuracyLabel);
        p.add(costLabel);
        p.add(waveLabel);
        p.add(reinforcementLabel);
        p.add(missionTimerBar);
        p.add(fuelBar);
        return p;
    }

    // ── Log Panel ──────────────────────────────────────────────────────────
    private JPanel buildLogPanel() {
        JPanel p = darkPanel(new BorderLayout());
        p.setBorder(titledBorder("◈ SYSTEM LOG"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(5, 12, 5));
        logArea.setForeground(GREEN_MAT);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 10));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBackground(BG_DARK);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Mission History Panel ──────────────────────────────────────────────
    private JPanel buildHistoryPanel() {
        JPanel p = darkPanel(new BorderLayout(2, 2));
        p.setBorder(titledBorder("◈ MISSION HISTORY"));
        p.setPreferredSize(new Dimension(0, 140));

        String[] cols = {"ID", "NAME", "HITS", "LOST", "ACC%"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = styledTable(historyTableModel);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBackground(BG_DARK);

        refreshHistoryBtn = militaryButton("REFRESH DB");
        refreshHistoryBtn.addActionListener(e -> loadMissionHistory());

        p.add(sp, BorderLayout.CENTER);
        p.add(refreshHistoryBtn, BorderLayout.SOUTH);
        return p;
    }

    // ── Bottom Bar ─────────────────────────────────────────────────────────
    private JPanel buildBottomBar() {
        JPanel bar = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER_COL));

        // Config
        JLabel droneL  = greenLabel("DRONES:", Font.PLAIN, 12);
        JLabel targetL = greenLabel("TARGETS:", Font.PLAIN, 12);
        droneCountSpinner  = new JSpinner(new SpinnerNumberModel(8, 3, 20, 1));
        targetCountSpinner = new JSpinner(new SpinnerNumberModel(10, 5, 20, 1));
        styleSpinner(droneCountSpinner);
        styleSpinner(targetCountSpinner);

        launchBtn = militaryButton("▶ LAUNCH MISSION");
        abortBtn  = militaryButton("■ ABORT");
        raidBtn   = militaryButton("⚡ START RAID");
        exportBtn = militaryButton("⬇ EXPORT LOG");
        JButton dbBtn = militaryButton("◈ DB MANAGER");

        launchBtn.addActionListener(e -> launchMission());
        abortBtn .addActionListener(e -> abortMission());
        raidBtn  .addActionListener(e -> startRaid());
        exportBtn.addActionListener(e -> exportLog());
        dbBtn    .addActionListener(e -> new DatabaseManager(sim, this));

        abortBtn.setForeground(RED_ALERT);
        raidBtn .setForeground(YELLOW_W);
        dbBtn   .setForeground(new Color(0, 180, 255));

        bar.add(droneL); bar.add(droneCountSpinner);
        bar.add(targetL); bar.add(targetCountSpinner);
        bar.add(Box.createHorizontalStrut(20));
        bar.add(launchBtn); bar.add(abortBtn); bar.add(raidBtn); bar.add(exportBtn); bar.add(dbBtn);

        return bar;
    }

    // ── Mission Control Logic ──────────────────────────────────────────────
    private void launchMission() {
        if (running) { appendLog("[Warning] Mission already active."); return; }

        int numDrones  = (int) droneCountSpinner.getValue();
        int numTargets = (int) targetCountSpinner.getValue();
        currentMissionName = "OPERATION-" + String.format("%04d", rand.nextInt(9999));

        // DB: create mission record
        currentMissionId = missionDAO.createMission(currentMissionName);

        sim.init(numDrones, numTargets, tacticalDisplay.getWidth(), tacticalDisplay.getHeight());

        // Sync drones to DB
        for (Drone d : sim.drones) {
            droneDAO.upsertDrone(d.droneId, "deployed", currentMissionId);
        }

        missionNameLabel.setText("MISSION: " + currentMissionName);
        statusLabel.setForeground(GREEN_MAT);
        statusLabel.setText("STATUS: ACTIVE");

        running = true;
        timeUp = false;
        finalSweepDone = false;
        startTime = System.currentTimeMillis();

        simThread = new Thread(this::simulationLoop);
        simThread.setDaemon(true);
        simThread.start();

        swingTimer.start();
        appendLog("[System] Mission launched: " + currentMissionName
                + " | Drones: " + numDrones + " | Targets: " + numTargets);
    }

    private void startRaid() {
        if (!running) {
            launchMission();
        }
        appendLog("[System] Raid swarm deployed!");
    }

    private void abortMission() {
        if (!running) return;
        running = false;
        timeUp = true;
        statusLabel.setForeground(RED_ALERT);
        statusLabel.setText("STATUS: ABORTED");
        appendLog("[System] Mission ABORTED by operator.");
        finalizeMission();
    }

    private void simulationLoop() {
        while (running) {
            sim.tick(tacticalDisplay.getWidth(), tacticalDisplay.getHeight());

            long elapsed = System.currentTimeMillis() - startTime;

            // Final sweep warning
            if (!finalSweepDone && elapsed >= SIM_DURATION_MS - 5000) {
                appendLog("[System] Final sweep initiated.");
                finalSweepDone = true;
            }

            // Time limit
            if (!timeUp && elapsed >= SIM_DURATION_MS) {
                appendLog("[System] Simulation time limit reached.");
                timeUp = true;
                sim.setTimeUp();
            }

            // Check reinforcement
            if (sim.isReinforcementNeeded()) {
                sim.markReinforcementRequested();
                int requested = 5 + sim.dronesLost;
                int rid = reinforcementDAO.requestDrones(currentMissionId, requested);
                reinforcementDAO.approveReinforcement(rid, requested);
                sim.addReinforcementDrones(requested, tacticalDisplay.getWidth(), tacticalDisplay.getHeight());
                SwingUtilities.invokeLater(() -> {
                    reinforcementLabel.setText("REINFORCE: +" + requested + " DEPLOYED");
                    reinforcementLabel.setForeground(GREEN_MAT);
                    statusLabel.setText("STATUS: REINFORCED");
                });
                appendLog("[REINFORCE] Losses exceeded 20% threshold. " + requested + " drones deployed.");
            }

            // Mission complete
            if (timeUp && sim.missiles.isEmpty() && sim.targets.isEmpty()) {
                running = false;
                SwingUtilities.invokeLater(this::finalizeMission);
                break;
            }

            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
    }

    private void finalizeMission() {
        swingTimer.stop();
        sim.sendDronesHome();
        double acc = sim.getAccuracy();

        // Update DB
        if (currentMissionId > 0) {
            missionDAO.completeMission(currentMissionId, sim.targetsHit, sim.dronesLost, acc);
        }

        statusLabel.setForeground(GREEN_MAT);
        statusLabel.setText("STATUS: COMPLETE");

        appendLog("[Mission] ══════════════════════════════");
        appendLog("[Mission] MISSION COMPLETE: " + currentMissionName);
        appendLog("[Mission] Targets Hit  : " + sim.targetsHit);
        appendLog("[Mission] Drones Lost  : " + sim.dronesLost);
        appendLog("[Mission] Accuracy     : " + String.format("%.1f%%", acc));
        appendLog("[Mission] Total Cost   : $" + sim.totalCost);
        appendLog("[Mission] Missiles Used: " + sim.totalMissilesUsed);
        appendLog("[Mission] ══════════════════════════════");

        loadMissionHistory();
    }

    // ── Table Refresh ──────────────────────────────────────────────────────
    private void refreshDroneTable() {
        droneTableModel.setRowCount(0);
        for (Drone d : sim.drones) {
            String icon = d.returningHome ? "↩ RTB" : (d.missilesLeft == 0 ? "⟳ RELOAD" : "✔ ACTIVE");
            droneTableModel.addRow(new Object[]{"D" + d.id, icon, d.missilesLeft});
        }
    }

    private void refreshTargetTable() {
        targetTableModel.setRowCount(0);
        for (Target t : sim.targets) {
            String icon = t.hitStatus.equals("hit") ? "✓ HIT" : (t.hitStatus.equals("missed") ? "✗ MISS" : "? PEND");
            targetTableModel.addRow(new Object[]{t.targetId, icon});
        }
    }

    private void updateStats() {
        killsLabel.setText("KILLS    : " + sim.targetsHit);
        lossesLabel.setText("LOSSES   : " + sim.dronesLost);
        accuracyLabel.setText(String.format("ACCURACY : %.1f%%", sim.getAccuracy()));
        costLabel.setText("COST     : $" + sim.totalCost);
        waveLabel.setText("WAVE     : " + sim.waveNumber);

        int avgFuel = sim.drones.isEmpty() ? 100
                : (int) (sim.drones.stream().mapToInt(d -> d.missilesLeft * 10).average().orElse(100));
        avgFuel = Math.min(avgFuel, 100);
        fuelBar.setValue(avgFuel);
        fuelBar.setString("AVG AMMO: " + avgFuel + "%");
    }

    private void updateTimerBar() {
        if (!running) return;
        long elapsed = System.currentTimeMillis() - startTime;
        missionTimerBar.setValue((int) Math.min(elapsed, SIM_DURATION_MS));
        long secs = elapsed / 1000;
        timerLabel.setText(String.format("T+%02d:%02d", secs / 60, secs % 60));
    }

    private void loadMissionHistory() {
        historyTableModel.setRowCount(0);
        try {
            ResultSet rs = missionDAO.getAllMissions();
            if (rs == null) return;
            while (rs.next()) {
                historyTableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("mission_name"),
                    rs.getInt("targets_hit"),
                    rs.getInt("drones_lost"),
                    String.format("%.1f", rs.getDouble("accuracy"))
                });
            }
        } catch (Exception e) {
            appendLog("[DB] History load error: " + e.getMessage());
        }
    }

    // ── Export ─────────────────────────────────────────────────────────────
    private void exportLog() {
        try (FileWriter fw = new FileWriter("mission_log.txt")) {
            fw.write("=== TEEKSHANA MISSION LOG ===\n");
            fw.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n\n");
            fw.write(logArea.getText());
            fw.write("\n\n=== MISSION SUMMARY ===\n");
            fw.write("Mission     : " + currentMissionName + "\n");
            fw.write("Targets Hit : " + sim.targetsHit + "\n");
            fw.write("Drones Lost : " + sim.dronesLost + "\n");
            fw.write(String.format("Accuracy    : %.1f%%\n", sim.getAccuracy()));
            fw.write("Total Cost  : $" + sim.totalCost + "\n");
            fw.write("Missiles    : " + sim.totalMissilesUsed + "\n");
            appendLog("[System] Log exported → mission_log.txt");
        } catch (IOException e) {
            appendLog("[Error] Export failed: " + e.getMessage());
        }
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("\n" + msg);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void shutdown() {
        running = false;
        swingTimer.stop();
        System.exit(0);
    }

    // ── Style Helpers ──────────────────────────────────────────────────────
    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG_DARK);
        return p;
    }

    private JLabel greenLabel(String text, int style, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Courier New", style, size));
        l.setForeground(GREEN_MAT);
        return l;
    }

    private JButton militaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier New", Font.BOLD, 12));
        b.setForeground(GREEN_MAT);
        b.setBackground(new Color(10, 35, 10));
        b.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(BG_PANEL);
        t.setForeground(GREEN_MAT);
        t.setGridColor(BORDER_COL);
        t.setFont(new Font("Courier New", Font.PLAIN, 11));
        t.getTableHeader().setBackground(new Color(15, 40, 15));
        t.getTableHeader().setForeground(GREEN_MAT);
        t.getTableHeader().setFont(new Font("Courier New", Font.BOLD, 11));
        t.setRowHeight(20);
        t.setSelectionBackground(new Color(0, 80, 0));
        return t;
    }

    private TitledBorder titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1), title);
        tb.setTitleFont(new Font("Courier New", Font.BOLD, 11));
        tb.setTitleColor(GREEN_DIM);
        return tb;
    }

    private void styleSpinner(JSpinner sp) {
        sp.setFont(new Font("Courier New", Font.PLAIN, 12));
        sp.setForeground(GREEN_MAT);
        sp.setBackground(BG_PANEL);
        sp.setPreferredSize(new Dimension(60, 28));
    }

    // ── Public helpers for DatabaseManager ────────────────────────────────
    public boolean isSimRunning()      { return running; }
    public int getCurrentMissionId()   { return currentMissionId; }
    public int getSimCanvasWidth()     { return tacticalDisplay.getWidth(); }
    public int getSimCanvasHeight()    { return tacticalDisplay.getHeight(); }

    // ── Standalone entry point ─────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MissionControl::new);
    }
}