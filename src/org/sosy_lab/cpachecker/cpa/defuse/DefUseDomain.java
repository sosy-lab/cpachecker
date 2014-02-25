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
package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DefUseDomain implements AbstractDomain {
  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {
            DefUseState defUseState1 = (DefUseState) element1;
            DefUseState defUseState2 = (DefUseState) element2;

            return defUseState2.containsAllOf(defUseState1);
    }

    @Override
    public AbstractState join(AbstractState element1, AbstractState element2) {
            // Useless code, but helps to catch bugs by causing cast exceptions
            DefUseState defUseState1 = (DefUseState) element1;
            DefUseState defUseState2 = (DefUseState) element2;

            Set<DefUseDefinition> joined = new HashSet<> ();
            for (DefUseDefinition definition : defUseState1) {
              joined.add(definition);
            }

            for (DefUseDefinition definition : defUseState2) {
                if (!joined.contains(definition)) {
                  joined.add(definition);
                }
            }

            return new DefUseState(joined);
    }
}
