//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2005 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2003 Nov 11: Merged changes from Rackspace project
// 2003 Jan 31: Cleaned up some unused imports.
// 2004 Sep 08: Completely reorganize to clean up the delete code.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.                                                            
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact: 
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.linkd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Category;

import org.opennms.core.utils.ThreadCategory;

import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.CapsdConfigFactory;
import org.opennms.netmgt.config.OpennmsServerConfigFactory;
import org.opennms.netmgt.eventd.EventListener;
import org.opennms.netmgt.utils.XmlrpcUtil;
import org.opennms.netmgt.xml.event.Event;

import org.opennms.netmgt.capsd.InsufficientInformationException;

/**
 * @author <a href="mailto:rssntn67@yahoo.it">Antonio Russo</a>
 * @author <a href="mailto:matt@opennms.org">Matt Brozowski </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
 */
final class LinkdEventProcessor implements EventListener {

    /**
     * 
     * @return Returns the xmlrpc.
     */
    public static boolean isXmlRpcEnabled() {
        return CapsdConfigFactory.getInstance().getXmlrpc().equals("true");
    }

    /**
     * local openNMS server name
     */
    private String m_localServer = null;

    /**
     * Set of event ueis that we should notify when we receive and when a
     * success or failure occurs.
     */
    private Set m_notifySet = new HashSet();

    /**
     * The Linkd rescan scheduler
     */
    private Linkd m_linkd;

    /**
     * Constructor
     * 
     * @param linkd.
     */
    
    LinkdEventProcessor(Linkd linkd) {
        m_linkd = linkd;

        // the local servername
        m_localServer = OpennmsServerConfigFactory.getInstance().getServerName();

        // Subscribe to eventd
        //
        createMessageSelectorAndSubscribe();
    }

    /**
     * Unsubscribe from eventd
     */
    public void close() {
        getLinkd().getIpcManager().removeEventListener(this);
    }

    /**
     * Create message selector to set to the subscription
     */
    private void createMessageSelectorAndSubscribe() {
        // Create the selector for the ueis this service is interested in
        //
        List<String> ueiList = new ArrayList<String>();

        // node gained service
        ueiList.add(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI);

        // node lost service
        ueiList.add(EventConstants.NODE_LOST_SERVICE_EVENT_UEI);

        // nodeDeleted
        ueiList.add(EventConstants.NODE_DELETED_EVENT_UEI);

        // node regained service
        ueiList.add(EventConstants.NODE_REGAINED_SERVICE_EVENT_UEI);

        getLinkd().getIpcManager().addEventListener(this, ueiList);
    }

    /**
     * Get the local server name
     */
    public String getLocalServer() {
        return m_localServer;
    }

    /**
     * Return an id for this event listener
     */
    public String getName() {
        return "Linkd:LinkdEventProcessor";
    }

    /**
     * @return
     */

    private Linkd getLinkd() {
        return m_linkd;
    }


    /**
     * Handle a Node Deleted Event
     * 
     * @param event
     */
    private void handleNodeDeleted(Event event) throws InsufficientInformationException {
 
        EventUtils.checkNodeId(event);

        // Remove the deleted node from the scheduler if it's an SNMP node
        getLinkd().deleteNode((int)event.getNodeid());
        // set to status = D in all the rows in table
        // atinterface, iprouteinterface, datalinkinterface,stpnode, stpinterface
    }

    /**
     * Handle a Node Gained Service Event if service is SNMP
     * 
     * @param event
     */
    private void handleNodeGainedService(Event event) throws InsufficientInformationException {
 
        EventUtils.checkNodeId(event);

        getLinkd().scheduleNodeCollection((int)event.getNodeid());
    }

    /**
     * Handle a Node Lost Service Event when service lost is SNMP
     * 
     * @param event
     */
    private void handleNodeLostService(Event event) throws InsufficientInformationException {
        
        EventUtils.checkNodeId(event);

        // Remove the deleted node from the scheduler
        getLinkd().suspendNodeCollection((int)event.getNodeid());
        // set to status = N in all the rows in table
        // atinterface, iprouteinterface, datalinkinterface,
    }

    /**
     * Handle a Node Regained Service Event where service is SNMP
     * 
     * @param event
     */
    private void handleRegainedService(Event event) throws InsufficientInformationException {
        
        EventUtils.checkNodeId(event);

        getLinkd().wakeUpNodeCollection((int)event.getNodeid());
    }

    /**
     * Notify Event Error to XML RPC Service if enabled
     * 
     * @param event
     * @param msg
     * @param ex
     */
    private void notifyEventError(Event event, String msg, Exception ex) {
        if (!isXmlRpcEnabled())
            return;

        long txNo = EventUtils.getLongParm(event, EventConstants.PARM_TRANSACTION_NO, -1L);
        if ((txNo != -1) && m_notifySet.contains(event.getUei())) {
            int status = EventConstants.XMLRPC_NOTIFY_FAILURE;
            XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, event.getUei(), msg + ex.getMessage(), status, "OpenNMS.Capsd");
        }
    }

    /**
     * Notify Event Received to XML RPC Service if enabled
     * 
     * @param event
     */
    private void notifyEventReceived(Event event) {
        if (!isXmlRpcEnabled())
            return;

        long txNo = EventUtils.getLongParm(event, EventConstants.PARM_TRANSACTION_NO, -1L);

        if ((txNo != -1) && m_notifySet.contains(event.getUei())) {
            StringBuffer message = new StringBuffer("Received event: ");
            message.append(event.getUei());
            message.append(" : ");
            message.append(event);
            int status = EventConstants.XMLRPC_NOTIFY_RECEIVED;
            XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, event.getUei(), message.toString(), status, "OpenNMS.Capsd");
        }
    }

    /**
     * Notify Event Success to XML RPC Service if enabled
     * 
     * @param event
     */
    private void notifyEventSuccess(Event event) {
        if (!isXmlRpcEnabled())
            return;

        long txNo = EventUtils.getLongParm(event, EventConstants.PARM_TRANSACTION_NO, -1L);

        if ((txNo != -1) && m_notifySet.contains(event.getUei())) {
            StringBuffer message = new StringBuffer("Completed processing event: ");
            message.append(event.getUei());
            message.append(" : ");
            message.append(event);
            int status = EventConstants.XMLRPC_NOTIFY_SUCCESS;
            XmlrpcUtil.createAndSendXmlrpcNotificationEvent(txNo, event.getUei(), message.toString(), status, "OpenNMS.Capsd");
        }
    }

    /**
     * This method is invoked by the EventIpcManager when a new event is
     * available for processing. Currently only text based messages are
     * processed by this callback. Each message is examined for its Universal
     * Event Identifier and the appropriate action is taking based on each UEI.
     * 
     * @param event
     *            The event.
     * 
     */
    public void onEvent(Event event) {
        Category log = ThreadCategory.getInstance(getClass());

        try {
        	int eventid = event.getDbid();
            String eventUei = event.getUei();
            String eventService = event.getService();
            if (eventUei == null) {
                return;
            }

            if (log.isInfoEnabled()) {
                log.info("onEvent: Received event " + eventid + " UEI "+ eventUei 
                		+ "; service " + eventService);
            }

            notifyEventReceived(event);

            if (eventUei.equals(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI) && eventService.equals("SNMP")) {
                if (log.isInfoEnabled()) {
                    log.info("onEvent: calling handleNodeGainedService for event " + eventid);
                }
                handleNodeGainedService(event);
            } else if (event.getUei().equals(EventConstants.NODE_LOST_SERVICE_EVENT_UEI)&& eventService.equals("SNMP")) {
                if (log.isInfoEnabled()) {
                    log.info("onEvent: calling handleNodeLostService for event " + eventid);
                }
                handleNodeLostService(event);
            } else if (event.getUei().equals(EventConstants.NODE_REGAINED_SERVICE_EVENT_UEI)&& eventService.equals("SNMP")) {
            	if (log.isInfoEnabled()) {
                    log.info("onEvent: calling handleRegainedService for event " + eventid);
                }
            	handleRegainedService(event);
            } else if (eventUei.equals(EventConstants.NODE_DELETED_EVENT_UEI)) {
            	if (log.isInfoEnabled()) {
                    log.info("onEvent: calling handleNodeDeleted for event " + eventid);
                }
                handleNodeDeleted(event);
            } 
            notifyEventSuccess(event);
        } catch (InsufficientInformationException ex) {
            log.info("onEvent: insufficient information in event, discarding it: " + ex.getMessage());
            notifyEventError(event, "Invalid parameters: ", ex);
        } catch (Throwable t) {
            log.error("onEvent: operation failed for event: " + event.getUei() + ", exception: " + t.getMessage());
        }
    } // end onEvent()
    

} // end class

