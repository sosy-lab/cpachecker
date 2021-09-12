// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.CfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public interface CCfaNodeTransformer extends CfaNodeTransformer {

  public static CCfaNodeTransformer DEFAULT =
      new CCfaNodeTransformer() {

        @Override
        public CFANode transformCfaNode(CFANode pOriginalCfaNode) {
          return new CFANode(pOriginalCfaNode.getFunction());
        }

        @Override
        public CFATerminationNode transformCfaTerminationNode(
            CFATerminationNode pOriginalCfaTerminationNode) {
          return new CFATerminationNode(pOriginalCfaTerminationNode.getFunction());
        }

        @Override
        public FunctionExitNode transformFunctionExitNode(
            FunctionExitNode pOriginalFunctionExitNode) {
          return new FunctionExitNode(pOriginalFunctionExitNode.getFunction());
        }

        @Override
        public CFunctionEntryNode transformCFunctionEntryNode(
            CFunctionEntryNode pOriginalCFunctionEntryNode, FunctionExitNode pNewFunctionExitNode) {
          return new CFunctionEntryNode(
              pOriginalCFunctionEntryNode.getFileLocation(),
              (CFunctionDeclaration) pOriginalCFunctionEntryNode.getFunction(),
              pNewFunctionExitNode,
              pOriginalCFunctionEntryNode.getReturnVariable());
        }

        @Override
        public CFANode transformCLabelNode(CLabelNode pOriginalCLabelNode) {
          return new CLabelNode(pOriginalCLabelNode.getFunction(), pOriginalCLabelNode.getLabel());
        }
      };

  CFunctionEntryNode transformCFunctionEntryNode(
      CFunctionEntryNode pOriginalCFunctionEntryNode, FunctionExitNode pNewFunctionExitNode);

  CFANode transformCLabelNode(CLabelNode pOriginalCLabelNode);
}
