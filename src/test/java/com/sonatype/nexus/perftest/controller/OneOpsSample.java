package com.sonatype.nexus.perftest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oneops.client.OneOpsClient;
import org.junit.Test;

import static com.sonatype.nexus.perftest.controller.JMXServiceURLs.jmxServiceURL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class OneOpsSample
{
  @Test
  public void hitNexus() throws Exception {
    String oneOpsApiToken = System.getenv("ONEOPS_API_TOKEN");
    assertThat(oneOpsApiToken, is(notNullValue()));

    String nexusIp = System.getenv("NEXUS_IP");
    assertThat(nexusIp, is(notNullValue()));

    OneOpsClient oneOps = OneOpsClient.builder()
        .baseUrl("https://oneops.prod.walmart.com")
        .apiToken(oneOpsApiToken)
        .build();

    List<String> agentIPs = oneOps.computeIps("platform", "TestDevtoolsNexus", "PerfTest", "Java", "compute");

    AgentPool pool = new AgentPool(agentIPs.stream()
        .map(ip -> jmxServiceURL(ip + ":5000"))
        .collect(Collectors.toList())
    );

    Nexus nexus = new Nexus(jmxServiceURL(nexusIp + ":1099"));
    nexus.addTrigger(new ThresholdTrigger<>(
            Nexus.QueuedThreadPool.activeThreads,
            (trigger, activeThreads) -> {
              System.out.println();
              System.out.println(
                  "!!!!!!!!!!!!!!!!! Nexus is dead (" + activeThreads + ") !!!!!!!!!!!!!!!!!"
              );
              System.out.println();
              //pool.releaseAll();
            }).setThreshold(395)
    );

    try {
      Collection<Agent> m01Agents = pool.acquire(1);

      Map<String, String> overrides = new HashMap<>();
      overrides.put("nexus.baseurl", "http://" + nexusIp + ":8081/nexus");

      m01Agents.parallelStream().forEach(client -> client.start("/app/all/releases/1.0.3/maven01-1.0.3", overrides));
      m01Agents.parallelStream().forEach(Agent::waitToFinish);
    }
    finally {
      pool.releaseAll();
    }
  }

  @Test
  public void stopAllAgents() throws Exception {
    String oneOpsApiToken = System.getenv("ONEOPS_API_TOKEN");
    assertThat(oneOpsApiToken, is(notNullValue()));

    OneOpsClient oneOps = OneOpsClient.builder()
        .baseUrl("https://oneops.prod.walmart.com")
        .apiToken(oneOpsApiToken)
        .build();

    List<String> agentIPs = oneOps.computeIps("platform", "TestDevtoolsNexus", "PerfTest", "Java", "compute");

    AgentPool pool = new AgentPool(agentIPs.stream()
        .map(ip -> jmxServiceURL(ip + ":5000"))
        .collect(Collectors.toList())
    );

    try {
      Collection<Agent> m01Agents = pool.acquire(1);
      m01Agents.parallelStream().forEach(Agent::stop);
    }
    finally {
      pool.releaseAll();
    }
  }


}
