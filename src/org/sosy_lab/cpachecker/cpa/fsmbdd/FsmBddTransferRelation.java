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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeSet;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.VariableDeclarationException;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * The transfer-relation of the FsmBdd CPA.
 */
@Options(prefix="cpa.fsmbdd")
public class FsmBddTransferRelation implements TransferRelation {

  @Option(description="Enable condition-block encoding?")
  private boolean conditionBlocking = true;

  @Option(description="Assume that state-conditions contain no disjunctions?")
  private boolean assumeConditionsWithoutDisjunctions = true;

  /**
   * Name of the variable that is used to encode the result of a function.
   * (gets scoped with the name of the function)
   *
   * Since "return" is a keyword, we should not get a
   * conflict with a program variable with the same name.
   */
  private static final String RESULT_VARIABLE_NAME = "return";

  /**
   * String that is used to separate the different
   * levels of a scoped variable name.
   */
  private static final String SCOPE_SEPARATOR = ".";

  /**
   * Reference to the DomainIntervalProvider.
   */
  private DomainIntervalProvider domainIntervalProvider;

  /**
   * Set of the global program variables.
   */
  private final Set<String> globalVariables;

  /**
   * Map of variables that are declared within one function.
   */
  private Map<String, Set<String>> variablesPerFunction = new HashMap<String, Set<String>>();

  /**
   * Cache to avoid rebuilding the BDD that represents
   * one control-flow edge.
   */
  private final Map<CExpression, BDD> expressionBddCache;

  private final FsmBddStatistics statistics;

  private final BDDFactory bddFactory;

  private final Random random = new Random();

  /**
   * Constructor.
   * @throws InvalidConfigurationException
   */
  public FsmBddTransferRelation(Configuration pConfig, FsmBddStatistics pStatistics, BDDFactory pBddfactory) throws InvalidConfigurationException {
    this.expressionBddCache = new HashMap<CExpression, BDD>();
    this.globalVariables = new HashSet<String>();
    this.variablesPerFunction = new HashMap<String, Set<String>>();
    this.statistics = pStatistics;
    this.bddFactory = pBddfactory;

    FsmBddState.statistic = statistics;

    pConfig.inject(this);
  }

  /**
   * Setter.
   */
  public void setDomainIntervalProvider(DomainIntervalProvider pDomainIntervalProvider) {
    this.domainIntervalProvider = pDomainIntervalProvider;
  }

  public boolean isAbstractionState(FsmBddState pSuccessor, CFANode pSuccLocation) {
    // Depending on the subsequent edges.

//    return true;
    for (CFAEdge e: CFAUtils.enteringEdges(pSuccLocation)) {
//      if (e.getEdgeType() != CFAEdgeType.BlankEdge) {
        if (e.getEdgeType() != CFAEdgeType.AssumeEdge) {
          return true;
        }
//      }
    }

    for (CFAEdge e: CFAUtils.leavingEdges(pSuccLocation)) {
//      if (e.getEdgeType() != CFAEdgeType.BlankEdge) {

        if (e.getEdgeType() != CFAEdgeType.AssumeEdge) {
          return true;
        }
//      }
    }

    return false;
  }

  /**
   * Computation of the abstract successor states.
   *
   * Apply another operation to compute the successor state
   * depending on the type of the CFA-edge.
   */
  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    final FsmBddState predecessor = (FsmBddState) pState;
    final FsmBddState successor = predecessor.cloneState(pCfaEdge.getSuccessor());

//    System.out.println("================================================================================");
//    System.out.println(String.format("%15s :  %s ", "Predecessor", predecessor));
//    System.out.println(String.format("%15s : (l %d) %s (l %d)", pCfaEdge.getEdgeType(), pCfaEdge.getPredecessor().getNodeNumber(), pCfaEdge.getRawStatement(), pCfaEdge.getSuccessor().getNodeNumber()));

    try {

      switch (pCfaEdge.getEdgeType()) {
        case AssumeEdge:
          handleAssumeEdge(predecessor, (CAssumeEdge) pCfaEdge, successor);
          break;

        case StatementEdge:
          handleStatementEdge(predecessor, (CStatementEdge) pCfaEdge, successor);
          break;

        case DeclarationEdge:
          handleDeclarationEdge(predecessor, (CDeclarationEdge) pCfaEdge, successor);
          break;

        case ReturnStatementEdge:
          handleReturnStatementEdge(predecessor, (CReturnStatementEdge) pCfaEdge, successor);
          break;

        case FunctionReturnEdge:
          handleFunctionReturnEdge(predecessor, (CFunctionReturnEdge) pCfaEdge, successor);
          break;

        case FunctionCallEdge:
          handleFunctionCallEdge(predecessor, (CFunctionCallEdge) pCfaEdge, successor);
          break;

        case MultiEdge:
          handleMultiEdge(predecessor, (MultiEdge) pCfaEdge, successor);
          break;

        case BlankEdge:
          break;

        default:
          assert(false);
      }
    } catch (VariableDeclarationException e) {
      throw new CPATransferException(e.getMessage());
    }

    if (conditionBlocking) {
      if (isAbstractionState(successor, pCfaEdge.getSuccessor())) {
        computeAbstraction(successor, pCfaEdge);
      }
    }

//    System.out.println(String.format("%15s : %s", "Successor", successor));
    statistics.signalNumOfEncodedAssumptions(successor.getEncodedAssumptions());

    // Return an empty set if the BDD evaluates to "false".
    if (successor.getStateBdd().isZero()) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  /**
   * Conjunctive normal form.
   * @return
   */
  private List<CExpression> breakIntoClauses(final CExpression pInput) {
    List<CExpression> result = new LinkedList<CExpression>();
    Stack<CExpression> toProcess = new Stack<CExpression>();

    toProcess.push(pInput);
    while (!toProcess.isEmpty()) {
      CExpression expr = toProcess.pop();
      if (expr instanceof CBinaryExpression) {
        CBinaryExpression bin = (CBinaryExpression) expr;
        switch (bin.getOperator()) {
        case LOGICAL_AND:
          toProcess.push(bin.getOperand1());
          toProcess.push(bin.getOperand2());
          continue;
        case LOGICAL_OR:
          throw new IllegalStateException("Condition contains disjunctions; this is not allowed!");
        }
      }

      result.add(expr);
    }

    return result;
  }

  private void computeAbstraction(FsmBddState pSuccessor, CFAEdge pEdge) throws CPATransferException {
   SortedMap<Integer, CExpression> conditionBlock = pSuccessor.getConditionBlock();
   if (conditionBlock == null) {
     return;
   }

   statistics.signalNumOfEncodedAssumptions(pSuccessor.getEncodedAssumptions());
   statistics.blockAbstractionAllTimer.start();

   BDD blockBdd = bddFactory.one();

   statistics.blockAbstractionBeginOnFirstEncodeTimer.start();
   TreeSet<Integer> conditionKeys = new TreeSet<Integer>(conditionBlock.keySet());
   //System.out.println(pSuccessor.toString());
   Iterator<Integer> it = conditionKeys.descendingIterator();
   while (it.hasNext()) {
     Integer key = it.next();
     CExpression keyCondition = conditionBlock.get(key);
     BDD exprBdd = getAssumptionAsBdd(pSuccessor, pEdge.getSuccessor().getFunctionName(), keyCondition, true);
     blockBdd = blockBdd.and(exprBdd);
   }

   statistics.blockAbstractionBeginOnFirstEncodeTimer.stop();


   statistics.blockAbstractionConjunctTimer.start();
   pSuccessor.conjunctStateWith(blockBdd);
   statistics.blockAbstractionConjunctTimer.stop();
   pSuccessor.resetConditionBlock();
   statistics.blockAbstractionAllTimer.stop();
  }

  private void handleMultiEdge(FsmBddState pPredecessor, MultiEdge pMultiEdge, final FsmBddState pSuccessor) throws CPATransferException {
    for (CFAEdge edge: pMultiEdge.getEdges()) {
      switch (edge.getEdgeType()) {
      case BlankEdge:
        break;
      case DeclarationEdge:
        handleDeclarationEdge(pPredecessor, (CDeclarationEdge) edge, pSuccessor);
        break;
      case StatementEdge:
        handleStatementEdge(pPredecessor, (CStatementEdge) edge, pSuccessor);
        break;
      case ReturnStatementEdge:
        handleReturnStatementEdge(pPredecessor, (CReturnStatementEdge) edge, pSuccessor);
        break;
      default:
        throw new CPATransferException("Unsupported edge within multi-edge: " + edge.getEdgeType().toString());
      }
    }
  }

  /**
   * Handle the return statement of a function.
   *
   * Example with a literal:
   *    return 17;
   *
   * Example with a ID-expression:
   *    return b;
   *
   * Example without any expression on the return edge.
   *    return;
   *
   */
  private void handleReturnStatementEdge(FsmBddState pPredecessor, CReturnStatementEdge pReturnEdge, final FsmBddState pSuccessor) throws CPATransferException {
    // Get the expression from the return edge.
    CExpression rightOfReturn = pReturnEdge.getExpression();

    // No expression on the return edge?
    if (rightOfReturn == null) {
      rightOfReturn = CNumericTypes.ZERO;
    }

    // The return-value of the function gets stored in the
    // variable RESULT_VARIABLE_NAME of the called function.
    String functionName = pReturnEdge.getPredecessor().getFunctionName();
    String scopedResultVariableName = getScopedVariableName(functionName, RESULT_VARIABLE_NAME);

    if (rightOfReturn instanceof CIdExpression) {
      // ID-expression on the return edge.
      CIdExpression idExpr = (CIdExpression) rightOfReturn;
      String scopedValueVariableName = getScopedVariableName(functionName, idExpr.getName());
      pSuccessor.assignVariableToVariable(scopedValueVariableName, scopedResultVariableName);
    } else {
      // Literal on the return edge.
      pSuccessor.assingConstantToVariable(scopedResultVariableName, domainIntervalProvider, rightOfReturn);
    }
  }

  /**
   * Handle function calls including passing of arguments.
   *
   * Example:
   *
   *  int foo(int arg1) {
   *    return arg1;
   *  }
   *
   *  int main() {
   *   int i = 10;
   *   int r;
   *   r = foo(i);
   *   return 0;
   *  }
   *
   */
  private void handleFunctionCallEdge(FsmBddState pPredecessor, CFunctionCallEdge pCfaEdge, final FsmBddState pSuccessor) throws CPATransferException {
    CFunctionEntryNode functionEntryNode = pCfaEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = pCfaEdge.getPredecessor().getFunctionName();

    // Parameters and arguments
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = pCfaEdge.getArguments();

    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert(paramNames.size() == arguments.size());
    }

    for (int i = 0; i < paramNames.size(); i++) {
      CExpression argument = arguments.get(i);
      String scopedParameterName = getScopedVariableName(calledFunctionName, paramNames.get(i));

      // Declare the parameter as an local variable within the (called) function.
      pSuccessor.declareGlobal(scopedParameterName, domainIntervalProvider.getIntervalMaximum());

      // Depends on the type of argument.
      if (argument instanceof CIdExpression) {
        // foo(a);
        CIdExpression idExpr = (CIdExpression) argument;
        String scopedArgumentName = getScopedVariableName(callerFunctionName, idExpr.getName());
        pSuccessor.assignVariableToVariable(scopedArgumentName, scopedParameterName);
      } else if (argument instanceof CLiteralExpression) {
        // foo(17);
        CLiteralExpression litExpr = (CLiteralExpression) argument;
        pSuccessor.assingConstantToVariable(scopedParameterName, domainIntervalProvider, litExpr);
      } else {
        throw new UnrecognizedCCodeException("Unsupported function argument.", pCfaEdge);
      }
    }
  }

  /**
   * Handle a function-return edge.
   *
   * Example:
   *  We return from the function "bar()" that was called by a function foo():
   *
   *  int bar() {
   *    return 3;
   *  }
   *
   *  void foo() {
   *    int v = bar();
   *  }
   *
   *  First, the current value of "foo.v" gets existential quantified;
   *  after that, the equality between "foo.v" and "bar.result"
   *  (the variable "result" of bar encodes the return value of the function)
   *  gets established and is conjuncted with the BDD of the successor state.
   * @throws CPATransferException
   *
   */
  private void handleFunctionReturnEdge(FsmBddState pPredecessor, CFunctionReturnEdge pFunctionReturnEdge, FsmBddState pSuccessor) throws CPATransferException {
    CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();
    CFunctionCall exprOnSummary = summaryEdge.getExpression();

    String callerFunctionName = pFunctionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp = (CFunctionCallAssignmentStatement) exprOnSummary;
      CExpression callerLeft = assignExp.getLeftHandSide();

      if (callerLeft instanceof CIdExpression) {
        CIdExpression idExpr = (CIdExpression) callerLeft;
        String scopedReturnVarName = getScopedVariableName(calledFunctionName, RESULT_VARIABLE_NAME);
        String scopedTargetVariableName = getScopedVariableName(callerFunctionName, idExpr.getName());

        pSuccessor.assignVariableToVariable(scopedReturnVarName, scopedTargetVariableName);
      } else {
        throw new UnrecognizedCCodeException("On return from function.", summaryEdge, callerLeft);
      }
    } else if (exprOnSummary instanceof CFunctionCallStatement) {
      // Return value not handled by caller.
    } else {
      throw new CPATransferException(String.format("Unsupported assignement (line %d) of function return value: %s", summaryEdge.getLineNumber(), exprOnSummary.getClass().getSimpleName()));
    }

    // Existential quantify all local variables of the function.
    for (String localVariable: variablesPerFunction.get(calledFunctionName)) {
      pSuccessor.undefineVariable(getScopedVariableName(calledFunctionName, localVariable));
    }
  }

  /**
   * Handle a assume edge.
   * E.g. (a == b)
   *
   * The BDD of the successor state gets constructed
   * by doing a conjunction of the assumption with the the BDD of the predecessor state.
   */
  private void handleAssumeEdge (FsmBddState pPredecessor, CAssumeEdge pAssumeEdge, FsmBddState pSuccessor) throws CPATransferException {
    if (conditionBlocking) {
      CExpression assumeExpr = pAssumeEdge.getExpression();
      if (!pAssumeEdge.getTruthAssumption()) {
        assumeExpr = new CUnaryExpression(assumeExpr.getFileLocation(), null, pAssumeEdge.getExpression(), UnaryOperator.NOT);
      }
      pSuccessor.conjunctToConditionBlock(assumeExpr);
    } else {
      BDD assumptionBdd = expressionBddCache.get(pAssumeEdge.getExpression());
      if (assumptionBdd == null) {
        assumptionBdd = getAssumptionAsBdd(pSuccessor, pAssumeEdge.getPredecessor().getFunctionName(), pAssumeEdge.getExpression(), false);
        expressionBddCache.put(pAssumeEdge.getExpression(), assumptionBdd);
      }

      if (!pAssumeEdge.getTruthAssumption()) {
        assumptionBdd = assumptionBdd.not();
      }

      pSuccessor.conjunctStateWith(assumptionBdd);
    }
  }

  /**
   * Handle a statement edges.
   * Currently, only statements of the form "IDExpression = LiteralExpression;" are supported.
   */
  private void handleStatementEdge (FsmBddState pPredecessor, CStatementEdge pStatementEdge, FsmBddState pSuccessor) throws CPATransferException {
    CStatement stmt = pStatementEdge.getStatement();
    String functionName = pStatementEdge.getPredecessor().getFunctionName();

    if (stmt instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement assign = (CExpressionAssignmentStatement) stmt;

      if (assign.getLeftHandSide() instanceof CIdExpression
      && assign.getRightHandSide() instanceof CLiteralExpression) {
        CIdExpression left = (CIdExpression) assign.getLeftHandSide();
        CLiteralExpression right = (CLiteralExpression) assign.getRightHandSide();

        String scopedTargetVariableName = getScopedVariableName(functionName, left.getName());
        pSuccessor.assingConstantToVariable(scopedTargetVariableName, domainIntervalProvider, right);
      } else {
        throw new UnrecognizedCCodeException("Unsupported CExpressionAssignmentStatement", pStatementEdge);
      }
    } else if (stmt instanceof CFunctionCallAssignmentStatement) {
      // CFunctionCallAssignmentStatement: This is a call to a function whose body is undefined.
      CFunctionCallAssignmentStatement callAssign = (CFunctionCallAssignmentStatement) stmt;
      if (callAssign.getLeftHandSide() instanceof CIdExpression) {
        CIdExpression left = (CIdExpression) callAssign.getLeftHandSide();
        pSuccessor.undefineVariable(getScopedVariableName(functionName, left.getName()));
      } else {
        throw new UnrecognizedCCodeException("Unsupported CFunctionCallAssignmentStatement", pStatementEdge);
      }
    } else if (stmt instanceof CFunctionCallStatement) {
      // We can ignore this case because it is a call to a function whose body is undefined.
    } else {
      throw new UnrecognizedCCodeException("Unsupported statement edge", pStatementEdge);
    }
  }

  /**
   * Handle a declaration edge.
   * For function declarations, the result variable gets introduced.
   * Variables get declared and, if there is an initializer expression, initialized.
   */
  private void handleDeclarationEdge (FsmBddState pPredecessor, CDeclarationEdge pDeclEdge, FsmBddState pSuccessor) throws CPATransferException {
    CDeclaration decl = pDeclEdge.getDeclaration();
    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;

      if (vdecl.getType() instanceof CSimpleType) {
        // Track global variables.
        if (decl.isGlobal()) {
          globalVariables.add(decl.getName());
        }

        // Declare the variable.
        String functionName = pDeclEdge.getPredecessor().getFunctionName();
        String scopedVariableName = getScopedVariableName(functionName, decl.getName());
        pSuccessor.declareGlobal(scopedVariableName, domainIntervalProvider.getIntervalMaximum());

        if (vdecl.getInitializer() != null) {
          if (vdecl.getInitializer() instanceof CInitializerExpression) {
            // Initialize the variable.
            CInitializerExpression init = (CInitializerExpression) vdecl.getInitializer();
            pSuccessor.assingConstantToVariable(scopedVariableName, domainIntervalProvider, init.getExpression());
          } else {
            throw new UnrecognizedCCodeException("Type of initializer not supported.", pDeclEdge);
          }
        }
      } else {
        throw new UnrecognizedCCodeException("Unsupported variable declaration: " + vdecl.getType().getClass().getSimpleName(), pDeclEdge);
      }
    } else if (decl instanceof CFunctionDeclaration) {
      // Introduce the result variable for the function.
      String functionName = decl.getName();
      String scopedResultVariableName = getScopedVariableName(functionName, RESULT_VARIABLE_NAME);
      pSuccessor.declareGlobal(scopedResultVariableName, domainIntervalProvider.getIntervalMaximum());
    } else {
      throw new UnrecognizedCCodeException("Unsupported declaration: " + decl.getClass().getSimpleName(), pDeclEdge);
    }
  }

  /**
   * Build a BDD that represents one valuation of a variable.
   */
  private BDD getValuedVarBdd(FsmBddState pOnState, String variableName, CExpression literal) throws CPATransferException {
    BDDDomain varDomain = pOnState.getGlobalVariableDomain(variableName);
    int literalIndex = domainIntervalProvider.mapLiteralToIndex(literal);
    return varDomain.ithVar(literalIndex);
  }

  /**
   * Transform a given assumption-expression
   * to a binary decision diagram.
   */
  private BDD getAssumptionAsBdd(final FsmBddState pOnState, final String pFunctionName, CExpression pExpression, final boolean positiveFirst)
      throws CPATransferException {
    DefaultCExpressionVisitor<BDD, CPATransferException> visitor = new DefaultCExpressionVisitor<BDD, CPATransferException>() {
      @Override
      public BDD visit(CBinaryExpression pE) throws CPATransferException {
        BDD result = expressionBddCache.get(pE);
        if (result != null) {
          return result;
        }

        switch (pE.getOperator()) {
        case EQUALS:
        case NOT_EQUALS: {
          try {
            // a == 123
            if(pE.getOperand1() instanceof CIdExpression
            && pE.getOperand2() instanceof CLiteralExpression) {

              String variableName = ((CIdExpression) pE.getOperand1()).getName();
              String scopedVariableName = getScopedVariableName(pFunctionName, variableName);

              result = getValuedVarBdd(pOnState, scopedVariableName, pE.getOperand2());

            // 123 == a
            } else if (pE.getOperand2() instanceof CIdExpression
                && pE.getOperand1() instanceof CLiteralExpression) {
              String variableName = ((CIdExpression) pE.getOperand2()).getName();
              String scopedVariableName = getScopedVariableName(pFunctionName, variableName);

              result = getValuedVarBdd(pOnState, scopedVariableName, pE.getOperand1());

            // a == -123
            } else if(pE.getOperand1() instanceof CIdExpression
                && pE.getOperand2() instanceof CUnaryExpression
                && ((CUnaryExpression)pE.getOperand2()).getOperator() == UnaryOperator.MINUS
                && ((CUnaryExpression) pE.getOperand2()).getOperand() instanceof CLiteralExpression) {

              String variableName = ((CIdExpression) pE.getOperand1()).getName();
              String scopedVariableName = getScopedVariableName(pFunctionName, variableName);

              result = getValuedVarBdd(pOnState, scopedVariableName, pE.getOperand2());

            // a == b
            } else if (pE.getOperand1() instanceof CIdExpression
            && pE.getOperand2() instanceof CIdExpression) {
              String variableName1 = ((CIdExpression) pE.getOperand1()).getName();
              String variableName2 = ((CIdExpression) pE.getOperand2()).getName();

              String scopedVariableName1 = getScopedVariableName(pFunctionName, variableName1);
              String scopedVariableName2 = getScopedVariableName(pFunctionName, variableName2);

              BDDDomain dom1 = pOnState.getGlobalVariableDomain(scopedVariableName1);
              BDDDomain dom2 = pOnState.getGlobalVariableDomain(scopedVariableName2);

              result = dom1.buildEquals(dom2);
            } else {
               throw new CPATransferException("Combination of operands not (yet) supported: "  + pE.getClass().getSimpleName().toString());
            }

            // Maybe the operation must be negated.
            if (pE.getOperator() == BinaryOperator.NOT_EQUALS) {
              result = result.not();
            }
          } catch (VariableDeclarationException ex) {
            throw new CPATransferException(ex.getMessage());
          }

          break;
        }
        case LOGICAL_AND: {
          BDD left = pE.getOperand1().accept(this);
          BDD right = pE.getOperand2().accept(this);

          result = left.and(right);
          break;
        }
        case LOGICAL_OR: {
          BDD left = pE.getOperand1().accept(this);
          BDD right = pE.getOperand2().accept(this);

          result = left.or(right);
          break;
        }
        default:
          throw new CPATransferException(String.format("Operator %s not (yet) supported!", pE.getOperator()));
        }

        expressionBddCache.put(pE, result);
        return result;
      }

      @Override
      public BDD visit(CUnaryExpression pE) throws CPATransferException {
        BDD result = expressionBddCache.get(pE);
        if (result != null) {
          return result;
        }

        switch (pE.getOperator()) {
        case NOT: {
            result = pE.getOperand().accept(this).not();
            break;
        }
        default:
          throw new CPATransferException(String.format("Operator %s not (yet) supported!", pE.getOperator()));
        }

        expressionBddCache.put(pE, result);
        return result;
      }

      @Override
      protected BDD visitDefault(CExpression pE) throws CPATransferException {
        BDD result = expressionBddCache.get(pE);
        if (result != null) {
          return result;
        }

        result = pE.accept(this);
        expressionBddCache.put(pE, result);
        return result;
      }
    };

    return pExpression.accept(visitor);
  }

//  /**
//   * Transform a given assumption-expression
//   * to a binary decision diagram.
//   */
//  private BDD getAssumptionAsBdd2(final FsmBddState pOnState, final String pFunctionName, CExpression pExpression, final boolean positiveFirst)
//      throws CPATransferException {
//    Stack<CExpression> parsingStack = new Stack<CExpression>();
//    Stack<BDD> valueStack = new Stack<BDD>();
//
//    parsingStack.push(pExpression);
//
//    BDD result = null;
//
//    while (parsingStack.size() > 0) {
//      CExpression activeExpression = parsingStack.pop();
//      BinaryOperator activeOperator = BinaryOperator.LOGICAL_AND;
//
//      if (activeExpression instanceof CBinaryExpression) {
//        CBinaryExpression bin = (CBinaryExpression) pExpression;
//        switch (bin.getOperator()) {
//        case LOGICAL_AND:
//        case LOGICAL_OR:
//          parsingStack.push(bin.getOperand1());
//          parsingStack.push(bin.getOperand2());
//          break;
//        case EQUALS:
//        case NOT_EQUALS:
//          BDD expressionBdd = getAssumptionAsBdd(pOnState, pFunctionName, activeExpression, false);
//          switch (activeOperator) {
//          case LOGICAL_AND:
//            result = result.and(expressionBdd); break;
//          case LOGICAL_OR:
//            result = result.or(expressionBdd); break;
//          default:
//            result = expressionBdd;
//          }
//          break;
//        default:
//          throw new CPATransferException("Operand of binary expression not supported!");
//        }
//    }
//
//    if (pExpression instanceof CBinaryExpression) {
//      CBinaryExpression bin = (CBinaryExpression) pExpression;
//      CExpression left = bin.getOperand1();
//      CExpression right = bin.getOperand2();
//      result = getAssumptionAsBdd(pOnState, pFunctionName, left, positiveFirst);
//
//      switch (bin.getOperator()) {
//      }
//
//    } else {
//      result = getAssumptionAsBdd(pOnState, pFunctionName, pExpression, positiveFirst);
//    }
//  }

  /**
   * Return the scoped name of the variable.
   */
  private String getScopedVariableName(String pFunctionName, String pVariableName) {
    if (globalVariables.contains(pVariableName)) {
      return pVariableName;
    } else {
      Set<String> variablesOfFunction = variablesPerFunction.get(pFunctionName);
      if (variablesOfFunction == null) {
        variablesOfFunction = new HashSet<String>();
        variablesPerFunction.put(pFunctionName, variablesOfFunction);
      }
      variablesOfFunction.add(pVariableName);

      return pFunctionName + SCOPE_SEPARATOR + pVariableName;
    }
  }

  /**
   * We do not use strengthening.
   */
  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
