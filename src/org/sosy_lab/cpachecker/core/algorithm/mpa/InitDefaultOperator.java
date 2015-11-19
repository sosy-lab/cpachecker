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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;


public class InitDefaultOperator implements InitOperator {

  @Override
  public Partitioning init(ReachedSet pReached, AbstractState pE0, Precision pPi0,
      Partitioning pPartitioning) {

    ARGReachedSet reached = new ARGReachedSet(pReached);

    ImmutableSet<Property> allProperties = MultiPropertyAlgorithm.getActiveProperties(pE0, pReached);

    Preconditions.checkArgument(!pPartitioning.isEmpty(), "This init operator requires at least one partition of properties!");
    Preconditions.checkState(allProperties.size() > 0, "There must be a set of properties that get checked!");

    // At the moment we check the first partition in the list
    ImmutableSet<Property> partitionToChcek = pPartitioning.getFirstPartition();

    // Reset the sets 'reached' and 'waitlist' to contain only
    //  the initial state and its initial precision
    ARGState e0 = AbstractStates.extractStateByType(pE0, ARGState.class);
    ImmutableList<ARGState> childs = ImmutableList.copyOf(e0.getChildren());
    for (ARGState e: childs) {
      reached.removeSubtree(e);
    }

    // Modify the 'waitlist': Blacklist those properties that are not in the partition!
    ARGCPA argCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);

    SetView<Property> toBlacklist = Sets.difference(allProperties, partitionToChcek);
    MultiPropertyAlgorithm.disablePropertiesForWaitlist(argCpa, pReached, toBlacklist);

    // Check
    ImmutableSet<Property> active = MultiPropertyAlgorithm.getActiveProperties(pReached.getWaitlist().iterator().next(), pReached);
    Preconditions.checkState(Sets.intersection(toBlacklist, active).size() == 0, "Blacklisted properties must not be active!");

    return pPartitioning.withoutFirst();
  }



}
