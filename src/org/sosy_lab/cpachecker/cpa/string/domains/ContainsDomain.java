// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

/*
 * Tracks if the string contains elements of a given set
 */
//@Options(prefix = "string.cpa")
public class ContainsDomain implements AbstractStringDomain<List<String>> {

  // @Option(description = "the given set to compare to", name = "containset")
  private List<String> givenset; // = new ArrayList<>();

  private static final DomainType TYPE = DomainType.CONTAINS;
  // private final StringOptions options;

  public ContainsDomain(StringOptions pOptions) {
    // options = pOptions;
    givenset = pOptions.getContainset();
  }

  @Override
  public Aspect<List<String>> addNewAspectOfThisDomain(String pVariable) {
    Builder<String> builder = new Builder<>();
    for (String given : givenset) {
      if (pVariable.contains(given)) {
        builder.add(given);
      }
    }
    return new Aspect<>(this, builder.build());
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean isLessOrEqual(Aspect<?> pFirst, Aspect<?> pSecond) {
    if (pFirst.getDomainType().equals(TYPE) && pSecond.getDomainType().equals(TYPE)) {
      List<String> l1 = (List<String>) pFirst.getValue();
      List<String> l2 = (List<String>) pSecond.getValue();

      if (l1.size() < l2.size()) {
        return false;
      }
      return l2.containsAll(l1);
    }
    return false;
  }

  @SuppressWarnings("unchecked") // Safe
  @Override
  public Aspect<List<String>> combineAspectsOfSameDom(Aspect<?> pFirst, Aspect<?> pSecond) {
    if (isLessOrEqual(pFirst, pSecond)) {
      return (Aspect<List<String>>) pSecond;
    }
    if (isLessOrEqual(pSecond, pFirst)) {
      return (Aspect<List<String>>) pFirst;
    }

    // Shouldnt be reached.. but lets be safe
    if (pFirst.getDomainType().equals(TYPE) && pSecond.getDomainType().equals(TYPE)) {
      List<String> l1 = (List<String>) pFirst.getValue();
      List<String> l2 = (List<String>) pSecond.getValue();
      return join(l1, l2);
    }
    return null;
  }

  private Aspect<List<String>> join(List<String> l1, List<String> l2) {
    Builder<String> builder = new Builder<>();
    builder.addAll(l1);
    for (String str : l2) {
      if (!l1.contains(str)) {
        builder.add(str);
      }
    }
    return new Aspect<>(this, builder.build());
  }
}
