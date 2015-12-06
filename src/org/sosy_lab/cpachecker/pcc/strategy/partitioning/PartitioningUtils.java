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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import java.util.Collection;
import java.util.HashSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.Multimap;


public final class PartitioningUtils {

  private PartitioningUtils() {}

  public static boolean areElementsCoveredByPartitionElement(final Collection<AbstractState> pInOtherPartitions,
      Multimap<CFANode, AbstractState> pInPartition, final StopOperator pStop, final Precision pPrec)
      throws CPAException, InterruptedException {
    HashSet<AbstractState> partitionNodes = new HashSet<>(pInPartition.values());

    for (AbstractState outState : pInOtherPartitions) {
      if (!partitionNodes.contains(outState)
          && !pStop.stop(outState, pInPartition.get(AbstractStates.extractLocation(outState)), pPrec)) {
        return false;
      }
    }

    return true;
  }

}
