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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;



/**
 * Helper class that collects the set of variables on which the initial (i.e., that last, as iteration runs in reverse)
 * assume edge in the given path depend on (i.e. the transitive closure).
 */
public class InitialAssumptionUseDefinitionCollector extends AssumptionUseDefinitionCollector {

  private boolean isInitialAssumption = true;

  @Override
  protected void handleAssumption(CFAEdge edge) {
    if(!isInitialAssumption) {
      return;
    }

    isInitialAssumption    = false;
    CAssumeEdge assumeEdge = (CAssumeEdge)edge;
    collectVariables(assumeEdge, assumeEdge.getExpression());
  }
}