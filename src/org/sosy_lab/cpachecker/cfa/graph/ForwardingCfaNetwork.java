// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.graph.ForwardingNetwork;

/**
 * A {@link CfaNetwork} that forwards all calls to a delegate {@link CfaNetwork}.
 *
 * <p>The delegate {@link CfaNetwork} is specified using {@link ForwardingCfaNetwork#delegate()}.
 */
public abstract class ForwardingCfaNetwork extends ForwardingNetwork<CFANode, CFAEdge>
    implements CfaNetwork {

  /**
   * Returns the delegate {@link CfaNetwork} to forward all calls to.
   *
   * @return the delegate {@link CfaNetwork} to forward all calls to
   */
  @Override
  protected abstract CfaNetwork delegate();

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return delegate().entryNodes();
  }

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return delegate().predecessor(pEdge);
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return delegate().successor(pEdge);
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    return delegate().functionEntryNode(pFunctionExitNode);
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return delegate().functionExitNode(pFunctionEntryNode);
  }
}
