package com.sonatype.nexus.perftest.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.nexus.perftest.Nexus;

import org.sonatype.nexus.client.core.subsystem.repository.GroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.HostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenGroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.rest.BaseUrl;

import com.bolyuba.nexus.plugin.npm.client.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Operation creating and optionally dropping the repository.
 */
public class CreateRepository
    extends AbstractNexusConfigurationOperation
{
  // format -> type -> Class
  // formats: maven, npm
  // types: hosted, proxy, group
  private static final Map<String, Map<String, Class<? extends Repository>>> repoTypes;

  static {
    repoTypes = new HashMap<>();
    repoTypes.put("maven", new HashMap<>());
    repoTypes.put("npm", new HashMap<>());

    repoTypes.get("maven").put("hosted", MavenHostedRepository.class);
    repoTypes.get("maven").put("proxy", MavenProxyRepository.class);
    repoTypes.get("maven").put("group", MavenGroupRepository.class);
    repoTypes.get("npm").put("hosted", NpmHostedRepository.class);
    repoTypes.get("npm").put("proxy", NpmProxyRepository.class);
    repoTypes.get("npm").put("group", NpmGroupRepository.class);
  }

  class NodeInfo
  {
    BaseUrl baseUrl;

    Repositories repositories;

    Repository repository;

    Exception createException;
  }

  private final String repo;

  private final String format;

  private final String type;

  private final String proxyOf;

  private final String members;

  private final boolean deleteRepository;

  private final boolean failIfExists;

  private final List<NodeInfo> nodeInfos;

  @JsonCreator
  public CreateRepository(@JacksonInject final Nexus nexus,
                          @JsonProperty("repo") String repo,
                          @JsonProperty("format") String format,
                          @JsonProperty("type") String type,
                          @JsonProperty("proxyOf") String proxyOf,
                          @JsonProperty("members") String members,
                          @JsonProperty("deleteRepository") boolean deleteRepository,
                          @JsonProperty("failIfExists") boolean failIfExists) throws Exception
  {
    super(nexus);
    this.repo = repo;
    this.format = format;
    this.type = type;
    this.proxyOf = proxyOf;
    this.members = members;
    this.deleteRepository = deleteRepository;
    this.failIfExists = failIfExists;

    this.nodeInfos = getNexusClients(newRepositoryFactories())
        .stream()
        .map(
            c -> {
              NodeInfo nodeInfo = new NodeInfo();
              nodeInfo.baseUrl = c.getConnectionInfo().getBaseUrl();
              nodeInfo.repositories = c.getSubsystem(Repositories.class);
              return nodeInfo;
            }
        )
        .collect(Collectors.toList());
    prepare();
  }

  public void prepare() throws Exception {
    for (NodeInfo nodeInfo : nodeInfos) {
      prepare(nodeInfo);
    }
  }

  private void prepare(final NodeInfo nodeInfo) throws Exception {
    try {
      nodeInfo.repository = create(nodeInfo.repositories);
      nodeInfo.repository.save();
      log.info("Created repository ({}): {} ({}, {})", nodeInfo.baseUrl, repo, format, type);
    }
    catch (Exception e) {
      if (!failIfExists) {
        nodeInfo.createException = e;
        nodeInfo.repository = nodeInfo.repositories.get(repo);
        log.info("Using existing repository ({}): {}", nodeInfo.baseUrl, repo);
      }
      else {
        throw e;
      }
    }
  }

  @Override
  public void cleanup() throws Exception {
    for (NodeInfo nodeInfo : nodeInfos) {
      cleanup(nodeInfo);
    }
  }

  private void cleanup(final NodeInfo nodeInfo) throws Exception {
    if (nodeInfo.createException == null && deleteRepository) {
      nodeInfo.repository.remove().save();
    }
  }

  private Repository create(final Repositories repositories) {
    if ("hosted".equals(type)) {
      Class<HostedRepository> repositoryClass = (Class<HostedRepository>) repoTypes.get(format).get(type);
      HostedRepository<HostedRepository> repository = repositories.create(repositoryClass, repo);
      return repository;
    }
    else if ("proxy".equals(type)) {
      Class<ProxyRepository> repositoryClass = (Class<ProxyRepository>) repoTypes.get(format).get(type);
      ProxyRepository<ProxyRepository> repository = repositories.create(repositoryClass, repo);
      repository.asProxyOf(proxyOf);
      if (MavenProxyRepository.class.isAssignableFrom(repository.getClass())) {
        MavenProxyRepository.class.cast(repository).doNotDownloadRemoteIndexes();
      }
      return repository;
    }
    else if ("group".equals(type)) {
      Class<GroupRepository> repositoryClass = (Class<GroupRepository>) repoTypes.get(format).get(type);
      GroupRepository<GroupRepository> repository = repositories.create(repositoryClass, repo);
      repository.addMember(members.split(","));
      return repository;
    }
    else {
      throw new IllegalArgumentException("Unknown type " + type);
    }
  }
}
