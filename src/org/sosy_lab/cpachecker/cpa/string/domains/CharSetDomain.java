// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

/*
 * Tracks the characters in the string
 */
public class CharSetDomain implements AbstractStringDomain<char[]> {

  private static final DomainType TYPE = DomainType.CHAR_SET;

  public CharSetDomain(@SuppressWarnings("unused") StringOptions pOptions) {
  }

  @Override
  public Aspect<char[]> addNewAspectOfThisDomain(String pVariable) {
    char[] temp =
        Arrays.asList(pVariable.split(""))
            .stream()
            .distinct()
            .collect(Collectors.joining())
            .toCharArray();
    return new Aspect<>(this, temp);
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public boolean isLessOrEqual(Aspect<?> p1, Aspect<?> p2) {
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      List<Character> firstChars = toCharList((char[]) p1.getValue());
      List<Character> scndChars = toCharList((char[]) p2.getValue());
      return scndChars.containsAll(firstChars);
    }
    return false;
  }

  @Override
  public Aspect<?> combineAspectsForStringConcat(Aspect<?> p1, Aspect<?> p2) {
    if (p1 instanceof UnknownAspect) {
      return p2;
    }
    if (p2 instanceof UnknownAspect) {
      return p1;
    }
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      char[] p1Val = (char[]) p1.getValue();
      char[] p2Val = (char[]) p2.getValue();
      char[] result = mergeArrays(p1Val, p2Val);
      return addNewAspectOfThisDomain(String.copyValueOf(result));
    }
    return null;
  }

  private char[] mergeArrays(char[] arr1, char[] arr2) {
    char[] result = new char[arr1.length + arr2.length];
    for (int i = 0; i < arr1.length; i++) {
      result[i] = arr1[i];
    }
    for (int i = arr1.length; i < result.length; i++) {
      int n = i - arr1.length;
      result[i] = arr2[n];
    }
    return result;
  }
  private List<Character> toCharList(char[] arr) {
    ImmutableList.Builder<Character> builder = new ImmutableList.Builder<>();
    for (char c : arr) {
      builder.add(c);
    }
    return builder.build();
  }
}
