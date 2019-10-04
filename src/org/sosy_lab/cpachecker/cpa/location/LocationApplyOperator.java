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
package org.sosy_lab.cpachecker.cpa.location;

import java.util.Map;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.cpa.location.LocationStateWithEdge.ProjectedLocationStateWithEdge;

public class LocationApplyOperator implements ApplyOperator {

  private final Map<CFANode, Boolean> cachedNodes = new TreeMap<>();

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pState1;
    LocationStateWithEdge state2 = (LocationStateWithEdge) pState2;

    if (state2.getAbstractEdge() instanceof WrapperCFAEdge) {
      // Ordinary transition
      return null;
    } else if (state1.getAbstractEdge() == EmptyEdge.getInstance()
        || state2.getAbstractEdge() == EmptyEdge.getInstance()) {
      return null;
    } else {
      return state1.updateEdge(EmptyEdge.getInstance());
    }
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    return ProjectedLocationStateWithEdge.getInstance();
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pParent;

    assert pEdge == state1.getAbstractEdge();
    assert pEdge instanceof WrapperCFAEdge;

    // That is important to remove CFAEdge, to avoid considering it
    // Evil hack!
    return ProjectedLocationStateWithEdge.getInstance();
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    return true;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    LocationState state = (LocationState) pState;
    CFANode node = state.locationNode;

    if (cachedNodes.containsKey(node)) {
      return cachedNodes.get(node);
    }

    boolean result = false;
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge edge = node.getLeavingEdge(i);
      if (!isRedundantEdge(edge)) {
        result = true;
      }
    }
    cachedNodes.put(node, result);
    return result;
  }

  private boolean isRedundantEdge(CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      return true;
    } else if (edge instanceof CDeclarationEdge) {
      CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
      if (decl instanceof CVariableDeclaration) {
        CInitializer init = ((CVariableDeclaration) decl).getInitializer();
        if (init == null) {
          return true;
        }
        if (init instanceof CInitializerExpression) {
          CExpression expr = ((CInitializerExpression) init).getExpression();
          if (expr instanceof CLiteralExpression) {
            return true;
          }
        }

      } else {
        return true;
      }
    } else if (edge instanceof FunctionReturnEdge) {
      return true;
    }

    return false;
  }

}
