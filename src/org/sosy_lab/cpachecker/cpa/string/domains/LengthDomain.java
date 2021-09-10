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

/*
 * Tracks the Length of String
 */
public class LengthDomain implements AbstractStringDomain {

  private static final DomainType TYPE = DomainType.LENGTH;
  // private final StringOptions options;

  private LengthDomain(StringOptions pOptions) {
    // options = pOptions;
  }

  @Override
  public Aspect toAdd(String pVariable) {
    return new Aspect(TYPE, String.valueOf(pVariable.length()));
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public AbstractStringDomain createInstance(StringOptions pOptions) {
    return new LengthDomain(pOptions);
  }

}
