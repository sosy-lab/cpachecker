// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotations;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLPredicateToExpressionTreeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLTermToCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/**
 * Class to create an invariant specification automaton (ISA) from a CFA containing ACSL
 * annotations.
 */
public class AutomatonACSLParser {

  private CFAWithACSLAnnotations cfa;
  private LogManager logger;
  private final ACSLPredicateToExpressionTreeVisitor visitor;

  public AutomatonACSLParser(CFAWithACSLAnnotations pCFA, LogManager pLogger) {
    cfa = pCFA;
    logger = pLogger;
    ACSLTermToCExpressionVisitor termVisitor = new ACSLTermToCExpressionVisitor(cfa, logger);
    visitor = new ACSLPredicateToExpressionTreeVisitor(termVisitor);
  }

  /**
   * Builds an ISA from the stored CFA that checks whether the contained ACSL annotations are
   * fulfilled.
   */
  public Automaton parseAsAutomaton() {
    try {
      String automatonName = "ACSLInvariantsAutomaton";
      String initialStateName = "VALID";
      ImmutableList.Builder<AutomatonTransition> transitions = ImmutableList.builder();
      for (CFANode node : cfa.getAllNodes()) {
        if (node.getNumLeavingEdges() > 0) {
          for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
            Collection<ACSLAnnotation> annotations = cfa.getEdgesToAnnotations().get(leavingEdge);
            if (!annotations.isEmpty()) {
              ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
              List<ExpressionTree<Object>> representations = new ArrayList<>(annotations.size());
              for (ACSLAnnotation annotation : annotations) {
                ACSLPredicate predicate = annotation.getPredicateRepresentation();
                representations.add(predicate.accept(visitor));
              }
              @SuppressWarnings("unchecked")
              ExpressionTree<AExpression> inv =
                  (ExpressionTree<AExpression>) (ExpressionTree<?>) factory.and(representations);
              createLocationInvariantsTransitions(transitions, leavingEdge, inv);
            }
          }
        }
      }
      AutomatonInternalState state =
          new AutomatonInternalState(initialStateName, transitions.build(), false, true, false);
      return new Automaton(
          automatonName, ImmutableMap.of(), ImmutableList.of(state), initialStateName);
    } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
      throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
    }
  }

  private void createLocationInvariantsTransitions(
      final ImmutableList.Builder<AutomatonTransition> pTransitions,
      final CFAEdge pEdge,
      final ExpressionTree<AExpression> pInvariant)
      throws UnrecognizedCodeException {
    CExpression cExpr = pInvariant.accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
    if (!(pInvariant instanceof LeafExpression<?>)
        || ((LeafExpression<?>) pInvariant).assumeTruth()) {
      // we must swap the c expression only when assume truth is true
      // because we are only interested in the negated expression anyway
      cExpr =
          new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
              .negateExpressionAndSimplify(cExpr);
    }
    pTransitions.add(createAutomatonInvariantErrorTransition(pEdge, ImmutableList.of(cExpr)));
  }

  private AutomatonTransition createAutomatonInvariantErrorTransition(
      final CFAEdge pEdge, final List<AExpression> pAssumptions) {
    return new AutomatonTransition.Builder(
            new AutomatonBoolExpr.MatchCFAEdgeExact(pEdge.getRawStatement()),
            AutomatonInternalState.ERROR)
        .withAssumptions(pAssumptions)
        .withTargetInformation(new StringExpression("Invariant not valid"))
        .build();
  }

  public static boolean isACSLAnnotatedFile(Path file) throws InvalidConfigurationException {
    try {
      String fileContent = Files.readString(file);
      return fileContent.contains("/*@") || fileContent.contains("//@");
    } catch (IOException e) {
      throw new WitnessParseException(e);
    }
  }

  /**
   * Determines whether the given CFA is isomorphic to the one stored in this.cfa.
   *
   * <p>Relies on CFAs being created deterministically, i.e., node and edge orders are assumed to
   * stay the same.
   */
  public boolean areIsomorphicCFAs(CFA other) {
    if (cfa.getAllNodes().size() != other.getAllNodes().size()) {
      return false;
    }
    Iterator<CFANode> nodes = cfa.getAllNodes().iterator();
    Iterator<CFANode> other_nodes = other.getAllNodes().iterator();
    while (nodes.hasNext()) {
      CFANode node = nodes.next();
      CFANode other_node = other_nodes.next();
      if (node.getNumLeavingEdges() != other_node.getNumLeavingEdges()
          || node.getNumEnteringEdges() != other_node.getNumEnteringEdges()) {
        return false;
      }
      for (int i = 0; i < node.getNumEnteringEdges(); i++) {
        CFAEdge edge = node.getEnteringEdge(i);
        CFAEdge other_edge = other_node.getEnteringEdge(i);
        if (!edge.getRawStatement().equals(other_edge.getRawStatement())) {
          return false;
        }
      }
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        CFAEdge other_edge = other_node.getLeavingEdge(i);
        if (!edge.getRawStatement().equals(other_edge.getRawStatement())) {
          return false;
        }
      }
    }
    return true;
  }
}
