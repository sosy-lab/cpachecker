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
package org.sosy_lab.cpachecker.cpa.programcounter;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public enum ProgramCounterTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {

    ProgramCounterState state = (ProgramCounterState) pState;

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        if (pCfaEdge instanceof ADeclarationEdge) {
          ADeclarationEdge edge = (ADeclarationEdge) pCfaEdge;
          if (edge.getDeclaration() instanceof AVariableDeclaration) {
            AVariableDeclaration declaration = (AVariableDeclaration) edge.getDeclaration();
            if (declaration.getQualifiedName().equals(CFASingleLoopTransformation.PROGRAM_COUNTER_VAR_NAME)) {
              if (declaration.getInitializer() instanceof AInitializerExpression) {
                IAExpression expression = ((AInitializerExpression) declaration.getInitializer()).getExpression();
                if (expression instanceof AIntegerLiteralExpression) {
                  BigInteger pcValue = ((AIntegerLiteralExpression) expression).getValue();
                  state = ProgramCounterState.getStateForValue(pcValue);
                }
              }
            }
          }
        }
        break;
      case AssumeEdge:
        if (pCfaEdge instanceof CFASingleLoopTransformation.ProgramCounterValueAssumeEdge) {
          CFASingleLoopTransformation.ProgramCounterValueAssumeEdge edge = (CFASingleLoopTransformation.ProgramCounterValueAssumeEdge) pCfaEdge;
          BigInteger value = BigInteger.valueOf(edge.getProgramCounterValue());
          if (edge.getTruthAssumption()) {
            if (state.containsValue(value)) {
              state = ProgramCounterState.getStateForValue(value);
            } else {
              return Collections.emptySet();
            }
          } else {
            state = state.remove(value);
            if (state.isBottom()) {
              return Collections.emptySet();
            }
          }
        }
        break;
      case StatementEdge:
        if (pCfaEdge instanceof CFASingleLoopTransformation.ProgramCounterValueAssignmentEdge) {
          CFASingleLoopTransformation.ProgramCounterValueAssignmentEdge edge = (CFASingleLoopTransformation.ProgramCounterValueAssignmentEdge) pCfaEdge;
          state = ProgramCounterState.getStateForValue(BigInteger.valueOf(edge.getProgramCounterValue()));
        }
        break;
    }
    if (state == null || state.isBottom()) {
      return Collections.emptySet();
    }
    return Collections.singleton(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
