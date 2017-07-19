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
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
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
import org.sosy_lab.cpachecker.util.Pair;

class CFAFunctionBuilder extends ASTVisitor {
  private final Scope scope;
  private final LogManager logger;
  private final ASTConverter astConverter;

  private final SortedMap<String, FunctionEntryNode> cfas = new TreeMap<>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private final List<Pair<ADeclaration, String>> globalDeclarations = Lists.newArrayList();

  private CFANode prevNode;
  private final String functionName;
  private final FunctionExitNode exitNode;

  private final FunctionDeclaration functionDeclaration;

  CFAFunctionBuilder(
      final FunctionDeclaration pFunctionDeclaration,
      final JSFunctionDeclaration pJSFunctionDeclaration,
      final Scope pScope,
      final LogManager pLogger,
      final ASTConverter pAstConverter) {
    functionDeclaration = pFunctionDeclaration;
    functionName = pJSFunctionDeclaration.getName();
    scope = pScope;
    logger = pLogger;
    astConverter = pAstConverter;
    exitNode = new FunctionExitNode(functionName);
    final JSFunctionEntryNode entryNode =
        new JSFunctionEntryNode(
            FileLocation.DUMMY, pJSFunctionDeclaration, exitNode, Optional.empty());
    prevNode = entryNode;
    exitNode.setEntryNode(entryNode);
    cfas.put(functionName, entryNode);
    cfaNodes.put(functionName, entryNode);
    cfaNodes.put(functionName, exitNode);
    addFunctionEntryNode();
  }

  CFAFunctionBuilder(
      final JSFunctionDeclaration pJSFunctionDeclaration,
      final Scope pScope,
      final LogManager pLogger,
      final ASTConverter pAstConverter) {
    this(null, pJSFunctionDeclaration, pScope, pLogger, pAstConverter);
  }

  CFAFunctionBuilder(
      final FunctionDeclaration pFunctionDeclaration,
      final Scope pScope,
      final LogManager pLogger,
      final ASTConverter pAstConverter) {
    this(
        pFunctionDeclaration,
        pAstConverter.convert(pFunctionDeclaration),
        pScope,
        pLogger,
        pAstConverter);
  }

  /**
   * Add a dummy edge to allow a function call as first statement. Without this edge {@link
   * CFASecondPassBuilder#insertCallEdgesRecursively()} would consider the function call as
   * unreachable.
   */
  private void addFunctionEntryNode() {
    final CFANode nextNode = new CFANode(functionName);
    cfaNodes.put(functionName, nextNode);
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge("", FileLocation.DUMMY, prevNode, nextNode, "Function start dummy edge"),
        logger);
    prevNode = nextNode;
  }

  /**
   * Process the passed function declaration. If the same function declaration is passed that has
   * been passed to the constructor, its children will be visited by this builder. If another
   * function declaration is passed then it is handled as a function declaration within the function
   * declaration that has been passed to the constructor.
   *
   * @param node The function declaration that has been passed to the constructor or a function
   *     declaration within the function declaration that has been passed to the constructor. You
   *     must not pass the function declaration of this builder. Pass it only to the constructor.
   * @return Only visit the children of the passed function declaration node if it is the same that
   *     has been passed to the constructor. Otherwise (i.e. a nested function declaration is
   *     passed), a new local builder is created for this function declaration that handles them.
   */
  @Override
  public boolean visit(final FunctionDeclaration node) {
    if (node.equals(functionDeclaration)) {
      return true;
    }
    final CFAFunctionBuilder innerFunctionBuilder =
        new CFAFunctionBuilder(node, scope, logger, astConverter);
    node.accept(innerFunctionBuilder);
    final ParseResult innerFunctionBuilderCFA = innerFunctionBuilder.createCFA();
    cfas.putAll(innerFunctionBuilderCFA.getFunctions());
    cfaNodes.putAll(innerFunctionBuilderCFA.getCFANodes());
    globalDeclarations.addAll(innerFunctionBuilderCFA.getGlobalDeclarations());
    return false;
  }

  @Override
  public boolean visit(final FunctionInvocation node) {
    final CFABuilder builder = new CFABuilder(logger, astConverter, functionName, prevNode);
    new FunctionInvocationCFABuilder(builder).append(node);

    final ParseResult builderParseResult = builder.getParseResult();
    cfas.putAll(builderParseResult.getFunctions());
    cfaNodes.putAll(builderParseResult.getCFANodes());
    globalDeclarations.addAll(builderParseResult.getGlobalDeclarations());

    prevNode = builder.getExitNode();

    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationStatement node) {
    final CFABuilder builder = new CFABuilder(logger, astConverter, functionName, prevNode);
    final VariableDeclarationStatementCFABuilder varBuilder =
        new VariableDeclarationStatementCFABuilder(builder);
    varBuilder.append(node);

    final ParseResult builderParseResult = builder.getParseResult();
    cfas.putAll(builderParseResult.getFunctions());
    cfaNodes.putAll(builderParseResult.getCFANodes());
    globalDeclarations.addAll(builderParseResult.getGlobalDeclarations());

    prevNode = builder.getExitNode();

    return false;
  }

  ParseResult createCFA() {
    // TODO do not add this edge if return has been called
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge("", FileLocation.DUMMY, prevNode, exitNode, "default return"), logger);
    return new ParseResult(cfas, cfaNodes, globalDeclarations, Language.JAVASCRIPT);
  }
}
