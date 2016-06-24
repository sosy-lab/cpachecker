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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class StaticCandidateProvider implements CandidateGenerator {

  private final ImmutableSet<CandidateInvariant> allCandidates;

  private final Set<CandidateInvariant> confirmedInvariants = Sets.newLinkedHashSet();

  private final Set<CandidateInvariant> candidates = Sets.newLinkedHashSet();

  private boolean produced = false;

  public StaticCandidateProvider(Iterable<? extends CandidateInvariant> pCandidates) {
    Iterables.addAll(candidates, pCandidates);
    allCandidates = ImmutableSet.copyOf(pCandidates);
  }

  @Override
  public boolean produceMoreCandidates() {
    if (produced) {
      return false;
    }
    produced = true;
    return !candidates.isEmpty();
  }

  @Override
  public boolean hasCandidatesAvailable() {
    return produced && !candidates.isEmpty();
  }

  @Override
  public void confirmCandidates(Iterable<CandidateInvariant> pCandidates) {
    for (CandidateInvariant candidate : pCandidates) {
      candidates.remove(candidate);
    }
    Iterables.addAll(confirmedInvariants, pCandidates);
  }

  @Override
  public Iterator<CandidateInvariant> iterator() {
    if (!produced) {
      return Collections.<CandidateInvariant>emptyIterator();
    }
    return candidates.iterator();
  }

  @Override
  public Set<CandidateInvariant> getConfirmedCandidates() {
    return Collections.unmodifiableSet(confirmedInvariants);
  }

  public Set<CandidateInvariant> getAllCandidates() {
    return allCandidates;
  }

}
