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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis;

import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.EmptyVariableUsageElement;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.VariableUsageState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis.VariableUsageType;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class EmptyVariableUsageElement extends VariableUsageState {

  public EmptyVariableUsageElement() {
    super(VariableUsageType.EMPTY);
  }

  private static final long serialVersionUID = 7811014835418326945L;

  @Override
  public boolean isLessOrEqual(VariableUsageState pOther)
      throws CPAException, InterruptedException {
    if (pOther.getType().equals(this.getType())) {
      return true;
    }
    throw new CPAException(
        "The empty variable usage element cannot be compared to " + pOther.toString());
  }

  @Override
  public VariableUsageState join(VariableUsageState pOther)
      throws CPAException, InterruptedException {
    if (pOther instanceof EmptyVariableUsageElement || pOther.getType().equals(this.getType())) {
      return new VariableUsageState(VariableUsageType.EMPTY);
    }
    throw new CPAException(
        "The empty variable usage element cannot be merged with " + pOther.toString());
  }

  @Override
  public VariableUsageState meet(VariableUsageState pOther) throws CPAException {
    if (pOther instanceof EmptyVariableUsageElement) {
      return new VariableUsageState(VariableUsageType.EMPTY);
    }
    throw new CPAException(
        "The empty variable usage element cannot be merged with " + pOther.toString());
  }


}
