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
package org.sosy_lab.cpachecker.util.harness;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class PointerFunctionExtractor {

  public static Set<AFunctionDeclaration>
      getExternUnimplementedPointerReturnTypeFunctions(CFA pCFA) {
    Set<AFunctionDeclaration> relevantPointerFunctionsState = new HashSet<>();

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
              boolean headIsEmpty = (cfa.getFunctionHead(declaration.getQualifiedName()) == null);
              boolean hasPointerReturnType =
                  (functionDeclaration.getType().getReturnType() instanceof CPointerType);
              if (hasPointerReturnType && headIsEmpty) {
                relevantPointerFunctionsState.add(functionDeclaration);
              }
            }
          }
        }
        return TraversalProcess.CONTINUE;
      }
    };
    CFATraversal.dfs().traverseOnce(pCFA.getMainFunction(), externalFunctionCollector);
    return relevantPointerFunctionsState;
  }

  public static Set<AFunctionDeclaration>
      getExternUnimplementedPointerTypeParameterFunctions(CFA pCFA) {
    Set<AFunctionDeclaration> relevantFunctions = new HashSet<>();

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
              boolean headIsEmpty = (cfa.getFunctionHead(declaration.getQualifiedName()) == null);

              boolean hasPointerParameter =
                  functionDeclaration.getParameters()
                      .stream()
                      .map(o -> o.getType())
                      .filter(type -> type instanceof CPointerType)
                      .findFirst()
                      .isPresent();
              if (hasPointerParameter && headIsEmpty) {
                relevantFunctions.add(functionDeclaration);
              }
            }
          }
        }
        return TraversalProcess.CONTINUE;
      }
    };
    CFATraversal.dfs().traverseOnce(pCFA.getMainFunction(), externalFunctionCollector);
    return relevantFunctions;
  }

}
