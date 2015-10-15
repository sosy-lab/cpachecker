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

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.InitOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonSafetyProperty;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;


public class DefaultInitOperator implements InitOperator {

  @Override
  public void init(ReachedSet pReached, ImmutableSet<ImmutableSet<Property>> pLastPartitioning) {
    // Build the list of properties that should not be checked in this run
    ImmutableSet.Builder<Property> blacklistBuilder = ImmutableSet.builder();
    ImmutableSet<Property> blacklisted = null;

  }

  private void adjustAutomataPrecision(final ReachedSet pReachedSet, final Set<Property> pViolatedProperties) {

    final HashSet<AutomatonSafetyProperty> violated = Sets.newHashSet(
      Collections2.transform(pViolatedProperties, new Function<Property, AutomatonSafetyProperty>() {
        @Override
        public AutomatonSafetyProperty apply(Property pArg0) {
          Preconditions.checkArgument(pArg0 instanceof AutomatonSafetyProperty);
          return (AutomatonSafetyProperty) pArg0;
        }

      }).iterator());

    // update the precision:
    //  (optional) disable some automata transitions (global precision)
    for (AbstractState e: pReachedSet.getWaitlist()) {

      final Precision pi = pReachedSet.getPrecision(e);

      final Precision piPrime = Precisions.replaceByFunction(pi, new Function<Precision, Precision>() {
        @Override
        public Precision apply(Precision pArg0) {
          if (pArg0 instanceof AutomatonPrecision) {
            AutomatonPrecision pi = (AutomatonPrecision) pArg0;
            return pi.cloneAndAddBlacklisted(violated);
          }
          return null;
        }
      });

      if (piPrime != null) {
        pReachedSet.updatePrecision(e, piPrime);
        throw new RuntimeException("Merge of precisions from subgraphs to pivot states not yet implemented!!!");
      }
    }

  }
}
