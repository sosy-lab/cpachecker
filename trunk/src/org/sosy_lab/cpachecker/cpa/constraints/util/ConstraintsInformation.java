// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/** Information about {@link Constraint}s needed for symbolic interpolation. */
public final class ConstraintsInformation {

  public static final ConstraintsInformation EMPTY = new ConstraintsInformation(ImmutableSet.of());

  private final Set<Constraint> constraints;

  public ConstraintsInformation(final Set<Constraint> pConstraints) {
    constraints = pConstraints;
  }

  public Set<Constraint> getConstraints() {
    return constraints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstraintsInformation that = (ConstraintsInformation) o;

    return constraints.size() == that.constraints.size()
        && constraints.containsAll(that.constraints);
  }

  @Override
  public int hashCode() {
    return constraints.hashCode();
  }

  @Override
  public String toString() {
    return "ConstraintsInformation[" + "constraints=" + constraints + ']';
  }
}
