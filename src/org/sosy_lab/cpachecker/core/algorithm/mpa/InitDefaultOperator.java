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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;


public class InitDefaultOperator implements InitOperator {

  @Override
  public void init(ReachedSet pReached, AbstractState pE0, Precision pPi0,
      ImmutableSet<ImmutableSet<Property>> pPartitioning) {

    ImmutableSet<Property> allProperties = MultiPropertyAlgorithm.getActiveProperties(pE0, pReached);

    Preconditions.checkArgument(pPartitioning.size() == 1, "This init operator requires exactly one partition of properties!");
    Preconditions.checkState(allProperties.size() > 0, "There must be a set of properties that get checked!");

    // Reset the sets 'reached' and 'waitlist' to contain only
    //  the initial state and its initial precision
    pReached.clear();
    pReached.add(pE0, pPi0);

    // Modify the 'waitlist': Blacklist those properties that are not in the partition!
    ImmutableSet<Property> toCheck = pPartitioning.iterator().next();
    SetView<Property> toBlacklist = Sets.difference(allProperties, toCheck);
    MultiPropertyAlgorithm.disablePropertiesForWaitlist(pReached, toBlacklist);
  }



}
