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
package org.sosy_lab.cpachecker.cfa.parser.timedautomata;

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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaBinaryVariableExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaLiteralValueExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaLiteralVariableExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.AutomatonDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.BinaryVariableExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.GotoDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.GuardDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.InitialConfigDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.InvariantDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.ModuleSpecificationContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.OperatorContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.ResetDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.StateDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.TransitionDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser.VariableConditionContext;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParserBaseVisitor;

class TCFABuilder extends TaGrammarParserBaseVisitor<Object> {
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
    List<TaVariableExpression> expressions = new ArrayList<>(pCtx.expressions.size());
    for (var expression : pCtx.expressions) {
      expressions.add((TaVariableExpression) visit(expression));
    }

    return new TaVariableCondition(getFileLocation(pCtx), expressions);
  }

  @Override
  public Object visitBinaryVariableExpression(BinaryVariableExpressionContext pCtx) {
    var fileLocation = getFileLocation(pCtx);
    var variable = new TaIdExpression(getFileLocation(pCtx.var), pCtx.var.getText());
    var constantValue = new BigInteger(pCtx.constant.getText());
    var constant = new TaLiteralValueExpression(getFileLocation(pCtx.constant), constantValue);
    var operator = (TaBinaryVariableExpression.BinaryOperator) visit(pCtx.operator());
    return new TaBinaryVariableExpression(fileLocation, variable, constant, operator);
  }

  @Override
  public Object visitOperator(OperatorContext pCtx) {
    if (pCtx.EQUAL() != null) {
      return TaBinaryVariableExpression.BinaryOperator.EQUALS;
    }
    if (pCtx.LESS() != null) {
      return TaBinaryVariableExpression.BinaryOperator.LESS_THAN;
    }
    if (pCtx.GREATER() != null) {
      return TaBinaryVariableExpression.BinaryOperator.GREATER_THAN;
    }
    if (pCtx.GREATEREQUAL() != null) {
      return TaBinaryVariableExpression.BinaryOperator.GREATER_EQUAL;
    }
    if (pCtx.LESSEQUAL() != null) {
      return TaBinaryVariableExpression.BinaryOperator.LESS_EQUAL;
    }
    return null;
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

    // add unique initial state with edges to all initial states
    var exitNode = new FunctionExitNode(currentDeclaration);
    var entryNode = new TCFAEntryNode(FileLocation.DUMMY, exitNode, currentDeclaration);
    for (var node : nodesByAutomaton.get(automatonName)) {
      if (((TCFANode) node).isInitialState()) {
        var edge = new BlankEdge("", FileLocation.DUMMY, entryNode, node, "initial dummy edge");
        entryNode.addLeavingEdge(edge);
        node.addEnteringEdge(edge);
      }
    }
    nodesByAutomaton.put(automatonName, entryNode);
    nodesByAutomaton.put(automatonName, exitNode);
    entryNodesByAutomaton.put(automatonName, entryNode);

    parsedNodesByName.clear();
    return null;
  }

  @Override
  public Object visitStateDefinition(StateDefinitionContext pCtx) {
    var stateName = pCtx.name.getText();
    TaVariableCondition invariant = null;
    if (pCtx.invariantDefinition() == null) {
      var alwaysTrueExpression = new TaLiteralVariableExpression(getFileLocation(pCtx), true);
      invariant = new TaVariableCondition(getFileLocation(pCtx), alwaysTrueExpression);
    } else {
      invariant = (TaVariableCondition) visit(pCtx.invariantDefinition());
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
    TaVariableCondition guard = null;
    if (pCtx.guardDefinition() == null) {
      var alwaysTrueExpression = new TaLiteralVariableExpression(getFileLocation(pCtx), true);
      guard = new TaVariableCondition(getFileLocation(pCtx), alwaysTrueExpression);
    } else {
      guard = (TaVariableCondition) visit(pCtx.guardDefinition());
    }

    Set<TaIdExpression> variablesToReset = null;
    if (pCtx.resetDefinition() == null) {
      variablesToReset = new HashSet<>();
    } else {
      variablesToReset = new HashSet<>(pCtx.resetDefinition().IDENTIFIER().size());
      for (var identifier : pCtx.resetDefinition().vars) {
        var idExpr = new TaIdExpression(getFileLocation(identifier), identifier.getText());
        variablesToReset.add(idExpr);
      }
    }

    TCFANode source = currentSourceNode;
    TCFANode target = (TCFANode) visit(pCtx.gotoDefinition());

    var edge = new TCFAEdge(getFileLocation(pCtx), source, target, guard, variablesToReset);
    source.addLeavingEdge(edge);
    target.addEnteringEdge(edge);

    return edge;
  }

  @Override
  public Object visitGuardDefinition(GuardDefinitionContext pCtx) {
    return visit(pCtx.variableCondition());
  }

  @Override
  public Object visitResetDefinition(ResetDefinitionContext pCtx) {
    Set<TaIdExpression> variablesToReset = new HashSet<>(pCtx.IDENTIFIER().size());
    for (var identifier : pCtx.vars) {
      var idExpr = new TaIdExpression(getFileLocation(identifier), identifier.getText());
      variablesToReset.add(idExpr);
    }
    return variablesToReset;
  }

  @Override
  public Object visitGotoDefinition(GotoDefinitionContext pCtx) {
    return parsedNodesByName.get(pCtx.state.getText());
  }
}
