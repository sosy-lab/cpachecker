// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation.basic;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.validation.AbstractCfaValidator;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidationResult;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidator;

/**
 * This {@link CfaValidator} ensures that there are no function entry nodes that have references to
 * unreachable function exit nodes (i.e., function exit nodes that have no entering edges and aren't
 * even part of the CFA).
 */
public final class NoUnreachableFunctionExitNode extends AbstractCfaValidator {

  private CfaValidationResult checkNode(CFANode pNode) {
    if (pNode instanceof FunctionEntryNode) {
      Optional<FunctionExitNode> optExitNode = ((FunctionEntryNode) pNode).getExitNode();
      if (optExitNode.isPresent()) {
        FunctionExitNode exitNode = optExitNode.orElseThrow();
        return check(
            exitNode.getNumEnteringEdges() > 0,
            "Function entry node '%s' has reference to unreachable function exit node '%s'",
            pNode,
            exitNode);
      }
    }
    return pass();
  }

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return CfaValidator.createNodeValidator(this::checkNode).check(pCfaNetwork, pCfaMetadata);
  }
}
