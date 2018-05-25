/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.ast.BooleanExpression.Type;
import jhoafparser.storage.StoredAutomaton;
import jhoafparser.storage.StoredEdgeWithLabel;
import jhoafparser.storage.StoredHeader;
import jhoafparser.storage.StoredHeader.NameAndExtra;
import jhoafparser.storage.StoredState;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.ltl.LtlParseException;

public class LtlParserUtils {

  private static final CParser parser =
      CParser.Factory.getParser(
          LogManager.createNullLogManager(),
          CParser.Factory.getDefaultOptions(),
          MachineModel.LINUX64);

  /**
   * Takes a {@link jhoafparser.storage.StoredAutomaton} as argument and transforms that into an
   * {@link org.sosy_lab.cpachecker.cpa.automaton.Automaton}.
   *
   * @param pStoredAutomaton the storedAutomaton created by parsing an input in HOA-format.
   * @return an automaton from the automaton-framework in CPAchecker
   * @throws LtlParseException if the transformation failed due to some false values in {@code
   *     pStoredAutomaton}. This is most likely caused by a bug.
   */
  public static Automaton transform(StoredAutomaton pStoredAutomaton) throws LtlParseException {
    Objects.requireNonNull(pStoredAutomaton);

    StoredHeader storedHeader = pStoredAutomaton.getStoredHeader();

    List<NameAndExtra<List<Object>>> accNames = storedHeader.getAcceptanceNames();
    if (!(Iterables.getOnlyElement(accNames).name.equals("Buchi"))) {
      throw new LtlParseException(
          String.format(
              "Only 'Buchi'-acceptance is allowed, but instead the following was found: %s",
              accNames));
    }

    String accCond = storedHeader.getAcceptanceCondition().toStringInfix();
    if (!accCond.equals("Inf(0)")) {
      throw new LtlParseException(
          String.format(
              "The only allowed acceptance-condition is %s, but instead the following was found: %s",
              "Inf(0)", accCond));
    }

    ImmutableSet<String> requiredProperties =
        ImmutableSet.of("trans-labels", "explicit-labels", "state-acc", "no-univ-branch");
    if (!storedHeader.getProperties().stream().allMatch(x -> requiredProperties.contains(x))) {
      throw new LtlParseException(
          String.format(
              "The storedAutomaton-param may only contain %s as properties, but instead the following were found: %s",
              requiredProperties, storedHeader.getProperties()));
    }

    List<Integer> initStateList = Iterables.getOnlyElement(storedHeader.getStartStates());
    if (initStateList.size() != 1) {
      throw new LtlParseException("Univeral initial states are not supported");
    }

    Automaton result;

    try {

      List<AutomatonInternalState> stateList = new ArrayList<>();
      for (int i = 0; i < pStoredAutomaton.getNumberOfStates(); i++) {
        StoredState storedState = pStoredAutomaton.getStoredState(i);
        List<AutomatonTransition> transitionList = new ArrayList<>();

        for (StoredEdgeWithLabel edge : pStoredAutomaton.getEdgesWithLabel(i)) {
          String expression = getExpression(storedHeader, edge.getLabelExpr());

          int successorStateId = Iterables.getOnlyElement(edge.getConjSuccessors()).intValue();
          String successorName = pStoredAutomaton.getStoredState(successorStateId).getInfo();
          transitionList.add(createTransition(assume(expression), successorName));
        }

        stateList.add(
            new AutomatonInternalState(storedState.getInfo(), transitionList, false, true));
      }

      String automatonName = storedHeader.getName().replace("BA ", "-- Buechi Automaton ");
      StoredState initState =
          pStoredAutomaton.getStoredState(Iterables.getOnlyElement(initStateList).intValue());

      result = new Automaton(automatonName, ImmutableMap.of(), stateList, initState.getInfo());
    } catch (InvalidAutomatonException e) {
      throw new RuntimeException(
          "The passed storedAutomaton-parameter produces an inconsistent automaton", e);
    }

    return result;
  }

  private static String getExpression(
      StoredHeader pStoredHeader, BooleanExpression<AtomLabel> pLabelExpr) {
    StringBuilder result = new StringBuilder();

    BooleanExpression<AtomLabel> left;
    BooleanExpression<AtomLabel> right;
    boolean paren;

    Type type = pLabelExpr.getType();
    switch (type) {
      case EXP_TRUE:
        return "true";
      case EXP_FALSE:
        return "false";
      case EXP_ATOM:
        int apIndex = pLabelExpr.getAtom().getAPIndex();
        return pStoredHeader.getAPs().get(apIndex);
      case EXP_OR:
        left = pLabelExpr.getLeft();
        paren = left.needsParentheses(Type.EXP_OR);
        if (paren) {
          result.append("(");
        }
        result.append(getExpression(pStoredHeader, left));
        if (paren) {
          result.append(")");
        }
        result.append(" | "); // eventually replace this with " || "

        right = pLabelExpr.getRight();
        paren = right.needsParentheses(Type.EXP_OR);
        if (paren) {
          result.append("(");
        }
        result.append(getExpression(pStoredHeader, right));
        if (paren) {
          result.append(")");
        }
        return result.toString();
      case EXP_AND:
        left = pLabelExpr.getLeft();
        paren = left.needsParentheses(Type.EXP_AND);
        if (paren) {
          result.append("(");
        }
        result.append(getExpression(pStoredHeader, left));
        if (paren) {
          result.append(")");
        }
        result.append(" & "); // eventually replace this with " && "

        right = pLabelExpr.getRight();
        paren = right.needsParentheses(Type.EXP_AND);
        if (paren) {
          result.append("(");
        }
        result.append(getExpression(pStoredHeader, right));
        if (paren) {
          result.append(")");
        }
        return result.toString();
      case EXP_NOT:
        left = pLabelExpr.getLeft();
        paren = left.needsParentheses(Type.EXP_NOT);
        result.append("!");
        if (paren) {
          result.append("(");
        }
        result.append(getExpression(pStoredHeader, left));
        if (paren) {
          result.append(")");
        }
        return result.toString();
      default:
        throw new RuntimeException("Unhandled expression type: " + type);
    }
  }

  private static CExpression assume(String pExpression) throws InvalidAutomatonException {
    CAstNode sourceAST =
        CParserUtils.parseSingleStatement(pExpression, parser, CProgramScope.empty());
    return ((CExpressionStatement) sourceAST).getExpression();
  }

  private static AutomatonTransition createTransition(
      CExpression pAssumption, String pFollowStateName) {
    return createTransition(ImmutableList.of(pAssumption), pFollowStateName);
  }

  private static AutomatonTransition createTransition(
      List<AExpression> pAssumptions, String pFollowStateName) {
    return new AutomatonTransition(
        AutomatonBoolExpr.TRUE,
        ImmutableList.of(),
        pAssumptions,
        ImmutableList.of(),
        pFollowStateName);
  }
}
