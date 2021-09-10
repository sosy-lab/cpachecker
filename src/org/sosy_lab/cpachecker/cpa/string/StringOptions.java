// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;


@Options(prefix = "cpa.string")
public class StringOptions {

  @Option(secure = true, name = "prefixlength", description = "Which prefixlength shall be tracked")
  private int prefixLength = 3;

  @Option(secure = true, name = "suffixlength", description = "Which suffixlength shall be tracked")
  private int suffixLength = 3;

  @Option(description = "Look if these Strings are Part of a String", name = "containset")
  private List<String> containset = new ArrayList<>();

  @Option(secure = true, name = "stringset", description = "Compare these Strings ")
  private ImmutableList<String> stringSet = ImmutableList.of();

  @Option(
    secure = true,
    name = "domains",
    values = {"PrefixDomain"},
    description = "which domains to use in StringCPA")
  private ImmutableList<String> domainList = ImmutableList.copyOf(Arrays.asList("PrefixDomain"));
  private ImmutableList<AbstractStringDomain> domains;

  public StringOptions(Configuration config) throws InvalidConfigurationException {
    domains = generateDomains(domainList);
    config.inject(this);
  }

  // TODO get different domains
  private ImmutableList<AbstractStringDomain> generateDomains(ImmutableList<String> domainList) {
    // return ImmutableList.of(new PrefixDomain(this));
    return null;
  }

  public int getPrefixLength() {
    return prefixLength;
  }

  public int getSuffixLength() {
    return suffixLength;
  }

  public ImmutableList<String> getStringSet() {
    return stringSet;
  }

  public List<String> getContainset() {
    return containset;
  }

  public ImmutableList<String> getDomainList() {
    return domainList;
  }

  public ImmutableList<AbstractStringDomain> getDomains() {
    return domains;
  }

}
