// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/** Builder to create an invariant specification automaton (ISA) to validate invariants */
public enum InvariantsSpecificationAutomatonBuilder {

  /**
   * Lets the automaton unchanged when calling {@link
   * InvariantsSpecificationAutomatonBuilder#build(Automaton, Configuration, LogManager,
   * ShutdownNotifier, CFA)}.
   */
  NO_ISA {

    @Override
    public Automaton build(
        Automaton pAutomaton,
        Configuration pConfig,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier,
        CFA pCfa) {
      return pAutomaton;
    }
  },

  /**
   * Defines an invariant specification automaton that refers to the structure of the original
   * witness automaton and that is extended with invariant based error states. Calling {@link
   * InvariantsSpecificationAutomatonBuilder#build(Automaton, Configuration, LogManager,
   * ShutdownNotifier, CFA)} only changes the name of the {@code Automaton} because the {@code
   * Automaton} already includes the invariant based error states when {@code
   * WITNESSBASED_INVARIANTSAUTOMATON} has been specified in {@link AutomatonGraphmlParser}.
   */
  WITNESSBASED_ISA {

    private static final String WITNESS_AUTOMATON_NAME = "WitnessBasedISA";

    @Override
    public Automaton build(
        Automaton pAutomaton,
        Configuration pConfig,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier,
        CFA pCfa) {
      try {
        return new Automaton(
            WITNESS_AUTOMATON_NAME,
            pAutomaton.getInitialVariables(),
            pAutomaton.getStates(),
            pAutomaton.getInitialState().getName());
      } catch (InvalidAutomatonException e) {
        throw new RuntimeException(
            "Changing the name of the automaton produces an incosistent automaton", e);
      }
    }
  },

  /**
   * Builds a invariants specification automaton using the invariants from a correctness witness.
   * The resulting witness has two states: init and error. A transitions to the error state consists
   * of the invariants location and the assumption of the negated invariant
   */
  TWOSTATES_ISA {

    private static final String WITNESS_AUTOMATON_NAME = "TwoStatesISA";
    private static final String INITIAL_STATE_NAME = "Init";

    @Override
    public Automaton build(
        Automaton pAutomaton,
        Configuration pConfig,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier,
        CFA pCfa)
        throws InterruptedException {
      try {
        WitnessInvariantsExtractor extractor =
            new WitnessInvariantsExtractor(pConfig, pAutomaton, pLogger, pCfa, pShutdownNotifier);
        final Set<ExpressionTreeLocationInvariant> invariants =
            extractor.extractInvariantsFromReachedSet();
        return buildInvariantsAutomaton(pCfa, pLogger, invariants);
      } catch (InvalidConfigurationException | CPAException e) {
        throw new RuntimeException(
            "Changing the name of the automaton produces an incosistent automaton", e);
      }
    }

    private Automaton buildInvariantsAutomaton(
        CFA pCfa, LogManager pLogger, Set<ExpressionTreeLocationInvariant> pInvariants) {
      try {
        String automatonName = WITNESS_AUTOMATON_NAME;
        String initialStateName = INITIAL_STATE_NAME;
        ImmutableList.Builder<AutomatonInternalState> states = ImmutableList.builder();
        ImmutableList.Builder<AutomatonTransition> initTransitions = ImmutableList.builder();
        for (ExpressionTreeLocationInvariant invariant : pInvariants) {
          @SuppressWarnings("unchecked")
          ExpressionTree<AExpression> inv =
              (ExpressionTree<AExpression>) (ExpressionTree<?>) invariant.asExpressionTree();
          CExpression cExpr = inv.accept(new ToCExpressionVisitor(pCfa.getMachineModel(), pLogger));
          if (inv instanceof LeafExpression<?>) {
            // we must swap the c expression when assume truth is false
            if (!((LeafExpression<?>) inv).assumeTruth()) {
              cExpr =
                  new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger)
                      .negateExpressionAndSimplify(cExpr);
            }
          }
          CExpression negCExpr =
              new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger)
                  .negateExpressionAndSimplify(cExpr);
          initTransitions.add(
              createTransitionWithCheckLocationAndAssumptionToError(
                  invariant.getLocation(), ImmutableList.of(negCExpr)));
          initTransitions.add(
              createTransitionWithCheckLocationAndAssumptionToInit(
                  invariant.getLocation(), ImmutableList.of(cExpr)));
        }
        AutomatonInternalState initState =
            new AutomatonInternalState(
                initialStateName, initTransitions.build(), false, true, false);
        states.add(initState);
        Map<String, AutomatonVariable> vars = ImmutableMap.of();
        return new Automaton(automatonName, vars, states.build(), initialStateName);
      } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
        throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
      }
    }

    private AutomatonTransition createTransitionWithCheckLocationAndAssumptionToError(
        CFANode pLocation, final List<AExpression> pAssumptions) {
      return new AutomatonTransition.Builder(
              createQueryLocationString(pLocation), AutomatonInternalState.ERROR)
          .withAssumptions(pAssumptions)
          .withTargetInformation(new StringExpression("Invariant not valid"))
          .build();
    }

    private AutomatonTransition createTransitionWithCheckLocationAndAssumptionToInit(
        CFANode pLocation, final List<AExpression> pAssumptions) {
      return new AutomatonTransition.Builder(createQueryLocationString(pLocation), "Init")
          .withAssumptions(pAssumptions)
          .build();
    }

    private AutomatonBoolExpr createQueryLocationString(final CFANode pNode) {
      return new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + pNode.getNodeNumber());
    }
  },

  /**
   * Builds a CFA based specification invariant automaton out of the invariants from a correctness
   * witness. The structure of the automaton follows the structure of the CFA: States and
   * transitions of the automaton are built corresponding to the nodes and edges of the CFA. For
   * each entering CFANode that matches with the invariant location two transitions and successor
   * states are built: One transition with the invariant assumption that goes to the next CFA based
   * state and another transition with the negated invariant assumption that goes to the Error
   * state.
   */
  CFABASED_ISA {

    private static final String WITNESS_AUTOMATON_NAME = "CFABasedISA";

    @Override
    public Automaton build(
        Automaton pAutomaton,
        Configuration pConfig,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier,
        CFA pCfa)
        throws InterruptedException {
      try {
        WitnessInvariantsExtractor extractor =
            new WitnessInvariantsExtractor(pConfig, pAutomaton, pLogger, pCfa, pShutdownNotifier);
        final Set<ExpressionTreeLocationInvariant> invariants =
            extractor.extractInvariantsFromReachedSet();
        return buildInvariantsAutomaton(pCfa, pLogger, invariants);
      } catch (InvalidConfigurationException | CPAException e) {
        throw new RuntimeException(
            "Changing the name of the automaton produces an incosistent automaton", e);
      }
    }

    private Automaton buildInvariantsAutomaton(
        CFA pCfa, LogManager pLogger, Set<ExpressionTreeLocationInvariant> pInvariants) {

      try {
        String automatonName = WITNESS_AUTOMATON_NAME;
        String initialStateName = createStateName(pCfa.getMainFunction());
        ImmutableList.Builder<AutomatonInternalState> states = ImmutableList.builder();
        Set<CFANode> invariantCFANodes = extractCFANodes(pInvariants);
        for (CFANode node : pCfa.getAllNodes()) {
          if (node.getNumLeavingEdges() > 0) {
            ImmutableList.Builder<AutomatonTransition> transitions = ImmutableList.builder();
            for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
              CFANode successor = leavingEdge.getSuccessor();
              boolean successorIsBottom = false;
              if (successor.getNumLeavingEdges() == 0) {
                successorIsBottom = true;
              }
              if (invariantCFANodes.contains(successor)) {
                ExpressionTreeLocationInvariant invariant =
                    getInvariantByLocation(pInvariants, successor);
                @SuppressWarnings("unchecked")
                ExpressionTree<AExpression> inv =
                    (ExpressionTree<AExpression>) (ExpressionTree<?>) invariant.asExpressionTree();
                createLocationInvariantsTransitions(
                    pCfa, pLogger, transitions, successor, inv, successorIsBottom);
              } else {
                transitions.add(
                    createAutomatonTransition(successor, ImmutableList.of(), successorIsBottom));
              }
            }
            AutomatonInternalState state =
                new AutomatonInternalState(
                    createStateName(node), transitions.build(), false, true, false);
            states.add(state);
          }
        }
        return new Automaton(automatonName, ImmutableMap.of(), states.build(), initialStateName);
      } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
        throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
      }
    }

    private void createLocationInvariantsTransitions(
        final CFA pCfa,
        final LogManager pLogger,
        final ImmutableList.Builder<AutomatonTransition> pTransitions,
        final CFANode pSuccessor,
        final ExpressionTree<AExpression> pInvariant,
        final boolean pSuccessorIsBottom)
        throws UnrecognizedCodeException {
      CExpression cExpr =
          pInvariant.accept(new ToCExpressionVisitor(pCfa.getMachineModel(), pLogger));
      if (pInvariant instanceof LeafExpression<?>) {
        // we must swap the c expression when assume truth is false
        if (!((LeafExpression<?>) pInvariant).assumeTruth()) {
          cExpr =
              new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger)
                  .negateExpressionAndSimplify(cExpr);
        }
      }
      CExpression negCExpr =
          new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger)
              .negateExpressionAndSimplify(cExpr);
      pTransitions.add(
          createAutomatonInvariantErrorTransition(pSuccessor, ImmutableList.of(negCExpr)));
      pTransitions.add(
          createAutomatonTransition(pSuccessor, ImmutableList.of(cExpr), pSuccessorIsBottom));
    }

    private AutomatonTransition createAutomatonTransition(
        final CFANode pSuccessor,
        final List<AExpression> pAssumptions,
        final boolean pSuccessorIsBottom) {
      if (pSuccessorIsBottom) {
        return new AutomatonTransition.Builder(
                createQueryLocationString(pSuccessor), AutomatonInternalState.BOTTOM)
            .withAssumptions(pAssumptions)
            .build();
      } else {
        return new AutomatonTransition.Builder(
                createQueryLocationString(pSuccessor), createStateName(pSuccessor))
            .withAssumptions(pAssumptions)
            .build();
      }
    }

    private AutomatonTransition createAutomatonInvariantErrorTransition(
        final CFANode pSuccessor, final List<AExpression> pAssumptions) {
      return new AutomatonTransition.Builder(
              createQueryLocationString(pSuccessor), createStateName(pSuccessor))
          .withAssumptions(pAssumptions)
          .withTargetInformation(new StringExpression("Invariant not valid"))
          .build();
    }

    private AutomatonBoolExpr createQueryLocationString(final CFANode pNode) {
      return new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + pNode.getNodeNumber());
    }

    private String createStateName(CFANode pNode) {
      return "S" + pNode.getNodeNumber();
    }

    private Set<CFANode> extractCFANodes(final Set<ExpressionTreeLocationInvariant> pInvariants) {
      return Collections3.transformedImmutableSetCopy(
          pInvariants, ExpressionTreeLocationInvariant::getLocation);
    }

    private ExpressionTreeLocationInvariant getInvariantByLocation(
        final Set<ExpressionTreeLocationInvariant> pInvariants, final CFANode pLocation) {
      return pInvariants.stream()
          .filter(inv -> inv.getLocation().equals(pLocation))
          .findFirst()
          .orElseThrow();
    }
  };

  /**
   * Builds an invariants specification automaton. If method is called by {@link
   * InvariantsSpecificationAutomatonBuilder#NO_ISA}, {@code pAutomaton} is returned without any
   * changes.
   *
   * @param pAutomaton - the correctness witness automaton used
   * @return the invariants specification automaton if specified
   */
  public abstract Automaton build(
      Automaton pAutomaton,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InterruptedException;
}
