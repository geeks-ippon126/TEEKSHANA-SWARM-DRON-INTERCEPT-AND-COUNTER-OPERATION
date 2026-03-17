package ui;

import core.Drone;
import core.Missile;
import core.MissionSimulator;
import core.Target;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TacticalDisplay extends JPanel {

    private MissionSimulator sim;
    private BufferedImage offscreen;
    private Graphics2D offG;

    private static final Color BG         = new Color(5, 20, 10);
    private static final Color DRONE_COL  = new Color(0, 255, 65);
    private static final Color TARGET_COL = new Color(255, 30, 30);
    private static final Color MISSILE    = new Color(255, 230, 0);
    private static final Color GRID       = new Color(0, 80, 30);
    private static final Color HUD_TEXT   = new Color(0, 200, 60);

    public TacticalDisplay(MissionSimulator sim) {
        this.sim = sim;
        setBackground(BG);
        setPreferredSize(new Dimension(700, 500));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();

        if (offscreen == null || offscreen.getWidth() != w || offscreen.getHeight() != h) {
            offscreen = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            offG = offscreen.createGraphics();
            offG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // Background
        offG.setColor(BG);
        offG.fillRect(0, 0, w, h);

        // Scanline effect
        offG.setColor(new Color(0, 255, 0, 8));
        for (int y2 = 0; y2 < h; y2 += 4) {
            offG.drawLine(0, y2, w, y2);
        }

        // Grid
        offG.setColor(GRID);
        for (int gx = 0; gx < w; gx += 50) offG.drawLine(gx, 0, gx, h);
        for (int gy = 0; gy < h; gy += 50) offG.drawLine(0, gy, w, gy);

        // Border radar rings
        offG.setColor(new Color(0, 120, 40));
        offG.drawOval(w / 2 - 200, h / 2 - 170, 400, 340);
        offG.drawOval(w / 2 - 130, h / 2 - 110, 260, 220);

        // Missiles
        for (Missile m : sim.missiles) {
            offG.setColor(MISSILE);
            offG.fillOval((int) m.x - 3, (int) m.y - 3, 6, 6);
            offG.setColor(new Color(255, 180, 0, 120));
            offG.fillOval((int) m.x - 5, (int) m.y - 5, 10, 10);
        }

        // Targets - red triangles
        offG.setFont(new Font("Courier New", Font.BOLD, 9));
        for (Target t : sim.targets) {
            offG.setColor(TARGET_COL);
            int[] xs = {(int) t.x, (int) t.x - 7, (int) t.x + 7};
            int[] ys = {(int) t.y - 8, (int) t.y + 6, (int) t.y + 6};
            offG.fillPolygon(xs, ys, 3);
            offG.setColor(new Color(255, 100, 100));
            offG.drawString(t.targetId, (int) t.x - 14, (int) t.y + 18);
        }

        // Drones - green diamonds
        offG.setFont(new Font("Courier New", Font.BOLD, 9));
        for (Drone d : sim.drones) {
            Color dc = d.returningHome    ? new Color(255, 200, 0)
                     : (d.missilesLeft == 0 ? new Color(100, 255, 200)
                     : DRONE_COL);
            offG.setColor(dc);
            int[] dxs = {(int) d.x,     (int) d.x - 8, (int) d.x,     (int) d.x + 8};
            int[] dys = {(int) d.y - 8, (int) d.y,     (int) d.y + 8, (int) d.y};
            offG.fillPolygon(dxs, dys, 4);
            offG.setColor(Color.WHITE);
            offG.drawString("D" + d.id + " [" + d.missilesLeft + "]",
                            (int) d.x - 15, (int) d.y - 11);
        }

        // HUD overlay
        offG.setFont(new Font("Courier New", Font.BOLD, 11));
        offG.setColor(HUD_TEXT);
        offG.drawString("DRONES  : " + sim.drones.size(),   10, 20);
        offG.drawString("TARGETS : " + sim.targets.size(),  10, 35);
        offG.drawString("MISSILES: " + sim.missiles.size(), 10, 50);
        offG.drawString("COST    : $" + sim.totalCost,      10, 65);
        offG.drawString("HITS    : " + sim.targetsHit,      10, 80);
        offG.drawString("WAVE    : " + sim.waveNumber,      10, 95);

        // Top-right watermark
        offG.setFont(new Font("Courier New", Font.BOLD, 10));
        offG.setColor(new Color(0, 180, 80, 180));
        offG.drawString("TEEKSHANA TACTICAL v1.0", w - 175, 16);

        g.drawImage(offscreen, 0, 0, this);
    }
}