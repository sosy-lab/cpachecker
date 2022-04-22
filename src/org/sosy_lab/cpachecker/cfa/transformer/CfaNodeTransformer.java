// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

@FunctionalInterface
public interface CfaNodeTransformer {

  CFANode transform(CFANode pCfaNode, CfaNetwork pCfaNetwork, CfaNodeSubstitution pSubstitution);
}
