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
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
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

/**
 * The transfer-relation of the FsmBdd CPA.
 */
public class FsmTransferRelation implements TransferRelation {

  /**
   * Name of the variable that is used to encode the result of a function.
   * (gets scoped with the name of the function)
   *
   * Since "return" is a keyword, we should not get a
   * conflict with a program variable with the same name.
   */
  private static final String RESULT_VARIABLE_NAME = "return";

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
  private final Map<CExpression, BDD> edgeBddCache;

  /**
   * Constructor.
   */
  public FsmTransferRelation() {
    this.edgeBddCache = new HashMap<CExpression, BDD>();
    this.globalVariables = new HashSet<String>();
    this.variablesPerFunction = new HashMap<String, Set<String>>();
  }

  /**
   * Setter.
   */
  public void setDomainIntervalProvider(DomainIntervalProvider pDomainIntervalProvider) {
    this.domainIntervalProvider = pDomainIntervalProvider;
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
    FsmState predecessor = (FsmState) pState;
    FsmState successor = null;

//    System.out.println("-----------------");
//    System.out.println(String.format("%15s : %s", "Predecessor", predecessor));
//    System.out.println(String.format("%15s : (l %d) %s (l %d)", pCfaEdge.getEdgeType(), pCfaEdge.getPredecessor().getNodeNumber(), pCfaEdge.getRawStatement(), pCfaEdge.getSuccessor().getNodeNumber()));

    try {
      switch (pCfaEdge.getEdgeType()) {
        case AssumeEdge:
          successor = handleAssumeEdge(predecessor, (CAssumeEdge) pCfaEdge);
          break;

        case StatementEdge:
          successor = handleStatementEdge(predecessor, (CStatementEdge) pCfaEdge);
          break;

        case DeclarationEdge:
          successor = handleDeclarationEdge(predecessor, (CDeclarationEdge) pCfaEdge);
          break;

        case ReturnStatementEdge:
          successor = handleReturnStatementEdge(predecessor, (CReturnStatementEdge) pCfaEdge);
          break;

        case FunctionReturnEdge:
          successor = handleFunctionReturnEdge(predecessor, (CFunctionReturnEdge) pCfaEdge);
          break;

        case FunctionCallEdge:
          successor = handleFunctionCallEdge(predecessor, (CFunctionCallEdge) pCfaEdge);
          break;

        case MultiEdge:
          successor = handleMultiEdge(predecessor, (MultiEdge) pCfaEdge);
          break;

        default:
          successor = predecessor.cloneState();
      }
    } catch (VariableDeclarationException e) {
      throw new UnrecognizedCCodeException(e.getMessage(), pCfaEdge);
    }

//    System.out.println(String.format("%15s : %s", "Successor", successor));

    // Return an empty set if the BDD evaluates to "false".
    if (successor.getStateBdd().isZero()) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  private FsmState handleMultiEdge(FsmState pPredecessor, MultiEdge pMultiEdge) throws CPATransferException {
    FsmState result = pPredecessor.cloneState();

    for (CFAEdge edge: pMultiEdge.getEdges()) {
      switch (edge.getEdgeType()) {
      case BlankEdge:
        break;
      case DeclarationEdge:
        handleDeclarationEdge(pPredecessor, (CDeclarationEdge) edge, result);
        break;
      case StatementEdge:
        handleStatementEdge(pPredecessor, (CStatementEdge) edge, result);
        break;
      case ReturnStatementEdge:
        handleReturnStatementEdge(pPredecessor, (CReturnStatementEdge) edge, result);
        break;
      default:
        throw new CPATransferException("Unsupported edge within multi-edge: " + edge.getEdgeType().toString());
      }
    }
    return result;
  }

  /**
   * Handle the statement that gets returned by a function.
   */
  private FsmState handleReturnStatementEdge(FsmState pPredecessor, CReturnStatementEdge pReturnEdge) throws CPATransferException {
    FsmState result = pPredecessor.cloneState();
    handleReturnStatementEdge(pPredecessor, pReturnEdge, result);
    return result;
  }

  private void handleReturnStatementEdge(FsmState pPredecessor, CReturnStatementEdge pReturnEdge, FsmState pSuccessor) throws CPATransferException {
    CExpression rightOfReturn = pReturnEdge.getExpression();
    if (rightOfReturn == null) {
      rightOfReturn = CNumericTypes.ZERO;
    }

    String functionName = pReturnEdge.getPredecessor().getFunctionName();
    String scopedResultVariableName = getScopedVariableName(functionName, RESULT_VARIABLE_NAME);
    if (rightOfReturn instanceof CIdExpression) {
      CIdExpression idExpr = (CIdExpression) rightOfReturn;
      String scopedValueVariableName = getScopedVariableName(functionName, idExpr.getName());
      pSuccessor.addConjunctionWith(getEqualVarsBdd(pSuccessor, scopedResultVariableName, scopedValueVariableName));
    } else {
      pSuccessor.doVariableAssignment(scopedResultVariableName, domainIntervalProvider, rightOfReturn);
    }
  }

  private FsmState handleFunctionCallEdge(FsmState pPredecessor, CFunctionCallEdge pCfaEdge) throws CPATransferException {
    FsmState result = pPredecessor.cloneState();

    CFunctionEntryNode functionEntryNode = pCfaEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = pCfaEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = pCfaEdge.getArguments();

    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert(paramNames.size() == arguments.size());
    }

    for (int i = 0; i < paramNames.size(); i++) {
      CExpression argument = arguments.get(i);
      String scopedParameterName = getScopedVariableName(calledFunctionName, paramNames.get(i));

      result.declareGlobal(scopedParameterName, domainIntervalProvider.getIntervalMaximum());

      if (argument instanceof CIdExpression) {
        CIdExpression idExpr = (CIdExpression) argument;
        String scopedArgumentName = getScopedVariableName(callerFunctionName, idExpr.getName());
        result.addConjunctionWith(getEqualVarsBdd(result, scopedParameterName, scopedArgumentName));
      } else if (argument instanceof CLiteralExpression) {
        CLiteralExpression litExpr = (CLiteralExpression) argument;
        result.doVariableAssignment(scopedParameterName, domainIntervalProvider, litExpr);
      } else {
        throw new UnrecognizedCCodeException("Unsupported function argument.", pCfaEdge);
      }
    }

    return result;
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
   *
   */
  private FsmState handleFunctionReturnEdge(FsmState pPredecessor, CFunctionReturnEdge pFunctionReturnEdge) throws UnrecognizedCCodeException, VariableDeclarationException {
    FsmState result = pPredecessor.cloneState();

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

        BDD equalsBdd = getEqualVarsBdd(result, scopedReturnVarName, scopedTargetVariableName);
        result.undefineVariable(scopedTargetVariableName);
        result.addConjunctionWith(equalsBdd);
      } else {
        throw new UnrecognizedCCodeException("On return from function.", summaryEdge, callerLeft);
      }
    } else {
      throw new UnrecognizedCCodeException("Unsupported assignement of function return value.", pFunctionReturnEdge);
    }

    // Existential quantify all local variables of the function.
    for (String localVariable: variablesPerFunction.get(calledFunctionName)) {
      result.undefineVariable(getScopedVariableName(calledFunctionName, localVariable));
    }

    return result;
  }

  /**
   * Handle a assume edge.
   * E.g. (a == b)
   *
   * The BDD of the successor state gets constructed
   * by doing a conjunction of the assumption with the the BDD of the predecessor state.
   */
  private FsmState handleAssumeEdge (FsmState pPredecessor, CAssumeEdge pAssumeEdge) throws CPATransferException {
    FsmState result = pPredecessor.cloneState();

    BDD assumptionBdd = edgeBddCache.get(pAssumeEdge.getExpression());
    if (assumptionBdd == null) {
      assumptionBdd = getAssumptionAsBdd(result, pAssumeEdge, pAssumeEdge.getExpression());
      edgeBddCache.put(pAssumeEdge.getExpression(), assumptionBdd);
    }
    if (!pAssumeEdge.getTruthAssumption()) {
      assumptionBdd = assumptionBdd.not();
    }

    result.addConjunctionWith(assumptionBdd);

    return result;
  }

  /**
   * Handle a statement edges.
   * Currently, only statements of the form "IDExpression = LiteralExpression;" are supported.
   */
  private FsmState handleStatementEdge (FsmState pPredecessor, CStatementEdge pStatementEdge) throws CPATransferException {
    FsmState result = pPredecessor.cloneState();
    handleStatementEdge(pPredecessor, pStatementEdge, result);
    return result;
  }

  private void handleStatementEdge (FsmState pPredecessor, CStatementEdge pStatementEdge, FsmState pSuccessor) throws CPATransferException {
    CStatement stmt = pStatementEdge.getStatement();
    if (stmt instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement assign = (CExpressionAssignmentStatement) stmt;
      if (assign.getLeftHandSide() instanceof CIdExpression
          && assign.getRightHandSide() instanceof CLiteralExpression) {
        CIdExpression left = (CIdExpression) assign.getLeftHandSide();
        CLiteralExpression right = (CLiteralExpression) assign.getRightHandSide();

        String functionName = pStatementEdge.getPredecessor().getFunctionName();
        pSuccessor.doVariableAssignment(getScopedVariableName(functionName, left.getName()), domainIntervalProvider, right);
      } else {
        throw new UnrecognizedCCodeException("Unsupported CExpressionAssignmentStatement", pStatementEdge);
      }
    }
  }

  /**
   * Handle a declaration edge.
   * For function declarations, the result variable gets introduced.
   * Variables get declared and, if there is an initializer expression, initialized.
   */
  private FsmState handleDeclarationEdge (FsmState pPredecessor, CDeclarationEdge pDeclEdge) throws CPATransferException {
    FsmState result = pPredecessor.cloneState();
    handleDeclarationEdge(pPredecessor, pDeclEdge, result);
    return result;
  }

  private void handleDeclarationEdge (FsmState pPredecessor, CDeclarationEdge pDeclEdge, FsmState pSuccessor) throws CPATransferException {
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
            pSuccessor.doVariableAssignment(scopedVariableName, domainIntervalProvider, init.getExpression());
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
      pSuccessor.declareGlobal(getScopedVariableName(functionName, RESULT_VARIABLE_NAME), domainIntervalProvider.getIntervalMaximum());
    } else {
      throw new UnrecognizedCCodeException("Unsupported declaration: " + decl.getClass().getSimpleName(), pDeclEdge);
    }
  }

  /**
   * Build a BDD that represents one valuation of a variable.
   */
  private BDD getValuedVarBdd(FsmState pOnState, String variableName, CExpression literal) throws CPATransferException {
    BDDDomain varDomain = pOnState.getGlobalVariableDomain(variableName);
    int literalIndex = domainIntervalProvider.mapLiteralToIndex(literal);
    return varDomain.ithVar(literalIndex);
  }

  /**
   * Build a BDD that represents the equality of two variables.
   */
  private BDD getEqualVarsBdd(FsmState pOnState, String var1, String var2) throws VariableDeclarationException {
    BDDDomain dom1 = pOnState.getGlobalVariableDomain(var1);
    BDDDomain dom2 = pOnState.getGlobalVariableDomain(var2);

    return dom1.buildEquals(dom2);
  }

  /**
   * Transform a given assumption-expression
   * to a binary decision diagram.
   */
  private BDD getAssumptionAsBdd(final FsmState pOnState, final CFAEdge pEdge, CExpression pExpression)
      throws CPATransferException {
    DefaultCExpressionVisitor<BDD, CPATransferException> visitor = new DefaultCExpressionVisitor<BDD, CPATransferException>() {
      @Override
      public BDD visit(CBinaryExpression pE) throws CPATransferException {
        switch (pE.getOperator()) {
        case EQUALS:
        case NOT_EQUALS: {
          try {
            BDD result;

            // a == 123
            if(pE.getOperand1() instanceof CIdExpression
            && pE.getOperand2() instanceof CLiteralExpression) {

              String variableName = ((CIdExpression) pE.getOperand1()).getName();
              String scopedVariableName = getScopedVariableName(pEdge.getPredecessor().getFunctionName(), variableName);

              result = getValuedVarBdd(pOnState, scopedVariableName, pE.getOperand2());

            // 123 == a
            } else if (pE.getOperand2() instanceof CIdExpression
                && pE.getOperand1() instanceof CLiteralExpression) {
              String variableName = ((CIdExpression) pE.getOperand2()).getName();
              String scopedVariableName = getScopedVariableName(pEdge.getPredecessor().getFunctionName(), variableName);

              result = getValuedVarBdd(pOnState, scopedVariableName, pE.getOperand1());

            // a == -123
            } else if(pE.getOperand1() instanceof CIdExpression
                && pE.getOperand2() instanceof CUnaryExpression
                && ((CUnaryExpression)pE.getOperand2()).getOperator() == UnaryOperator.MINUS
                && ((CUnaryExpression) pE.getOperand2()).getOperand() instanceof CLiteralExpression) {

              String variableName = ((CIdExpression) pE.getOperand1()).getName();
              String scopedVariableName = getScopedVariableName(pEdge.getPredecessor().getFunctionName(), variableName);

              result = getValuedVarBdd(pOnState, scopedVariableName, pE.getOperand2());

            // a == b
            } else if (pE.getOperand1() instanceof CIdExpression
            && pE.getOperand2() instanceof CIdExpression) {
              String variableName1 = ((CIdExpression) pE.getOperand1()).getName();
              String variableName2 = ((CIdExpression) pE.getOperand2()).getName();

              String scopedVariableName1 = getScopedVariableName(pEdge.getPredecessor().getFunctionName(), variableName1);
              String scopedVariableName2 = getScopedVariableName(pEdge.getPredecessor().getFunctionName(), variableName2);

              result = getEqualVarsBdd(pOnState, scopedVariableName1, scopedVariableName2);

            } else {
               throw new UnrecognizedCCodeException("Combination of operands not (yet) supported.", pEdge, pE);
            }

            // Maybe the operation must be negated.
            if (pE.getOperator() == BinaryOperator.NOT_EQUALS) {
              result = result.not();
            }

            return result;
          } catch (VariableDeclarationException ex) {
            throw new UnrecognizedCCodeException(ex.getMessage(), pEdge, pE);
          }
        }
        case LOGICAL_AND: {
          BDD left = pE.getOperand1().accept(this);
          BDD right = pE.getOperand1().accept(this);
          return left.and(right);
        }
        case LOGICAL_OR: {
          BDD left = pE.getOperand1().accept(this);
          BDD right = pE.getOperand1().accept(this);
          return left.or(right);
        }
        default:
          throw new UnrecognizedCCodeException(String.format("Operator %s not (yet) supported!", pE.getOperator()), pEdge, pE);
        }
      }

      @Override
      public BDD visit(CUnaryExpression pE) throws CPATransferException {
        switch (pE.getOperator()) {
        case NOT: return pE.getOperand().accept(this).not();
        default:
          throw new UnrecognizedCCodeException(String.format("Operator %s not (yet) supported!", pE.getOperator()), pEdge, pE);
        }
      }

      @Override
      protected BDD visitDefault(CExpression pExp) throws CPATransferException {
        return pExp.accept(this);
      }
    };

    return pExpression.accept(visitor);
  }

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
