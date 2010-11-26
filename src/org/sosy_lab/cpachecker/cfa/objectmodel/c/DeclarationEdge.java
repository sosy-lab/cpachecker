/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.ast.IASTDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclarator;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;

import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class DeclarationEdge extends AbstractCFAEdge {

  private final IASTSimpleDeclaration declaration;

  public DeclarationEdge(IASTSimpleDeclaration declaration, int lineNumber,
      CFANode predecessor, CFANode successor) {
    super(declaration.getRawSignature(), lineNumber, predecessor, successor);
    this.declaration = declaration;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.DeclarationEdge;
  }

  public IASTDeclarator[] getDeclarators() {
    return declaration.getDeclarators();
  }

  public IASTDeclSpecifier getDeclSpecifier() {
    return declaration.getDeclSpecifier();
  }

  @Override
  public IASTSimpleDeclaration getRawAST() {
    return declaration;
  }
}
