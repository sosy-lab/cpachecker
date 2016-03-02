/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


public interface CandidateGenerator extends Iterable<CandidateInvariant> {

  /**
   * Tries to produce more candidates.
   *
   * @return {@code true} if more candidates were produced,
   * {@code false} otherwise.
   */
  boolean produceMoreCandidates();

  /**
   * Checks if there are candidates currently available.
   *
   * If no candidates are available, more can be requested to be produced by
   * calling {@link #produceMoreCandidates}.
   *
   * @return {@code true} if there are any candidates,
   * {@code false} if more need to be produced first.
   */
  boolean hasCandidatesAvailable();

  /**
   * Confirms the given candidates, so that they are no longer provided as
   * candidates.
   */
  void confirmCandidates(Iterable<CandidateInvariant> pCandidates);

  /**
   * Returns the confirmed candidate invariants.
   */
  Set<? extends CandidateInvariant> getConfirmedCandidates();

  @Override
  Iterator<CandidateInvariant> iterator();

  public static CandidateGenerator EMPTY_GENERATOR =
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
          return Collections.emptySet();
        }
      };

}
