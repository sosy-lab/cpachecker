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

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;


public class InitDefaultOperator implements InitOperator {

  @Override
  public Partitioning init(Set<Property> pAllProperties, ConfigurableProgramAnalysis pCPA,
      ReachedSet pReached, Partitioning pPartitioning, CFA pCfa)
          throws CPAException, InterruptedException {

    final ARGCPA argCpa = CPAs.retrieveCPA(pCPA, ARGCPA.class);

    Preconditions.checkArgument(!pPartitioning.isEmpty(), "This init operator requires at least one partition of properties!");
    Preconditions.checkState(pAllProperties.size() > 0, "There must be a set of properties that get checked!");

    // At the moment we check the first partition in the list
    ImmutableSet<Property> partitionToChcek = pPartitioning.getFirstPartition();

    final CFANode initLocation = pCfa.getMainFunction();
    final AbstractState initialState = pCPA.getInitialState(initLocation, StateSpacePartition.getDefaultPartition());
    final Precision initialPrecision = pCPA.getInitialPrecision(initLocation, StateSpacePartition.getDefaultPartition());

    // Reset the sets 'reached' and 'waitlist' to contain only
    //  the initial state and its initial precision
    pReached.clear();
    pReached.add(initialState, initialPrecision);

    // Modify the 'waitlist': Blacklist those properties that are not in the partition!
    SetView<Property> toBlacklist = Sets.difference(pAllProperties, partitionToChcek);
    Precisions.updatePropertyBlacklistOnWaitlist(argCpa, pReached, toBlacklist);

    // Perform a precision adjustment on all states of the waitlist to
    //  reflect the changed precision.

    Preconditions.checkState(pReached.size() == 1);

    Precision pi = pReached.getPrecision(initialState);
    Optional<PrecisionAdjustmentResult> precResult = pCPA.getPrecisionAdjustment().prec(initialState, pi, pReached,
        Functions.<AbstractState>identity(), initialState);

    AbstractState ePrime = precResult.isPresent()
        ? precResult.get().abstractState()
            : initialState;
    pReached.remove(initialState);
    pReached.add(ePrime, pi);

    // Check
    ImmutableSet<Property> active = MultiPropertyAnalysis.getActiveProperties(pReached.getWaitlist().iterator().next(), pReached);
    Preconditions.checkState(Sets.intersection(toBlacklist, active).size() == 0, "Blacklisted properties must not be active!");

    return pPartitioning.withoutFirst();

  }



}
