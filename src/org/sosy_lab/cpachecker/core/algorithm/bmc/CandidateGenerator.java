// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;

public interface CandidateGenerator extends Iterable<CandidateInvariant> {

  /**
   * Tries to produce more candidates.
   *
   * @return {@code true} if more candidates were produced, {@code false} otherwise.
   */
  boolean produceMoreCandidates();

  /**
   * Checks if there are candidates currently available.
   *
   * <p>If no candidates are available, more can be requested to be produced by calling {@link
   * #produceMoreCandidates}.
   *
   * @return {@code true} if there are any candidates, {@code false} if more need to be produced
   *     first.
   */
  boolean hasCandidatesAvailable();

  /** Confirms the given candidates, so that they are no longer provided as candidates. */
  void confirmCandidates(Iterable<CandidateInvariant> pCandidates);

  /** Returns the confirmed candidate invariants. */
  Set<? extends CandidateInvariant> getConfirmedCandidates();

  /**
   * Suggests candidate invariants to the generator. The generator is not required to follow the
   * suggestion.
   *
   * @param pCandidates the suggested candidates.
   */
  default boolean suggestCandidates(Iterable<? extends CandidateInvariant> pCandidates) {
    return false;
  }

  @Override
  Iterator<CandidateInvariant> iterator();

  CandidateGenerator EMPTY_GENERATOR =
      new CandidateGenerator() {

        @Override
        public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
          // Do nothing
        }

        @Override
        public boolean produceMoreCandidates() {
          return false;
        }

        @Override
        public boolean hasCandidatesAvailable() {
          return false;
        }

        @Override
        public Iterator<CandidateInvariant> iterator() {
          return Collections.emptyIterator();
        }

        @Override
        public Set<CandidateInvariant> getConfirmedCandidates() {
          return ImmutableSet.of();
        }
      };
}
