// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.k3safetyspec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3EnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3InvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.builder.K3TermBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class K3SafetySpecTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;

  public K3SafetySpecTransferRelation(CFA pCfa) {
    cfa = pCfa;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(pState instanceof K3SafetySpecState state)) {
      throw new CPATransferException("Expected K3SafetySpecState, got " + pState.getClass());
    }

    if (state.hasPropertyViolation()) {
      // Once we have a property violation there is no need to continue.
      return ImmutableList.of();
    }

    Builder<K3SafetySpecState> outStates = ImmutableList.builder();

    Set<K3TagProperty> propertiesToProof =
        cfa.getMetadata()
            .getK3CfaMetadata()
            .orElseThrow()
            .tagAnnotations()
            .get(cfaEdge.getPredecessor());

    // First construct one successor per property we need to proof
    for (K3TagProperty property : propertiesToProof) {
      K3SafetySpecState successorState =
          switch (property) {
            case K3AssertTag pK3AssertTag ->
                new K3SafetySpecState(
                    ImmutableSet.of(K3TermBuilder.booleanNegation(pK3AssertTag.getTerm())), true);
            case K3EnsuresTag pK3EnsuresTag -> null;
            case K3InvariantTag pK3InvariantTag -> null;
            case K3RequiresTag pK3RequiresTag -> null;
          };
      outStates.add(successorState);
    }

    // Second add the continuation of the original program, where we assume that everything could
    // be proven correct
    // TODO: Actually fill this in with the assumptions
    outStates.add(new K3SafetySpecState(ImmutableSet.of(), false));

    return outStates.build();
  }
}
