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
package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.common.Pair;

public class IASTIdExpression extends IASTExpression {

  private final String name;
  private final IASTSimpleDeclaration declaration;
  // for relyguarantee prototype
  private Pair<String, Integer> primed  = null;
  private Integer tid = null;

  public IASTIdExpression(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IType pType,
      final String pName, final IASTSimpleDeclaration pDeclaration) {
    super(pRawSignature, pFileLocation, pType);
    name = pName;
    declaration = pDeclaration;
  }

  public String getName() {
    return name;
  }

  @Deprecated
  @Override
  public String getRawSignature() {
    // TODO Auto-generated method stub
    return super.getRawSignature();
  }

  /**
   * Get the declaration of the variable.
   * The result may be null if the variable was not declared.
   */
  public IASTSimpleDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public <R, X extends Exception> R accept(ExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(RightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return name;
  }

  // for relyguarantee prototype
  public void setPrimed(String name, int index) {
    this.primed = new Pair<String, Integer>(name, index);
  }

  // for relyguarantee prototype
  public Pair<String, Integer> getPrimed() {
    return this.primed;
  }

  // for relyguarantee prototype
  public Integer getTid() {
    return tid;
  }

  // for relyguarantee prototype
  public void setTid(Integer pTid) {
    tid = pTid;
  }




}
