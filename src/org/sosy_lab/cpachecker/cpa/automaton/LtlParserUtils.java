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
import com.google.common.collect.ImmutableList.Builder;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ltl.LtlParseException;

public class LtlParserUtils {

  private static final LogManager LOG_MANAGER = LogManager.createNullLogManager();
  private static final MachineModel LINUX64 = MachineModel.LINUX64;

  private static final CParser PARSER =
      CParser.Factory.getParser(LOG_MANAGER, CParser.Factory.getDefaultOptions(), LINUX64);

  /**
   * Takes a {@link jhoafparser.storage.StoredAutomaton} as argument and transforms that into an
   * {@link org.sosy_lab.cpachecker.cpa.automaton.Automaton}.
   *
   * @param pStoredAutomaton the storedAutomaton created by parsing an input in HOA-format.
   * @return an automaton from the automaton-framework in CPAchecker
   * @throws LtlParseException if the transformation fails due to some false values in {@code
   *     pStoredAutomaton}. This is most likely caused by a bug.
   */
  public static Automaton transform(StoredAutomaton pStoredAutomaton)
      throws LtlParseException, UnrecognizedCodeException {
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
          int successorStateId = Iterables.getOnlyElement(edge.getConjSuccessors()).intValue();
          String successorName = pStoredAutomaton.getStoredState(successorStateId).getInfo();

          transitionList.addAll(getTransitions(storedHeader, edge.getLabelExpr(), successorName));
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

  private static List<AutomatonTransition> getTransitions(
      StoredHeader pStoredHeader, BooleanExpression<AtomLabel> pLabelExpr, String pSuccessorName)
      throws InvalidAutomatonException, UnrecognizedCodeException {
    Builder<AutomatonTransition> transitions = ImmutableList.builder();

    switch (pLabelExpr.getType()) {
      case EXP_OR:
        transitions.addAll(getTransitions(pStoredHeader, pLabelExpr.getLeft(), pSuccessorName));
        transitions.addAll(getTransitions(pStoredHeader, pLabelExpr.getRight(), pSuccessorName));
        break;
      default:
        Builder<AExpression> expressions = ImmutableList.builder();
        expressions.addAll(getExpressions(pStoredHeader, pLabelExpr));
        transitions.add(createTransition(expressions.build(), pSuccessorName));
        break;
    }

    return transitions.build();
  }

  private static List<CExpression> getExpressions(
      StoredHeader pStoredHeader, BooleanExpression<AtomLabel> pLabelExpr)
      throws InvalidAutomatonException, UnrecognizedCodeException {
    Builder<CExpression> expBuilder = ImmutableList.builder();

    Type type = pLabelExpr.getType();
    switch (type) {
      case EXP_TRUE:
        return ImmutableList.of(assume("true"));
      case EXP_FALSE:
        return ImmutableList.of(assume("false"));
      case EXP_ATOM:
        int apIndex = pLabelExpr.getAtom().getAPIndex();
        return ImmutableList.of(assume(pStoredHeader.getAPs().get(apIndex)));
      case EXP_OR:
        expBuilder.addAll(getExpressions(pStoredHeader, pLabelExpr.getLeft()));
        expBuilder.addAll(getExpressions(pStoredHeader, pLabelExpr.getRight()));
        break;
      case EXP_AND:
        expBuilder.addAll(getExpressions(pStoredHeader, pLabelExpr.getLeft()));
        expBuilder.addAll(getExpressions(pStoredHeader, pLabelExpr.getRight()));
        break;
      case EXP_NOT:
        CBinaryExpressionBuilder b = new CBinaryExpressionBuilder(LINUX64, LOG_MANAGER);
        CExpression exp =
            Iterables.getOnlyElement(getExpressions(pStoredHeader, pLabelExpr.getLeft()));
        expBuilder.add(b.negateExpressionAndSimplify(exp));
        break;
      default:
        throw new RuntimeException("Unhandled expression type: " + type);
    }

    return expBuilder.build();
  }

  private static CExpression assume(String pExpression) throws InvalidAutomatonException {
    CAstNode sourceAST =
        CParserUtils.parseSingleStatement(pExpression, PARSER, CProgramScope.empty());
    return ((CExpressionStatement) sourceAST).getExpression();
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
