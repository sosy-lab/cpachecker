// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.Network;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.graph.ForwardingNetwork;

/**
 * A {@link CfaNetwork} that forwards all calls to a delegate {@link CfaNetwork} or delegate {@link
 * Network}.
 *
 * <p>All {@link CfaNetwork} specific calls (i.e., methods not in {@link Network}) are forwarded to
 * the delegate specified using {@link ForwardingCfaNetwork#delegateCfaNetwork()}. All other calls
 * are forwarded to the delegate specified using {@link ForwardingCfaNetwork#delegateNetwork()}.
 * These delegates can be the same {@link CfaNetwork} instance (default implementation).
 */
interface ForwardingCfaNetwork extends CfaNetwork, ForwardingNetwork<CFANode, CFAEdge> {

  /**
   * Returns the delegate {@link CfaNetwork} to forward {@link CfaNetwork} specific calls to.
   *
   * @return the delegate {@link CfaNetwork} to forward {@link CfaNetwork} specific calls to
   */
  CfaNetwork delegateCfaNetwork();

  @Override
  default Network<CFANode, CFAEdge> delegateNetwork() {
    return delegateCfaNetwork();
  }

  @Override
  default CFANode predecessor(CFAEdge pEdge) {
    return delegateCfaNetwork().predecessor(pEdge);
  }

  @Override
  default CFANode successor(CFAEdge pEdge) {
    return delegateCfaNetwork().successor(pEdge);
  }

  @Override
  default FunctionEntryNode functionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {
    return delegateCfaNetwork().functionEntryNode(pFunctionSummaryEdge);
  }

  @Override
  default Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return delegateCfaNetwork().functionExitNode(pFunctionEntryNode);
  }

  @Override
  default FunctionSummaryEdge functionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {
    return delegateCfaNetwork().functionSummaryEdge(pFunctionCallEdge);
  }

  @Override
  default FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {
    return delegateCfaNetwork().functionSummaryEdge(pFunctionReturnEdge);
  }
}
