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
package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CReturnStatement extends AReturnStatement implements CAstNode {



  public CReturnStatement(final FileLocation pFileLocation,
                             final CExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public CExpression getReturnValue() {
    return (CExpression) super.getReturnValue();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CReturnStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
