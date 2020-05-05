/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.cta;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAExitNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.AutomatonDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.BinaryVariableExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.FalseExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.GotoDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.GuardDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.InitialConfigDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.InvariantDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.ModuleSpecificationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.StateDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.TransitionDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.TrueExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableConditionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParserBaseVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

class TCFABuilder extends CTAGrammarParserBaseVisitor<Object> {
  private Set<String> initialStates;
  private Map<String, TCFANode> parsedNodesByName;
  private TCFANode currentSourceNode;
  private SortedSetMultimap<String, CFANode> nodesByAutomaton;
  NavigableMap<String, FunctionEntryNode> entryNodesByAutomaton;
  private TaDeclaration currentDeclaration;

  private String fileName;

  public TCFABuilder() {
    initialStates = new HashSet<>();
    parsedNodesByName = new HashMap<>();
    nodesByAutomaton = TreeMultimap.create();
    entryNodesByAutomaton = new TreeMap<>();
  }

  public void setFileName(String pFileName) {
    fileName = pFileName;
  }

  public SortedSetMultimap<String, CFANode> getNodesByAutomatonMap() {
    return nodesByAutomaton;
  }

  public NavigableMap<String, FunctionEntryNode> getEntryNodesByAutomatonMap() {
    return entryNodesByAutomaton;
  }

  private FileLocation getFileLocation(ParserRuleContext ctx) {
    if (ctx != null && fileName != null) {
      return FileLocation.DUMMY;
    }
    return FileLocation.DUMMY;
  }

  private FileLocation getFileLocation(Token token) {
    if (token != null) {
      return FileLocation.DUMMY;
    }
    return FileLocation.DUMMY;
  }

  @Override
  public Object visitModuleSpecification(ModuleSpecificationContext pCtx) {
    if (pCtx.initialConfigDefinition() != null) {
      visit(pCtx.initialConfigDefinition());
    }
    if (pCtx.automatonDefinition() != null) {
      visit(pCtx.automatonDefinition());
    }
    return null;
  }

  @Override
  public Object visitInitialConfigDefinition(InitialConfigDefinitionContext pCtx) {
    pCtx.stateNames.forEach(name -> initialStates.add(name.getText()));

    return null;
  }

  @Override
  public Object visitVariableCondition(VariableConditionContext pCtx) {
    List<AExpression> expressions = new ArrayList<>(pCtx.expressions.size());
    for (var expression : pCtx.expressions) {
      expressions.add((AExpression) visit(expression));
    }

    return createConjunctExpression(getFileLocation(pCtx), expressions);
  }

  @Override
  public Object visitBinaryVariableExpression(BinaryVariableExpressionContext pCtx) {
    var fileLocation = getFileLocation(pCtx);
    var variable = createIdExpression(getFileLocation(pCtx.var), pCtx.var.getText());
    var constant = createLiteralExpression(getFileLocation(pCtx.constant), pCtx.constant.getText());
    var operator = getOperatorFromString(pCtx.operator().getText());

    return createBinaryExpression(fileLocation, variable, constant, operator);
  }

  @Override
  public Object visitTrueExpression(TrueExpressionContext ctx) {
    return createAlwaysTrueExpression(getFileLocation(ctx));
  }

  @Override
  public Object visitFalseExpression(FalseExpressionContext ctx) {
    return createLiteralExpression(getFileLocation(ctx), "0");
  }

  @Override
  public Object visitAutomatonDefinition(AutomatonDefinitionContext pCtx) {
    var automatonName = pCtx.IDENTIFIER().getText();
    currentDeclaration = new TaDeclaration(getFileLocation(pCtx), automatonName);

    // first parse all the nodes, before edges can be parsed
    for (var stateDefinition : pCtx.stateDefinition()) {
      var newNode = (TCFANode) visit(stateDefinition);
      nodesByAutomaton.put(automatonName, newNode);
      parsedNodesByName.put(newNode.getName(), newNode);
    }

    // process the edges which are defined by their source states
    for (var stateDefinition : pCtx.stateDefinition()) {
      currentSourceNode = parsedNodesByName.get(stateDefinition.name.getText());
      for (var transitionDefinition : stateDefinition.transitionDefinition()) {
        visit(transitionDefinition);
      }
    }

    // unique entry and exit nodes are required for CFAs:
    var exitNode = new TCFAExitNode(currentDeclaration);
    var entryNode = new TCFAEntryNode(FileLocation.DUMMY, exitNode, currentDeclaration);
    exitNode.setEntryNode(entryNode);

    // add edges from entry node to all initial states
    for (var node : nodesByAutomaton.get(automatonName)) {
      if (((TCFANode) node).isInitialState()) {
        var edge = new BlankEdge("", FileLocation.DUMMY, entryNode, node, "initial dummy edge");
        entryNode.addLeavingEdge(edge);
        node.addEnteringEdge(edge);
      }
    }
    nodesByAutomaton.put(automatonName, entryNode);
    entryNodesByAutomaton.put(automatonName, entryNode);

    // add dummy edges from each state to the exit node
    for (var node : nodesByAutomaton.get(automatonName)) {
      var edge = new BlankEdge("", FileLocation.DUMMY, node, exitNode, "dummy edge");
      node.addLeavingEdge(edge);
      exitNode.addEnteringEdge(edge);
    }
    nodesByAutomaton.put(automatonName, exitNode);

    parsedNodesByName.clear();
    return null;
  }

  @Override
  public Object visitStateDefinition(StateDefinitionContext pCtx) {
    var stateName = pCtx.name.getText();
    AExpression invariant = null;
    if (pCtx.invariantDefinition() == null) {
      invariant = createAlwaysTrueExpression(getFileLocation(pCtx));
    } else {
      invariant = (AExpression) visit(pCtx.invariantDefinition());
    }

    return new TCFANode(
        stateName, invariant, currentDeclaration, initialStates.contains(stateName));
  }

  @Override
  public Object visitInvariantDefinition(InvariantDefinitionContext pCtx) {
    return visit(pCtx.variableCondition());
  }

  @Override
  public Object visitTransitionDefinition(TransitionDefinitionContext pCtx) {
    AExpression guard = null;
    var fileLocation = getFileLocation(pCtx);
    if (pCtx.guardDefinition() == null) {
      guard = createAlwaysTrueExpression(fileLocation);
    } else {
      var gd = pCtx.guardDefinition();
      guard = (AExpression) visit(gd);
    }

    Set<AIdExpression> variablesToReset = null;
    if (pCtx.resetDefinition() == null) {
      variablesToReset = new HashSet<>();
    } else {
      variablesToReset = new HashSet<>(pCtx.resetDefinition().IDENTIFIER().size());
      for (var identifier : pCtx.resetDefinition().vars) {
        var idExpr = createIdExpression(getFileLocation(identifier), identifier.getText());
        variablesToReset.add(idExpr);
      }
    }

    TCFANode source = currentSourceNode;
    TCFANode target = (TCFANode) visit(pCtx.gotoDefinition());

    var edge = new TCFAEdge(fileLocation, source, target, guard, variablesToReset);
    source.addLeavingEdge(edge);
    target.addEnteringEdge(edge);

    return edge;
  }

  @Override
  public Object visitGuardDefinition(GuardDefinitionContext pCtx) {
    return visit(pCtx.variableCondition());
  }

  @Override
  public Object visitGotoDefinition(GotoDefinitionContext pCtx) {
    return parsedNodesByName.get(pCtx.state.getText());
  }

  private static AExpression createConjunctExpression(
      FileLocation pFileLocation, List<AExpression> expressions) {
    if (expressions.size() == 0) {
      return createAlwaysTrueExpression(pFileLocation);
    }

    if (expressions.size() == 1) {
      return expressions.get(0);
    }

    AExpression result = expressions.get(0);
    for (var expr : expressions) {
      result = createBinaryExpression(pFileLocation, result, expr, BinaryOperator.BINARY_AND);
    }

    return result;
  }

  private static CBinaryExpression createBinaryExpression(
      FileLocation pFileLocation, AExpression op1, AExpression op2, BinaryOperator operator) {
    return new CBinaryExpression(
        pFileLocation,
        createDummyType(),
        createDummyType(),
        (CExpression) op1,
        (CExpression) op2,
        operator);
  }

  private static CType createDummyType() {
    return CVoidType.create(false, false);
  }

  private static CIdExpression createIdExpression(FileLocation pFileLocation, String name) {
    return new CIdExpression(pFileLocation, createDummyType(), name, null);
  }

  private static CExpression createAlwaysTrueExpression(FileLocation pFileLocation) {
    return createLiteralExpression(pFileLocation, "1");
  }

  private static CIntegerLiteralExpression createLiteralExpression(
      FileLocation pFileLocation, String rawValue) {
    var value = new BigInteger(rawValue);
    return new CIntegerLiteralExpression(pFileLocation, createDummyType(), value);
  }

  private static BinaryOperator getOperatorFromString(String op) {
    switch (op) {
      case "<":
        return BinaryOperator.LESS_THAN;
      case "<=":
        return BinaryOperator.LESS_EQUAL;
      case ">":
        return BinaryOperator.GREATER_THAN;
      case ">=":
        return BinaryOperator.GREATER_EQUAL;
      case "==":
        return BinaryOperator.EQUALS;
      default:
        return BinaryOperator.EQUALS;
    }
  }
}
