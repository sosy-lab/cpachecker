// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import java.util.List;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

/*
 * Tracks if the string contains elements of a given set
 */
//@Options(prefix = "string.cpa")
public class ContainsDomain implements AbstractStringDomain {

  // @Option(description = "the given set to compare to", name = "containset")
  private List<String> givenset; // = new ArrayList<>();

  private static final DomainType TYPE = DomainType.CONTAINS;
  // private final StringOptions options;

  private ContainsDomain(StringOptions pOptions) {
    // options = pOptions;
    givenset = pOptions.getContainset();
  }

  @Override
  public Aspect toAdd(String pVariable) {
    StringBuilder sb = new StringBuilder();
    for (String given : givenset) {
      if (pVariable.contains(sb)) {
        sb.append(given);
      }
    }
    return new Aspect(TYPE, sb.toString());
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public AbstractStringDomain createInstance(StringOptions pOptions) {
    return new ContainsDomain(pOptions);
  }

}
