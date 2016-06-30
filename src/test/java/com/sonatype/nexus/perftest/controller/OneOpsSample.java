package com.sonatype.nexus.perftest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oneops.client.OneOpsClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.perftest.controller.JMXServiceURLs.jmxServiceURL;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class OneOpsSample
{
  private static final Logger log = LoggerFactory.getLogger(OneOpsSample.class);

  @Test
  public void hitNexus() throws Exception {
    String username = System.getenv("ONEOPS_USERNAME");
    assertThat(username, is(notNullValue()));

    String password = System.getenv("ONEOPS_PASSWORD");
    assertThat(password, is(notNullValue()));

    String nexusIp = System.getenv("NEXUS_IP");
    assertThat(nexusIp, is(notNullValue()));

    OneOpsClient oneOps = OneOpsClient.builder()
        .baseUrl("https://oneops.prod.walmart.com")
        .username(username)
        .password(password)
        .build();

    List<String> agentIPs = oneOps.computeIps("platform", "TestDevtoolsNexus", "PerfTest", "Java", "compute");

    AgentPool pool = new AgentPool(agentIPs.stream()
        .map(ip -> jmxServiceURL(ip + ":5000"))
        .collect(toList())
    );

    Nexus nexus = new Nexus(jmxServiceURL(nexusIp + ":1099"), username, password);
    nexus.addTrigger(new NotificationTrigger(Nexus.ObjectNames.healthCheckNotifier(), n -> {
      System.out.println();
      System.out.println(
          "!!!!!!!!!!!!!!!!! " + n.getType() + " - " + n.getMessage() + " !!!!!!!!!!!!!!!!!"
      );
      System.out.println();
    }));

    try {
      Collection<Agent> m01Agents = pool.acquire(50);

      Map<String, String> overrides = new HashMap<>();
      overrides.put("nexus.baseurl", "http://" + nexusIp + ":8081/nexus");
      overrides.put("nexus.username", username);
      overrides.put("nexus.password", password);
      overrides.put("test.duration", "2 MINUTES");

      m01Agents.parallelStream().forEach(client -> {
        try {
          client.load("/app/all/releases/1.0.4/npm01-1.0.4", overrides);
        }
        catch (Exception e) {
          log.error("Problem", e);
        }
      });

      m01Agents.parallelStream().forEach(client -> {
        try {
          client.start();
        }
        catch (Exception e) {
          log.error("Problem", e);
        }
      });

      List<Swarm> m01Swarms = m01Agents.stream().map(Agent::getSwarms).flatMap(Collection::stream).collect(toList());
      m01Swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        control.setRateMultiplier(1);
        control.setRateSleepMillis(1000);
      });
      m01Agents.parallelStream().forEach(Agent::waitToFinish);
      m01Swarms.parallelStream().map(Swarm::getControl).forEach(control -> {
        List<String> failures = control.getFailures();
        if (failures.size() > 0) {
          System.out.println("----------");
          failures.stream().forEach(failure -> System.out.println("  |------> " + failure));
        }
      });
      m01Swarms.stream().forEach(swarm -> assertThat(swarm.get(Swarm.Failure.count), is(equalTo(0L))));
    }
    finally {
      pool.releaseAll();
    }
  }

  @Test
  public void stopAllAgents() throws Exception {
    String username = System.getenv("ONEOPS_USERNAME");
    assertThat(username, is(notNullValue()));

    String password = System.getenv("ONEOPS_PASSWORD");
    assertThat(password, is(notNullValue()));

    OneOpsClient oneOps = OneOpsClient.builder()
        .baseUrl("https://oneops.prod.walmart.com")
        .username(username)
        .password(password)
        .build();

    List<String> agentIPs = oneOps.computeIps("platform", "TestDevtoolsNexus", "PerfTest", "Java", "compute");

    AgentPool pool = new AgentPool(agentIPs.stream()
        .map(ip -> jmxServiceURL(ip + ":5000"))
        .collect(toList())
    );

    try {
      Collection<Agent> m01Agents = pool.acquireAll();
      m01Agents.parallelStream().forEach(Agent::stop);
    }
    finally {
      pool.releaseAll();
    }
  }


}
