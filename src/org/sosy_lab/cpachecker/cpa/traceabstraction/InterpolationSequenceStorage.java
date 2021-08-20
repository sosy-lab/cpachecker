// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;

/**
 * This class manages all predicates that accrue during a refinement. For that it stores them for
 * later use and offers additional utility methods.
 */
class InterpolationSequenceStorage {

  // TODO: potentially merge/refactor functionality of this class with PredicatePrecision.
  // For now predicates other than function predicates are ignored, although they also need to be
  // considered eventually.

  private final Set<InterpolationSequence> itpSequences = new HashSet<>();

  ImmutableSet<InterpolationSequence> getInterpolationSequences() {
    return ImmutableSet.copyOf(itpSequences);
  }

  boolean isEmpty() {
    return itpSequences.isEmpty();
  }

  void addItpSequence(InterpolationSequence pItpSequence) {
    checkNotNull(pItpSequence);
    itpSequences.add(pItpSequence);
  }

  ImmutableSet<InterpolationSequence> difference(Set<InterpolationSequence> pOtherSequences) {
    return Sets.difference(itpSequences, pOtherSequences).immutableCopy();
  }
}
