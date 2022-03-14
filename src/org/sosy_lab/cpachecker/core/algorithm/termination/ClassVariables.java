// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
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
    return visitor.globalDeclarations.build();
  }

  public ImmutableSetMultimap<String, CVariableDeclaration> getLocalDeclarations() {
    return visitor.localDeclarations.build();
  }

  private static final class DeclarationCollectionCFAVisitor extends DefaultCFAVisitor {

    private final ImmutableSet.Builder<CVariableDeclaration> globalDeclarations =
        ImmutableSet.builder();

    private final ImmutableSetMultimap.Builder<String, CVariableDeclaration> localDeclarations =
        ImmutableSetMultimap.builder();

    private DeclarationCollectionCFAVisitor() {}

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      if (pNode instanceof CFunctionEntryNode) {
        String functionName = pNode.getFunctionName();
        List<CParameterDeclaration> parameters =
            ((CFunctionEntryNode) pNode).getFunctionParameters();
        parameters.stream()
            .map(CParameterDeclaration::asVariableDeclaration)
            .forEach(decl -> localDeclarations.put(functionName, decl));
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
