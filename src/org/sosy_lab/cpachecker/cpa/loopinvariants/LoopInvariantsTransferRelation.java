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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterables;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.AddExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Addition;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Constant;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Multiplication;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.PolynomExpression;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.Variable;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.logging.Level;

public class LoopInvariantsTransferRelation extends SingleEdgeTransferRelation {

  private CFA cfa;
  private LogManager logger;

  public LoopInvariantsTransferRelation(CFA pCfa, LogManager pLog) {
    this.cfa = pCfa;
    this.logger = pLog;
  }

  @SuppressWarnings("null")
  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState pState,
      Precision pPrecision, CFAEdge pCfaEdge) {

    LoopInvariantsState state = (LoopInvariantsState) pState;
    LoopInvariantsState newState = new LoopInvariantsState();

    newState = setEdgeInLoop(pCfaEdge, state);

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge:
        if (pCfaEdge instanceof ADeclarationEdge) {
          ADeclarationEdge edge = (ADeclarationEdge) pCfaEdge;
          if (edge.getDeclaration() instanceof AVariableDeclaration) {
            AVariableDeclaration declaration = (AVariableDeclaration) edge.getDeclaration();
            String variableName = declaration.getName();
            if (!variableName.contains("CPAchecker")) {
              AInitializer init = declaration.getInitializer();
              if (init != null) {
                if (init instanceof AInitializerExpression) {
                  AInitializerExpression initExpr = (AInitializerExpression) init;
                  AExpression exp = initExpr.getExpression();

                  PolynomExpression poly = handleExpression(exp);

                  if (poly != null) {
                    if (poly instanceof Constant) {
                      newState.addVariableValue(variableName, ((Constant) poly).getValue());
                    } else {
                      Polynom polynom = new Polynom(poly, logger);
                      OptionalDouble value = polynom.evalPolynom(null);
                      if (value.isPresent()) {
                        newState.addVariableValue(variableName, value.getAsDouble());
                      } else {
                        if (poly != null) {
                          if (newState.getInLoop()) {
                            newState.addPolynom(new Addition(
                                new Multiplication(new Constant(-1),
                                    new Variable(variableName + "(n+1)")),
                                (AddExpression) poly));
                          } else {
                            newState.addPolynomOutsideOfLoop(new Addition(
                                new Multiplication(new Constant(-1),
                                    new Variable(variableName + "(n+1)")),
                                (AddExpression) poly));
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        break;

      case StatementEdge:
        AStatementEdge sEdge = (AStatementEdge) pCfaEdge;
        AStatement statement = sEdge.getStatement();

        StatementVisitor v = new StatementVisitor();
        PolynomExpression poly = statement.accept(v);
        if (poly != null) {
          if (newState.getInLoop()) {
            newState.addPolynom(poly);
          } else {
            newState.addPolynomOutsideOfLoop(poly);
          }
        }

        break;

      case ReturnStatementEdge:
        AReturnStatementEdge rEdge = (AReturnStatementEdge) pCfaEdge;
        Optional<? extends AExpression> optExpr = rEdge.getExpression();
        if (optExpr.isPresent()) {
          PolynomExpression newPoly = handleExpression(optExpr.get());
          if (newPoly != null) {
            if (newState.getInLoop()) {
              newState.addPolynom(newPoly);
            } else {
              newState.addPolynomOutsideOfLoop(newPoly);
            }
          }
        }
        break;
      default:
        break;
    }

    if (state.getInLoop() && newState.isLoopHead()) {
      CalculationHelper.calculateGroebnerBasis(newState, logger);
      logger.log(Level.INFO, "Resulting invariants: " + newState.getInvariant().toString());
      newState.setPolynoms(new LinkedList<PolynomExpression>());
    }
    return Collections.singleton(newState);
  }

  private LoopInvariantsState setEdgeInLoop(CFAEdge pCfaEdge, LoopInvariantsState state) {
    LoopInvariantsState newState;
    newState = state.copy();

    CFANode node = pCfaEdge.getPredecessor();

    if (cfa.getLoopStructure().isPresent()) {
      LoopStructure loopStructure = cfa.getLoopStructure().get();
      if (loopStructure.getCount() == 1) {
        Loop singleLoop = Iterables.getOnlyElement(loopStructure.getAllLoops());
        if (singleLoop.getLoopHeads().size() == 1) {
          CFANode loopHead = Iterables.getOnlyElement(singleLoop.getLoopHeads());
          ImmutableCollection<Loop> loops = loopStructure.getAllLoops();
          Set<CFANode> loopNodes = loops.iterator().next().getLoopNodes();
          newState.setInLoop(loopNodes.contains(node));
          newState.setLoopHead(loopHead.equals(node));
        }
      }
    }
    return newState;
  }

  private PolynomExpression handleExpression(AExpression expression) {
    LoopInvariantsExpressionVisitor v = new LoopInvariantsExpressionVisitor();
    CExpression cExpression = (CExpression) expression;
    PolynomExpression polyExp = cExpression.accept(v);
    return polyExp;
  }

}
