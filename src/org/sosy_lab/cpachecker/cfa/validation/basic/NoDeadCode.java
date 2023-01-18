// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation.basic;

import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.validation.AbstractCfaValidator;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidationResult;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidator;

/** This check ensures that there is no obvious dead code in a CFA. */
public final class NoDeadCode extends AbstractCfaValidator {

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return CfaValidator.createNodeValidator(
            node ->
                check(
                    pCfaNetwork.inDegree(node) > 0 || node instanceof FunctionEntryNode,
                    "Dead code: node '%s' has no incoming edges (successors are %s)",
                    node,
                    pCfaNetwork.successors(node)))
        .check(pCfaNetwork, pCfaMetadata);
  }
}
