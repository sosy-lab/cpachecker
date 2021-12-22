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
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

/*
 * Tracks the prefix of a string.
 * The max length that is tracked depends on the variable prefixLength.
 */
public class PrefixDomain implements AbstractStringDomain<String> {

  private final int prefixLength;

  private static final DomainType TYPE = DomainType.PREFFIX;

  public PrefixDomain(StringOptions pOptions) {
    prefixLength = pOptions.getPrefixLength();
  }

  @Override
  public Aspect<String> addNewAspect(String pVariable) {

    int temp = prefixLength;

    if (prefixLength > pVariable.length()) {
      temp = pVariable.length();
    }

    return new Aspect<>(this, pVariable.substring(0, temp));
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public boolean isLessOrEqual(Aspect<?> p1, Aspect<?> p2) {

    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {

      String val1 = (String) p1.getValue();
      String val2 = (String) p2.getValue();

    if (val1.length() == val2.length()) {
      return val1.equals(val2);
    }

    if (val1.length() > val2.length()) {
      return val2.equals(val1.substring(0, val2.length()));
    }

  }
    return false;
  }

  @Override
  public Aspect<?> combineAspectsForStringConcat(Aspect<?> p1, Aspect<?> p2) {
    if (p1 instanceof UnknownAspect || p2 instanceof UnknownAspect) {
      return p1;
    }

    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      @SuppressWarnings("unchecked")//Safe, because check of TYPE 
      String firstPrefix = ((String) p1.getValue());
      int p1Len = firstPrefix.length();
      if (prefixLength <= p1Len) {
        return p1;
      } else {
        @SuppressWarnings("unchecked")//Safe, because check of TYPE
        String secondPrefix = ((String) p2.getValue());
        String res = firstPrefix + secondPrefix.substring(0, prefixLength - p1Len);
        return new Aspect<>(this, res);
      }
    }

    return UnknownAspect.getInstance();
  }

}
