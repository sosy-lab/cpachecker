// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public enum PthreadFunction {
  CREATE("pthread_create");
  // TODO more pthread functions

  public final String name;

  PthreadFunction(String pName) {
    this.name = pName;
  }

  /**
   * @return true if the given CFAEdge is a call to the given pthread function
   */
  public static boolean isEdgeCallToFunction(CFAEdge pCfaEdge, PthreadFunction pPthreadFunction) {
    Optional<AAstNode> aAstNode = pCfaEdge.getRawAST();
    return aAstNode.isPresent()
        && aAstNode.get() instanceof CFunctionCallStatement
        && ((CFunctionCallStatement) aAstNode.get())
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals(pPthreadFunction.name);
  }
}
