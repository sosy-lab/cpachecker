/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.speci;

import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.livevar.DeclarationCollectingVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix="cpa.speci")
public class SpecInferenceTransferRelation extends ForwardingTransferRelation<SpecInferenceState, SpecInferenceState, Precision> {

  @Option(secure = true, description = "The string a function call has to contain in order to be considered as a function where to start tracking a handle.")
  private String searchStart = "open";

  @Option(secure = true, description = "The string a function call has to contain in order to be considered as a function where to stop tracking a handle.")
  private String searchStop = "close";

  public SpecInferenceTransferRelation(Configuration config)
      throws InvalidConfigurationException {

    config.inject(this);
  }

  @Override
  protected SpecInferenceState handleAssumption(AssumeEdge pCfaEdge, AExpression pExpression,
      boolean pTruthAssumption) throws CPATransferException {

    if (state.getHandle() != null) {
      CFANode pred = pCfaEdge.getPredecessor();

//      if (pred.isLoopStart()) {

        return new SpecInferenceState().startTracking(state.getHandle(), "MATCH ASSUME -> ASSUME {" + pCfaEdge.getCode() + "} ");

//      } else if (containsHandle(handleExpression(expression))) {
//        return state.addAutomatonState("MATCH ASSUME -> ASSUME {" + cfaEdge.getCode() + "} ");
//      }
    }

    return state;
  }


  @Override
  protected SpecInferenceState handleFunctionCallEdge(FunctionCallEdge cfaEdge,
      List<? extends AExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {

    // if we don't have a handle to track, function calls are not of interest
    if (state.getHandle() != null) {

      Collection<ASimpleDeclaration> vars = handleExpression(arguments);

      // if handle is not contained in parameters, nothing to do
      if (containsHandle(vars)) {
        String s = "MATCH {" + cfaEdge.getCode() + "} ->";
        if (calledFunctionName.contains(searchStop)) {
          return state.stopTracking(s);
        } else {
          return state.addAutomatonState(s);
        }

      }
    }

    return state;
  }

  @Override
  protected SpecInferenceState handleFunctionReturnEdge(FunctionReturnEdge pCfaEdge,
      FunctionSummaryEdge pFnkCall, AFunctionCall pSummaryExpr, String pCallerFunctionName)
          throws CPATransferException {
    return state;
  }

  @Override
  protected SpecInferenceState handleDeclarationEdge(ADeclarationEdge pCfaEdge, ADeclaration pDecl)
      throws CPATransferException {
    return state;
  }

  @Override
  protected SpecInferenceState handleStatementEdge(AStatementEdge pCfaEdge, AStatement statement)
      throws CPATransferException {

    if (statement instanceof AExpressionAssignmentStatement) {
      return handleAssignments((AAssignment) statement);

    } else if (statement instanceof AExpressionStatement) {
      if (state.getHandle() == null) {
        return state;
      }
      AExpressionStatement s = (AExpressionStatement) statement;
      if (containsHandle(handleExpression(s.getExpression()))) {

        return state.addAutomatonState("MATCH {" + pCfaEdge.getCode() + "} ->");

      }

    } else if (statement instanceof AFunctionCallAssignmentStatement) {
      return handleAssignments((AAssignment) statement);

    } else if (statement instanceof AFunctionCallStatement) {

      if (state.getHandle() == null) {
        return state;
      } else {
        AFunctionCallStatement s = (AFunctionCallStatement) statement;

        if (containsHandle(handleExpression(s.getFunctionCallExpression().getParameterExpressions()))) {
          String funcName = ((AIdExpression) s.getFunctionCallExpression().getFunctionNameExpression()).getName();

          if (funcName.contains(searchStop)) {
            return state.stopTracking("MATCH {" + pCfaEdge.getCode() + "} ->");
          } else {
            return state.addAutomatonState("MATCH {" + pCfaEdge.getCode() + "} ->");
          }

        }
      }

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }

    return state;
  }

  @Override
  protected SpecInferenceState handleReturnStatementEdge(AReturnStatementEdge pCfaEdge) throws CPATransferException {

    // FIXME magic word
    if (pCfaEdge.getSuccessor().getFunctionName().equals("main")) {
      return state.addAutomatonState("MATCH EXIT -> ");
    }

    return state;
  }

  @Override
  protected SpecInferenceState handleFunctionSummaryEdge(FunctionSummaryEdge pCfaEdge) throws CPATransferException {
    // TODO can I ignore this completely?
    return super.handleFunctionSummaryEdge(pCfaEdge);
  }

  private Collection<ASimpleDeclaration> handleExpression(List<? extends AExpression> expression) {

    HashSet<ASimpleDeclaration> retval = new HashSet<>();

    for (AExpression e : expression) {
      retval.addAll(acceptAll(e));
    }

    return retval;
  }

  private Collection<ASimpleDeclaration> handleExpression(AExpression expression) {
    return from(acceptAll(expression)).toSet();
  }

  private SpecInferenceState handleAssignments(AAssignment as) throws CPATransferException {

    AIdExpression left;
    if (as.getLeftHandSide() instanceof AIdExpression) {
      left = (AIdExpression)as.getLeftHandSide();
    } else {
      return state;
    }

    SpecInferenceState succ = null;

    if (as instanceof AExpressionAssignmentStatement) {
      if (state.getHandle() != null) {
        if (containsHandle(handleExpression((AExpression) as.getRightHandSide()))) {
          succ = state.addAutomatonState("MATCH {" + as.toString() + "} -> ");
        }
      }

    } else if (as instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement funcStmt = (AFunctionCallAssignmentStatement) as;
      String funcName = ((AIdExpression) funcStmt.getRightHandSide().getFunctionNameExpression()).getName();

      if (state.getHandle() != null) {
        if (containsHandle(getParameters(funcStmt.getFunctionCallExpression().getParameterExpressions()))) {
          String s = "MATCH { " + funcStmt.getLeftHandSide().toString() + " = " + funcStmt.getRightHandSide().toString() + "} ->";
          if (funcName.contains(searchStop)) {
            succ = state.stopTracking(s);
          } else {
            succ = state.addAutomatonState(s);
          }
        }
      } else {
        if (funcName.contains(searchStart)) {
          succ = state.startTracking(left.getName(), "MATCH {" + as.toString() + "} -> ");
        }
      }

    } else {
      throw new AssertionError("Unhandled assignment type.");
    }

    if (state.getHandle() != null && left.getName().equals(state.getHandle())) {
      throw new CPATransferException("Handle is being reassigned!");
    }

    return succ != null ? succ : state;

  }

  private Collection<ASimpleDeclaration> getParameters(List<? extends AExpression> pParams) {
    Collection<ASimpleDeclaration> params = new ArrayList<>();
    for (AExpression expression : pParams) {
      params.addAll(handleExpression(expression));
    }
    return params;
  }

  private boolean containsHandle(Collection<ASimpleDeclaration> d) {
    boolean containsHandle = false;
    for (ASimpleDeclaration e : d) {
      if (e.getName().equals(state.getHandle())) {
        containsHandle = true;
        break;
      }
    }
    return containsHandle;
  }

  private static Set<ASimpleDeclaration> acceptAll(AExpression exp) {
    return exp.<Set<ASimpleDeclaration>, Set<ASimpleDeclaration>, Set<ASimpleDeclaration>, RuntimeException, RuntimeException, DeclarationCollectingVisitor>accept_(
                           new DeclarationCollectingVisitor());
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState,
      List<AbstractState> pOtherStates, CFAEdge pCfaEdge, Precision pPrecision)
          throws CPATransferException, InterruptedException {
    return null;
  }


}
