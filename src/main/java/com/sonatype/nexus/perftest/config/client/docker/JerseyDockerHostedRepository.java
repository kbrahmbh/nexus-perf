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

import com.sonatype.nexus.perftest.config.client.DockerHostedRepository;

import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyHostedRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryResource;

public class JerseyDockerHostedRepository
    extends JerseyHostedRepository<DockerHostedRepository>
    implements DockerHostedRepository
{
  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.Repository";

  static final String PROVIDER = "docker-hosted";

  public JerseyDockerHostedRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyDockerHostedRepository(final JerseyNexusClient nexusClient,
                                      final RepositoryResource settings)
  {
    super(nexusClient, settings);
  }

  @Override
  protected RepositoryResource createSettings() {
    final RepositoryResource settings = super.createSettings();

    settings.setProviderRole(JerseyDockerHostedRepository.PROVIDER_ROLE);
    settings.setProvider(JerseyDockerHostedRepository.PROVIDER);
    settings.setRepoPolicy("RELEASE");
    settings.setIndexable(false);

    return settings;
  }
}
