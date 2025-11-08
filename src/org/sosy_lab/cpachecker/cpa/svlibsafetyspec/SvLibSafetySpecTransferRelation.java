// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.svlibsafetyspec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.builder.SvLibTermBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SvLibSafetySpecTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;
  private final LogManager logger;

  public SvLibSafetySpecTransferRelation(CFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    logger = pLogger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(pState instanceof SvLibSafetySpecState state)) {
      throw new CPATransferException("Expected SvLibSafetySpecState, got " + pState.getClass());
    }

    if (state.hasPropertyViolation()) {
      // Once we have a property violation there is no need to continue.
      logger.log(Level.FINE, "Attempting to transfer from a property-violating state.");
      return ImmutableList.of();
    }

    ImmutableList.Builder<SvLibSafetySpecState> outStates = ImmutableList.builder();

    Set<SvLibTagProperty> propertiesToProof =
        cfa.getMetadata()
            .getSvLibCfaMetadata()
            .orElseThrow()
            .tagAnnotations()
            .get(cfaEdge.getPredecessor());

    // First construct one successor per property we need to proof
    for (SvLibTagProperty property : propertiesToProof) {
      SvLibSafetySpecState successorState =
          switch (property) {
            case SvLibCheckTrueTag pSvLibCheckTruetTag ->
                new SvLibSafetySpecState(
                    ImmutableSet.of(
                        SvLibTermBuilder.booleanNegation(pSvLibCheckTruetTag.getTerm())),
                    true);
            case SvLibEnsuresTag pSvLibEnsuresTag ->
                throw new UnsupportedCodeException(
                    "Ensures tags are not supported in safety specifications.", cfaEdge);
            case SvLibInvariantTag pSvLibInvariantTag ->
                throw new UnsupportedCodeException(
                    "Invariant tags are not supported in safety specifications.", cfaEdge);
            case SvLibRequiresTag pSvLibRequiresTag ->
                throw new UnsupportedCodeException(
                    "Requires tags are not supported in safety specifications.", cfaEdge);
          };
      outStates.add(successorState);
    }

    // Second add the continuation of the original program, where we assume that everything could
    // be proven correct
    // TODO: Actually fill this in with the assumptions
    outStates.add(new SvLibSafetySpecState(ImmutableSet.of(), false));

    return outStates.build();
  }
}
