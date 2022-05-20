// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.programcounter;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssignmentEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ProgramCounterTransferRelation extends SingleEdgeTransferRelation {

  static final TransferRelation INSTANCE = new ProgramCounterTransferRelation();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ProgramCounterState state = (ProgramCounterState) pState;

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        if (pCfaEdge instanceof ADeclarationEdge) {
          ADeclarationEdge edge = (ADeclarationEdge) pCfaEdge;
          if (edge.getDeclaration() instanceof AVariableDeclaration) {
            AVariableDeclaration declaration = (AVariableDeclaration) edge.getDeclaration();
            if (declaration
                .getQualifiedName()
                .equals(CFASingleLoopTransformation.PROGRAM_COUNTER_VAR_NAME)) {
              if (declaration.getInitializer() instanceof AInitializerExpression) {
                AExpression expression =
                    ((AInitializerExpression) declaration.getInitializer()).getExpression();
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
        if (pCfaEdge instanceof ProgramCounterValueAssumeEdge) {
          ProgramCounterValueAssumeEdge edge = (ProgramCounterValueAssumeEdge) pCfaEdge;
          BigInteger value = BigInteger.valueOf(edge.getProgramCounterValue());
          if (edge.getTruthAssumption()) {
            if (state.containsValue(value)) {
              state = ProgramCounterState.getStateForValue(value);
            } else {
              return ImmutableSet.of();
            }
          } else {
            state = state.remove(value);
            if (state.isBottom()) {
              return ImmutableSet.of();
            }
          }
        }
        break;
      case StatementEdge:
        if (pCfaEdge instanceof ProgramCounterValueAssignmentEdge) {
          ProgramCounterValueAssignmentEdge edge = (ProgramCounterValueAssignmentEdge) pCfaEdge;
          state =
              ProgramCounterState.getStateForValue(
                  BigInteger.valueOf(edge.getProgramCounterValue()));
        }
        break;
      default:
        // Program counter variable does not occur in other edges.
        break;
    }
    if (state == null || state.isBottom()) {
      return ImmutableSet.of();
    }
    return Collections.singleton(state);
  }
}
