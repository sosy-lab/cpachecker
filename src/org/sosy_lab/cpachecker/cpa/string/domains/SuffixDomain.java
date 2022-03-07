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
 * Tracks the suffix of a string.
 * The max. length of the suffix depends on suffixLength
 */
public class SuffixDomain implements AbstractStringDomain<String> {

  private int suffixLength;

  private static final DomainType TYPE = DomainType.SUFFIX;

  public SuffixDomain(StringOptions pOptions) {
    suffixLength = pOptions.getSuffixLength();
  }

  @Override
  public Aspect<String> addNewAspect(String pVariable) {

    int len = pVariable.length();

    if (len < suffixLength) {
      return new Aspect<>(this, pVariable);
    }

    return new Aspect<>(this, pVariable.substring(len - suffixLength, len));
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

      if (val1.length() < val2.length()) {
        return val1.equals(val2.substring(val1.length() - suffixLength, suffixLength));
      }
    }

    return false;
  }

  @Override
  public Aspect<?> combineAspectsForStringConcat(Aspect<?> p1, Aspect<?> p2) {
    if (p1 instanceof UnknownAspect) {
      return p2;
    } else if (p2 instanceof UnknownAspect) {
      return p1;
    }
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      @SuppressWarnings("unchecked") // Safe, because check of TYPE
      String suffixSecondAspect = (String) p2.getValue();
      int aspect2Len = suffixSecondAspect.length();

      if (suffixLength <= aspect2Len) {
        return p2;
      } else {
        @SuppressWarnings("unchecked") // Safe, because check of TYPE
        String suffixFirstAspect = (String) p1.getValue();
        int aspect1Len = suffixFirstAspect.length();
        String res =
            suffixFirstAspect.substring(aspect1Len - aspect2Len, aspect1Len) + suffixSecondAspect;
        return new Aspect<>(this, res);
      }
    }
    return UnknownAspect.getInstance();
  }
}
