// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

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
            .sorted()
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
      List<Character> firstChars = toCharacterList((char[]) p1.getValue());
      List<Character> scndChars = toCharacterList((char[]) p2.getValue());
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
      StringBuilder builder = new StringBuilder();
      builder.append(p1.getValue());
      builder.append(p2.getValue());
      return addNewAspectOfThisDomain(builder.toString());
    }
    return null;
  }

  private List<Character> toCharacterList(char[] arr) {
    com.google.common.collect.ImmutableList.Builder<Character> builder =
        new com.google.common.collect.ImmutableList.Builder<>();
    for (char c : arr) {
      builder.add(c);
    }
    return builder.build();
  }
}
