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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils.VariableExtractor;


/**
 * Checks if a certain variable is defined at most once by the program.
 */
public class SingleDefinitionChecker implements PropertyChecker {

  private final String varDefName;
  private ProgramDefinitionPoint point;

  public SingleDefinitionChecker(String varWithSingleDef) {
    varDefName = varWithSingleDef;
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean satisfiesProperty(Collection<AbstractState> pCertificate) {
    point = null;
    ReachingDefState rdState;

    for (AbstractState elem : pCertificate) {
      // check if variable is at most defined once
      rdState = AbstractStates.extractStateByType(elem, ReachingDefState.class);
      if (rdState == ReachingDefState.topElement
          || !(stillSingleDefinition(rdState.getGlobalReachingDefinitions().get(varDefName)) && stillSingleDefinition(rdState
              .getLocalReachingDefinitions().get(varDefName)))) {
        return false;
      }
    }
    return true;
  }

  private boolean stillSingleDefinition(Set<DefinitionPoint> definitions) {
    if (definitions == null) {
      return true;
    }
    for (DefinitionPoint p : definitions) {
      if (p instanceof ProgramDefinitionPoint) {
        // check if there is another known definition
        if (point == null) {
          if (isDefinitionInProgram((ProgramDefinitionPoint) p)) {
            point = (ProgramDefinitionPoint) p;
          }
        } else if (!p.equals(point)) {
          // check if it is a real definition
          if (isDefinitionInProgram((ProgramDefinitionPoint) p)) { return false; }
        }
      }
    }
    return true;
  }

  private boolean isDefinitionInProgram(ProgramDefinitionPoint pdp) {
    CFAEdge edge;
    CExpression left;
    if (pdp.getDefinitionEntryLocation().hasEdgeTo(pdp.getDefinitionExitLocation())) {
      edge = pdp.getDefinitionEntryLocation().getEdgeTo(pdp.getDefinitionExitLocation());
      if (edge instanceof CStatementEdge) {
        left = null;
        if (((CStatementEdge) edge).getStatement() instanceof CExpressionAssignmentStatement) {
          left = ((CExpressionAssignmentStatement) ((CStatementEdge) edge).getStatement()).getLeftHandSide();
        }
        if (((CStatementEdge) edge).getStatement() instanceof CFunctionCallAssignmentStatement) {
          left = ((CFunctionCallAssignmentStatement) ((CStatementEdge) edge).getStatement()).getLeftHandSide();
        }
        if (left != null) {
          VariableExtractor extractor = new VariableExtractor(edge);
          extractor.resetWarning();
          String var;
          try {
            var = left.accept(extractor);
          } catch (UnsupportedCCodeException e) {
            var = null;
          }
          if (var != null && var.equals(varDefName)) {
            return true;
          }
        }
      }
      if (edge instanceof CDeclarationEdge
          && ((CDeclarationEdge) edge).getDeclaration() instanceof CVariableDeclaration
          && ((CVariableDeclaration) ((CDeclarationEdge) edge).getDeclaration()).getInitializer() != null
          && ((CVariableDeclaration) ((CDeclarationEdge) edge).getDeclaration()).getName().equals(varDefName)) {
        return true;
      }
    }
    return false;
  }

}
