/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

/**
 * An edge to store declarations for global variables. These are different
 * from standard declarations, in that they can have also an initializer
 * for the declared variable
 */
public class GlobalDeclarationEdge extends DeclarationEdge {

  public GlobalDeclarationEdge(final String pRawSignature, final int pLineNumber,
      final CFANode pPredecessor, final CFANode pSuccessor, final IASTDeclaration pDeclaration) {

    super(pRawSignature, pLineNumber, pPredecessor, pSuccessor, pDeclaration);
  }

  @Override
  public boolean isGlobal() {
    return true;
  }
}
