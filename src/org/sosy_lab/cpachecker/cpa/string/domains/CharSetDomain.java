// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Chars;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

/*
 * Tracks the characters in a string
 */
public class CharSetDomain implements AbstractStringDomain<ImmutableSet<Character>> {

  private static final DomainType TYPE = DomainType.CHAR_SET;

  public CharSetDomain(@SuppressWarnings("unused") StringOptions pOptions) {
  }

  @Override
  public Aspect<ImmutableSet<Character>> addNewAspect(String pVariable) {
    char[] charArr = pVariable.toCharArray();
    List<Character> charList = Chars.asList(charArr);
    return createAspect(ImmutableSet.copyOf(charList));
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public boolean isLessOrEqual(Aspect<?> p1, Aspect<?> p2) {
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      @SuppressWarnings("unchecked") // Safe, because we checked if p1 belongs to this domain
      ImmutableSet<Character> firstChars = (ImmutableSet<Character>) p1.getValue();
      @SuppressWarnings("unchecked") // Safe, same reason
      ImmutableSet<Character> scndChars = (ImmutableSet<Character>) p2.getValue();
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
      @SuppressWarnings("unchecked") // Safe, because we checked if p1 belongs to this domain
      ImmutableSet<Character> firstChars = (ImmutableSet<Character>) p1.getValue();
      @SuppressWarnings("unchecked") // Safe, same reason
      ImmutableSet<Character> scndChars = (ImmutableSet<Character>) p2.getValue();
      ImmutableSet.Builder<Character> builder = new ImmutableSet.Builder<>();
      ImmutableSet<Character> result = builder.addAll(firstChars).addAll(scndChars).build();
      return createAspect(result);
    }
    return UnknownAspect.getInstance();
  }

  private Aspect<ImmutableSet<Character>> createAspect(ImmutableSet<Character> charList) {
    return new Aspect<>(this, charList);
  }
}
