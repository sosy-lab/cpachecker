/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval.Refiner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class IntervalAnalysisInterpolant
    implements Interpolant<IntervalAnalysisState, IntervalAnalysisInterpolant> {

  /** the variable assignment of the interpolant */
  public PersistentMap<String, Interval> variables;
  /** the interpolant representing "true" */
  public static final IntervalAnalysisInterpolant TRUE = new IntervalAnalysisInterpolant();

  /** the interpolant representing "false" */
  public static final IntervalAnalysisInterpolant FALSE = new IntervalAnalysisInterpolant(null);

  /** Constructor for a new, empty interpolant, i.e. the interpolant representing "true" */
  private IntervalAnalysisInterpolant() {
    variables = PathCopyingPersistentTreeMap.of();
  }

  /**
   * Constructor for a new interpolant representing the given variable assignment
   *
   * @param pAssignment the variable assignment to be represented by the interpolant
   */
  public IntervalAnalysisInterpolant(PersistentMap<String, Interval> pAssignment) {
    variables = pAssignment;
  }

  /**
   * This method serves as factory method for an initial, i.e. an interpolant representing "true"
   */
  public static IntervalAnalysisInterpolant createInitial() {
    return new IntervalAnalysisInterpolant();
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {

    if (isFalse()) {
      return Collections.emptySet();
    }
    Set<MemoryLocation> mem = new HashSet<>();
    for (String s : variables.keySet()) {
      mem.add(MemoryLocation.valueOf(s));
    }
    return mem;
  }

  /**
   * This method joins to value-analysis interpolants. If the underlying map contains different
   * values for a key contained in both maps, the behaviour is undefined.
   *
   * @param other the value-analysis interpolant to join with this one
   * @return a new value-analysis interpolant containing the joined mapping of this and the other
   *     value-analysis interpolant
   */
  @Override
  public IntervalAnalysisInterpolant join(final IntervalAnalysisInterpolant other) {
    if (variables == null || other.variables == null) {
      return IntervalAnalysisInterpolant.FALSE;
    }

    // add other itp mapping - one by one for now, to check for correctness
    // newAssignment.putAll(other.assignment);
    PersistentMap<String, Interval> newAssignment = variables;
    for (Entry<String, Interval> entry : other.variables.entrySet()) {
      if (newAssignment.containsKey(entry.getKey())) {
        assert (entry.getValue().equals(other.variables.get(entry.getKey())))
            : "interpolants mismatch in " + entry.getKey();
      }
      newAssignment = newAssignment.putAndCopy(entry.getKey(), entry.getValue());
    }

    return new IntervalAnalysisInterpolant(newAssignment);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(variables);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    IntervalAnalysisInterpolant other = (IntervalAnalysisInterpolant) obj;
    return Objects.equals(variables, other.variables);
  }

  /**
   * The method checks for trueness of the interpolant.
   *
   * @return true, if the interpolant represents "true", else false
   */
  @Override
  public boolean isTrue() {
    return !isFalse() && variables.isEmpty();
  }

  /**
   * The method checks for falseness of the interpolant.
   *
   * @return true, if the interpolant represents "false", else true
   */
  @Override
  public boolean isFalse() {
    return variables == null;
  }

  /**
   * This method serves as factory method to create a value-analysis state from the interpolant
   *
   * @return a value-analysis state that represents the same variable assignment as the interpolant
   */
  @Override
  public IntervalAnalysisState reconstructState() {
    if (variables == null) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      return new IntervalAnalysisState(variables);
    }
  }

  @Override
  public String toString() {
    if (isFalse()) {
      return "FALSE";
    }

    if (isTrue()) {
      return "TRUE";
    }

    return variables.toString();
  }

  @Override
  public int getSize() {
    return isTrivial() ? 0 : variables.size();
  }
}
