# Taxi Problem: Node Estimation in DTN
Undergraduate thesis project (2018/2019) by **Layung I. Aprilia**.

This project estimates the total number of nodes in a **Delay Tolerant Network (DTN)** using the **Taxi Problem** estimator, then uses that estimate to decide how many message copies the **Spray and Wait** routing protocol should spray. It is implemented as an extension of [The ONE Simulator](https://github.com/akeranen/the-one) v1.6 and developed in NetBeans.

## 📌 About the Project
In a DTN, nodes are connected only intermittently, so no node knows how many nodes exist in the network. That number matters for Spray and Wait, whose performance depends heavily on the initial number of copies (`L`) of each message. Normally `L` is a fixed value chosen by hand.

This project removes that manual choice:

1. Each node keeps a set of the node IDs it has met (the "taxi numbers" it has seen).
2. When two nodes meet, they merge their sets, so knowledge spreads through the network.
3. Each node applies the **Taxi Problem** formula to its set to estimate the total number of nodes `N`.
4. When a node creates a message, it sets the initial number of copies to **`N / 2`**, then routes it with binary Spray and Wait.

### The estimator
With `m` as the highest node ID in the set and `k` as the number of distinct nodes in the set:

```
N_estimate = floor( (m^(k+1) - (m-1)^(k+1)) / (m^k - (m-1)^k) )
```

The implementation is in [`SprayAndWaitRouterWithTaxiProblem.java`](the-one-1.6/routing/sprayandwait/SprayAndWaitRouterWithTaxiProblem.java), which plugs into The ONE's `DecisionEngineRouter`. A standard Spray and Wait with a fixed number of copies ([`SprayandWaitDE.java`](the-one-1.6/routing/sprayandwait/SprayandWaitDE.java)) is included as the baseline for comparison.

## 🎯 Objectives
* Estimate the number of nodes in a DTN without global knowledge, using the Taxi Problem.
* Use the estimate to set the number of Spray and Wait copies automatically.
* Compare the result against Spray and Wait with fixed copy counts, on real contact traces and a synthetic scenario.

## 🛠️ Technologies Used
| Component | Details |
|---|---|
| Language | Java (source/target 1.8) |
| IDE | NetBeans (Ant project) |
| Simulator | The ONE Simulator v1.6 |
| Routing | Spray and Wait (binary mode) via `DecisionEngineRouter` |
| Libraries | `DTNConsoleConnection.jar`, `ECLA.jar`, `junit-4.8.2.jar`, `uncommons-maths-1.2.1.jar` |

## 📂 Project Structure
```
Taxi-Problem/
├── library/                       # Required .jar dependencies + Haggle4 trace
├── one simulator/                 # NetBeans project (build.xml, nbproject/)
└── the-one-1.6/                   # The ONE Simulator source with the thesis additions
    ├── routing/sprayandwait/
    │   ├── SprayAndWaitRouterWithTaxiProblem.java   # Taxi Problem estimator + Spray and Wait
    │   ├── SprayandWaitDE.java                      # Baseline: Spray and Wait, fixed copies
    │   └── CountingTaxiProblem.java                 # Interface to read the node estimate
    ├── report/
    │   ├── MessageReportGetCounting.java            # Records node estimate per host over time
    │   ├── AverageConvergenTimeReport.java          # Average convergence over time
    │   └── ResiduConvergenTimeReportFIX.java        # Residual convergence over time
    ├── scenario/                  # Contact traces
    ├── taxi_settings_*.txt        # Taxi Problem scenarios
    └── saw_settings_*.txt         # Baseline Spray and Wait scenarios
```

### Libraries (`library/`)
| File | Description |
|---|---|
| `DTNConsoleConnection.jar` | Console connection library used by The ONE |
| `ECLA.jar` | Library dependency of The ONE |
| `junit-4.8.2.jar` | JUnit, for unit tests |
| `uncommons-maths-1.2.1.jar` | Math / random number library |
| `Haggle4-Cam-Imote.csv` | Haggle (Cambridge iMote) contact trace |

### Scenarios
| Dataset | Nodes | Taxi Problem settings | Baseline settings |
|---|---|---|---|
| Haggle3 (Infocom5) trace | 41 | `taxi_settings_haggle3.txt` | `saw_settings_haggle3.txt` |
| Reality Mining trace | 97 | `taxi_settings_reality.txt` | `saw_settings_reality.txt` |
| Random (RandomWaypoint) | 100 | `taxi_settings_random.txt` | `saw_settings_random.txt` |

Contact traces are in `the-one-1.6/scenario/`: `Haggle3-Infocom5.csv`, `Haggle4-Cam-Imote.csv`, `RealityConnectionTraceFinal.txt`, and `sassy.csv`.

In the baseline scenarios, `DecisionEngineRouter.nrofCopies` is given a list of values (for example `[4; 5; 40; 49; 95; 97]` for Haggle3), so ONE runs one simulation per value.

## 🚀 Getting Started
### Prerequisites
* **JDK 8** (the project targets Java 1.8; the old `javac -extdirs` build script does not work on Java 9+)
* NetBeans IDE (recommended)

### Run with NetBeans
1. Clone the repository:
   ```bash
   git clone https://github.com/kliaw/Taxi-Problem.git
   ```
2. Open the `one simulator` folder as a NetBeans project.
3. The project file still points to the original author's machine (`C:\Users\TAJarkom\Documents\SKRIPSI\...`). Fix the paths under **Project Properties → Libraries** (add the four `.jar` files from `library/`) and **Sources** (set the source root to `the-one-1.6`).
4. Set the program arguments under **Project Properties → Run → Arguments**, for example:
   ```
   -b 1 taxi_settings_haggle3.txt
   ```
5. Set the working directory to `the-one-1.6` and run (main class: `core.DTNSim`).

The project includes ready-made run configurations in `nbproject/private/configs/`, such as `taxi_haggle3` and `saw_reality`.

### Run from the command line
```bash
cd the-one-1.6
# compile (Windows: compile.bat), then:
./one.sh -b 1 taxi_settings_haggle3.txt
```

`-b N` runs in batch mode (no GUI) for `N` runs. Use `-b 1` for the Taxi Problem scenarios. For a baseline scenario with a list of `nrofCopies` values, set `N` to the number of values (for example `-b 6` for `saw_settings_haggle3.txt`). Leave out `-b` to use the GUI.

### Output

Reports are written to `the-one-1.6/reports/Report_Taxi/`. The Taxi Problem scenarios produce four reports: `MessageReportGetCounting` (node estimate per host), `MessageStatsReport` (delivery statistics), `AverageConvergenTimeReport`, and `ResiduConvergenTimeReportFIX`.

## 📚 References

* Keränen, A., Ott, J., Kärkkäinen, T. *The ONE Simulator for DTN Protocol Evaluation*. SIMUTools 2009.
* Spyropoulos, T., Psounis, K., Raghavendra, C. *Spray and Wait: An Efficient Routing Scheme for Intermittently Connected Mobile Networks*. ACM SIGCOMM WDTN 2005.
* Contact traces: Haggle (Cambridge iMote, Infocom) and MIT Reality Mining datasets.

## ⚠️ Notes
This is an academic project from 2018/2019, kept for archival and reference purposes. The `the-one-1.6` folder contains the unmodified The ONE 1.6 source plus the additions listed above; see its own `LICENSE` for license terms.

## 👤 Author

**Layung I. Aprilia** ([@kliaw](https://github.com/kliaw))
