/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.sonatype.nexus.perftest.config.client.docker;

import com.sonatype.nexus.perftest.config.client.DockerGroupRepository;

import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyGroupRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;

public class JerseyDockerGroupRepository
    extends JerseyGroupRepository<DockerGroupRepository>
    implements DockerGroupRepository
{
  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.GroupRepository";

  static final String PROVIDER = "docker-group";

  public JerseyDockerGroupRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyDockerGroupRepository(final JerseyNexusClient nexusClient,
                                     final RepositoryGroupResource settings)
  {
    super(nexusClient, settings);
  }

  @Override
  protected RepositoryGroupResource createSettings() {
    final RepositoryGroupResource settings = super.createSettings();

    settings.setProviderRole(JerseyDockerGroupRepository.PROVIDER_ROLE);
    settings.setProvider(JerseyDockerGroupRepository.PROVIDER);

    return settings;
  }
}
