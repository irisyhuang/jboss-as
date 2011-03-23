/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.embedded.demos.ejb3.dd;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@Run(RunModeType.IN_CONTAINER)
public class DDBasedSLSBTestCase {

    private static final String MODULE_NAME = "dd-based-slsb";

    private static final String JAR_NAME = MODULE_NAME + ".jar";

    @Deployment
    public static JavaArchive getDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addPackage(DDBasedSLSBTestCase.class.getPackage());
        jar.addManifestResource("demos/ejb3/ejb-jar.xml", "ejb-jar.xml");
        jar.addManifestResource("demos/ejb3/MANIFEST.MF", "MANIFEST.MF");
        return jar;
    }

    /**
     * Tests that all possible local view bindings of a Stateless bean are available.
     *
     * @throws Exception
     */
    @Test
    public void testLocalBindingsOnSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = DDBasedSLSB.class.getSimpleName();
        Echo bean = (Echo) ctx.lookup("java:global/" + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());

        String msg = "Simple echo!";
        String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected return message from bean", msg, echo);

    }
}
