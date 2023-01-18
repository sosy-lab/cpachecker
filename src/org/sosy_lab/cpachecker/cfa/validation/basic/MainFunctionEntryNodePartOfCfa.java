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

/** This {@link CfaValidator} ensures that the main function entry node is part of the CFA. */
public final class MainFunctionEntryNodePartOfCfa extends AbstractCfaValidator {

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    FunctionEntryNode mainFunctionEntryNode = pCfaMetadata.getMainFunctionEntry();
    return check(
        pCfaNetwork.nodes().contains(mainFunctionEntryNode),
        "Main function entry node '%s' isn't part of the CFA",
        mainFunctionEntryNode);
  }
}
