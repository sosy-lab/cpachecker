// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;
import org.sosy_lab.cpachecker.util.Pair;

public class Aspect {

  private Pair<DomainType, String> domainAndValue;

  public Aspect(DomainType pType, String pValue) {
    domainAndValue = Pair.of(pType, pValue);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Aspect)) {
      return false;
    }
    Aspect asp = (Aspect) obj;
    return asp.domainAndValue.getFirst() == this.domainAndValue.getFirst()
        && asp.domainAndValue.getSecond() == this.domainAndValue.getSecond();
  }

  @Override
  public String toString() {
    return "(" + domainAndValue.getFirst().toString() + "," + domainAndValue.getSecond() + ")";
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
