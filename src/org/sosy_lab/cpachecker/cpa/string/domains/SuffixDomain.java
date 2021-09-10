// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

//@Options(prefix = "string.cpa")
public class SuffixDomain implements AbstractStringDomain {

  // @Option(
  // secure = true,
  // name = "suffixlength",
  // description = "which suffixlength shall be tracked")
  // private int suffixLength = 3;
  private int suffixLength;
  private static final DomainType TYPE = DomainType.SUFFIX;
  // private final StringOptions options;

  private SuffixDomain(StringOptions pOptions) {
    // options = pOptions;
    suffixLength = pOptions.getSuffixLength();
  }

  @Override
  public Aspect toAdd(String pVariable) {
    int len = pVariable.length();
    if (len < suffixLength) {
      return new Aspect(TYPE, pVariable);
    }
    return new Aspect(TYPE, pVariable.substring(len - suffixLength, len));
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public AbstractStringDomain createInstance(StringOptions pOptions) {
    return new SuffixDomain(pOptions);
  }

}
