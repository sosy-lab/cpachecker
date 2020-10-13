// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public abstract class GlobalRefinementStrategy extends RefinementStrategy {

  protected GlobalRefinementStrategy(Solver pSolver) {
    super(pSolver);
  }

  public abstract void initializeGlobalRefinement();

  public abstract void updatePrecisionAndARG() throws InterruptedException;

  public abstract void resetGlobalRefinement();

  public abstract Collection<CFANode> getAllAffectedNodes();

  public abstract Collection<CFANode> getNodesWithUniquePredicates();

  public abstract int getSizeOfPrecision();
}
