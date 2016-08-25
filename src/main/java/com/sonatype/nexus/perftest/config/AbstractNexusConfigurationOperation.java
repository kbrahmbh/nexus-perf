/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.sonatype.nexus.perftest.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.PerformanceTest.NexusConfigurator;
import com.sonatype.nexus.perftest.operation.AbstractNexusOperation;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

public abstract class AbstractNexusConfigurationOperation
    extends AbstractNexusOperation
    implements NexusConfigurator
{
  public AbstractNexusConfigurationOperation(final Nexus nexus) {
    super(nexus);
  }

  protected List<NexusClient> getNexusClients(final SubsystemFactory<?, JerseyNexusClient>... subsystemFactories) {
    if (nexus.isCluster()) {
      ArrayList<NexusClient> clients = new ArrayList<>();
      for (String memberUrl : nexus.getMemberurls()) {
        clients.add(getNexusClientForBaseUrl(memberUrl, subsystemFactories));
      }
      return clients;
    }
    else {
      return Collections.singletonList(getNexusClient(subsystemFactories));
    }
  }
}
