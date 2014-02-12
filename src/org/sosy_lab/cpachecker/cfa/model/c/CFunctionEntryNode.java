/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.model.c;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class CFunctionEntryNode extends FunctionEntryNode {

  public CFunctionEntryNode(final int pLineNumber,
      final CFunctionDeclaration pFunctionDefinition,
      final FunctionExitNode pExitNode,
      final List<String> pParameterNames) {

    super(pLineNumber, pFunctionDefinition.getName(), pExitNode, pFunctionDefinition, pParameterNames);
  }

  @Override
  public CFunctionDeclaration getFunctionDefinition() {
    return  (CFunctionDeclaration)functionDefinition;
  }



  @Override
  public List<CParameterDeclaration> getFunctionParameters() {
    return getFunctionDefinition().getParameters();
  }
}
