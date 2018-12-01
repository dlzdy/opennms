/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.dao.hibernate;

import java.util.List;

import org.opennms.netmgt.dao.api.TopologyEntityDao;
import org.opennms.netmgt.model.CdpLinkTopologyEntity;
import org.opennms.netmgt.model.IpInterfaceTopologyEntity;
import org.opennms.netmgt.model.NodeTopologyEntity;
import org.opennms.netmgt.model.SnmpInterfaceTopologyEntity;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class TopologyEntityDaoHibernate extends HibernateDaoSupport implements TopologyEntityDao {

    @Override
    public List<NodeTopologyEntity> getNodeTopologyEntities() {
        return (List<NodeTopologyEntity>)getHibernateTemplate().find(
                "select new org.opennms.netmgt.model.NodeTopologyEntity(n.id, n.type, n.sysObjectId, n.label, n.location) from org.opennms.netmgt.model.OnmsNode n");
    }

    @Override
    public List<CdpLinkTopologyEntity> getCdpLinkTopologyEntities() {
        return (List<CdpLinkTopologyEntity>)getHibernateTemplate().find(
                "select new org.opennms.netmgt.model.CdpLinkTopologyEntity(l.id, l.node.id, l.cdpCacheIfIndex, " +
                        "l.cdpInterfaceName, l.cdpCacheAddress, l.cdpCacheDeviceId, l.cdpCacheDevicePort) from org.opennms.netmgt.model.CdpLink l");
    }

    @Override
    public List<SnmpInterfaceTopologyEntity> getSnmpTopologyEntities() {
        return (List<SnmpInterfaceTopologyEntity>)getHibernateTemplate().find(
                "select new org.opennms.netmgt.model.SnmpInterfaceTopologyEntity(" +
                        "i.id, i.ifIndex, i.ifName, i.ifSpeed, i.node.id) from org.opennms.netmgt.model.OnmsSnmpInterface i");
    }

    @Override
    public List<IpInterfaceTopologyEntity> getIpTopologyEntities() {
        return (List<IpInterfaceTopologyEntity>)getHibernateTemplate().find(
                "select new org.opennms.netmgt.model.IpInterfaceTopologyEntity(" +
                        "i.id, i.ipAddress, i.isManaged, i.isSnmpPrimary, i.node.id, i.snmpInterface.id) " +
                        "from org.opennms.netmgt.model.OnmsIpInterface i");
    }
}
