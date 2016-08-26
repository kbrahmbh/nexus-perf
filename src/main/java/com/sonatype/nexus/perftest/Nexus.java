/*
 * Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package com.sonatype.nexus.perftest;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class Nexus
{
  private final String baseurl;

  private final List<String> memberurls;

  private final String username;

  private final String password;

  public Nexus() {
    this.baseurl = System.getProperty("nexus.baseurl");
    this.username = System.getProperty("nexus.username");
    this.password = System.getProperty("nexus.password");
    this.memberurls = collectMemberUrls(System.getProperty("nexus.memberurls"));
  }

  @JsonCreator
  public Nexus(@JsonProperty("baseurl") String baseurl,
               @JsonProperty("username") String username,
               @JsonProperty("password") String password,
               @JsonProperty(value = "memberurls", required = false) String memberurls)
  {
    this.baseurl = baseurl;
    this.username = username;
    this.password = password;
    this.memberurls = collectMemberUrls(memberurls);
  }

  private List<String> collectMemberUrls(@Nullable final String memberUrlsString) {
    if (memberUrlsString != null) {
      return ImmutableList.copyOf(Splitter.on(',').omitEmptyStrings().split(memberUrlsString));
    }
    return null;
  }

  public String getBaseurl() {
    return baseurl;
  }

  public boolean isCluster() {
    return memberurls != null && !memberurls.isEmpty();
  }

  public List<String> getMemberurls() {
    if (memberurls != null) {
      return memberurls;
    }
    return Collections.emptyList();
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
