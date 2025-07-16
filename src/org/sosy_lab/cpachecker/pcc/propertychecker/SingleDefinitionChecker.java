// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils.VariableExtractor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Checks if a certain variable is defined at most once by the program. */
public class SingleDefinitionChecker implements PropertyChecker {

  private final MemoryLocation varDefName;
  private ProgramDefinitionPoint point;

  public SingleDefinitionChecker(String varWithSingleDef) {
    varDefName = MemoryLocation.parseExtendedQualifiedName(varWithSingleDef);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
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
          || !(stillSingleDefinition(rdState.getGlobalReachingDefinitions().get(varDefName))
              && stillSingleDefinition(rdState.getLocalReachingDefinitions().get(varDefName)))) {
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
      if (p instanceof ProgramDefinitionPoint programDefinitionPoint) {
        // check if there is another known definition
        if (point == null) {
          if (isDefinitionInProgram(programDefinitionPoint)) {
            point = programDefinitionPoint;
          }
        } else if (!p.equals(point)) {
          // check if it is a real definition
          if (isDefinitionInProgram(programDefinitionPoint)) {
            return false;
          }
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
      if (edge instanceof CStatementEdge cStatementEdge) {
        left = null;
        if (cStatementEdge.getStatement() instanceof CExpressionAssignmentStatement) {
          left = ((CExpressionAssignmentStatement) cStatementEdge.getStatement()).getLeftHandSide();
        }
        if (cStatementEdge.getStatement() instanceof CFunctionCallAssignmentStatement) {
          left =
              ((CFunctionCallAssignmentStatement) cStatementEdge.getStatement()).getLeftHandSide();
        }
        if (left != null) {
          VariableExtractor extractor = new VariableExtractor(edge);
          extractor.resetWarning();
          MemoryLocation var;
          try {
            var = left.accept(extractor);
          } catch (UnsupportedCodeException e) {
            var = null;
          }
          if (var != null && var.equals(varDefName)) {
            return true;
          }
        }
      }
      if (edge instanceof CDeclarationEdge cDeclarationEdge
          && cDeclarationEdge.getDeclaration() instanceof CVariableDeclaration
          && ((CVariableDeclaration) cDeclarationEdge.getDeclaration()).getInitializer() != null
          && MemoryLocation.forDeclaration(cDeclarationEdge.getDeclaration()).equals(varDefName)) {
        return true;
      }
    }
    return false;
  }
}
