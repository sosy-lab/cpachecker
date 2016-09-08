/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;

import java.util.Set;

public abstract class PropertyScopeInstance {
  private static int id = 0;

  public static AbstractionPropertyScopeInstance create(AbstractionFormula startFormula) {
    id++;
    return new AbstractionPropertyScopeInstance(startFormula);
  }

  public static AutomatonPropertyScopeInstance create(AutomatonState startState) {
    id++;
    return new AutomatonPropertyScopeInstance(startState);
  }

  public static class AbstractionPropertyScopeInstance extends PropertyScopeInstance {
    private final AbstractionFormula startFormula;

    private AbstractionPropertyScopeInstance(AbstractionFormula pStartFormula) {
      startFormula = pStartFormula;
    }

    public AbstractionFormula getStartFormula() {
      return startFormula;
    }
  }

  public static class AutomatonPropertyScopeInstance extends PropertyScopeInstance {
    private final AutomatonState startState;

    private AutomatonPropertyScopeInstance(AutomatonState pStartState) {
      startState = pStartState;
    }

    public AutomatonState getStartState() {
      return startState;
    }
  }

  @Override
  public String toString() {
    return String.format("%s (%d)", getClass().getName(), id);
  }
}
