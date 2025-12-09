// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.svlibsafetyspec;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSafetyTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.visitors.ModifiedVariablesCollectorVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.specification.SvLibSpecificationInformation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class SvLibSafetySpecTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;
  private final SvLibSpecificationInformation specInfo;
  private final LogManager logger;
  private final boolean doingWitnessValidation;

  public SvLibSafetySpecTransferRelation(
      CFA pCfa,
      SvLibSpecificationInformation pSpecInfo,
      LogManager pLogger,
      boolean pDoingWitnessValidation) {
    cfa = pCfa;
    specInfo = pSpecInfo;
    logger = pLogger;
    doingWitnessValidation = pDoingWitnessValidation;
  }

  private Set<SvLibSimpleDeclaration> collectModifiedVariables(Set<CFAEdge> pEdges)
      throws UnsupportedCodeException {
    // Now collect all modified variables across all these loops
    ImmutableSet.Builder<SvLibSimpleDeclaration> modifiedVariablesBuilder = ImmutableSet.builder();
    for (CFAEdge cfaEdge : pEdges) {
      switch (cfaEdge) {
        case SvLibFunctionCallEdge pSvLibFunctionCallEdge -> {
          // TODO: We need to collect the global variables modified by the function
          //  call
          throw new UnsupportedCodeException(
              "Collecting modified variables accross function calls is not yet supported.",
              cfaEdge);
        }
        case SvLibStatementEdge pSvLibStatementEdge -> {
          ModifiedVariablesCollectorVisitor modifiedVarsVisitor =
              new ModifiedVariablesCollectorVisitor();
          modifiedVariablesBuilder.addAll(
              pSvLibStatementEdge.getStatement().accept(modifiedVarsVisitor));
        }
        default -> {
          // Other edges cannot modify variables
        }
      }
    }

    return modifiedVariablesBuilder.build();
  }

  private SvLibSafetySpecAssumptionState handleAssertionOfInvariantAtLoop(
      CFAEdge pCfaEdge,
      SvLibInvariantTag pSvLibInvariantTag,
      ImmutableSet<SvLibRelationalTerm> pAssumptions)
      throws UnsupportedCodeException {
    // First find the loop structure which corresponds to the loop head given by this
    // invariant
    if (cfa.getLoopStructure().isEmpty()) {
      throw new UnsupportedCodeException("Invariants require loop structure analysis.", pCfaEdge);
    }

    // TODO: This will get trickier with `continue`, `break` and `goto`'s but in principle
    //  the way to do this should be similar to all of them, but they may require a better
    //  matching of the CFA node to the loop heads
    LoopStructure loopStructure = cfa.getLoopStructure().orElseThrow();
    ImmutableSet<Loop> loopsAtHead = loopStructure.getLoopsForLoopHead(pCfaEdge.getPredecessor());

    // Now collect all modified variables across all these loops
    Set<SvLibSimpleDeclaration> modifiedVariables =
        collectModifiedVariables(
            FluentIterable.from(loopsAtHead).transformAndConcat(Loop::getInnerLoopEdges).toSet());

    if (FluentIterable.from(loopsAtHead)
        .transformAndConcat(Loop::getInnerLoopEdges)
        .contains(pCfaEdge)) {
      // We are entering the loop, so we need to havoc all variables modified in the loop,
      // assume the invariant holds and assert it once we reach the loop head again
      return new SvLibSafetySpecFutureAssertionState(
          FluentIterable.from(pAssumptions).append(pSvLibInvariantTag.getTerm()).toSet(),
          modifiedVariables,
          pCfaEdge.getSuccessor(),
          pSvLibInvariantTag.getTerm());
    } else if (FluentIterable.from(loopsAtHead)
        .transformAndConcat(Loop::getOutgoingEdges)
        .contains(pCfaEdge)) {
      // We are exiting the loop, so we need to havoc all variables modified in the loop
      // and then assume the invariant holds to continue
      return new SvLibSafetySpecAssumptionState(
          FluentIterable.from(pAssumptions).append(pSvLibInvariantTag.getTerm()).toSet(),
          modifiedVariables,
          false);
    }

    throw new UnsupportedCodeException(
        "Invariants are only supported at loop heads currently, "
            + "if this is the head of a goto loop then this "
            + "may not yet be handled correctly",
        pCfaEdge);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(pState instanceof SvLibSafetySpecAssumptionState state)) {
      throw new CPATransferException("Expected SvLibSafetySpecState, got " + pState.getClass());
    }

    if (state.hasPropertyViolation()) {
      // Once we have a property violation there is no need to continue.
      logger.log(Level.FINE, "Attempting to transfer from a property-violating state.");
      return ImmutableList.of();
    }

    Set<SvLibTagProperty> propertiesToProof =
        specInfo.tagAnnotations().get(pCfaEdge.getPredecessor());

    ImmutableSet.Builder<SvLibSafetySpecAssumptionState> outStates =
        ImmutableSet.builderWithExpectedSize(propertiesToProof.size() + 1);

    // First collect all the properties that need to hold at the successor of this edge and create a
    // successor for each asserting that it holds, at the same time collect all the assumptions to
    // then be able to use them to continue the control-flow as required
    ImmutableSet.Builder<SvLibRelationalTerm> assumptionsBuilder =
        ImmutableSet.builderWithExpectedSize(propertiesToProof.size());
    for (SvLibTagProperty property : propertiesToProof) {
      switch (property) {
        case SvLibRequiresTag pSvLibRequiresTag -> {
          // Requires tags are handled by putting them into the assertions to be checked in a later
          // state
        }
        case SvLibSafetyTagProperty pSvLibSafetyTagProperty -> {
          assumptionsBuilder.add(pSvLibSafetyTagProperty.getTerm());
          outStates.add(
              new SvLibSafetySpecAssumptionState(
                  ImmutableSet.of(
                      SvLibRelationalTerm.booleanNegation(pSvLibSafetyTagProperty.getTerm())),
                  ImmutableSet.of(),
                  true));
        }
      }
    }

    // Now iterate over all the successors again and obtain the modular abstraction successors,
    // where we need to remember if there was at least one modular abstraction. In case there was
    // none, we need to add the continuation of the original program as well. We also always add the
    // continuation of the original program in case we are doing verification and not witness
    // validation.
    boolean hasModularAbstraction = false;

    for (SvLibTagProperty property : propertiesToProof) {
      switch (property) {
        case SvLibCheckTrueTag pSvLibCheckTrueTag -> {
          // Not a modular abstraction so we continue
        }
        case SvLibEnsuresTag pSvLibEnsuresTag -> {
          // The ensures tags are handled by the required tags
        }
        case SvLibInvariantTag pSvLibInvariantTag -> {
          hasModularAbstraction = true;
          outStates.add(
              handleAssertionOfInvariantAtLoop(
                  pCfaEdge, pSvLibInvariantTag, assumptionsBuilder.build()));
        }

        case SvLibRequiresTag pSvLibRequiresTag -> {
          throw new UnsupportedCodeException("Requires tags are not yet supported", pCfaEdge);
        }
      }
    }

    // There is a dependence on if we want to do
    // witness validation or verification. In case we just want to do verification, then we
    // continue with the original program, since we just keep adding states.
    // In case we are doing witness
    // validation, we **must** abstract using the invariants, and therefore, we can only continue
    // through the modular abstractions, unless there are none, in which case we also need to
    // continue with the original program
    if (!(doingWitnessValidation && hasModularAbstraction)) {
      outStates.add(
          new SvLibSafetySpecAssumptionState(
              assumptionsBuilder.build(), ImmutableSet.of(), state.hasPropertyViolation()));
    }

    return outStates.build();
  }
}
