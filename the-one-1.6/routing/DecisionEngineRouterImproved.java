/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.*;
import java.util.*;
import routing.util.*;
import util.*;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public class DecisionEngineRouterImproved extends ActiveRouter
{
        public static final String PUBSUB_NS = "DecisionEngineRouterImproved";
	public static final String ENGINE_SETTING = "decisionEngine";
	public static final String TOMBSTONE_SETTING = "tombstones";
	public static final String CONNECTION_STATE_SETTING = "";
        /** All router default static update interval every 15 minutes */
        public static final int UPDATE_INTERVAL = 900;
	
        protected double lastUpdate = Double.MIN_VALUE;
	protected boolean tombstoning;
	protected RoutingDecisionEngineImproved improvedDecider;
	protected List<Tuple<Message, Connection>> outgoingMessages;
	
	protected Set<String> tombstones;
        
        private EnergyModel routerEnergy;
        private MessageTransferAcceptPolicy routerPolicy;
	
	/** 
	 * Used to save state machine when new connections are made. See comment in
	 * changedConnection() 
	 */
	protected Map<Connection, Integer> conStates;
	
	public DecisionEngineRouterImproved(Settings s)
	{
		super(s);
		
		Settings routeSettings = new Settings(PUBSUB_NS);
		
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		
		improvedDecider = (RoutingDecisionEngineImproved)routeSettings.createIntializedObject(
				"routing." + routeSettings.getSetting(ENGINE_SETTING));
		
		if(routeSettings.contains(TOMBSTONE_SETTING))
			tombstoning = routeSettings.getBoolean(TOMBSTONE_SETTING);
		else
			tombstoning = false;
		
		if(tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
                
                if (routeSettings.contains(EnergyModel.INIT_ENERGY_S)) {
			this.routerEnergy = new EnergyModel(routeSettings);
		} else {
			this.routerEnergy = null; /* no energy model */
		}
                
                this.routerPolicy = new MessageTransferAcceptPolicy(routeSettings);
	}

	public DecisionEngineRouterImproved(DecisionEngineRouterImproved r)
	{
		super(r);
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		improvedDecider = (RoutingDecisionEngineImproved) r.improvedDecider.replicate();
		this.routerPolicy = r.routerPolicy;
		this.routerEnergy = (r.routerEnergy != null ? r.routerEnergy.replicate() : null);
                tombstoning = r.tombstoning;
		
		if(this.tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
	}

	@Override
	public MessageRouter replicate()
	{
		return new DecisionEngineRouterImproved(this);
	}

	@Override
	public boolean createNewMessage(Message m)
	{
		if(improvedDecider.newMessage(m))
		{
			if(m.getId().equals("M7"))
				System.out.println("Host: " + getHost() + " Creating M7");
			makeRoomForNewMessage(m.getSize());
			//revised by Matthew
			m.setTtl(this.msgTtl);
			
			addToMessages(m, true);
			
			findConnectionsForNewMessage(m, getHost());
			return true;
		}
		return false;
	}
	
	@Override
	public void changedConnection(Connection con)
	{
                
                DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		DecisionEngineRouterImproved otherRouter = (DecisionEngineRouterImproved)otherNode.getRouter();
		if(con.isUp())
		{
			improvedDecider.connectionUp(myHost, otherNode);
			
                        if (this.routerEnergy != null && otherRouter.routerEnergy != null) {
                                this.routerEnergy.reduceDiscoveryEnergy();
                                otherRouter.routerEnergy.reduceDiscoveryEnergy();
                                System.out.println(myHost+" and "+otherNode+" energy has been reduce at "+this.routerEnergy.getEnergy());
                        }
                        
                        
			if(shouldNotifyPeer(con))
			{
				this.doExchange(con, otherNode);
				otherRouter.didExchange(con);
			}
			
			/*
			 * Once we have new information computed for the peer, we figure out if
			 * there are any messages that should get sent to this peer.
			 */
			Collection<Message> msgs = getMessageCollection();
			for(Message m : msgs)
			{
				if(improvedDecider.shouldSendMessageToHost(m, otherNode))
					outgoingMessages.add(new Tuple<Message,Connection>(m, con));
			}
		}
		else
		{
			improvedDecider.connectionDown(myHost, otherNode);
			
			conStates.remove(con);
			
			/*
			 * If we  were trying to send message to this peer, we need to remove them
			 * from the outgoing List.
			 */
			for(Iterator<Tuple<Message,Connection>> i = outgoingMessages.iterator(); 
					i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getValue() == con)
					i.remove();
			}
		}
	}
	
	protected void doExchange(Connection con, DTNHost otherHost)
	{
		conStates.put(con, 1);
		improvedDecider.doExchangeForNewConnection(con, otherHost);
	}
	
	/**
	 * Called by a peer DecisionEngineRouter to indicated that it already 
	 * performed an information exchange for the given connection.
	 * 
	 * @param con Connection on which the exchange was performed
	 */
	protected void didExchange(Connection con)
	{
		conStates.put(con, 1);
	}
	
	@Override
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		
		if (!con.isReadyForTransfer()) {
			return TRY_LATER_BUSY;
		}
                
                if (!routerPolicy.acceptSending(getHost(), 
				con.getOtherNode(getHost()), con, m)) {
			return MessageRouter.DENIED_POLICY;
		}
		
		retVal = con.startTransfer(getHost(), m);
		if (retVal == RCV_OK) { // started transfer
			addToSendingConnections(con);
		}
		else if(tombstoning && retVal == DENIED_DELIVERED)
		{
			this.deleteMessage(m.getId(), false);
			tombstones.add(m.getId());
		}
		else if (deleteDelivered && (retVal == DENIED_OLD || retVal == DENIED_DELIVERED) && 
				improvedDecider.shouldDeleteOldMessage(m, con.getOtherNode(getHost()))) {
			/* final recipient has already received the msg -> delete it */
			if(m.getId().equals("M7"))
				System.out.println("Host: " + getHost() + " told to delete M7");
			this.deleteMessage(m.getId(), false);
		}
		
		return retVal;
	}

        protected int checkReceiving(Message m, DTNHost from){
                super.checkReceiving(m, from);
            
                if (routerEnergy != null && routerEnergy.getEnergy() <= 0) {
			return MessageRouter.DENIED_LOW_RESOURCES;
		}
		
		if (!routerPolicy.acceptReceiving(from, getHost(), m)) {
			return MessageRouter.DENIED_POLICY;
		}
                
                return RCV_OK;
        }
        
        
	@Override
	public int receiveMessage(Message m, DTNHost from)
	{
		if(isDeliveredMessage(m) || (tombstoning && tombstones.contains(m.getId())))
			return DENIED_DELIVERED;
			
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from)
	{
		Message incoming = removeFromIncomingBuffer(id, from);
	
		if (incoming == null) {
			throw new SimError("No message with ID " + id + " in the incoming "+
					"buffer of " + getHost());
		}
		
		incoming.setReceiveTime(SimClock.getTime());
		
		Message outgoing = incoming;
		for (Application app : getApplications(incoming.getAppID())) {
			// Note that the order of applications is significant
			// since the next one gets the output of the previous.
			outgoing = app.handle(outgoing, getHost());
			if (outgoing == null) break; // Some app wanted to drop the message
		}
		
		Message aMessage = (outgoing==null)?(incoming):(outgoing);
		
		boolean isFinalRecipient = improvedDecider.isFinalDest(aMessage, getHost());
		boolean isFirstDelivery =  isFinalRecipient && 
			!isDeliveredMessage(aMessage);
		
		if (outgoing!=null && improvedDecider.shouldSaveReceivedMessage(aMessage, getHost())) 
		{
			// not the final recipient and app doesn't want to drop the message
			// -> put to buffer
			addToMessages(aMessage, false);
			
			// Determine any other connections to which to forward a message
			findConnectionsForNewMessage(aMessage, from);
		}
		
		if (isFirstDelivery)
		{
			this.deliveredMessages.put(id, aMessage);
		}
		
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferred(aMessage, from, getHost(),
					isFirstDelivery);
		}
		
		return aMessage;
	}

	@Override
	protected void transferDone(Connection con)
	{
		Message transferred = this.getMessage(con.getMessage().getId());
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(transferred.getId()) && 
					t.getValue().equals(con))
			{
				i.remove();
				break;
			}
		}
		
                improvedDecider.transferDone(con);
                
		if(improvedDecider.shouldDeleteSentMessage(transferred, con.getOtherNode(getHost())))
		{
			if(transferred.getId().equals("M7"))
				System.out.println("Host: " + getHost() + " deleting M7 after transfer");
			this.deleteMessage(transferred.getId(), false);
			
			
		}
	}

	@Override
	public void update()
	{
		super.update();
                
                if (routerEnergy != null) {
			NetworkInterface iface = getHost().getInterface(1);
			routerEnergy.update(iface, getHost().getComBus());
		}
                
                improvedDecider.update(getHost());
		
				
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		tryMessagesForConnected(outgoingMessages);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(!this.hasMessage(t.getKey().getId()))
			{
				i.remove();
			}
		}
	}
	
	@Override
	public void deleteMessage(String id, boolean drop)
	{
		super.deleteMessage(id, drop);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(id))
			{
				i.remove();
			}
		}
	}

	public RoutingDecisionEngineImproved getDecisionEngine()
	{
		return this.improvedDecider;
	}

	protected boolean shouldNotifyPeer(Connection con)
	{
		Integer i = conStates.get(con);
		return i == null || i < 1;
	}
	
        /**
	 * Returns true if the node has energy left (i.e., energy modeling is
	 * enabled OR (is enabled and model has energy left))
	 * @return has the node energy
	 */
	public boolean hasEnergy() {
		return this.routerEnergy == null || this.routerEnergy.getEnergy() > 0;
	}
        
	protected void findConnectionsForNewMessage(Message m, DTNHost from)
	{
		for(Connection c : getConnections())
		{
			DTNHost other = c.getOtherNode(getHost());
			if(other != from && improvedDecider.shouldSendMessageToHost(m, other))
			{
				if(m.getId().equals("M7"))
					System.out.println("Adding attempt for M7 from: " + getHost() + " to: " + other);
				outgoingMessages.add(new Tuple<Message, Connection>(m, c));
			}
		}
	}
        
        public RoutingInfo getRoutingInfo(){
            RoutingInfo top = super.getRoutingInfo();
		if (routerEnergy != null) {
			top.addMoreInfo(new RoutingInfo("Energy level: " + 
					String.format("%.2f mAh", routerEnergy.getEnergy() / 3600)));
		}
		return top;
        }
}
