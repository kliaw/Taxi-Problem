/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.epidemic;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.Collection;
import routing.RoutingDecisionEngine;

/**
 *
 * @author TAJarkom
 */
public class EpidemicRouter implements RoutingDecisionEngine{

    public EpidemicRouter(Settings s){
        
    }
    
    public EpidemicRouter(EpidemicRouter prototype){
        
    }
    
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()== aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        Collection<Message> myMessageCollection = thisHost.getMessageCollection();
        
        for (Message messgae : myMessageCollection) {
            if (messgae.getId().equals(m.getId())) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return true;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new EpidemicRouter(this);
    }
    
}
