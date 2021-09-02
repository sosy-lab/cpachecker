// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

@Options(prefix = "string.cpa")
public class PrefixDomain implements AbstractStringDomain {

  @Option(
    secure = true,
    name = "prefixlength",
    values = {"PrefixDomain"},
    description = "which prefixlength shall be tracked")
  private int prefixLength = 3;
  private static final DomainType TYPE = DomainType.PREFFIX;


  @Override
  public Aspect toAdd(String pVariable) {
    int temp = prefixLength;
    if (prefixLength > pVariable.length()) {
      temp = pVariable.length();
    }
    return new Aspect(TYPE, pVariable.substring(0, temp));
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }
}
