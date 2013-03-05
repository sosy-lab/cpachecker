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
package org.sosy_lab.cpachecker.cfa.ast;


public abstract class AExpressionAssignmentStatement extends AStatement implements IAssignment {

  private final IAExpression leftHandSide;
  private final IAExpression rightHandSide;

  public AExpressionAssignmentStatement(FileLocation pFileLocation, IAExpression pLeftHandSide,
      IAExpression pRightHandSide) {
    super(pFileLocation);
    leftHandSide = pLeftHandSide;
    rightHandSide = pRightHandSide;
  }

  @Override
  public String toASTString() {
    return leftHandSide.toASTString()
        + " = " + rightHandSide.toASTString() + ";";
  }

  @Override
  public IAExpression getLeftHandSide() {
    return leftHandSide;
  }

  @Override
  public IAExpression getRightHandSide() {
    return rightHandSide;
  }

  @Override
  public IAStatement asStatement() {
    return this;
  }

  @Override
  public <R, X extends Exception> R accept(AStatementVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((leftHandSide == null) ? 0 : leftHandSide.hashCode());
    result = prime * result + ((rightHandSide == null) ? 0 : rightHandSide.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof AExpressionAssignmentStatement)) { return false; }
    AExpressionAssignmentStatement other = (AExpressionAssignmentStatement) obj;
    if (leftHandSide == null) {
      if (other.leftHandSide != null) { return false; }
    } else if (!leftHandSide.equals(other.leftHandSide)) { return false; }
    if (rightHandSide == null) {
      if (other.rightHandSide != null) { return false; }
    } else if (!rightHandSide.equals(other.rightHandSide)) { return false; }
    return true;
  }

}
