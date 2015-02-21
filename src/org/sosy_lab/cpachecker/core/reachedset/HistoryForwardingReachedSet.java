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
import java.util.Collection;

import com.google.common.collect.ImmutableSet;

/**
 * Implementation of ReachedSet that forwards all calls to another instance.
 * The target instance is changeable. Remembers and provides all instances
 * to which calls are forwarded
 */
public class HistoryForwardingReachedSet extends ForwardingReachedSet {

  private final Collection<ReachedSet> usedReachedSets;

  public HistoryForwardingReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
    usedReachedSets = new ArrayList<>();
    usedReachedSets.add(pDelegate);
  }

  @Override
  public void setDelegate(ReachedSet pDelegate) {
    super.setDelegate(pDelegate);
    usedReachedSets.add(pDelegate);
  }

  public Collection<ReachedSet> getAllReachedSetsUsedAsDelegates(){
    return ImmutableSet.copyOf(usedReachedSets);
  }

}
