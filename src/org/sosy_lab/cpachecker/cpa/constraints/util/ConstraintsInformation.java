/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.constraints.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * Information about {@link Constraint}s needed for symbolic interpolation.
 */
public class ConstraintsInformation {

  public static final ConstraintsInformation EMPTY = new ConstraintsInformation(ImmutableSet.of());

  private final Set<Constraint> constraints;

  public ConstraintsInformation(
      final Set<Constraint> pConstraints
  ) {
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

    ConstraintsInformation that = (ConstraintsInformation)o;

    return constraints.size() == that.constraints.size()
        && constraints.containsAll(that.constraints);

  }

  @Override
  public int hashCode() {
    return constraints.hashCode();
  }

  @Override
  public String toString() {
    return "ConstraintsInformation[" +
        "constraints=" + constraints +
        ']';
  }
}
