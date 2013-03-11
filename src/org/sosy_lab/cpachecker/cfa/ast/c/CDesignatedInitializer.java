/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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


import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.Initializer;

public class CDesignatedInitializer extends Initializer implements CInitializer {


  private final CDesignator left;
  private final CInitializer right;

  public CDesignatedInitializer(FileLocation pFileLocation, final CDesignator pLeft, final CInitializer pRight) {
    super(pFileLocation);
    left = pLeft;
    right = pRight;
  }

  @Override
  public String toASTString() {
      return left.toASTString() + " = " + right.toASTString();
  }

  public CDesignator getLeftHandSide() {
    return left;
  }

  public CInitializer getRightHandSide() {
    return right;
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(left);
    result = prime * result + Objects.hashCode(right);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CDesignatedInitializer)
        || !super.equals(obj)) {
      return false;
    }

    CDesignatedInitializer other = (CDesignatedInitializer) obj;

    return Objects.equals(other.left, left) && Objects.equals(other.right, right);
  }

}