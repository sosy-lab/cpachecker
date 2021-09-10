// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

/*
 * Tracks if the string is an element of a given set of strings
 */
//@Options(prefix = "string.cpa")
public class StringSetDomain implements AbstractStringDomain {

  // @Option(secure = true, name = "givenset", description = "The given Set to compare to")
  private ImmutableList<String> givenSet = ImmutableList.of();
  private static final DomainType TYPE = DomainType.STRING_SET;
  // private final StringOptions options;

  private StringSetDomain(StringOptions pOptions) {
    // options = pOptions;
    givenSet = ImmutableList.copyOf(pOptions.getStringSet());
  }

  @Override
  public Aspect toAdd(String pVariable) {
    if (givenSet.contains(pVariable)) {
      return new Aspect(TYPE, pVariable);
    }
    return new Aspect(TYPE, "");
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public AbstractStringDomain createInstance(StringOptions pOptions) {
    return new StringSetDomain(pOptions);
  }

}
