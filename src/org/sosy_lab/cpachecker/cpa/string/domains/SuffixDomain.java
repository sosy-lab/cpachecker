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

public class SuffixDomain implements AbstractStringDomain<String> {


  private int suffixLength;
  private static final DomainType TYPE = DomainType.SUFFIX;

  public SuffixDomain(StringOptions pOptions) {
    suffixLength = pOptions.getSuffixLength();
  }

  @Override
  public Aspect<String> addNewAspectOfThisDomain(String pVariable) {
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
  public boolean isLessOrEqual(Aspect<?> pFirst, Aspect<?> pSecond) {
    if (pFirst.getDomainType().equals(TYPE) && pSecond.getDomainType().equals(TYPE)) {
      String val1 = (String) pFirst.getValue();
      String val2 = (String) pSecond.getValue();
    if (val1.length() == val2.length()) {
      return val1.equals(val2);
    }
    if (val1.length() < val2.length()) {
      return val1.equals(val2.substring(val1.length() - suffixLength, suffixLength));
    }
  }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Aspect<String> combineAspectsOfSameDom(Aspect<?> pFirst, Aspect<?> pSecond) {
    if (pFirst.getDomainType().equals(TYPE) && pSecond.getDomainType().equals(TYPE)) {
      int p2Len = ((String) pSecond.getValue()).length();
      if (suffixLength < p2Len) {
        return (Aspect<String>) pFirst;
      } else {
        String res =
            ((String) pFirst.getValue()).substring(0, suffixLength - p2Len)
                + ((String) pSecond.getValue());
        return new Aspect<>(this, res);
      }
    }
    return null;
  }


}
