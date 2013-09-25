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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

enum InvariantsDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public InvariantsState join(AbstractState pElement1, AbstractState pElement2) {
    InvariantsState element1 = (InvariantsState) pElement1;
    InvariantsState element2 = (InvariantsState) pElement2;
    InvariantsState result = element1.join(element2, false);
    if (result != element1 && result != element2) {
      Set<CFANode> locations = new HashSet<>();
      for (Map.Entry<CFANode, Collection<InvariantsState>> entry : element1.getStateMap().asMap().entrySet()) {
        if (entry.getValue().contains(element1) && entry.getValue().contains(element2)) {
          locations.add(entry.getKey());
        }
      }
      for (CFANode location : locations) {
        element1.getStateMap().put(location, result);
      }
    }
    return result;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {
    if (pElement1 == pElement2) {
      return true;
    }
    InvariantsState element1 = (InvariantsState) pElement1;
    InvariantsState element2 = (InvariantsState) pElement2;
    return element1.isLessThanOrEqualTo(element2);
  }

}
