// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;

public class GAPNode {

  /** The CFANode preceding a global access, i.e. one or more leaving CFAEdge is a global access. */
  public final CFANode node;

  /** The function return node of {@link GAPNode#node} to track the current context. */
  public final CFANode functionReturnNode;

  /**
   * The predicate abstract state used to make predicate checks on the leaving edges of {@link
   * GAPNode#node}
   */
  public final PredicateAbstractState predicateAbstractState;

  /**
   * Returns an object representing a Global Access Preceding (GAP) CFANode. The leaving CFAEdge(s)
   * of {@link GAPNode#node} are reads or writes to global / shared variables.
   *
   * @param pNode CFANode preceding a global access, i.e. one or more leaving CFAEdge is a global
   *     access
   * @param pFunctionReturnNode function return node of {@link GAPNode#node} to track the current
   *     context
   * @param pPredicateAbstractState predicate abstract state used to make predicate checks on the
   *     leaving edges of {@link GAPNode#node}, i.e. the global accesses
   */
  public GAPNode(
      @NonNull CFANode pNode,
      @Nullable CFANode pFunctionReturnNode,
      @NonNull PredicateAbstractState pPredicateAbstractState) {

    node = pNode;
    functionReturnNode = pFunctionReturnNode;
    predicateAbstractState = pPredicateAbstractState;
  }

  /**
   * Returns {@link GAPNode#node}, used to stream collections of GAPNodes.
   *
   * @return {@link GAPNode#node}
   */
  public CFANode getNode() {
    return node;
  }
}
