// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.MutableGraph;

/**
 * This class provides functions to alter a given edge by one parameter. The functions return a new
 * instance of the edge that is equal to the once given except for the replaced parameter.
 */
public abstract class EdgeMutator {
  private final CFA clonedCFA;

  public EdgeMutator(CFA cfa, Configuration config, LogManager logger) {
    clonedCFA = cloneCFA(cfa, config, logger);
  }

  static CFA cloneCFA(CFA cfa, Configuration config, LogManager logger) {

    MutableGraph<CFANode, CFAEdge> mutableGraph = CCfaTransformer.createMutableGraph(cfa);

    return CCfaTransformer.createCfa(
        config, logger, cfa, mutableGraph, (originalCfaEdge, originalAstNode) -> originalAstNode);
  }

  public CFA getClonedCFA() {
    return clonedCFA;
  }
}
