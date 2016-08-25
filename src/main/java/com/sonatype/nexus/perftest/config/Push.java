/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.config;

import java.io.File;

import com.sonatype.nexus.perftest.ClientSwarm.ClientRequestInfo;
import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.maven.ArtifactDeployer;

import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Performs series of PUTs against given repository or nexus baseUrl, usable to pre-populate hosted repositories.
 *
 * @todo: for now this class is maven2 specific! Make it more usable for other formats too
 */
public class Push
    extends AbstractNexusConfigurationOperation
{
  private final File basedir;

  private final File pomTemplate;

  private final String groupId;

  private final String repo;

  private final Repositories repositories;

  @JsonCreator
  public Push(@JacksonInject Nexus nexus,
              @JsonProperty("artifactsBasedir") File basedir,
              @JsonProperty("pomTemplate") File pomTemplate,
              @JsonProperty(value = "groupId", required = false) String groupId,
              @JsonProperty("repo") String repo) throws Exception
  {
    super(nexus);
    this.basedir = basedir;
    this.pomTemplate = pomTemplate;
    this.groupId = groupId == null ? "test.group" : groupId;
    this.repo = repo;
    this.repositories = getNexusClient(newRepositoryFactories()).getSubsystem(Repositories.class);
    perform();
  }

  public void perform() throws Exception {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      MavenHostedRepository repository = repositories.get(MavenHostedRepository.class, repo);
      final ArtifactDeployer deployer = new ArtifactDeployer(client, repository.contentUri());

      log.info("PUSH: {} started", deployer.getBaseUrl());

      final String version = "1.0"; // TODO: param? generated?

      long bytes = 0;
      int count = 0;
      for (File file : basedir.listFiles()) {
        if (file.getName().endsWith(".jar")) {
          final String artifactId = String.format("artifact-%d", count++);
          bytes += deployer.deployPom(groupId, artifactId, version, pomTemplate);
          bytes += deployer.deployJar(groupId, artifactId, version, file);
        }
      }
      log.info("PUSH: {} -> {} bytes ({} artifacts)", deployer.getBaseUrl(), bytes, count);
    }
  }

  @Override
  public void cleanup() throws Exception {
    // nothing
  }
}
