// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;

public class StaticCandidateProvider implements CandidateGenerator {

  private int nextOrdinal = Integer.MIN_VALUE;

  private final Map<CandidateInvariant, Integer> order = new HashMap<>();

  private final Set<CandidateInvariant> allCandidates;

  private final Set<CandidateInvariant> confirmedInvariants = new LinkedHashSet<>();

  private final Set<CandidateInvariant> refutedInvariants = new LinkedHashSet<>();

  private final NavigableSet<CandidateInvariant> candidates =
      new TreeSet<>(Comparator.comparingInt(order::get));

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
    return new Iterator<>() {

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
        checkState(candidate != null);
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
