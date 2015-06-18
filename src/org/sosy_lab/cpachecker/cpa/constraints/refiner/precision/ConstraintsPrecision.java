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
package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Precision for {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 *
 * @param <T> type of the increment
 */
public interface ConstraintsPrecision extends Precision {

  /**
   * Returns whether the given <code>Constraint</code> is tracked by this precision.
   */
  boolean isTracked(Constraint pConstraint, CFANode pLocation);

  ConstraintsPrecision join(ConstraintsPrecision pOther);
  
  ConstraintsPrecision withIncrement(Increment<?> pIncrement);

  class Increment<T> {
    private Multimap<CFANode, T> trackedLocally = HashMultimap.create();
    private Multimap<String, T> trackedInFunction = HashMultimap.create();
    private Set<T> trackedGlobally = new HashSet<>();

    private Increment(
        final Multimap<CFANode, T> pTrackedLocally,
        final Multimap<String, T> pTrackedInFunction,
        final Set<T> pTrackedGlobally
    ) {
      trackedLocally = pTrackedLocally;
      trackedInFunction = pTrackedInFunction;
      trackedGlobally = pTrackedGlobally;
    }

    public Set<T> getTrackedGlobally() {
      return trackedGlobally;
    }

    public Multimap<String, T> getTrackedInFunction() {
      return trackedInFunction;
    }

    public Multimap<CFANode, T> getTrackedLocally() {
      return trackedLocally;
    }

    public static <S> Builder<S> builder() {
      return new Builder<S>();
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("ConstraintsPrecision.Increment[");

      if (!trackedLocally.isEmpty()) {
        sb.append("\n\tTracked locally:\n");
        sb.append(trackedLocally.toString());
      }
      if (!trackedInFunction.isEmpty()) {
        sb.append("\n\tTracked in function:\n");
        sb.append(trackedInFunction.toString());
      }
      if (!trackedGlobally.isEmpty()) {
        sb.append("\n\tTracked globally:\n");
        sb.append(trackedGlobally.toString());
      }
      sb.append("]");

      return sb.toString();
    }

    public static class Builder<T> {
      private Multimap<CFANode, T> trackedLocally = HashMultimap.create();
      private Multimap<String, T> trackedInFunction = HashMultimap.create();;
      private Set<T> trackedGlobally = new HashSet<>();

      private Builder() {
        // DO NOTHING
      }

      public Builder<T> locallyTracked(
          final CFANode pNode,
          final T pConstraint
      ) {
        trackedLocally.put(pNode, pConstraint);
        return this;
      }

      public Builder<T> locallyTracked(
          final CFANode pNode,
          final Collection<T> pConstraints
      ) {
        trackedLocally.putAll(pNode, pConstraints);
        return this;
      }

      public Builder<T> locallyTracked(
          final Multimap<CFANode, T> pTrackedLocally
      ) {
        trackedLocally.putAll(pTrackedLocally);
        return this;
      }

      public Builder<T> functionWiseTracked(
          final String pFunctionName,
          final T pConstraint
      ) {
        trackedInFunction.put(pFunctionName, pConstraint);
        return this;
      }

      public Builder<T> functionWiseTracked(
          final Multimap<String, T> pTrackedFunctionWise
      ) {
        trackedInFunction.putAll(pTrackedFunctionWise);
        return this;
      }

      public Builder<T> globallyTracked(final T pConstraint) {
        trackedGlobally.add(pConstraint);
        return this;
      }

      public Builder<T> globallyTracked(final Set<T> pTrackedGlobally) {
        trackedGlobally.addAll(pTrackedGlobally);
        return this;
      }

      public Increment<T> build() {
        return new Increment<>(trackedLocally, trackedInFunction, trackedGlobally);
      }

    }
  }
}
