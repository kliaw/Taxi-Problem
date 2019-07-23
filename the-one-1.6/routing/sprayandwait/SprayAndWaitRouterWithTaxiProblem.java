/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.sprayandwait;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import report.Report;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Katarine Layung Indra Aprilia | 155314013
 */
public class SprayAndWaitRouterWithTaxiProblem extends Report implements RoutingDecisionEngine, CountingTaxiProblem{

    public static final String NROF_COPIES = "nrofCopies";
    public static final String BINARY_MODE = "binaryMode";
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouterWithTaxiProblem";
    public static final String MSG_COUNT_PROPERTY = "."
            + "copies";
    protected int initialNrofCopies;
    protected boolean isBinary;
    private int totalEstimationOfTheNode;
    protected Set<DTNHost> theCollections;

    public SprayAndWaitRouterWithTaxiProblem(Settings s) {
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            isBinary = false; 
        }

        if (s.contains(NROF_COPIES)) {
            initialNrofCopies = s.getInt(NROF_COPIES);
        }
    }

    public SprayAndWaitRouterWithTaxiProblem(SprayAndWaitRouterWithTaxiProblem proto) {

        this.initialNrofCopies = proto.initialNrofCopies;
        this.isBinary = proto.isBinary;
        this.theCollections = new HashSet<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    @Override
    /**
     * print the host and the list of the node that met
     */
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        SprayAndWaitRouterWithTaxiProblem partner = getOtherDecisionEngine(peer);
        
            this.totalEstimationOfTheNode = this.CountTotalEstimationOfTheNode();
            partner.totalEstimationOfTheNode = partner.CountTotalEstimationOfTheNode();
            
            System.out.println(thisHost.getAddress() + " Isi Set = "+this.theCollections);
            System.out.println(peer.getAddress() + " Isi Set = "+partner.theCollections);
            System.out.println("");
    }

    @Override
    /**
     * doExchange is for scenario node sharing of the informations or not
     * if not share the set just comment .addAll(partner) and .addAll(this.the..)
     */
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {

        DTNHost thisHost = con.getOtherNode(peer);
        SprayAndWaitRouterWithTaxiProblem partner = getOtherDecisionEngine(peer);
            this.theCollections.add(peer);
            partner.theCollections.add(thisHost);
            this.theCollections.addAll(partner.theCollections);
            partner.theCollections.addAll(this.theCollections);
    }

    @Override
    /**
     * estimation of the copies at the network with n/2 of total nodes
     */
    public boolean newMessage(Message m
    ) {
        initialNrofCopies = this.totalEstimationOfTheNode / 2;
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost
    ) {
        return m.getTo() == aHost;
    }

    @Override
    /**
     * estimation of the copies at the network with n/2 of total nodes
     */
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        } else {
            nrofCopies = 1;
        }
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return true;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (nrofCopies > 1) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {

        if (m.getTo() == otherHost) {
            return false;
        }
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (isBinary) {
            nrofCopies /= 2;
        } else {
            nrofCopies--;
        }
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return false;

    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld
    ) {

        return m.getTo() == hostReportingOld;
    }

    @Override
    public RoutingDecisionEngine replicate() {

        return new SprayAndWaitRouterWithTaxiProblem(this);
    }

    private SprayAndWaitRouterWithTaxiProblem getOtherDecisionEngine(DTNHost h) {

        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + "with other routers of same type";

        return (SprayAndWaitRouterWithTaxiProblem) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    /**
     * the formula of the taxi problem algorithm
     * then the result is rounded down
     */
    private int CountTotalEstimationOfTheNode() {
        int tracehold = Integer.MIN_VALUE;
        for (Iterator<DTNHost> iterator = theCollections.iterator(); iterator.hasNext();) {
            DTNHost next = iterator.next();

            if (next.getAddress() > tracehold) {
                tracehold = next.getAddress();
            }
        }
        double atas = Math.pow(tracehold, (theCollections.size() + 1)) - Math.pow((tracehold - 1), (theCollections.size() + 1));
        double bawah = Math.pow(tracehold, theCollections.size()) - Math.pow((tracehold - 1), theCollections.size());
        return (int) (Math.floor(atas / bawah));

    }

    @Override
    public int getCountTotalEstimationOfTheNode() {
        return this.totalEstimationOfTheNode;
    }

 
    private int getTotalEstimationOfTheNode() {
        return totalEstimationOfTheNode;
    }

    private void setTotalEstimationOfTheNode(int totalEstimationOfTheNode) {
        this.totalEstimationOfTheNode = totalEstimationOfTheNode;
    }

}
