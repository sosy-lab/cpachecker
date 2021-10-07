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
public class LengthDomain implements AbstractStringDomain<Integer> {

  private static final DomainType TYPE = DomainType.LENGTH;

  public LengthDomain(@SuppressWarnings("unused") StringOptions pOptions) {
  }

  @Override
  public Aspect<Integer> addNewAspectOfThisDomain(String pVariable) {
    return new Aspect<>(this, pVariable.length());
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public boolean isLessOrEqual(Aspect<?> p1, Aspect<?> p2) {
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      int len1 = (int) p1.getValue();
      int len2 = (int) p2.getValue();
      return len1 >= len2;
    }
    return false;
  }

  @Override
  public Aspect<Integer> combineAspectsOfSameDom(Aspect<?> pFirst, Aspect<?> pSecond) {
    // TODO Auto-generated method stub
    if (pFirst.getDomainType().equals(TYPE) && pSecond.getDomainType().equals(TYPE)) {
      int len1 = (int) pFirst.getValue();
      int len2 = (int) pSecond.getValue();
      return new Aspect<>(this, len1 + len2);
    }
    return null;
  }

}
