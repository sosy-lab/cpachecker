// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This class manages all predicates that accrue during a refinement. For that it stores them for
 * later use and offers additional utility methods.
 */
class InterpolationSequenceStorage {

  private final Set<InterpolationSequence> itpSequences = new HashSet<>();
  private final SetMultimap<InterpolationSequence, InterpolationSequence> referenceMap =
      HashMultimap.create();

  ImmutableSet<InterpolationSequence> getInterpolationSequences() {
    return ImmutableSet.copyOf(itpSequences);
  }

  boolean isEmpty() {
    return itpSequences.isEmpty();
  }

  void addItpSequence(InterpolationSequence pNewSequence) {
    checkNotNull(pNewSequence);
    InterpolationSequence subset = tryFindSubset(pNewSequence);
    if (subset != null) {
      // the new interpolation sequence is a superset of the removed one
      verify(itpSequences.remove(subset));
      referenceMap.put(subset, pNewSequence);
      updateReferenceMap(subset, pNewSequence);
    }
    itpSequences.add(pNewSequence);
  }

  private void updateReferenceMap(
      InterpolationSequence pSubset, InterpolationSequence pNewSequence) {
    SetMultimap<InterpolationSequence, InterpolationSequence> invertedReferences =
        Multimaps.invertFrom(referenceMap, HashMultimap.create());

    Set<InterpolationSequence> sequencesPointingToSubset = invertedReferences.get(pSubset);
    for (InterpolationSequence seq : sequencesPointingToSubset) {
      referenceMap.replaceValues(seq, ImmutableSet.of(pNewSequence));
    }
  }

  Optional<InterpolationSequence> getUpdatedItpSequence(InterpolationSequence pOldSequence) {
    return referenceMap.get(pOldSequence).stream().collect(MoreCollectors.toOptional());
  }

  private InterpolationSequence tryFindSubset(InterpolationSequence pItpSequence) {
    for (InterpolationSequence itpSequence : itpSequences) {
      if (itpSequence.isStrictSubsetOf(pItpSequence)) {
        return itpSequence;
      }
    }
    return null;
  }

  ImmutableSet<InterpolationSequence> difference(Set<InterpolationSequence> pOtherSequences) {
    return Sets.difference(itpSequences, pOtherSequences).immutableCopy();
  }
}
