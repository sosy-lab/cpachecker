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
package org.sosy_lab.cpachecker.core.reachedset;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of ReachedSet that forwards all calls to another instance.
 * The target instance is changeable. Remembers and provides all instances
 * to which calls are forwarded
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
    Preconditions.checkArgument(pCurrentCpa != null);
    cpas.add(pCurrentCpa);
  }

  public List<ConfigurableProgramAnalysis> getCPAs() {
    return ImmutableList.copyOf(cpas);
  }

}
