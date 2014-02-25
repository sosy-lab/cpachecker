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
package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;




public abstract class AFunctionCallAssignmentStatement extends AStatement implements IAssignment, AFunctionCall {


  private final IALeftHandSide leftHandSide;
  private final AFunctionCallExpression rightHandSide;

  public AFunctionCallAssignmentStatement(FileLocation pFileLocation,
                                          IALeftHandSide pLeftHandSide,
                                          AFunctionCallExpression pRightHandSide) {
    super(pFileLocation);

    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;

  }

  @Override
  public IALeftHandSide getLeftHandSide() {
    return leftHandSide;
  }


  @Override
  public AFunctionCallExpression getRightHandSide() {
    return rightHandSide;
  }


  @Override
  public AFunctionCallExpression getFunctionCallExpression() {
    return rightHandSide;
  }

  @Override
  public String toASTString() {
    return leftHandSide.toASTString()
        + " = " + rightHandSide.toASTString() + ";";
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(leftHandSide);
    result = prime * result + Objects.hashCode(rightHandSide);
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

    if (!(obj instanceof AFunctionCallAssignmentStatement)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionCallAssignmentStatement other = (AFunctionCallAssignmentStatement) obj;

    return Objects.equals(other.leftHandSide, leftHandSide)
            && Objects.equals(other.rightHandSide, rightHandSide);
  }

}