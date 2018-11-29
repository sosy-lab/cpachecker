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
package org.sosy_lab.cpachecker.cpa.harness;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "cpa.harness")
public class HarnessTransferRelation
    extends SingleEdgeTransferRelation {


  private final LogManager logger;
  private final ImmutableSet<String> externPointerFunctions;

  public HarnessTransferRelation(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this, HarnessTransferRelation.class);
    logger = new LogManagerWithoutDuplicates(pLogger);
    externPointerFunctions = extractExternPointerFunctions(pCFA);
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pElement, Precision pPrecision, CFAEdge pEdge)
          throws CPATransferException {

    HarnessState p = (HarnessState) pElement;

    switch (pEdge.getEdgeType()) {
      case StatementEdge: {
        AStatement statement = ((AStatementEdge) pEdge).getStatement();
        if (statement instanceof AFunctionCall) {
          AFunctionCall functionCall = (AFunctionCall) statement;
          AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
          if (nameExpression instanceof AIdExpression) {
            AIdExpression idExpression = (AIdExpression) nameExpression;
            if (externPointerFunctions.contains(idExpression.getName())) {
              boolean identifierExists = p.pointerMap.keySet().filter(key -> key.identifier == statement.leftHandSide)
              if (aliasGroup.isPresent()) {
                return pElement.declarePointerVariable()
              }
              if (pEdge.getEdgeType() instanceof CVariableDeclaration) {
                return pElement.declarePointerVariable((CVariableDeclaration) pEdge)
              }
            }
          }

          return Collections.singleton(pElement);
          // Fall Must-Alias und !== Assume Edge

        }
      }
      default: {
        break;
      }
    }
    return Collections.singleton(pElement);
  }

  private ImmutableSet<String> extractExternPointerFunctions(CFA pCFA) {
    Set<String> externPointerReturnFunctions = new HashSet<>();
    Set<String> externPointerParameterFunctions = new HashSet<>();
    CFAVisitor externalFunctionCollector = new CFAVisitor() {

      private CFA cfa = pCFA;

      @Override
      public TraversalProcess visitNode(CFANode pNode) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitEdge(CFAEdge pEdge) {
        if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
          ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
          ADeclaration declaration = declarationEdge.getDeclaration();
          if (declaration instanceof AFunctionDeclaration) {
            AFunctionDeclaration functionDeclaration = (AFunctionDeclaration) declaration;
            if (!cfa.getAllFunctionNames().contains(functionDeclaration.getName())) {
              logger.log(Level.INFO, cfa.getAllFunctionNames());
              boolean headIsEmpty = (cfa.getFunctionHead(declaration.getQualifiedName()) == null);

              boolean hasPointerParameter = functionDeclaration.getParameters().stream().filter(o -> o.getType() instanceof CPointerType).findFirst().isPresent();
              if (hasPointerParameter) {
                externPointerParameterFunctions.add(functionDeclaration.getName());
              }
              boolean hasPointerReturnType = (functionDeclaration.getType().getReturnType() instanceof CPointerType);
              if (hasPointerReturnType && headIsEmpty) {
                externPointerReturnFunctions.add(functionDeclaration.getName());
              }
            }
          }
        }
        return TraversalProcess.CONTINUE;
      }
    };
    CFATraversal.dfs().traverseOnce(pCFA.getMainFunction(), externalFunctionCollector);
    ImmutableSet<String> res = ImmutableSet.copyOf(externPointerReturnFunctions);
    return res;
  }


}