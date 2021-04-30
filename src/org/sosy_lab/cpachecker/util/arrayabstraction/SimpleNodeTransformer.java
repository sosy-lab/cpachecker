// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.util.CCfaTransformer;

class SimpleNodeTransformer implements CCfaTransformer.NodeTransformer {

  @Override
  public CFANode transformCfaNode(CFANode pOldCfaNode) {
    return new CFANode(pOldCfaNode.getFunction());
  }

  @Override
  public CFATerminationNode transformCfaTerminationNode(CFATerminationNode pOldCfaTerminationNode) {
    return new CFATerminationNode(pOldCfaTerminationNode.getFunction());
  }

  @Override
  public FunctionExitNode transformFunctionExitNode(FunctionExitNode pOldFunctionExitNode) {
    return new FunctionExitNode(pOldFunctionExitNode.getFunction());
  }

  @Override
  public CFunctionEntryNode transformCFunctionEntryNode(
      CFunctionEntryNode pOldCFunctionEntryNode, FunctionExitNode pNewFunctionExitNode) {
    return new CFunctionEntryNode(
        pOldCFunctionEntryNode.getFileLocation(),
        (CFunctionDeclaration) pOldCFunctionEntryNode.getFunction(),
        pNewFunctionExitNode,
        pOldCFunctionEntryNode.getReturnVariable());
  }

  @Override
  public CFANode transformCLabelNode(CLabelNode pOldCLabelNode) {
    return new CLabelNode(pOldCLabelNode.getFunction(), pOldCLabelNode.getLabel());
  }
}
