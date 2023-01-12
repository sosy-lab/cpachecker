// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public final class PartitioningUtils {

  private PartitioningUtils() {}

  public static boolean areElementsCoveredByPartitionElement(
      final Collection<AbstractState> pInOtherPartitions,
      Multimap<CFANode, AbstractState> pInPartition,
      final StopOperator pStop,
      final Precision pPrec)
      throws CPAException, InterruptedException {
    Set<AbstractState> partitionNodes = new HashSet<>(pInPartition.values());

    for (AbstractState outState : pInOtherPartitions) {
      if (!partitionNodes.contains(outState)
          && !pStop.stop(
              outState, pInPartition.get(AbstractStates.extractLocation(outState)), pPrec)) {
        return false;
      }
    }

    return true;
  }
}
