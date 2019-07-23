/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.Connection;
import core.DTNHost;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public interface RoutingDecisionEngineImproved extends RoutingDecisionEngine{
    
    /**
     * Called when update times come
     * 
     * @param host the updated host
     */
    public void update(DTNHost host);
    
    /**
     * Called after a message was successfully transfered
     * 
     * @param con the requested connection
     */
    public void transferDone(Connection con);
}
