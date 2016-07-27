/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BINARY_AND;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BINARY_OR;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_EQUAL;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_THAN;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.MULTIPLY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.PLUS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.LONG_INT;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.termination.RankingRelation;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationUtils;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.LexicographicRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.LinearRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.NestedRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;

class RankingRelationBuilder {

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final FormulaManagerView formulaManagerView;

  private final IntegerFormulaManagerView integerFormulaManager;

  public RankingRelationBuilder(
      MachineModel pMachineModel, LogManager pLogger, FormulaManagerView pFormulaManagerView) {
    binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    integerFormulaManager = formulaManagerView.getIntegerFormulaManager();
  }

  public RankingRelation fromTerminationArgument(
      TerminationArgument pTerminationArgument, Set<CVariableDeclaration> pRelevantVariables)
      throws UnrecognizedCCodeException {
    return fromRankingFunction(pRelevantVariables, pTerminationArgument.getRankingFunction());
  }

  private RankingRelation fromRankingFunction(
      Set<CVariableDeclaration> pRelevantVariables, RankingFunction rankingFunction)
      throws UnrecognizedCCodeException {

    if (rankingFunction instanceof LinearRankingFunction) {
      AffineFunction function = ((LinearRankingFunction) rankingFunction).getComponent();
      return fromAffineFunction(pRelevantVariables, function);

    } else if (rankingFunction instanceof LexicographicRankingFunction) {
      return fromLexicographicRankingFunction(
          (LexicographicRankingFunction) rankingFunction, pRelevantVariables);

    } else if (rankingFunction instanceof NestedRankingFunction) {
      return fromNestedRankingFunction(
          (NestedRankingFunction) rankingFunction, pRelevantVariables);

    } else {
      throw new UnsupportedOperationException(rankingFunction.getName());
    }
  }

  private RankingRelation fromLexicographicRankingFunction(
      LexicographicRankingFunction rankingFunction, Set<CVariableDeclaration> pRelevantVariables)
      throws UnrecognizedCCodeException {

    CExpression cExpression = CIntegerLiteralExpression.ZERO;
    List<BooleanFormula> formulas = Lists.newArrayList();

    for (RankingFunction component : rankingFunction.getComponents()) {
      RankingRelation rankingRelation = fromRankingFunction(pRelevantVariables, component);
      CExpression cExpressionComponent = rankingRelation.asCExpression();
      cExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              cExpression, cExpressionComponent, BINARY_OR);
      formulas.add(rankingRelation.asFormula());
    }

    BooleanFormula formula = formulaManagerView.getBooleanFormulaManager().or(formulas);
    return new RankingRelation(cExpression, formula, binaryExpressionBuilder, formulaManagerView);
  }

  private RankingRelation fromAffineFunction(Set<CVariableDeclaration> pRelevantVariables,
      AffineFunction function) throws UnrecognizedCCodeException {
    RankingRelationComponents components =
        createRankingRelationComponents(function, pRelevantVariables);

    CBinaryExpression rankingRelation = createRankingRelationExpression(components);
    BooleanFormula rankingRelationFormula = createRankingRelationFormula(components);

    return new RankingRelation(
        rankingRelation, rankingRelationFormula, binaryExpressionBuilder, formulaManagerView);
  }

  private CBinaryExpression createRankingRelationExpression(
      RankingRelationComponents components) throws UnrecognizedCCodeException {
    CExpression unprimedFunction = components.getUnprimedExpression();
    CExpression primedFunction = components.getPrimedExpression();
    CExpression unprimedGreatorThanZero =
        binaryExpressionBuilder.buildBinaryExpression(primedFunction, ZERO, GREATER_EQUAL);
    CExpression primedLessThanUnprimed =
        binaryExpressionBuilder.buildBinaryExpression(unprimedFunction, primedFunction, LESS_THAN);

    CBinaryExpression rankingRelation =
        binaryExpressionBuilder.buildBinaryExpression(
            unprimedGreatorThanZero, primedLessThanUnprimed, BINARY_AND);
    return rankingRelation;
  }

  private RankingRelationComponents createRankingRelationComponents(AffineFunction function,
      Set<CVariableDeclaration> pRelevantVariables) throws UnrecognizedCCodeException {
    // x -> x'
    Map<CVariableDeclaration, CVariableDeclaration> allVariables =
        Maps.toMap(pRelevantVariables, TerminationUtils::createPrimedVariable);

    // f(x')
    CExpression primedFunction = createLiteral(function.getConstant());
    List<IntegerFormula> primedFormulaSummands = Lists.newArrayList();
    primedFormulaSummands.add(integerFormulaManager.makeNumber(function.getConstant()));

    // f(x)
    CExpression unprimedFunction = createLiteral(function.getConstant());
    List<IntegerFormula> unprimedFormulaSummands = Lists.newArrayList();
    unprimedFormulaSummands.add(integerFormulaManager.makeNumber(function.getConstant()));

    for (RankVar rankVar : function.getVariables()) {
      BigInteger coefficient = function.get(rankVar);
      CLiteralExpression cCoefficient = createLiteral(coefficient);
      String variableName = rankVar.getGloballyUniqueId();
      CVariableDeclaration variableDec =
          pRelevantVariables
              .stream()
              .filter(v -> v.getQualifiedName().equals(variableName))
              .findAny()
              .get();

      CVariableDeclaration primedVariableDec = allVariables.get(variableDec);
      primedFunction = addSummand(primedFunction, cCoefficient, primedVariableDec);
      unprimedFunction = addSummand(unprimedFunction, cCoefficient, variableDec);

      primedFormulaSummands.add(createSummand(coefficient, primedVariableDec.getQualifiedName()));
      unprimedFormulaSummands.add(createSummand(coefficient, variableName));
    }

   return new RankingRelationComponents(
            unprimedFunction, primedFunction, unprimedFormulaSummands, primedFormulaSummands);
  }

  private RankingRelation fromNestedRankingFunction(
      NestedRankingFunction pRankingFunction, Set<CVariableDeclaration> pRelevantVariables)
          throws UnrecognizedCCodeException {
    Preconditions.checkArgument(pRankingFunction.getComponents().length > 0);

    BooleanFormula phaseConditionFormula = formulaManagerView.getBooleanFormulaManager().makeTrue();
    CExpression phaseConditionExpression = CIntegerLiteralExpression.ONE;

    List<CExpression> componentExpressions = Lists.newArrayList();
    List<BooleanFormula> componentFormulas = Lists.newArrayList();

    for (AffineFunction component : pRankingFunction.getComponents()) {
      RankingRelation componentRelation = fromAffineFunction(pRelevantVariables, component);

      CBinaryExpression componentExpression = binaryExpressionBuilder.buildBinaryExpression(
          phaseConditionExpression, componentRelation.asCExpression(), BINARY_AND);
      componentExpressions.add(componentExpression);
      BooleanFormula componentFormula =
          formulaManagerView.makeAnd(phaseConditionFormula, componentRelation.asFormula());
      componentFormulas.add(componentFormula);

      // update phase condition
      RankingRelationComponents rankingRelationComponents =
          createRankingRelationComponents(component, pRelevantVariables);

      IntegerFormula zero = integerFormulaManager.makeNumber(0L);
      BooleanFormula unprimedLessThanZeroFormula =
          formulaManagerView.makeLessThan(
              rankingRelationComponents.getUnprimedFormula(), zero, true);
      phaseConditionFormula =
          formulaManagerView.makeAnd(phaseConditionFormula, unprimedLessThanZeroFormula);

      CExpression unprimedLessThanZeroExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              rankingRelationComponents.getUnprimedExpression(), ZERO, GREATER_EQUAL);
      phaseConditionExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              phaseConditionExpression, unprimedLessThanZeroExpression, BINARY_AND);
    }

    BooleanFormula formula = formulaManagerView.getBooleanFormulaManager().or(componentFormulas);
    CExpression expression =
        componentExpressions
            .stream()
            .reduce(
                (op1, op2) ->
                    binaryExpressionBuilder.buildBinaryExpressionUnchecked(op1, op2, BINARY_OR))
            .get();

    return new RankingRelation(expression, formula, binaryExpressionBuilder, formulaManagerView);
  }

  private CExpression addSummand(
      CExpression sum, CLiteralExpression coefficient, CVariableDeclaration variable)
      throws UnrecognizedCCodeException {
    CIdExpression unprimedVariable = new CIdExpression(DUMMY, variable);
    CBinaryExpression unprimedSummand =
        binaryExpressionBuilder.buildBinaryExpression(coefficient, unprimedVariable, MULTIPLY);
    return binaryExpressionBuilder.buildBinaryExpression(sum, unprimedSummand, PLUS);
  }

  private CLiteralExpression createLiteral(BigInteger value) {
    return CIntegerLiteralExpression.createDummyLiteral(value.longValueExact(), LONG_INT);
  }

  private BooleanFormula createRankingRelationFormula(RankingRelationComponents components) {

    IntegerFormula primedFormula = components.getPrimedFormula();
    IntegerFormula unprimedFormula = components.getUnprimedFormula();

    IntegerFormula zero = integerFormulaManager.makeNumber(0);
    BooleanFormula unprimedGreatorThanZeroFormula =
        formulaManagerView.makeGreaterOrEqual(primedFormula, zero, true);
    BooleanFormula primedLessThanUnprimedFormula =
        formulaManagerView.makeLessThan(unprimedFormula, primedFormula, true);

    BooleanFormula rankingRelationFormula =
        formulaManagerView.makeAnd(unprimedGreatorThanZeroFormula, primedLessThanUnprimedFormula);
    return rankingRelationFormula;
  }

  private IntegerFormula createSummand(BigInteger pCoefficient, String pVariable) {
    IntegerFormula variable = integerFormulaManager.makeVariable(pVariable);
    IntegerFormula coefficient = integerFormulaManager.makeNumber(pCoefficient);
    return integerFormulaManager.multiply(coefficient, variable);
  }

  private final class RankingRelationComponents {

    private final CExpression unprimedExpression;
    private final CExpression primedExpression;
    private final List<IntegerFormula> unprimedFormulaSummands;
    private final List<IntegerFormula> primedFormulaSummands;

    RankingRelationComponents(
        CExpression pUnprimedExpression,
        CExpression pPrimedExpression,
        List<IntegerFormula> pUnprimedFormulaSummands,
        List<IntegerFormula> pPrimedFormulaSummands) {
          unprimedExpression = pUnprimedExpression;
          primedExpression = pPrimedExpression;
          unprimedFormulaSummands = pUnprimedFormulaSummands;
          primedFormulaSummands = pPrimedFormulaSummands;
    }

    public CExpression getPrimedExpression() {
      return primedExpression;
    }

    public CExpression getUnprimedExpression() {
      return unprimedExpression;
    }

    public IntegerFormula getPrimedFormula() {
      return integerFormulaManager.sum(primedFormulaSummands);
    }

    public IntegerFormula getUnprimedFormula() {
      return integerFormulaManager.sum(unprimedFormulaSummands);
    }
  }
}
