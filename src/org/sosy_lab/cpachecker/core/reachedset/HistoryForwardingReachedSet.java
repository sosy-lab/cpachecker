// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * Implementation of ReachedSet that forwards all calls to another instance. The target instance is
 * changeable. Remembers and provides all instances to which calls are forwarded
 */
public class HistoryForwardingReachedSet extends ForwardingReachedSet {

  private final List<ReachedSet> usedReachedSets;
  private final List<ConfigurableProgramAnalysis> cpas;

  public HistoryForwardingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
    usedReachedSets = new ArrayList<>();
    cpas = new ArrayList<>();
  }

  @Override
  public void setDelegate(ReachedSet pDelegate) {
    super.setDelegate(pDelegate);
    usedReachedSets.add(pDelegate);
  }

  public List<ReachedSet> getAllReachedSetsUsedAsDelegates() {
    return ImmutableList.copyOf(usedReachedSets);
  }

  public void saveCPA(ConfigurableProgramAnalysis pCurrentCpa) {
    checkNotNull(pCurrentCpa);
    cpas.add(pCurrentCpa);
  }

  public List<ConfigurableProgramAnalysis> getCPAs() {
    return ImmutableList.copyOf(cpas);
  }
}
