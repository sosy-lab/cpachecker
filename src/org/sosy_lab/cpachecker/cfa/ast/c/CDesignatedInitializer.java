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
package org.sosy_lab.cpachecker.cfa.ast.c;


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
    int result = 1;
    result = prime * result + ((left == null) ? 0 : left.hashCode());
    result = prime * result + ((right == null) ? 0 : right.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof CDesignatedInitializer)) { return false; }
    CDesignatedInitializer other = (CDesignatedInitializer) obj;
    if (left == null) {
      if (other.left != null) { return false; }
    } else if (!left.equals(other.left)) { return false; }
    if (right == null) {
      if (other.right != null) { return false; }
    } else if (!right.equals(other.right)) { return false; }
    return true;
  }

}