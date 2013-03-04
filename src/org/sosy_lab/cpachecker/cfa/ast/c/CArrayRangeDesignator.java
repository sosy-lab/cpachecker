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

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;


public class CArrayRangeDesignator extends CDesignator {

  private final IAExpression rangeFloor;
  private final IAExpression rangeCeiling;
  private final CDesignator arrayDesignator;

  public CArrayRangeDesignator(final FileLocation pFileLocation,
                          final CExpression pRangeFloor,
                          final CExpression pRangeCeiling,
                          final CDesignator pArrayDesignator) {
     super(pFileLocation);
     rangeFloor = pRangeFloor;
     rangeCeiling = pRangeCeiling;
     arrayDesignator = pArrayDesignator;
  }

  public CDesignator getArrayDesignator() {
    return arrayDesignator;
  }

  public CExpression getFloorExpression() {
    return (CExpression) rangeFloor;
  }

  public CExpression getCeilExpression() {
    return (CExpression) rangeCeiling;
  }

  @Override
  public String toASTString() {
    return "[" + rangeFloor.toASTString() + " ... " + rangeCeiling.toASTString() + "]";
  }

  @Override
  public String toParenthesizedASTString() {
    return toASTString();
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((arrayDesignator == null) ? 0 : arrayDesignator.hashCode());
    result = prime * result + ((rangeCeiling == null) ? 0 : rangeCeiling.hashCode());
    result = prime * result + ((rangeFloor == null) ? 0 : rangeFloor.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof CArrayRangeDesignator)) { return false; }
    CArrayRangeDesignator other = (CArrayRangeDesignator) obj;
    if (arrayDesignator == null) {
      if (other.arrayDesignator != null) { return false; }
    } else if (!arrayDesignator.equals(other.arrayDesignator)) { return false; }
    if (rangeCeiling == null) {
      if (other.rangeCeiling != null) { return false; }
    } else if (!rangeCeiling.equals(other.rangeCeiling)) { return false; }
    if (rangeFloor == null) {
      if (other.rangeFloor != null) { return false; }
    } else if (!rangeFloor.equals(other.rangeFloor)) { return false; }

    return true;
  }


}
