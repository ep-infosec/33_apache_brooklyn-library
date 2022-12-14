/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.entity.database.postgresql;

import java.util.Arrays;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.core.location.PortRanges;
import org.apache.brooklyn.util.ssh.BashCommandsConfigurable;
import org.testng.annotations.Test;
import org.apache.brooklyn.entity.database.DatastoreMixins.DatastoreCommon;
import org.apache.brooklyn.entity.database.VogellaExampleAccess;
import org.apache.brooklyn.location.jclouds.JcloudsLocation;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.net.Protocol;
import org.apache.brooklyn.util.ssh.IptablesCommandsConfigurable;
import org.apache.brooklyn.util.ssh.IptablesCommandsConfigurable.Chain;
import org.apache.brooklyn.util.ssh.IptablesCommandsConfigurable.Policy;

import com.google.common.collect.ImmutableList;

/**
 * The PostgreSqlRackspaceLiveTest installs Postgresql on various operating systems like Ubuntu, CentOS, Red Hat etc. To
 * make sure that PostgreSql works like expected on these Operating Systems.
 */
public class PostgreSqlRackspaceLiveTest extends PostgreSqlIntegrationTest {
    @Test(groups = "Live")
    public void test_Debian_6() throws Exception {
        test("Debian 6");
    }

    @Test(groups = "Live")
    public void test_Ubuntu_10_0() throws Exception {
        test("Ubuntu 10.0");
    }

    @Test(groups = "Live")
    public void test_Ubuntu_11_0() throws Exception {
        test("Ubuntu 11.0");
    }

    @Test(groups = "Live")
    public void test_Ubuntu_12_0() throws Exception {
        test("Ubuntu 12.0");
    }

    @Test(groups = "Live")
    public void test_CentOS_6_0() throws Exception {
        test("CentOS 6.0");
    }

    @Test(groups = "Live")
    public void test_CentOS_5_6() throws Exception {
        test("CentOS 5.6");
    }

    @Test(groups = "Live")
    public void test_Fedora_17() throws Exception {
        test("Fedora 17");
    }

    @Test(groups = "Live")
    public void test_Red_Hat_Enterprise_Linux_6() throws Exception {
        test("Red Hat Enterprise Linux 6");
    }

    @Override
    @Test(groups = "Live")
    public void test_localhost() throws Exception {
        super.test_localhost();
    }
    
    public void test(String osRegex) throws Exception {
        PostgreSqlNode psql = app.createAndManageChild(EntitySpec.create(PostgreSqlNode.class)
                .configure(DatastoreCommon.CREATION_SCRIPT_CONTENTS, CREATION_SCRIPT)
                .configure(PostgreSqlNode.POSTGRESQL_PORT, PortRanges.fromInteger(5432))
                .configure(PostgreSqlNode.SHARED_MEMORY, "32MB"));

        mgmt.getBrooklynProperties().put("brooklyn.location.jclouds.rackspace-cloudservers-uk.imageNameRegex", osRegex);
        mgmt.getBrooklynProperties().remove("brooklyn.location.jclouds.rackspace-cloudservers-uk.image-id");
        mgmt.getBrooklynProperties().remove("brooklyn.location.jclouds.rackspace-cloudservers-uk.imageId");
        mgmt.getBrooklynProperties().put("brooklyn.location.jclouds.rackspace-cloudservers-uk.inboundPorts", Arrays.asList(22, 5432));
        JcloudsLocation jcloudsLocation = (JcloudsLocation) mgmt.getLocationRegistry().getLocationManaged("jclouds:rackspace-cloudservers-uk");

        app.start(ImmutableList.of(jcloudsLocation));

        SshMachineLocation l = (SshMachineLocation) psql.getLocations().iterator().next();
        l.execCommands("add iptables rule", ImmutableList.of(new IptablesCommandsConfigurable(BashCommandsConfigurable.newInstance()).insertIptablesRule(Chain.INPUT, Protocol.TCP, 5432, Policy.ACCEPT)));

        String url = psql.getAttribute(DatastoreCommon.DATASTORE_URL);
        new VogellaExampleAccess("org.postgresql.Driver", url).readModifyAndRevertDataBase();
    }
}
