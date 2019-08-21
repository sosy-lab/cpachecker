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
 */
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class ClassVariables {

  private DeclarationCollectionCFAVisitor visitor;

  private ClassVariables(DeclarationCollectionCFAVisitor visitor, CFA pCfa) {
    this.visitor = visitor;

    for (CFANode function : pCfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(function, visitor);
    }
  }

  public static ClassVariables collectDeclarations(CFA pCfa) {
    checkNotNull(pCfa);
    return new ClassVariables(new DeclarationCollectionCFAVisitor(), pCfa);
  }

  public Set<CVariableDeclaration> getDeclarations(Loop pLoop) {
    String function = pLoop.getLoopHeads().iterator().next().getFunctionName();
    Set<CVariableDeclaration> relevantVariabels =
        ImmutableSet.<CVariableDeclaration>builder()
            .addAll(getGlobalDeclarations())
            .addAll(getLocalDeclarations().get(function))
            .build();
    return relevantVariabels;
  }

  public ImmutableSet<CVariableDeclaration> getGlobalDeclarations() {
    return ImmutableSet.copyOf(visitor.globalDeclarations);
  }

  public ImmutableSetMultimap<String, CVariableDeclaration> getLocalDeclarations() {
    return ImmutableSetMultimap.copyOf(visitor.localDeclarations);
  }

  private static final class DeclarationCollectionCFAVisitor extends DefaultCFAVisitor {

    private final Set<CVariableDeclaration> globalDeclarations = Sets.newLinkedHashSet();

    private final Multimap<String, CVariableDeclaration> localDeclarations =
        MultimapBuilder.hashKeys().linkedHashSetValues().build();

    private DeclarationCollectionCFAVisitor() {}

    @Override
    public TraversalProcess visitNode(CFANode pNode) {

      if (pNode instanceof CFunctionEntryNode) {
        String functionName = pNode.getFunctionName();
        List<CParameterDeclaration> parameters =
            ((CFunctionEntryNode) pNode).getFunctionParameters();
        parameters
            .stream()
            .map(CParameterDeclaration::asVariableDeclaration)
            .forEach(localDeclarations.get(functionName)::add);
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {

      if (pEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

          if (variableDeclaration.isGlobal()) {
            globalDeclarations.add(variableDeclaration);

          } else {
            String functionName = pEdge.getPredecessor().getFunctionName();
            localDeclarations.put(functionName, variableDeclaration);
          }
        }
      }
      return TraversalProcess.CONTINUE;
    }
  }
}
