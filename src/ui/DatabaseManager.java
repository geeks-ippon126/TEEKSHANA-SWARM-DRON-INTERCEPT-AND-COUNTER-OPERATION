package ui;

import core.MissionSimulator;
import dao.DroneDAO;
import dao.MissionDAO;
import dao.ReinforcementDAO;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;

/**
 * DatabaseManager — full CRUD UI for all drone_swarm tables.
 * Pass a live MissionSimulator reference to enable "Deploy to Sim".
 * Open via ◈ DB MANAGER button in MissionControl, or run standalone.
 */
public class DatabaseManager extends JFrame {

    // ── Live sim reference (null when opened standalone)
    private final MissionSimulator sim;
    private final MissionControl   owner;

    // ── DAOs
    private final MissionDAO       missionDAO       = new MissionDAO();
    private final DroneDAO         droneDAO         = new DroneDAO();
    private final ReinforcementDAO reinforcementDAO = new ReinforcementDAO();

    // ── Colors
    private static final Color BG_DARK    = new Color(10, 10, 10);
    private static final Color BG_PANEL   = new Color(18, 28, 18);
    private static final Color GREEN_MAT  = new Color(0, 255, 65);
    private static final Color GREEN_DIM  = new Color(0, 160, 40);
    private static final Color RED_ALERT  = new Color(255, 60, 60);
    private static final Color YELLOW_W   = new Color(255, 220, 0);
    private static final Color BLUE_INFO  = new Color(0, 180, 255);
    private static final Color ORANGE_DEP = new Color(255, 140, 0);
    private static final Color BORDER_COL = new Color(0, 120, 40);

    // ── Tabs
    private JTabbedPane tabs;

    // Missions tab
    private DefaultTableModel missionModel;
    private JTable missionTable;
    private JTextField mf_name, mf_hits, mf_lost, mf_accuracy;
    private JComboBox<String> mf_status;

    // Drones tab
    private DefaultTableModel droneModel;
    private JTable droneTable;
    private JTextField df_droneId, df_missionId, df_fuel;
    private JComboBox<String> df_status;

    // Reinforcements tab
    private DefaultTableModel reinforcementModel;
    private JTable reinforcementTable;
    private JTextField rf_missionId, rf_requested, rf_approved, rf_deployCount;
    private JComboBox<String> rf_status;
    private JLabel simStatusLabel;

    // Status bar
    private JLabel statusBar;

    // ── Constructors ───────────────────────────────────────────────────────
    /** Called from MissionControl — passes live sim + owner for canvas size */
    public DatabaseManager(MissionSimulator sim, MissionControl owner) {
        super("◈ TEEKSHANA — DATABASE MANAGER");
        this.sim   = sim;
        this.owner = owner;
        init();
    }

    /** Standalone — no sim connected */
    public DatabaseManager() {
        super("◈ TEEKSHANA — DATABASE MANAGER");
        this.sim   = null;
        this.owner = null;
        init();
    }

    private void init() {
        setSize(1150, 730);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(4, 4));

        add(buildTitleBar(),  BorderLayout.NORTH);
        add(buildTabs(),      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        setVisible(true);
        loadAll();
        updateSimStatusLabel();
        setStatus("Database Manager ready. Select a tab to manage records.", GREEN_MAT);
    }

    // ── Title Bar ──────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = dark(new BorderLayout());
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COL));
        bar.setPreferredSize(new Dimension(0, 48));

        JLabel title = new JLabel("  ◈ DRONE SWARM  —  DATABASE MANAGER  ◈");
        title.setFont(new Font("Courier New", Font.BOLD, 17));
        title.setForeground(GREEN_MAT);
        bar.add(title, BorderLayout.WEST);

        JPanel right = dark(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton refreshAll = milBtn("↺  REFRESH ALL", GREEN_MAT);
        refreshAll.addActionListener(e -> { loadAll(); setStatus("All tables refreshed.", GREEN_MAT); });
        right.add(refreshAll);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Tabs ───────────────────────────────────────────────────────────────
    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane();
        tabs.setBackground(BG_DARK);
        tabs.setForeground(GREEN_MAT);
        tabs.setFont(new Font("Courier New", Font.BOLD, 12));

        tabs.addTab("◈ MISSIONS",       buildMissionsTab());
        tabs.addTab("◈ DRONES",         buildDronesTab());
        tabs.addTab("◈ REINFORCEMENTS", buildReinforcementsTab());

        return tabs;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MISSIONS TAB
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildMissionsTab() {
        JPanel root = dark(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        String[] cols = {"ID", "NAME", "HITS", "LOST", "ACCURACY", "STATUS", "START", "END"};
        missionModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        missionTable = styledTable(missionModel);
        missionTable.getSelectionModel().addListSelectionListener(e -> populateMissionForm());
        root.add(scroll(missionTable), BorderLayout.CENTER);

        JPanel form = dark(new BorderLayout(4, 4));
        form.setBorder(titledBorder("◈ MISSION RECORD"));
        form.setPreferredSize(new Dimension(0, 160));

        JPanel fields = dark(new GridLayout(2, 4, 8, 6));
        mf_name     = field(""); mf_hits = field("0");
        mf_lost     = field("0"); mf_accuracy = field("0.0");
        mf_status   = combo("active", "completed", "failed");

        fields.add(labeled("Mission Name:", mf_name));
        fields.add(labeled("Targets Hit:",  mf_hits));
        fields.add(labeled("Drones Lost:",  mf_lost));
        fields.add(labeled("Accuracy %:",   mf_accuracy));
        fields.add(labeled("Status:",       mf_status));
        form.add(fields, BorderLayout.CENTER);

        JPanel btns = dark(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton createBtn = milBtn("➕ CREATE", GREEN_MAT);
        JButton updateBtn = milBtn("✏  UPDATE", YELLOW_W);
        JButton deleteBtn = milBtn("🗑  DELETE", RED_ALERT);
        JButton clearBtn  = milBtn("✕  CLEAR",  BLUE_INFO);

        createBtn.addActionListener(e -> createMission());
        updateBtn.addActionListener(e -> updateMission());
        deleteBtn.addActionListener(e -> deleteMission());
        clearBtn .addActionListener(e -> clearMissionForm());

        btns.add(createBtn); btns.add(updateBtn);
        btns.add(deleteBtn); btns.add(clearBtn);
        form.add(btns, BorderLayout.SOUTH);
        root.add(form, BorderLayout.SOUTH);
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRONES TAB
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildDronesTab() {
        JPanel root = dark(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        String[] cols = {"ID", "DRONE ID", "STATUS", "MISSION ID", "FUEL", "LAST UPDATE"};
        droneModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        droneTable = styledTable(droneModel);
        droneTable.getSelectionModel().addListSelectionListener(e -> populateDroneForm());
        root.add(scroll(droneTable), BorderLayout.CENTER);

        JPanel form = dark(new BorderLayout(4, 4));
        form.setBorder(titledBorder("◈ DRONE RECORD"));
        form.setPreferredSize(new Dimension(0, 160));

        JPanel fields = dark(new GridLayout(2, 3, 8, 6));
        df_droneId   = field("DRONE-XXX");
        df_missionId = field("0");
        df_fuel      = field("100");
        df_status    = combo("active", "deployed", "destroyed", "returning");

        fields.add(labeled("Drone ID:",   df_droneId));
        fields.add(labeled("Mission ID:", df_missionId));
        fields.add(labeled("Fuel Level:", df_fuel));
        fields.add(labeled("Status:",     df_status));
        form.add(fields, BorderLayout.CENTER);

        JPanel btns = dark(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton createBtn = milBtn("➕ CREATE", GREEN_MAT);
        JButton updateBtn = milBtn("✏  UPDATE", YELLOW_W);
        JButton deleteBtn = milBtn("🗑  DELETE", RED_ALERT);
        JButton clearBtn  = milBtn("✕  CLEAR",  BLUE_INFO);

        createBtn.addActionListener(e -> createDrone());
        updateBtn.addActionListener(e -> updateDrone());
        deleteBtn.addActionListener(e -> deleteDrone());
        clearBtn .addActionListener(e -> clearDroneForm());

        btns.add(createBtn); btns.add(updateBtn);
        btns.add(deleteBtn); btns.add(clearBtn);
        form.add(btns, BorderLayout.SOUTH);
        root.add(form, BorderLayout.SOUTH);
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REINFORCEMENTS TAB  ← Deploy to Sim lives here
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildReinforcementsTab() {
        JPanel root = dark(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // ── Sim status banner at top
        JPanel banner = dark(new BorderLayout());
        banner.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        banner.setPreferredSize(new Dimension(0, 34));
        simStatusLabel = new JLabel();
        simStatusLabel.setFont(new Font("Courier New", Font.BOLD, 12));
        simStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        banner.add(simStatusLabel, BorderLayout.CENTER);
        root.add(banner, BorderLayout.NORTH);

        // ── Table
        String[] cols = {"ID", "MISSION ID", "REQUESTED", "APPROVED", "STATUS", "REQUEST TIME"};
        reinforcementModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        reinforcementTable = styledTable(reinforcementModel);
        reinforcementTable.getSelectionModel().addListSelectionListener(e -> populateReinforcementForm());
        root.add(scroll(reinforcementTable), BorderLayout.CENTER);

        // ── Form
        JPanel form = dark(new BorderLayout(4, 4));
        form.setBorder(titledBorder("◈ REINFORCEMENT RECORD"));
        form.setPreferredSize(new Dimension(0, 175));

        // Fields row
        JPanel fields = dark(new GridLayout(1, 4, 8, 6));
        rf_missionId  = field("0");
        rf_requested  = field("5");
        rf_approved   = field("0");
        rf_status     = combo("pending", "approved", "deployed");
        fields.add(labeled("Mission ID:",  rf_missionId));
        fields.add(labeled("Requested:",   rf_requested));
        fields.add(labeled("Approved:",    rf_approved));
        fields.add(labeled("Status:",      rf_status));
        form.add(fields, BorderLayout.CENTER);

        // ── Button rows
        JPanel btnArea = dark(new BorderLayout(0, 4));

        // Row 1 — standard CRUD
        JPanel crudBtns = dark(new FlowLayout(FlowLayout.LEFT, 8, 2));
        JButton createBtn = milBtn("➕ CREATE",  GREEN_MAT);
        JButton updateBtn = milBtn("✏  UPDATE",  YELLOW_W);
        JButton deleteBtn = milBtn("🗑  DELETE",  RED_ALERT);
        JButton clearBtn  = milBtn("✕  CLEAR",   BLUE_INFO);
        createBtn.addActionListener(e -> createReinforcement());
        updateBtn.addActionListener(e -> updateReinforcement());
        deleteBtn.addActionListener(e -> deleteReinforcement());
        clearBtn .addActionListener(e -> clearReinforcementForm());
        crudBtns.add(createBtn); crudBtns.add(updateBtn);
        crudBtns.add(deleteBtn); crudBtns.add(clearBtn);

        // Row 2 — Deploy to Sim (the new feature)
        JPanel deployPanel = dark(new FlowLayout(FlowLayout.LEFT, 8, 2));
        deployPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));

        JLabel deployLabel = new JLabel("DEPLOY COUNT:");
        deployLabel.setFont(new Font("Courier New", Font.BOLD, 11));
        deployLabel.setForeground(GREEN_DIM);

        rf_deployCount = field("5");
        rf_deployCount.setPreferredSize(new Dimension(60, 28));

        JButton deployBtn    = milBtn("⚡ DEPLOY TO SIM",    ORANGE_DEP);
        JButton quickSendBtn = milBtn("⚡ QUICK SEND SELECTED", ORANGE_DEP);
        deployBtn   .setPreferredSize(new Dimension(165, 30));
        quickSendBtn.setPreferredSize(new Dimension(195, 30));

        deployBtn   .addActionListener(e -> deployToSim());
        quickSendBtn.addActionListener(e -> quickDeploySelected());

        // Tooltip hints
        deployBtn.setToolTipText("Deploy the number entered in DEPLOY COUNT to the live simulation");
        quickSendBtn.setToolTipText("Select a row above — deploys its APPROVED count directly into the sim");

        deployPanel.add(deployLabel);
        deployPanel.add(rf_deployCount);
        deployPanel.add(deployBtn);
        deployPanel.add(quickSendBtn);

        btnArea.add(crudBtns,    BorderLayout.NORTH);
        btnArea.add(deployPanel, BorderLayout.SOUTH);
        form.add(btnArea, BorderLayout.SOUTH);

        root.add(form, BorderLayout.SOUTH);
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DEPLOY TO SIM LOGIC
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Deploy a custom count from the DEPLOY COUNT field directly into
     * the running simulation. Works whether a row is selected or not.
     */
    private void deployToSim() {
        if (sim == null) {
            setStatus("No simulation connected. Open DB Manager from inside the running sim.", RED_ALERT);
            showNoSimDialog();
            return;
        }
        if (!isSimRunning()) {
            setStatus("No active mission running. Launch a mission first, then deploy.", YELLOW_W);
            return;
        }

        int count = intVal(rf_deployCount);
        if (count <= 0) {
            setStatus("Deploy count must be at least 1.", RED_ALERT);
            return;
        }

        // Save to DB as approved reinforcement
        int missionId = owner.getCurrentMissionId();
        int rid = reinforcementDAO.requestDrones(missionId, count);
        if (rid > 0) reinforcementDAO.approveReinforcement(rid, count);

        // Push directly into live sim
        int w = owner.getSimCanvasWidth();
        int h = owner.getSimCanvasHeight();
        sim.addReinforcementDrones(count, w, h);

        loadReinforcements();
        updateSimStatusLabel();
        setStatus("⚡ " + count + " reinforcement drone(s) deployed into ACTIVE simulation!", ORANGE_DEP);
    }

    /**
     * Select a row in the reinforcements table → click QUICK SEND SELECTED
     * → its APPROVED count is deployed immediately into the sim.
     */
    private void quickDeploySelected() {
        if (sim == null) {
            setStatus("No simulation connected.", RED_ALERT);
            showNoSimDialog();
            return;
        }
        if (!isSimRunning()) {
            setStatus("No active mission running. Launch a mission first.", YELLOW_W);
            return;
        }

        int row = reinforcementTable.getSelectedRow();
        if (row < 0) {
            setStatus("Select a reinforcement row first, then click QUICK SEND SELECTED.", YELLOW_W);
            return;
        }

        int approved = intVal2(reinforcementModel.getValueAt(row, 3)); // APPROVED column
        int recId    = intVal2(reinforcementModel.getValueAt(row, 0)); // ID column

        if (approved <= 0) {
            setStatus("Approved count is 0 for this record. Set Approved > 0 then UPDATE first.", YELLOW_W);
            return;
        }

        // Mark as deployed in DB
        reinforcementDAO.updateReinforcement(recId,
            intVal2(reinforcementModel.getValueAt(row, 1)),
            intVal2(reinforcementModel.getValueAt(row, 2)),
            approved, "deployed");

        // Push into live sim
        int w = owner.getSimCanvasWidth();
        int h = owner.getSimCanvasHeight();
        sim.addReinforcementDrones(approved, w, h);

        loadReinforcements();
        updateSimStatusLabel();
        setStatus("⚡ " + approved + " approved drone(s) from record #" + recId + " deployed into sim!", ORANGE_DEP);
    }

    private boolean isSimRunning() {
        return sim != null && owner != null && owner.isSimRunning();
    }

    private void updateSimStatusLabel() {
        if (simStatusLabel == null) return;
        if (sim == null) {
            simStatusLabel.setText("  ● SIM: NOT CONNECTED  —  open this window from inside a running mission to enable deploy");
            simStatusLabel.setForeground(RED_ALERT);
        } else if (isSimRunning()) {
            simStatusLabel.setText("  ● SIM: ACTIVE  |  Drones: " + sim.drones.size()
                + "  |  Wave: " + sim.waveNumber
                + "  |  Hits: " + sim.targetsHit
                + "  ←  deploy reinforcements directly into this mission");
            simStatusLabel.setForeground(ORANGE_DEP);
        } else {
            simStatusLabel.setText("  ● SIM: CONNECTED but no mission running  —  launch a mission then deploy");
            simStatusLabel.setForeground(YELLOW_W);
        }
    }

    private void showNoSimDialog() {
        JOptionPane.showMessageDialog(this,
            "The DB Manager was opened standalone or before a mission started.\n\n" +
            "To deploy reinforcements into a live simulation:\n" +
            "  1. Close this window\n" +
            "  2. Press  ▶ LAUNCH MISSION  in the main Teekshana window\n" +
            "  3. Then click  ◈ DB MANAGER  — the sim will be connected\n" +
            "  4. Go to  ◈ REINFORCEMENTS  tab and click  ⚡ DEPLOY TO SIM",
            "Simulation Not Connected",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOAD / REFRESH
    // ══════════════════════════════════════════════════════════════════════
    private void loadAll()           { loadMissions(); loadDrones(); loadReinforcements(); }

    private void loadMissions() {
        missionModel.setRowCount(0);
        try {
            ResultSet rs = missionDAO.getAllMissions();
            if (rs == null) return;
            while (rs.next()) {
                missionModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("mission_name"),
                    rs.getInt("targets_hit"), rs.getInt("drones_lost"),
                    String.format("%.2f", rs.getDouble("accuracy")),
                    rs.getString("status"), rs.getString("start_time"), rs.getString("end_time")
                });
            }
        } catch (Exception e) { setStatus("Error loading missions: " + e.getMessage(), RED_ALERT); }
    }

    private void loadDrones() {
        droneModel.setRowCount(0);
        try {
            ResultSet rs = droneDAO.getAllDrones();
            if (rs == null) return;
            while (rs.next()) {
                droneModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("drone_id"), rs.getString("status"),
                    rs.getObject("mission_id"), rs.getInt("fuel_level"), rs.getString("last_update")
                });
            }
        } catch (Exception e) { setStatus("Error loading drones: " + e.getMessage(), RED_ALERT); }
    }

    private void loadReinforcements() {
        reinforcementModel.setRowCount(0);
        try {
            ResultSet rs = reinforcementDAO.getAllReinforcements();
            if (rs == null) return;
            while (rs.next()) {
                reinforcementModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getInt("mission_id"),
                    rs.getInt("requested_drones"), rs.getInt("approved_drones"),
                    rs.getString("status"), rs.getString("request_time")
                });
            }
        } catch (Exception e) { setStatus("Error loading reinforcements: " + e.getMessage(), RED_ALERT); }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MISSION CRUD
    // ══════════════════════════════════════════════════════════════════════
    private void createMission() {
        String name = mf_name.getText().trim();
        if (name.isEmpty()) { setStatus("Mission name cannot be empty.", RED_ALERT); return; }
        try {
            int id = missionDAO.createMission(name);
            if (id > 0) {
                missionDAO.updateMission(id, name, intVal(mf_hits), intVal(mf_lost),
                    dblVal(mf_accuracy), (String) mf_status.getSelectedItem());
                loadMissions();
                setStatus("✔ Mission created — ID: " + id, GREEN_MAT);
                clearMissionForm();
            }
        } catch (Exception e) { setStatus("Create failed: " + e.getMessage(), RED_ALERT); }
    }

    private void updateMission() {
        int row = missionTable.getSelectedRow();
        if (row < 0) { setStatus("Select a mission row to update.", YELLOW_W); return; }
        int id = (int) missionModel.getValueAt(row, 0);
        try {
            boolean ok = missionDAO.updateMission(id, mf_name.getText().trim(),
                intVal(mf_hits), intVal(mf_lost), dblVal(mf_accuracy),
                (String) mf_status.getSelectedItem());
            loadMissions();
            setStatus(ok ? "✔ Mission ID " + id + " updated." : "No rows changed.", ok ? GREEN_MAT : YELLOW_W);
        } catch (Exception e) { setStatus("Update failed: " + e.getMessage(), RED_ALERT); }
    }

    private void deleteMission() {
        int row = missionTable.getSelectedRow();
        if (row < 0) { setStatus("Select a mission row to delete.", YELLOW_W); return; }
        int id = (int) missionModel.getValueAt(row, 0);
        String name = (String) missionModel.getValueAt(row, 1);
        if (JOptionPane.showConfirmDialog(this,
            "Delete mission \"" + name + "\" (ID " + id + ")?\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            boolean ok = missionDAO.deleteMission(id);
            loadMissions();
            setStatus(ok ? "🗑 Mission ID " + id + " deleted." : "Delete failed — FK constraint?", ok ? GREEN_MAT : RED_ALERT);
            clearMissionForm();
        } catch (Exception e) { setStatus("Delete failed: " + e.getMessage(), RED_ALERT); }
    }

    private void populateMissionForm() {
        int row = missionTable.getSelectedRow(); if (row < 0) return;
        mf_name.setText(str(missionModel.getValueAt(row, 1)));
        mf_hits.setText(str(missionModel.getValueAt(row, 2)));
        mf_lost.setText(str(missionModel.getValueAt(row, 3)));
        mf_accuracy.setText(str(missionModel.getValueAt(row, 4)));
        mf_status.setSelectedItem(str(missionModel.getValueAt(row, 5)));
    }

    private void clearMissionForm() {
        mf_name.setText(""); mf_hits.setText("0");
        mf_lost.setText("0"); mf_accuracy.setText("0.0");
        mf_status.setSelectedIndex(0); missionTable.clearSelection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRONE CRUD
    // ══════════════════════════════════════════════════════════════════════
    private void createDrone() {
        String droneId = df_droneId.getText().trim();
        if (droneId.isEmpty()) { setStatus("Drone ID cannot be empty.", RED_ALERT); return; }
        try {
            boolean ok = droneDAO.insertDrone(droneId, (String) df_status.getSelectedItem(),
                intVal(df_missionId), intVal(df_fuel));
            loadDrones();
            setStatus(ok ? "✔ Drone " + droneId + " created." : "Insert failed — ID may exist.", ok ? GREEN_MAT : RED_ALERT);
            if (ok) clearDroneForm();
        } catch (Exception e) { setStatus("Create failed: " + e.getMessage(), RED_ALERT); }
    }

    private void updateDrone() {
        int row = droneTable.getSelectedRow();
        if (row < 0) { setStatus("Select a drone row to update.", YELLOW_W); return; }
        int id = (int) droneModel.getValueAt(row, 0);
        try {
            boolean ok = droneDAO.updateDrone(id, df_droneId.getText().trim(),
                (String) df_status.getSelectedItem(), intVal(df_missionId), intVal(df_fuel));
            loadDrones();
            setStatus(ok ? "✔ Drone ID " + id + " updated." : "No rows changed.", ok ? GREEN_MAT : YELLOW_W);
        } catch (Exception e) { setStatus("Update failed: " + e.getMessage(), RED_ALERT); }
    }

    private void deleteDrone() {
        int row = droneTable.getSelectedRow();
        if (row < 0) { setStatus("Select a drone row to delete.", YELLOW_W); return; }
        int id = (int) droneModel.getValueAt(row, 0);
        String did = str(droneModel.getValueAt(row, 1));
        if (JOptionPane.showConfirmDialog(this, "Delete drone \"" + did + "\" (ID " + id + ")?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            boolean ok = droneDAO.deleteDrone(id);
            loadDrones();
            setStatus(ok ? "🗑 Drone ID " + id + " deleted." : "Delete failed.", ok ? GREEN_MAT : RED_ALERT);
            clearDroneForm();
        } catch (Exception e) { setStatus("Delete failed: " + e.getMessage(), RED_ALERT); }
    }

    private void populateDroneForm() {
        int row = droneTable.getSelectedRow(); if (row < 0) return;
        df_droneId.setText(str(droneModel.getValueAt(row, 1)));
        df_status.setSelectedItem(str(droneModel.getValueAt(row, 2)));
        Object mid = droneModel.getValueAt(row, 3);
        df_missionId.setText(mid == null ? "0" : str(mid));
        df_fuel.setText(str(droneModel.getValueAt(row, 4)));
    }

    private void clearDroneForm() {
        df_droneId.setText("DRONE-XXX"); df_missionId.setText("0");
        df_fuel.setText("100"); df_status.setSelectedIndex(0);
        droneTable.clearSelection();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REINFORCEMENT CRUD
    // ══════════════════════════════════════════════════════════════════════
    private void createReinforcement() {
        try {
            int mid = intVal(rf_missionId); int req = intVal(rf_requested);
            int rid = reinforcementDAO.requestDrones(mid, req);
            if (rid > 0) reinforcementDAO.updateReinforcement(rid, mid, req,
                intVal(rf_approved), (String) rf_status.getSelectedItem());
            loadReinforcements();
            setStatus(rid > 0 ? "✔ Reinforcement created — ID: " + rid : "Insert failed.", rid > 0 ? GREEN_MAT : RED_ALERT);
            if (rid > 0) clearReinforcementForm();
        } catch (Exception e) { setStatus("Create failed: " + e.getMessage(), RED_ALERT); }
    }

    private void updateReinforcement() {
        int row = reinforcementTable.getSelectedRow();
        if (row < 0) { setStatus("Select a reinforcement row to update.", YELLOW_W); return; }
        int id = (int) reinforcementModel.getValueAt(row, 0);
        try {
            boolean ok = reinforcementDAO.updateReinforcement(id,
                intVal(rf_missionId), intVal(rf_requested),
                intVal(rf_approved), (String) rf_status.getSelectedItem());
            loadReinforcements();
            setStatus(ok ? "✔ Reinforcement ID " + id + " updated." : "No rows changed.", ok ? GREEN_MAT : YELLOW_W);
        } catch (Exception e) { setStatus("Update failed: " + e.getMessage(), RED_ALERT); }
    }

    private void deleteReinforcement() {
        int row = reinforcementTable.getSelectedRow();
        if (row < 0) { setStatus("Select a row to delete.", YELLOW_W); return; }
        int id = (int) reinforcementModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete reinforcement record ID " + id + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            boolean ok = reinforcementDAO.deleteReinforcement(id);
            loadReinforcements();
            setStatus(ok ? "🗑 Reinforcement ID " + id + " deleted." : "Delete failed.", ok ? GREEN_MAT : RED_ALERT);
            clearReinforcementForm();
        } catch (Exception e) { setStatus("Delete failed: " + e.getMessage(), RED_ALERT); }
    }

    private void populateReinforcementForm() {
        int row = reinforcementTable.getSelectedRow(); if (row < 0) return;
        rf_missionId.setText(str(reinforcementModel.getValueAt(row, 1)));
        rf_requested.setText(str(reinforcementModel.getValueAt(row, 2)));
        rf_approved .setText(str(reinforcementModel.getValueAt(row, 3)));
        rf_status   .setSelectedItem(str(reinforcementModel.getValueAt(row, 4)));
        // Auto-fill deploy count from approved value
        rf_deployCount.setText(str(reinforcementModel.getValueAt(row, 3)));
        updateSimStatusLabel();
    }

    private void clearReinforcementForm() {
        rf_missionId.setText("0"); rf_requested.setText("5");
        rf_approved.setText("0"); rf_status.setSelectedIndex(0);
        rf_deployCount.setText("5");
        reinforcementTable.clearSelection();
    }

    // ── Status Bar ─────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = dark(new BorderLayout());
        bar.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER_COL));
        bar.setPreferredSize(new Dimension(0, 28));
        statusBar = new JLabel("  Ready.");
        statusBar.setFont(new Font("Courier New", Font.PLAIN, 11));
        statusBar.setForeground(GREEN_MAT);
        bar.add(statusBar, BorderLayout.WEST);
        return bar;
    }

    private void setStatus(String msg, Color col) {
        SwingUtilities.invokeLater(() -> {
            statusBar.setText("  " + msg);
            statusBar.setForeground(col);
        });
    }

    // ── Style Helpers ──────────────────────────────────────────────────────
    private JPanel dark(LayoutManager lm) {
        JPanel p = new JPanel(lm); p.setBackground(BG_DARK); return p;
    }

    private JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBackground(BG_DARK);
        sp.getViewport().setBackground(BG_PANEL);
        return sp;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(BG_PANEL); t.setForeground(GREEN_MAT);
        t.setGridColor(BORDER_COL);
        t.setFont(new Font("Courier New", Font.PLAIN, 11));
        t.getTableHeader().setBackground(new Color(15, 40, 15));
        t.getTableHeader().setForeground(GREEN_MAT);
        t.getTableHeader().setFont(new Font("Courier New", Font.BOLD, 11));
        t.setRowHeight(22);
        t.setSelectionBackground(new Color(0, 100, 0));
        t.setSelectionForeground(Color.WHITE);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        return t;
    }

    private JButton milBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier New", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(new Color(12, 35, 12));
        b.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(130, 30));
        return b;
    }

    private JTextField field(String def) {
        JTextField f = new JTextField(def);
        f.setBackground(new Color(5, 18, 5)); f.setForeground(GREEN_MAT);
        f.setCaretColor(GREEN_MAT);
        f.setFont(new Font("Courier New", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        return f;
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(new Color(5, 18, 5)); cb.setForeground(GREEN_MAT);
        cb.setFont(new Font("Courier New", Font.PLAIN, 12));
        return cb;
    }

    private JPanel labeled(String label, JComponent comp) {
        JPanel p = dark(new BorderLayout(2, 2));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Courier New", Font.BOLD, 10));
        l.setForeground(GREEN_DIM);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private TitledBorder titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1), title);
        tb.setTitleFont(new Font("Courier New", Font.BOLD, 11));
        tb.setTitleColor(GREEN_DIM);
        return tb;
    }

    private int intVal(JTextField f) {
        try { return Integer.parseInt(f.getText().trim()); } catch (Exception e) { return 0; }
    }
    private int intVal2(Object o) {
        try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return 0; }
    }
    private double dblVal(JTextField f) {
        try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return 0.0; }
    }
    private String str(Object o) { return o == null ? "" : o.toString(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DatabaseManager::new);
    }
}