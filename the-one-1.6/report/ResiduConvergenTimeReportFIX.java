package report;

/**
 * Records the average buffer occupancy and its variance with format:
 * <p>
 * <Simulation time> <average buffer occupancy % [0..100]> <variance>
 * </p>
 *
 */
import java.util.*;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.UpdateListener;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.sprayandwait.CountingTaxiProblem;

public class ResiduConvergenTimeReportFIX extends Report implements UpdateListener {

    /**
     * Record occupancy every nth second -setting id ({@value}). Defines the
     * interval how often (seconds) a new snapshot of buffer occupancy is taken
     * previous:5
     */
    public static final String NODE_PERWAKTU = "nodepersatuanwaktu";
    /**
     * Default value for the snapshot interval
     */
    public static final int DEFAULT_WAKTU = 1800;
    private double lastRecord = Double.MIN_VALUE;
    private int interval;
    private Map<DTNHost, ArrayList<Double>> countingGossip = new HashMap<DTNHost, ArrayList<Double>>();

    public ResiduConvergenTimeReportFIX() {
        super();

        Settings settings = getSettings();
        if (settings.contains(NODE_PERWAKTU)) {
            interval = settings.getInt(NODE_PERWAKTU);
        } else {
            interval = DEFAULT_WAKTU;
        }
    }

    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastRecord >= interval) {
            lastRecord = SimClock.getTime();
            printLine(hosts);
        }
    }

    /**
     * Prints a snapshot of the average buffer occupancy
     *
     * @param hosts The list of hosts in the simulation
     */
    private void printLine(List<DTNHost> hosts) {
        /**
         * nrofNode is maximum counting or estimation at every movement
         * if use Haggle3 so should uncomment int nrofNode = 40;
         */
        
//        int nrofNode = 40; // Haggle 3
//        int nrofNode = 35; // Haggle 4
        int nrofNode = 99; // Random
//        int nrofNode = 96; // Reality

        int residu = 0;
        double rata = 0;
        for (DTNHost h : hosts) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            CountingTaxiProblem n = (CountingTaxiProblem) de;

            int temp = (int) n.getCountTotalEstimationOfTheNode();
            if (temp < nrofNode) {
                residu++;
            }
        }
        /**
         * print the interval and total node
         */
        int TotalResidu = residu;
        String output = format((int) SimClock.getTime()) + " \t " + format(TotalResidu);
        write(output);
    }
}
