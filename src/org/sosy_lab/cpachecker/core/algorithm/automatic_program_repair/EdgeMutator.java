// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.collect.TreeMultimap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFATraversal;

/**
 * This class provides functions to alter a given edge by one parameter. The functions return a new
 * instance of the edge that is equal to the once given except for the replaced parameter.
 */
public abstract class EdgeMutator {
  private CFA clonedCFA;

  public EdgeMutator(CFA cfa) {
    clonedCFA = cloneCFA(cfa);
  }

  public CFA getClonedCFA() {
    return clonedCFA;
  }

  /* TODO create deep copy */
  static MutableCFA cloneCFA(CFA cfa) {
    final TreeMultimap<String, CFANode> nodes = TreeMultimap.create();

    for (final String function : cfa.getAllFunctionNames()) {
      nodes.putAll(
          function, CFATraversal.dfs().collectNodesReachableFrom(cfa.getFunctionHead(function)));
    }

    MutableCFA clonedCFA =
        new MutableCFA(
            cfa.getMachineModel(),
            cfa.getAllFunctions(),
            nodes,
            cfa.getMainFunction(),
            cfa.getFileNames(),
            cfa.getLanguage());

    cfa.getLoopStructure().ifPresent(clonedCFA::setLoopStructure);
    cfa.getLiveVariables().ifPresent(clonedCFA::setLiveVariables);

    return clonedCFA;
  }
}
