/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest.config;

import com.sonatype.nexus.perftest.Nexus;
import com.sonatype.nexus.perftest.maven.DownloadAction;
import com.sonatype.nexus.perftest.paths.DownloadPaths;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Performs series of GETs against given repository or nexus baseUrl, usable to pre-populate proxy repositories.
 */
public class Pull
    extends AbstractNexusConfigurationOperation
{
  private final DownloadPaths paths;

  private final DownloadAction downloadAction;

  public Pull(@JacksonInject Nexus nexus,
              @JsonProperty("repo") String repo,
              @JsonProperty("paths") DownloadPaths paths) throws Exception
  {
    super(nexus);
    this.paths = paths;
    this.downloadAction = new DownloadAction(repo == null ? getNexusUrl() : getRepoBaseurl(repo));
    prepare();
  }

  public void prepare() throws Exception {
    log.info("PULL: {} started", downloadAction.getBaseUrl());
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      int count = 0;
      long bytes = 0;
      for (String path : paths.getAll()) {
        bytes += downloadAction.download(client, path);
        count++;
      }
      log.info("PULL: {} -> {} bytes ({} reqs)", downloadAction.getBaseUrl(), bytes, count);
    }
  }

  @Override
  public void cleanup() throws Exception {
    // nothing
  }
}
