/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import com.google.common.collect.Lists;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSFunctionType;
import org.sosy_lab.cpachecker.util.Pair;

class CFAFunctionBuilder extends ASTVisitor {
  private final Scope scope;
  private final LogManager logger;
  private final ASTConverter astConverter;

  private SortedMap<String, FunctionEntryNode> cfas = new TreeMap<>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private final List<Pair<ADeclaration, String>> globalDeclarations = Lists.newArrayList();

  private CFANode prevNode;
  private final String functionName;
  private final FunctionExitNode exitNode;

  CFAFunctionBuilder(
      final JSFunctionDeclaration functionDeclaration,
      final Scope pScope,
      final LogManager pLogger,
      final ASTConverter pAstConverter) {
    functionName = functionDeclaration.getName();
    scope = pScope;
    logger = pLogger;
    astConverter = pAstConverter;
    exitNode = new FunctionExitNode(functionName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY, functionDeclaration, exitNode, Optional.empty());
    prevNode = entryNode;
    exitNode.setEntryNode(entryNode);
    cfas.put(functionName, entryNode);
    cfaNodes.put(functionName, entryNode);
    cfaNodes.put(functionName, exitNode);
  }

  CFAFunctionBuilder(
      final FunctionDeclaration node,
      final Scope pScope,
      final LogManager pLogger,
      final ASTConverter pAstConverter) {
    this(convertFunctionDeclaration(node, pAstConverter), pScope, pLogger, pAstConverter);
  }

  private static String getFunctionName(final FunctionDeclaration node) {
    return ((SimpleName) node.getMethodName()).getIdentifier();
  }

  private static JSFunctionDeclaration convertFunctionDeclaration(
      final FunctionDeclaration node, final ASTConverter astConverter) {
    return new JSFunctionDeclaration(
        astConverter.getFileLocation(node),
        new JSFunctionType(JSAnyType.ANY, Collections.emptyList()),
        getFunctionName(node),
        Collections.emptyList());
  }

  @Override
  public boolean visit(final FunctionDeclaration node) {
    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationStatement node) {
    @SuppressWarnings("unchecked")
    final List<VariableDeclarationFragment> variableDeclarationFragments = node.fragments();
    for (final VariableDeclarationFragment variableDeclarationFragment :
        variableDeclarationFragments) {
      final CFANode nextNode = new CFANode(functionName);
      cfaNodes.put(functionName, nextNode);

      addEdge(prevNode, nextNode, variableDeclarationFragment);

      prevNode = nextNode;
    }
    return super.visit(node);
  }

  private void addEdge(
      final CFANode pPredecessor,
      final CFANode pSuccessor,
      final VariableDeclarationFragment pVariableDeclarationFragment) {
    final JSVariableDeclaration variableDeclaration =
        astConverter.convert(pVariableDeclarationFragment);
    CFACreationUtils.addEdgeToCFA(
        new JSDeclarationEdge(
            variableDeclaration.toASTString(),
            astConverter.getFileLocation(pVariableDeclarationFragment),
            pPredecessor,
            pSuccessor,
            variableDeclaration),
        logger);
  }

  ParseResult createCFA() {
    // add dummy edge from previous node to exit node
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge("", FileLocation.DUMMY, prevNode, exitNode, "end of file"), logger);
    return new ParseResult(cfas, cfaNodes, globalDeclarations, Language.JAVASCRIPT);
  }
}
