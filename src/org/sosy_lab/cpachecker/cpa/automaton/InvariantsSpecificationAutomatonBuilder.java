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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/** Builder to create an invariant specification automaton (ISA) to validate invariants */
public enum InvariantsSpecificationAutomatonBuilder {

  /**
   * Lets the automaton unchanged when calling {@link
   * InvariantsSpecificationAutomatonBuilder#build(Automaton, Configuration, LogManager, CFA)}.
   */
  NO_ISA {

    @Override
    public Automaton build(
        Automaton pAutomaton, Configuration pConfig, LogManager pLogger, CFA pCfa) {
      return pAutomaton;
    }
  },

  /**
   * Defines an invariant specification automaton that refers to the structure of the original
   * witness automaton and that is extended with invariant based error states. Calling {@link
   * InvariantsSpecificationAutomatonBuilder#build(Automaton, Configuration, LogManager, CFA)} only
   * changes the name of the {@code Automaton} because the {@code Automaton} already includes the
   * invariant based error states when {@code WITNESSBASED_INVARIANTSAUTOMATON} has been specified
   * in {@link AutomatonGraphmlParser}.
   */
  WITNESSBASED_ISA {

    private static final String WITNESS_AUTOMATON_NAME = "WitnessBasedISA";

    @Override
    public Automaton build(
        Automaton pAutomaton, Configuration pConfig, LogManager pLogger, CFA pCfa) {
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
        Automaton pAutomaton, Configuration pConfig, LogManager pLogger, CFA pCfa) {
      ShutdownManager shutdownManager = ShutdownManager.create();
      ShutdownNotifier shutdownNotifier = shutdownManager.getNotifier();
      try {
        WitnessInvariantsExtractor extractor =
            new WitnessInvariantsExtractor(pConfig, pAutomaton, pLogger, pCfa, shutdownNotifier);
        final Set<ExpressionTreeLocationInvariant> invariants = Sets.newLinkedHashSet();
        extractor.extractInvariantsFromReachedSet(invariants);
        return buildInvariantsAutomaton(pCfa, pLogger, invariants);
      } catch (InvalidConfigurationException | CPAException e) {
        throw new RuntimeException(
            "Changing the name of the automaton produces an incosistent automaton", e);
      }
    }

    @SuppressWarnings("unchecked")
    private Automaton buildInvariantsAutomaton(
        CFA pCfa, LogManager pLogger, Set<ExpressionTreeLocationInvariant> pInvariants) {
      try {
        String automatonName = WITNESS_AUTOMATON_NAME;
        String initialStateName = INITIAL_STATE_NAME;
        List<AutomatonInternalState> states = Lists.newLinkedList();
        List<AutomatonTransition> initTransitions = Lists.newLinkedList();
        for (ExpressionTreeLocationInvariant invariant : pInvariants) {
          ExpressionTree<?> inv = invariant.asExpressionTree();
          ExpressionTree<AExpression> invA = (ExpressionTree<AExpression>) inv;
          CExpression cExpr =
              invA.accept(new ToCExpressionVisitor(pCfa.getMachineModel(), pLogger));
          if (invA instanceof LeafExpression<?>) {
            // we must swap the c expression when assume truth is false
            if (!((LeafExpression<?>) invA).assumeTruth()) {
              cExpr =
                  new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger)
                      .negateExpressionAndSimplify(cExpr);
            }
          }
          CExpression negCExpr =
              new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger)
                  .negateExpressionAndSimplify(cExpr);
          List<AExpression> assumptionWithCExpr = Collections.singletonList(cExpr);
          List<AExpression> assumptionWithNegCExpr = Collections.singletonList(negCExpr);
          initTransitions.add(
              createTransitionWithCheckLocationAndAssumptionToError(
                  invariant.getLocation(), assumptionWithNegCExpr));
          initTransitions.add(
              createTransitionWithCheckLocationAndAssumptionToInit(
                  invariant.getLocation(), assumptionWithCExpr));
        }
        AutomatonInternalState initState =
            new AutomatonInternalState(initialStateName, initTransitions, false, true, false);
        states.add(initState);
        Map<String, AutomatonVariable> vars = ImmutableMap.of();
        return new Automaton(automatonName, vars, states, initialStateName);
      } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
        throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
      }
    }

    private AutomatonTransition createTransitionWithCheckLocationAndAssumptionToError(
        CFANode pLocation, final List<AExpression> pAssumptions) {
      return new AutomatonTransition.Builder(
              createQueryLocationString(pLocation), AutomatonInternalState.ERROR)
          .withAssumptions(pAssumptions)
          .withViolatedPropertyDescription(new StringExpression("Invariant not valid"))
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
        Automaton pAutomaton, Configuration pConfig, LogManager pLogger, CFA pCfa) {
      ShutdownManager shutdownManager = ShutdownManager.create();
      ShutdownNotifier shutdownNotifier = shutdownManager.getNotifier();
      try {
        WitnessInvariantsExtractor extractor =
            new WitnessInvariantsExtractor(pConfig, pAutomaton, pLogger, pCfa, shutdownNotifier);
        final Set<ExpressionTreeLocationInvariant> invariants = Sets.newLinkedHashSet();
        extractor.extractInvariantsFromReachedSet(invariants);
        return buildInvariantsAutomaton(pCfa, pLogger, invariants);
      } catch (InvalidConfigurationException | CPAException e) {
        throw new RuntimeException(
            "Changing the name of the automaton produces an incosistent automaton", e);
      }
    }

    @SuppressWarnings("unchecked")
    private Automaton buildInvariantsAutomaton(
        CFA pCfa, LogManager pLogger, Set<ExpressionTreeLocationInvariant> pInvariants) {

      try {
        String automatonName = WITNESS_AUTOMATON_NAME;
        String initialStateName = createStateName(pCfa.getMainFunction());
        List<AutomatonInternalState> states = Lists.newLinkedList();
        Set<CFANode> invariantCFANodes = extractCFANodes(pInvariants);
        for (CFANode node : pCfa.getAllNodes()) {
          if (node.getNumLeavingEdges() > 0) {
            List<AutomatonTransition> transitions = Lists.newLinkedList();
            for (int i = 0; i < node.getNumLeavingEdges(); i++) {
              CFAEdge leavingEdge = node.getLeavingEdge(i);
              CFANode successor = leavingEdge.getSuccessor();
              boolean successorIsBottom = false;
              if (successor.getNumLeavingEdges() == 0) {
                successorIsBottom = true;
              }
              if (invariantCFANodes.contains(successor)) {
                ExpressionTreeLocationInvariant invariant =
                    getInvariantByLocation(pInvariants, successor);
                ExpressionTree<?> inv = invariant.asExpressionTree();
                ExpressionTree<AExpression> invA = (ExpressionTree<AExpression>) inv;
                createLocationInvariantsTransitions(
                    pCfa, pLogger, transitions, successor, invA, successorIsBottom);
              } else {
                transitions.add(
                    createAutomatonTransition(successor, ImmutableList.of(), successorIsBottom));
              }
            }
            AutomatonInternalState state =
                new AutomatonInternalState(createStateName(node), transitions, false, true, false);
            states.add(state);
          }
        }
        return new Automaton(automatonName, ImmutableMap.of(), states, initialStateName);
      } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
        throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
      }
    }

    private void createLocationInvariantsTransitions(
        final CFA pCfa,
        final LogManager pLogger,
        final List<AutomatonTransition> pTransitions,
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
      List<AExpression> assumptionWithNegCExpr = Collections.singletonList(negCExpr);
      pTransitions.add(createAutomatonInvariantErrorTransition(pSuccessor, assumptionWithNegCExpr));
      List<AExpression> assumptionWithCExpr = Collections.singletonList(cExpr);
      pTransitions.add(
          createAutomatonTransition(pSuccessor, assumptionWithCExpr, pSuccessorIsBottom));
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
          .withViolatedPropertyDescription(new StringExpression("Invariant not valid"))
          .build();
    }

    private AutomatonBoolExpr createQueryLocationString(final CFANode pNode) {
      return new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + pNode.getNodeNumber());
    }

    private String createStateName(CFANode pNode) {
      return "S" + pNode.getNodeNumber();
    }

    private Set<CFANode> extractCFANodes(final Set<ExpressionTreeLocationInvariant> pInvariants) {
      return pInvariants
          .stream()
          .map(ExpressionTreeLocationInvariant::getLocation)
          .collect(Collectors.toSet());
    }

    private ExpressionTreeLocationInvariant getInvariantByLocation(
        final Set<ExpressionTreeLocationInvariant> pInvariants, final CFANode pLocation) {
      Set<ExpressionTreeLocationInvariant> filteredInvariants =
          pInvariants
              .stream()
              .filter(inv -> inv.getLocation().equals(pLocation))
              .collect(Collectors.toSet());
      return filteredInvariants.iterator().next();
    }
  };

  /**
   * Builds an invariants specification automaton. If method is called by {@link
   * InvariantsSpecificationAutomatonBuilder#NO_ISA}, {@code pAutomaton} is returned without any
   * changes.
   *
   * @param pAutomaton - the correctness witness automaton used
   * @param pConfig - the configuration
   * @param pLogger - the logger
   * @param pCfa - the cfa
   * @return the invariants specification automaton if specified
   */
  public abstract Automaton build(
      Automaton pAutomaton, Configuration pConfig, LogManager pLogger, CFA pCfa);
}
