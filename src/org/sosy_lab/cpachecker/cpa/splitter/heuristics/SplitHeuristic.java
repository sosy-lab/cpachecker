// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.splitter.heuristics;

import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public interface SplitHeuristic {

  // methods should be have the same for both kinds of assumption edges

  boolean removeSplitIndices(CFAEdge pCfaEdge);

  Collection<Integer> getIndicesToRemove(CFAEdge pCfaEdge);

  boolean divideSplitIndices(CFAEdge pCfaEdge);

  int divideIntoHowManyParts(CFAEdge pCfaEdge);

  interface Factory {
    SplitHeuristic create(Configuration pConfig, LogManager pLogger, int pMaxSplits)
        throws InvalidConfigurationException;
  }
}
