// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

/*
 * Tracks if the string is an element of a given set of strings
 */
public class StringSetDomain implements AbstractStringDomain<List<String>> {

  private static final DomainType TYPE = DomainType.STRING_SET;
  private StringOptions options;

  public StringSetDomain(StringOptions pOptions) {
    options = pOptions;
  }

  /*
   * Should only be called for StringLiterals
   */
  @Override
  public Aspect<List<String>> addNewAspect(String pVariable) {

    ImmutableList<String> givenSet = ImmutableList.copyOf(options.getStringSet());

    if (givenSet.contains(pVariable)) {

      return new Aspect<>(this, givenSet);
    }

    return new Aspect<>(this, ImmutableList.of());
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean isLessOrEqual(Aspect<?> p1, Aspect<?> p2) {

    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {

      List<String> val1 = (List<String>) p1.getValue();
      List<String> val2 = (List<String>) p2.getValue();

      if (val1.size() < val2.size()) {
        return false;
      }
      return val2.containsAll(val1);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Aspect<?> combineAspectsForStringConcat(Aspect<?> p1, Aspect<?> p2) {

    if (p1 instanceof UnknownAspect) {
      return p2;
    }
    if (p2 instanceof UnknownAspect) {
      return p1;
    }

    if (isLessOrEqual(p1, p2)) {
      return p2;
    }
    if (isLessOrEqual(p2, p1)) {
      return p1;
    }

    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {

      List<String> val1 = (List<String>) p1.getValue();
      List<String> val2 = (List<String>) p2.getValue();
      return join(val1, val2);
    }

    return UnknownAspect.getInstance();
  }

  private Aspect<List<String>> join(List<String> l1, List<String> l2) {

    ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
    builder.addAll(l1);

    for (String str : l2) {
      if (!l1.contains(str)) {
        builder.add(str);
      }
    }

    return new Aspect<>(this, builder.build());
  }
}
