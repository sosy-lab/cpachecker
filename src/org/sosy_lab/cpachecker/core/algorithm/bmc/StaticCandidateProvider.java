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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

public class StaticCandidateProvider implements CandidateGenerator {

  private int nextOrdinal = Integer.MIN_VALUE;

  private final Map<CandidateInvariant, Integer> order = new HashMap<>();

  private final Set<CandidateInvariant> allCandidates;

  private final Set<CandidateInvariant> confirmedInvariants = Sets.newLinkedHashSet();

  private final Set<CandidateInvariant> refutedInvariants = Sets.newLinkedHashSet();

  private final NavigableSet<CandidateInvariant> candidates =
      new TreeSet<>(
          (a, b) -> {
            return Integer.compare(order.get(a), order.get(b));
          });

  private boolean produced = false;

  public StaticCandidateProvider(Iterable<? extends CandidateInvariant> pCandidates) {
    addAllCandidates(pCandidates);
    allCandidates = ImmutableSet.copyOf(pCandidates);
  }

  private boolean addAllCandidates(Iterable<? extends CandidateInvariant> pCandidateInvariants) {
    boolean changed = false;
    for (CandidateInvariant candidateInvariant : pCandidateInvariants) {
      if (addCandidate(candidateInvariant)) {
        changed = true;
      }
    }
    return changed;
  }

  private boolean addCandidate(CandidateInvariant pCandidateInvariant) {
    if (confirmedInvariants.contains(pCandidateInvariant)
        || refutedInvariants.contains(pCandidateInvariant)
        || order.containsKey(pCandidateInvariant)) {
      return false;
    }
    Integer oldValue = order.put(pCandidateInvariant, nextOrdinal++);
    assert oldValue == null;
    if (!candidates.add(pCandidateInvariant)) {
      order.remove(pCandidateInvariant);
      return false;
    }
    return true;
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
      if (order.containsKey(candidate)) {
        candidates.remove(candidate);
        order.remove(candidate);
      }
    }
    Iterables.addAll(confirmedInvariants, pCandidates);
  }

  @Override
  public Iterator<CandidateInvariant> iterator() {
    if (!produced) {
      return Collections.emptyIterator();
    }
    final Iterator<CandidateInvariant> iterator = candidates.descendingIterator();
    return new Iterator<CandidateInvariant>() {

      private @Nullable CandidateInvariant candidate;

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public CandidateInvariant next() {
        return candidate = iterator.next();
      }

      @Override
      public void remove() {
        if (candidate == null) {
          throw new IllegalStateException();
        }
        refutedInvariants.add(candidate);
        iterator.remove();
        order.remove(candidate);
        candidate = null;
      }
    };
  }

  @Override
  public Set<CandidateInvariant> getConfirmedCandidates() {
    return Collections.unmodifiableSet(confirmedInvariants);
  }

  @Override
  public boolean suggestCandidates(Iterable<? extends CandidateInvariant> pCandidates) {
    return addAllCandidates(pCandidates);
  }

  public Set<CandidateInvariant> getAllCandidates() {
    return allCandidates;
  }

}
