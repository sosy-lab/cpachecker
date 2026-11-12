// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;

/** The ACSL predicates that hold at a location, which are assumptions for other analyses. */
public class ACSLState implements AbstractStateWithAssumptions {

  private final ImmutableSet<AcslPredicate> annotations;

  ACSLState(Set<AcslPredicate> pAnnotations) {
    annotations = ImmutableSet.copyOf(pAnnotations);
  }

  @Override
  public ImmutableList<AcslPredicate> getAssumptions() {
    return annotations.asList();
  }

  @Override
  public int hashCode() {
    return annotations.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof ACSLState that && annotations.equals(that.annotations);
  }

  @Override
  public String toString() {
    return "ACSLState " + annotations;
  }
}
