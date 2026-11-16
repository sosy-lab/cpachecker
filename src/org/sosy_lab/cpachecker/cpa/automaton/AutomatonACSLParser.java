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
import com.google.common.collect.ImmutableSetMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.acsl.AcslInvariants;
import org.sosy_lab.cpachecker.cpa.acsl.AcslToExpressionTreeConverter;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
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

  private final CFA cfa;
  private final LogManager logger;
  private final AcslToExpressionTreeConverter converter;

  public AutomatonACSLParser(CFA pCFA, LogManager pLogger) {
    cfa = pCFA;
    logger = pLogger;
    converter = new AcslToExpressionTreeConverter(cfa.getMachineModel(), logger);
  }

  /**
   * Builds an ISA from the stored CFA that checks whether the contained ACSL annotations are
   * fulfilled.
   *
   * @param pTaskCfa the CFA of the task, which needs to be isomorphic to the stored CFA
   */
  public Automaton parseAsAutomaton(CFA pTaskCfa) {
    try {
      String automatonName = "ACSLInvariantsAutomaton";
      String initialStateName = "VALID";
      ImmutableList.Builder<AutomatonTransition> transitions = ImmutableList.builder();
      ImmutableSetMultimap<CFANode, AcslPredicate> invariants = AcslInvariants.of(cfa);
      // The CFAs are isomorphic, so their nodes correspond to each other in iteration order
      Map<CFANode, CFANode> taskNodes = new HashMap<>();
      Iterator<CFANode> taskNodeIterator = pTaskCfa.nodes().iterator();
      for (CFANode node : cfa.nodes()) {
        taskNodes.put(node, taskNodeIterator.next());
      }
      for (CFANode node : invariants.keySet()) {
        ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
        List<ExpressionTree<Object>> representations = new ArrayList<>();
        for (AcslPredicate predicate : invariants.get(node)) {
          if (converter.canConvert(predicate)) {
            representations.add(converter.convert(predicate));
          } else {
            logger.log(Level.WARNING, "Cannot check ACSL predicate", predicate, "at", node);
          }
        }
        @SuppressWarnings("unchecked")
        ExpressionTree<AExpression> inv =
            (ExpressionTree<AExpression>) (ExpressionTree<?>) factory.and(representations);
        // The invariant holds after each edge that enters its node
        CFANode taskNode = taskNodes.get(node);
        for (int i = 0; i < taskNode.getNumEnteringEdges(); i++) {
          createLocationInvariantsTransitions(transitions, taskNode.getEnteringEdge(i), inv);
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
            new AutomatonBoolExpr.MatchCFAEdgeNodes(pEdge), AutomatonInternalState.ERROR)
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
    if (cfa.nodes().size() != other.nodes().size()) {
      return false;
    }
    Iterator<CFANode> nodes = cfa.nodes().iterator();
    Iterator<CFANode> other_nodes = other.nodes().iterator();
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
