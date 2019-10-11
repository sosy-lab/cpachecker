/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class UnreachableSegmentation<T extends ExtendedCompletLatticeAbstractState<T>>
    extends ArraySegmentationState<T> {

  public UnreachableSegmentation(ArraySegmentationState<T> pPreviousState) {
    super(pPreviousState);
  }


  private static final long serialVersionUID = -3937221925009806448L;

  @Override
  public boolean isLessOrEqual(ArraySegmentationState<T> pOther)
      throws CPAException, InterruptedException {
    return true;
  }

  @Override
  public ArraySegmentationState<T> join(ArraySegmentationState<T> pOther)
      throws CPAException, InterruptedException {
    return pOther;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (language.equals(Language.C) && !splitCondition.equals(CIntegerLiteralExpression.ONE)) {
      builder.append(this.splitCondition.toASTString() + ": ");
    }
    builder.append("[x]");
    return builder.toString();
  }




}
