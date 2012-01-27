/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.base.Optional;

public class DeclarationEdge extends AbstractCFAEdge {

  private final IASTDeclaration declaration;

  public DeclarationEdge(final String pRawSignature, final int pLineNumber,
      final CFANode pPredecessor, final CFANode pSuccessor, final IASTDeclaration pDeclaration) {

    super(pRawSignature, pLineNumber, pPredecessor, pSuccessor);
    declaration = pDeclaration;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.DeclarationEdge;
  }

  public StorageClass getStorageClass() {
    return declaration.getStorageClass();
  }

  public IType getDeclSpecifier() {
    return declaration.getDeclSpecifier();
  }

  public String getName() {
    return declaration.getName();
  }

  public IASTInitializer getInitializer() {
    if (declaration instanceof IASTVariableDeclaration) {
      return ((IASTVariableDeclaration) declaration).getInitializer();
    } else {
      return null;
    }
  }

  @Override
  public Optional<IASTDeclaration> getRawAST() {
    return Optional.of(declaration);
  }

  @Override
  public String getCode() {
    return declaration.toASTString();
  }

  public boolean isGlobal() {
    return false;
  }
}
