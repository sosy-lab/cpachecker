/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.hybrid;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.CParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueProvider;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.util.ExpressionUtils;
import org.sosy_lab.cpachecker.cpa.hybrid.util.StrengthenOperatorFactory;
import org.sosy_lab.cpachecker.cpa.hybrid.visitor.HybridValueDeclarationTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.visitor.HybridValueIdExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix="cpa.hybrid")
public class HybridAnalysisTransferRelation
    extends ForwardingTransferRelation<HybridAnalysisState, HybridAnalysisState, VariableTrackingPrecision> {

  @Option(secure = true, name = "trackAssumptions", description = "Determines whether to track assumptions occurring within the code in the hybrid analysis states.")
  private boolean trackAssumptions = false;

  @Option(secure = true, name = "trackAssignments", description = "Determines whether to track assignments occurring within the code in the hybrid analysis states.")
  private boolean trackAssignments = false;

  private final CFA cfa;
  private final LogManager logger;

  // the value provider is not final, because the strategy might change over time
  private HybridValueProvider valueProvider;
  private final HybridValueDeclarationTransformer valueDeclarationTransformer;
  private final HybridValueIdExpressionTransformer valueIdExpressionTransformer;


  public HybridAnalysisTransferRelation(
      CFA pCfa,
      LogManager pLogger,
      HybridValueProvider pValueProvider,
      HybridValueDeclarationTransformer pHybridValueDeclarationTransformer,
      HybridValueIdExpressionTransformer pHybridValueIdExpressionTransformer)
  {
    this.cfa = pCfa;
    this.logger = pLogger;
    this.valueProvider = pValueProvider;
    this.valueDeclarationTransformer = pHybridValueDeclarationTransformer;
    this.valueIdExpressionTransformer = pHybridValueIdExpressionTransformer;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    // make sure the state to strengthen is of the correct domain
    assert pState instanceof HybridAnalysisState;

    // the correct operator will be generated by the factory
    HybridStrengthenOperator operator;
    HybridAnalysisState stateToStrengthen = (HybridAnalysisState) pState;

    for(AbstractState otherState : otherStates) {
      operator = StrengthenOperatorFactory.provideStrenghtenOperator(otherState);
      stateToStrengthen = operator.strengthen(stateToStrengthen, otherState, cfaEdge);
      super.setInfo(stateToStrengthen, pPrecision, cfaEdge);
    }

    super.resetInfo();
    return Collections.singleton(stateToStrengthen);
  }


  @Override
  protected @Nullable HybridAnalysisState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // HybridAnalysis can only handle binary Assumptions
    assert expression instanceof CBinaryExpression;

    // if the edge does not introduce a new assumption or assumptions should not be tracked at all
    if(!trackAssumptions || state.getAssumptions().contains(expression))
    {
      return HybridAnalysisState.copyOf(state);
    }

    Set<CBinaryExpression> assumptions = Sets.newHashSet(state.getExplicitAssumptions());
    CBinaryExpression binaryExpression = (CBinaryExpression) expression;
    CIdExpression variableId = (CIdExpression) binaryExpression.getOperand1();

    // existing variable (within state)
    if(state.tracksVariable(variableId)) {
      // check for existence of the first operand -> assumptionEdge updates the assumption of an already existing variable
      final Collection<CBinaryExpression> matchingAssumptions =
          CollectionUtils.getApplyingElements(assumptions, assumption -> assumption.getOperand1().equals(variableId));

      // at this point exactly one assumption for a variable should existredshift
      if(matchingAssumptions.size() != 1) {
        throw new CPATransferException("Multiple assumptions for the same variable in this state.");
      }

      @Nullable CBinaryExpression existingAssumption = CollectionUtils.first(matchingAssumptions);

      if(existingAssumption != null) {
        // replace the assumption
        assumptions.remove(existingAssumption);
      }
    }

    // possible inversion of logical operation
    assumptions.add(ExpressionUtils.getASTWithTruthAssumption(cfaEdge, binaryExpression));

    return new HybridAnalysisState(ImmutableSet.copyOf(assumptions));
  }

  @Override
  protected @Nullable HybridAnalysisState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    
    Value value = valueProvider.delegateVisit(decl.getType());
    CExpression newAssumption = valueDeclarationTransformer.transform(value, decl, BinaryOperator.EQUALS);

    return HybridAnalysisState.copyWithNewAssumptions(state, newAssumption);
  }

  @Override
  protected HybridAnalysisState handleStatementEdge(CStatementEdge pCStatementEdge, CStatement pCStatement)
    throws  CPATransferException {

    if(pCStatement instanceof CExpressionStatement || pCStatement instanceof CFunctionCallStatement) {
      return HybridAnalysisState.copyOf(state);
    }

    if(pCStatement instanceof CExpressionAssignmentStatement) {

      if(!trackAssignments) {
        return HybridAnalysisState.copyOf(state);
      }
      // handle assignment
      Collection<CStatement> singletonList = Collections.singleton(pCStatement);
      try {
        Collection<CExpression> expressions =
            CollectionUtils.ofType(
                CParserUtils
                    .convertStatementsToAssumptions(singletonList, cfa.getMachineModel(), logger),
                CExpression.class);

        // build new state
        CExpression assignment = CollectionUtils.first(expressions); // first and only

        // save to call with null, because of null check on usage of the edge
        return handleAssumption(null, assignment, true);

      } catch (InvalidAutomatonException e) {
        throw new CPATransferException("Unable to parse CStatement into assumption", e);
      }
    }

    if(pCStatement instanceof CFunctionCallAssignmentStatement) {

      CFunctionCallAssignmentStatement statement = (CFunctionCallAssignmentStatement) pCStatement;
      CExpression functionNameExpression = statement.getFunctionCallExpression().getFunctionNameExpression();

      boolean isNondetFunctionCall = false;

      if(functionNameExpression instanceof CIdExpression) {
        String name = ((CIdExpression) functionNameExpression).getName();
        isNondetFunctionCall = name.startsWith("__VERIFIER_nondet");
      }

      // handle nondet value
      if(isNondetFunctionCall) {

        Value value = valueProvider.delegateVisit(statement.getLeftHandSide().getExpressionType());
        CIdExpression leftHandSide = (CIdExpression) statement.getLeftHandSide(); // TODO: check if it is assignable from
        CExpression newAssumption = valueIdExpressionTransformer.transform(value, leftHandSide, BinaryOperator.EQUALS);

        return HybridAnalysisState.copyWithNewAssumptions(state, newAssumption);
      }
    }


    return HybridAnalysisState.copyOf(state);
  }

}