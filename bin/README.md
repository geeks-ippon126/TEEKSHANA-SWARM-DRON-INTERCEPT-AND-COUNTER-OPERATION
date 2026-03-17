# 🛸 Teekshana Tactical — Drone Swarm Mission Simulator

A real-time Java Swing application simulating autonomous drone swarm operations, with a MySQL-backed mission database, CRUD management panel, and live tactical display.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [How It Works](#how-it-works)
- [Configuration](#configuration)

---

## Overview

Teekshana Tactical is a desktop simulation where a configurable swarm of drones autonomously detects, tracks, and neutralises moving targets across waves. All mission outcomes, drone telemetry, and reinforcement requests are persisted to a MySQL database in real time.

---

## Features

- **Live Tactical Display** — Green-on-black HUD with drones (diamonds), targets (triangles), and missiles rendered using double-buffered Java2D graphics.
- **Wave System** — Targets spawn in escalating waves; each wave grows by one additional target.
- **Autonomous Combat AI** — Each drone finds the nearest unengaged target and fires automatically.
- **Reload & Ammo Management** — Drones carry 10 missiles, auto-reload after a 5-second cooldown.
- **Reinforcement Logic** — If drone losses exceed 20% of the initial fleet, reinforcement drones are automatically deployed and logged.
- **Mission Database** — Full CRUD for missions, drones, and reinforcement records via MySQL.
- **Mission History** — Sortable table of past missions with targets hit, drones lost, and accuracy.
- **CSV Export** — One-click export of mission log to a timestamped `.csv` file.
- **60-Second Mission Timer** — Missions auto-complete after 60 seconds; drones return to base.

---

## Project Structure

```
src/
├── core/
│   ├── Drone.java              # Drone entity: movement, firing, reload logic
│   ├── Missile.java            # Missile entity: homing trajectory, hit detection
│   ├── MissionSimulator.java   # Simulation engine: tick loop, wave spawner, stats
│   └── Target.java             # Target entity: random movement, hit status
│
├── ui/
│   ├── MissionControl.java     # Main JFrame: controls, tables, DB integration
│   └── TacticalDisplay.java    # JPanel: real-time 2D tactical rendering
│
├── dao/
│   ├── DroneDAO.java           # CRUD for drones table
│   ├── MissionDAO.java         # CRUD for missions table
│   └── ReinforcementDAO.java   # CRUD for reinforcements table
│
└── utils/
    └── DatabaseConnection.java # Singleton MySQL connection manager
```

---

## Architecture

```
MissionControl (UI)
    │
    ├── MissionSimulator (Engine)
    │       ├── Drone[]     → movement, firing, reload
    │       ├── Target[]    → movement, hit status
    │       └── Missile[]   → homing, impact detection
    │
    ├── TacticalDisplay (Renderer)
    │
    └── DAO Layer
            ├── MissionDAO      → missions table
            ├── DroneDAO        → drones table
            └── ReinforcementDAO → reinforcements table
                    │
                    └── DatabaseConnection (utils)
                                └── MySQL: drone_swarm DB
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 11 or higher |
| MySQL Server | 8.0+ |
| MySQL Connector/J | 8.x (`mysql-connector-j-x.x.x.jar`) |

Add `mysql-connector-j-*.jar` to your project's classpath/libraries before building.

---

## Database Setup

Run the following SQL to create the required schema:

```sql
CREATE DATABASE IF NOT EXISTS drone_swarm;
USE drone_swarm;

CREATE TABLE missions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    mission_name VARCHAR(100),
    targets_hit  INT DEFAULT 0,
    drones_lost  INT DEFAULT 0,
    accuracy     DOUBLE DEFAULT 0.0,
    status       VARCHAR(20) DEFAULT 'active',
    start_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time     TIMESTAMP NULL
);

CREATE TABLE drones (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    drone_id    VARCHAR(20) UNIQUE,
    status      VARCHAR(20) DEFAULT 'active',
    mission_id  INT,
    fuel_level  INT DEFAULT 100,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE reinforcements (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    mission_id       INT,
    requested_drones INT,
    approved_drones  INT DEFAULT 0,
    status           VARCHAR(20) DEFAULT 'pending',
    request_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Update credentials in `DatabaseConnection.java` if needed:

```java
private static final String USER     = "root";
private static final String PASSWORD = "your_password_here";
```

---

## Running the Application

1. Clone or download the project.
2. Add `mysql-connector-j-*.jar` to the build path.
3. Ensure MySQL is running and the schema is created.
4. Run `MissionControl.java` as the main entry point.

---

## How It Works

1. **Configure** — Set the number of drones and targets per wave using the spinners.
2. **Launch** — Click **LAUNCH MISSION**. The simulation starts a 60-second timer.
3. **Simulation loop** — Every 30 ms:
   - Drones patrol autonomously and fire at the nearest unengaged target.
   - Missiles home in on targets; hits are logged and counted.
   - New waves spawn when the field is cleared or every ~3.6 seconds if targets drop below 3.
   - If losses exceed 20%, reinforcements are auto-deployed and recorded in the DB.
4. **Mission End** — After 60 seconds, drones return home, final stats are saved to MySQL.
5. **History & Export** — View past missions in the History tab or export the log as CSV.

---

## Configuration

| Constant | Location | Default | Description |
|---|---|---|---|
| `SIM_DURATION_MS` | `MissionControl` | `60000` | Mission length in ms |
| `MISSILES_PER_DRONE` | `Drone` | `10` | Missiles per drone load |
| `RELOAD_TIME_MS` | `Drone` | `5000` | Reload cooldown in ms |
| `MISSILE_COST` | `Drone` | `$1000` | Cost per missile fired |
| `WAVE_INTERVAL_TICKS` | `MissionSimulator` | `120` | Ticks between forced waves |
| `LOSS_THRESHOLD` | `MissionSimulator` | `0.20` | Drone loss % triggering reinforcement |

---

## Database Manager

The `DatabaseManager.java` panel provides a standalone CRUD interface to directly browse and edit all three tables (missions, drones, reinforcements) without running a simulation.

---

*TEEKSHANA TACTICAL v1.0*
