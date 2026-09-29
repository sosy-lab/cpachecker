// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition;

import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A domain that can propagate conditions backwards through a graph and merge alternatives exactly.
 * The carrier type is private to the domain; graph exploration does not inspect its representation.
 * Implementations must preserve the disjunction of both inputs when merging.
 */
public interface MergeableViolationConditionOperator<T> extends ViolationConditionOperator {
  T initialCondition(Optional<ARGState> pPreviousCondition);

  T prepend(T pCondition, List<CFAEdge> pEdges) throws CPATransferException, InterruptedException;

  T union(T pFirst, T pSecond) throws InterruptedException;

  Optional<AbstractState> finishGraph(ARGState pRoot, T pCondition)
      throws CPATransferException, InterruptedException, SolverException;
}
