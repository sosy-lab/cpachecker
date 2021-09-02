// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.string.domains.AbstractStringDomain;
import org.sosy_lab.cpachecker.cpa.string.domains.PrefixDomain;


@Options(prefix = "cpa.string")
public class StringOptions {

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
    return ImmutableList.of(new PrefixDomain());
  }

  public ImmutableList<AbstractStringDomain> getDomains() {
    return domains;
  }

}
