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
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect.UnknownAspect;

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
  public boolean isLessOrEqual(Aspect<?> p1, Aspect<?> p2) {
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      List<String> l1 = (List<String>) p1.getValue();
      List<String> l2 = (List<String>) p2.getValue();

      if (l1.size() < l2.size()) {
        return false;
      }
      return l2.containsAll(l1);
    }
    return false;
  }

  @SuppressWarnings("unchecked") // Safe
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

    // Shouldnt be reached.. but lets be safe
    if (p1.getDomainType().equals(TYPE) && p2.getDomainType().equals(TYPE)) {
      List<String> l1 = (List<String>) p1.getValue();
      List<String> l2 = (List<String>) p2.getValue();
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
