/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundStateFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Equal;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor.VariableNameExtractor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptAllVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptSpecifiedVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * This is a CPA for collecting simple syntactic invariants about integer variables.
 */
public class InvariantsCPA extends AbstractCPA {

  private static final CollectVarsVisitor<CompoundState> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Options(prefix="cpa.invariants")
  public static class InvariantsOptions {

    @Option(values={"JOIN", "SEP"}, toUppercase=true,
        description="which merge operator to use for InvariantCPA")
    private String merge = "JOIN";

    private int interestingPredicatesDepth = 6;

  }

  private final InvariantsOptions options;

  private final boolean useBitvectors;

  private final Configuration config;

  private final LogManager logManager;

  private final ReachedSetFactory reachedSetFactory;

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(InvariantsCPA.class).withOptions(InvariantsOptions.class);
  }

  public InvariantsCPA(Configuration config, LogManager logger, InvariantsOptions options,
      ReachedSetFactory pReachedSetFactory, CFA pCfa) throws InvalidConfigurationException {
    super(options.merge, "sep", InvariantsDomain.INSTANCE, InvariantsTransferRelation.INSTANCE);
    this.config = config;
    this.logManager = logger;
    this.reachedSetFactory = pReachedSetFactory;
    this.cfa = pCfa;
    this.useBitvectors = true;
    this.options = options;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    try {
      Configuration.Builder configurationBuilder = Configuration.builder().copyFrom(config);
      configurationBuilder.setOption("output.disable", "true");
      configurationBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPA");
      configurationBuilder.setOption("specification", "config/specification/default.spc");

      ConfigurableProgramAnalysis cpa = new CPABuilder(configurationBuilder.build(), logManager, reachedSetFactory).buildCPAs(cfa);
      ReachedSet reached = reachedSetFactory.create();
      reached.add(cpa.getInitialState(pNode), cpa.getInitialPrecision(pNode));
      new CPAAlgorithm(cpa, logManager, config).run(reached);
      Set<CFAEdge> relevantEdges = new HashSet<>();
      Set<InvariantsFormula<CompoundState>> interestingAssumptions = new HashSet<>();
      for (AbstractState state : FluentIterable.from(reached).filter(AbstractStates.IS_TARGET_STATE)) {
        CFANode location = AbstractStates.extractLocation(state);
        Queue<CFANode> nodes = new ArrayDeque<>();
        Queue<Integer> distances = new ArrayDeque<>();
        nodes.offer(location);
        distances.offer(0);
        while (!nodes.isEmpty()) {
          location = nodes.poll();
          int distance = distances.poll();
          for (int i = 0; i < location.getNumEnteringEdges(); ++i) {
            CFAEdge edge = location.getEnteringEdge(i);
            if (relevantEdges.add(edge)) {
              nodes.offer(edge.getPredecessor());
              if ((options.interestingPredicatesDepth < 0 || distance < options.interestingPredicatesDepth) && edge instanceof AssumeEdge) {
                InvariantsFormula<CompoundState> assumption = ((CAssumeEdge) edge).getExpression().accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(edge));
                if (assumption instanceof LogicalNot<?>) { // We don't care about negations here
                  assumption = ((LogicalNot<CompoundState>) assumption).getNegated();
                }
                interestingAssumptions.add(assumption);
                distances.offer(distance + 1);
              } else {
                distances.offer(distance);
              }
            }
          }
        }
      }
      // Collect all variables of relevant assume edges
      Set<String> relevantVariables = new HashSet<>();
      for (CFAEdge edge : relevantEdges) {
        ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(edge);
        if (edge instanceof CAssumeEdge) {
          InvariantsFormula<CompoundState> assumption = ((CAssumeEdge) edge).getExpression().accept(etfv);
          relevantVariables.addAll(assumption.accept(COLLECT_VARS_VISITOR));
        }
      }

      // Collect all variables related to variables found on relevant assume edges from other edges with a fix point iteration
      expand(relevantVariables, relevantEdges);
      Set<String> interestingVariables = new HashSet<>();
      for (InvariantsFormula<CompoundState> interestingAssumption : interestingAssumptions) {
        interestingVariables.addAll(interestingAssumption.accept(COLLECT_VARS_VISITOR));
        expand(interestingVariables, relevantEdges);
      }
      Iterator<InvariantsFormula<CompoundState>> interestingAssumptionIterator = interestingAssumptions.iterator();
      while (interestingAssumptionIterator.hasNext()) {
        InvariantsFormula<CompoundState> interestingAssumption = interestingAssumptionIterator.next();
        if (interestingAssumption instanceof Equal<?>) {
          String varName = null;
          Equal<CompoundState> equal = (Equal<CompoundState>) interestingAssumption;
          if (equal.getOperand1() instanceof Variable<?> && (equal.getOperand2() instanceof Constant<?> || equal.getOperand2() instanceof Variable<?>)) {
            varName = ((Variable<?>) equal.getOperand1()).getName();
          } else if (equal.getOperand2() instanceof Variable<?> && (equal.getOperand1() instanceof Constant<?> || equal.getOperand1() instanceof Constant<?>)) {
            varName = ((Variable<?>) equal.getOperand2()).getName();
          }
          if (interestingVariables.contains(varName)) {
            interestingAssumptionIterator.remove();
          }
        }
      }

      //VariableSelection<CompoundState> variableSelection = new AcceptAllVariableSelection<>();
      VariableSelection<CompoundState> variableSelection = new AcceptSpecifiedVariableSelection<>(relevantVariables);
      ImmutableSet.copyOf(interestingAssumptions);
      return new InvariantsState(this.useBitvectors,
          variableSelection,
          ImmutableSet.copyOf(relevantEdges),
          ImmutableSet.copyOf(interestingAssumptions),
          ImmutableSet.copyOf(interestingVariables));
    } catch (InvalidConfigurationException | CPAException | InterruptedException e) {
      this.logManager.logException(Level.SEVERE, e, "Unable to select specific variables. Defaulting to selecting all variables.");
    }
    return new InvariantsState(this.useBitvectors, new AcceptAllVariableSelection<CompoundState>());
  }

  private static void expand(Set<String> pRelevantVariables, Set<CFAEdge> pCfaEdges) {
    int size = 0;
    while (pRelevantVariables.size() > size) {
      size = pRelevantVariables.size();
      for (CFAEdge edge : pCfaEdges) {
        try {
          expand(pRelevantVariables, edge);
        } catch (UnrecognizedCCodeException e) {
          // If an exception occurred, we simply do not expand the set of variables but may continue
        }
      }
    }
  }

  private static void expand(Set<String> pRelevantVariables, CFAEdge pCfaEdge) throws UnrecognizedCCodeException {
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge:
      // Assume that all assume edge variables are already recorded
      break;
    case BlankEdge:
      break;
    case CallToReturnEdge:
      break;
    case DeclarationEdge:
      handleDeclaration((CDeclarationEdge) pCfaEdge, pRelevantVariables);
      break;
    case FunctionCallEdge:
      handleFunctionCall((CFunctionCallEdge) pCfaEdge, pRelevantVariables);
      break;
    case FunctionReturnEdge:
      handleFunctionReturn((CFunctionReturnEdge) pCfaEdge, pRelevantVariables);
      break;
    case MultiEdge:
      Iterator<CFAEdge> edgeIterator = ((MultiEdge) pCfaEdge).iterator();
      while (edgeIterator.hasNext()) {
        expand(pRelevantVariables, edgeIterator.next());
      }
      break;
    case ReturnStatementEdge:
      handleReturnStatement((CReturnStatementEdge) pCfaEdge, pRelevantVariables);
      break;
    case StatementEdge:
      handleStatementEdge((CStatementEdge) pCfaEdge, pRelevantVariables);
      break;
    default:
      break;
    }
  }

  private static void handleFunctionCall(final CFunctionCallEdge pEdge, Set<String> pRelevantVariables) throws UnrecognizedCCodeException {

    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CExpression> actualParams = pEdge.getArguments();

    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();

      InvariantsFormula<CompoundState> value = actualParam.accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(new VariableNameExtractor() {

        @Override
        public String extract(CExpression pCExpression) throws UnrecognizedCCodeException {
          return InvariantsTransferRelation.getVarName(pCExpression, pEdge, pEdge.getPredecessor().getFunctionName());
        }
      }));

      String formalParam = InvariantsTransferRelation.scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());
      if (pRelevantVariables.contains(formalParam)) {
        pRelevantVariables.addAll(value.accept(COLLECT_VARS_VISITOR));
      }
    }

    return;
  }

  private static void handleDeclaration(CDeclarationEdge pEdge, Set<String> pRelevantVariables) throws UnrecognizedCCodeException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = InvariantsTransferRelation.scope(varName, pEdge.getSuccessor().getFunctionName());
    }

    final InvariantsFormula<CompoundState> value;
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pEdge));
    } else {
      value = CompoundStateFormulaManager.INSTANCE.asConstant(CompoundState.top());
    }

    if (pRelevantVariables.contains(varName)) {
      pRelevantVariables.addAll(value.accept(COLLECT_VARS_VISITOR));
    }
  }

  private static void handleStatementEdge(CStatementEdge pCStatementEdge, Set<String> pRelevantVariables) throws UnrecognizedCCodeException {
    ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pCStatementEdge);
    CStatementEdge statementEdge = pCStatementEdge;
    CStatement statement = statementEdge.getStatement();
    if (statement instanceof CAssignment) {
      CAssignment assignment = (CAssignment) statement;
      handleAssignment(pCStatementEdge.getPredecessor().getFunctionName(), pCStatementEdge, assignment.getLeftHandSide(), assignment.getRightHandSide().accept(etfv), pRelevantVariables);
    }
  }

  private static void handleAssignment(String pFunctionName, CFAEdge pCfaEdge, CExpression leftHandSide, InvariantsFormula<CompoundState> pValue, Set<String> pRelevantVariables) throws UnrecognizedCCodeException {
    ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pCfaEdge);
    final String varName;
    if (leftHandSide instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscriptExpression = (CArraySubscriptExpression) leftHandSide;
      varName = InvariantsTransferRelation.getVarName(arraySubscriptExpression.getArrayExpression(), pCfaEdge, pFunctionName);
      InvariantsFormula<CompoundState> subscript = arraySubscriptExpression.getSubscriptExpression().accept(etfv);
      for (String relevantVar : pRelevantVariables) {
        if (relevantVar.equals(varName) || relevantVar.startsWith(varName + "[")) {
          pRelevantVariables.addAll(pValue.accept(COLLECT_VARS_VISITOR));
          pRelevantVariables.add(varName);
          pRelevantVariables.addAll(subscript.accept(COLLECT_VARS_VISITOR));
          break;
        }
      }
    } else {
      varName = InvariantsTransferRelation.getVarName(leftHandSide, pCfaEdge, pFunctionName);
      if (pRelevantVariables.contains(varName)) {
        pRelevantVariables.addAll(pValue.accept(COLLECT_VARS_VISITOR));
      }
    }
  }

  private static void handleReturnStatement(CReturnStatementEdge pCStatementEdge, Set<String> pRelevantVariables) throws UnrecognizedCCodeException {
    String calledFunctionName = pCStatementEdge.getPredecessor().getFunctionName();
    CExpression returnedExpression = pCStatementEdge.getExpression();
    // If the return edge has no statement, no return value is passed: "return;"
    if (returnedExpression == null) {
      return;
    }
    ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pCStatementEdge);
    InvariantsFormula<CompoundState> returnedInvExpression = returnedExpression.accept(etfv);
    String returnValueName = InvariantsTransferRelation.scope(InvariantsTransferRelation.RETURN_VARIABLE_BASE_NAME, calledFunctionName);
    if (pRelevantVariables.contains(returnValueName)) {
      pRelevantVariables.addAll(returnedInvExpression.accept(COLLECT_VARS_VISITOR));
    }
  }

  private static void handleFunctionReturn(CFunctionReturnEdge pFunctionReturnEdge, Set<String> pRelevantVariables)
      throws UnrecognizedCCodeException {
      CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();

      CFunctionCall expression = summaryEdge.getExpression();

      String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

      String returnValueName = InvariantsTransferRelation.scope(InvariantsTransferRelation.RETURN_VARIABLE_BASE_NAME, calledFunctionName);

      InvariantsFormula<CompoundState> value = CompoundStateFormulaManager.INSTANCE.asVariable(returnValueName);

      // expression is an assignment operation, e.g. a = g(b);
      if (expression instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)expression;

        handleAssignment(pFunctionReturnEdge.getSuccessor().getFunctionName(), pFunctionReturnEdge, funcExp.getLeftHandSide(), value, pRelevantVariables);
      }
  }
}