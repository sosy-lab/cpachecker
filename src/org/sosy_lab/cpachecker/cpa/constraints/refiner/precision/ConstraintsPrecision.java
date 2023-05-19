// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/** Precision for {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}. */
public interface ConstraintsPrecision extends Precision {

  /** Returns whether the given <code>Constraint</code> is tracked by this precision. */
  boolean isTracked(Constraint pConstraint, CFANode pLocation);

  ConstraintsPrecision join(ConstraintsPrecision pOther);

  ConstraintsPrecision withIncrement(Increment pIncrement);

  class Increment {
    private Multimap<CFANode, Constraint> trackedLocally = HashMultimap.create();
    private Multimap<String, Constraint> trackedInFunction = HashMultimap.create();
    private Set<Constraint> trackedGlobally = new HashSet<>();

    private Increment(
        final Multimap<CFANode, Constraint> pTrackedLocally,
        final Multimap<String, Constraint> pTrackedInFunction,
        final Set<Constraint> pTrackedGlobally) {
      trackedLocally = pTrackedLocally;
      trackedInFunction = pTrackedInFunction;
      trackedGlobally = pTrackedGlobally;
    }

    public Set<Constraint> getTrackedGlobally() {
      return trackedGlobally;
    }

    public Multimap<String, Constraint> getTrackedInFunction() {
      return trackedInFunction;
    }

    public Multimap<CFANode, Constraint> getTrackedLocally() {
      return trackedLocally;
    }

    public static Builder builder() {
      return new Builder();
    }

    @Override
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

    public static class Builder {
      private Multimap<CFANode, Constraint> trackedLocally = HashMultimap.create();
      private Multimap<String, Constraint> trackedInFunction = HashMultimap.create();
      private Set<Constraint> trackedGlobally = new HashSet<>();

      private Builder() {
        // DO NOTHING
      }

      @CanIgnoreReturnValue
      public Builder locallyTracked(final CFANode pNode, final Constraint pConstraint) {
        trackedLocally.put(pNode, pConstraint);
        return this;
      }

      @CanIgnoreReturnValue
      public Builder locallyTracked(
          final CFANode pNode, final Collection<Constraint> pConstraints) {
        trackedLocally.putAll(pNode, pConstraints);
        return this;
      }

      @CanIgnoreReturnValue
      public Builder locallyTracked(final Multimap<CFANode, Constraint> pTrackedLocally) {
        trackedLocally.putAll(pTrackedLocally);
        return this;
      }

      @CanIgnoreReturnValue
      public Builder functionWiseTracked(final String pFunctionName, final Constraint pConstraint) {
        trackedInFunction.put(pFunctionName, pConstraint);
        return this;
      }

      @CanIgnoreReturnValue
      public Builder functionWiseTracked(final Multimap<String, Constraint> pTrackedFunctionWise) {
        trackedInFunction.putAll(pTrackedFunctionWise);
        return this;
      }

      @CanIgnoreReturnValue
      public Builder globallyTracked(final Constraint pConstraint) {
        trackedGlobally.add(pConstraint);
        return this;
      }

      @CanIgnoreReturnValue
      public Builder globallyTracked(final Set<Constraint> pTrackedGlobally) {
        trackedGlobally.addAll(pTrackedGlobally);
        return this;
      }

      public Increment build() {
        return new Increment(trackedLocally, trackedInFunction, trackedGlobally);
      }
    }
  }
}
