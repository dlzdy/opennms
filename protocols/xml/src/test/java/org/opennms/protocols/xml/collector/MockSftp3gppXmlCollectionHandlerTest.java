/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.protocols.xml.collector;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * The Test Class for MockSftp3gppXmlCollectionHandler.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class MockSftp3gppXmlCollectionHandlerTest {

    /**
     * Test parser.
     *
     * @throws Exception the exception
     */
    @Test
    public void testParser() throws Exception {
        MockSftp3gppXmlCollectionHandler handler = new MockSftp3gppXmlCollectionHandler();

        String format = handler.get3gppFormat("cdmaSc");
        Assert.assertEquals("system|/=/v=1/sg-name=<mmeScSgName>|", format);
        Map<String,String> properties = handler.get3gppProperties(format, "system|/=/v=1/sg-name=GA|");
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("system|/=/v=1/sg-name=GA|", properties.get("instance"));
        Assert.assertEquals("GA", properties.get("sg-name"));
        Assert.assertEquals("sg-name=GA", properties.get("label"));

        format = handler.get3gppFormat("gbBssgp");
        Assert.assertEquals("nse|/=/v=1/nse-id=<nseNumber>|/=/v=1/sg-name=<sgsnGtlSgName>/su-number=<n>", format);
        properties = handler.get3gppProperties(format, "nse|/=/v=1/nse-id=1201|/=/v=1/sg-name=GB71/su-number=1");
        Assert.assertEquals(5, properties.size());
        Assert.assertEquals("nse|/=/v=1/nse-id=1201|/=/v=1/sg-name=GB71/su-number=1", properties.get("instance"));
        Assert.assertEquals("1201", properties.get("nse-id"));
        Assert.assertEquals("GB71", properties.get("sg-name"));
        Assert.assertEquals("1", properties.get("su-number"));
        Assert.assertEquals("nse-id=1201, sg-name=GB71, su-number=1", properties.get("label"));
    }

}
