// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta;

import static com.google.common.base.Verify.verify;

import com.google.common.base.Optional;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.math.BigDecimal;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAExitNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.AutomatonDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.BinaryVariableExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.GotoDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.GuardDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.InitialConfigDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.InvariantDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.ModuleSpecificationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.StateDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.TransitionDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableConditionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableDeclarationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParserBaseVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

class TCFABuilder extends CTAGrammarParserBaseVisitor<Object> {
  private SortedSetMultimap<String, CFANode> nodesByAutomaton;
  private NavigableMap<String, FunctionEntryNode> entryNodesByAutomaton;
  private TCFANode currentTransitionSourceNode;
  private Map<String, TCFANode> currentParsedNodesByName;
  private TaDeclaration currentDeclaration;
  private Map<String, CIdExpression> currentIdExpressionsByVariableName;
  private Set<String> currentClocksOfModule;
  private Set<String> currentActionsOfModule;
  private Set<String> currentInitialStates;

  private String fileName;

  public TCFABuilder() {
    currentInitialStates = new HashSet<>();
    currentParsedNodesByName = new HashMap<>();
    nodesByAutomaton = TreeMultimap.create();
    entryNodesByAutomaton = new TreeMap<>();
    currentIdExpressionsByVariableName = new HashMap<>();
    currentClocksOfModule = new HashSet<>();
    currentActionsOfModule = new HashSet<>();
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
    if (ctx != null) {
      return new FileLocation(fileName, 0, 0, 0, 0);
    }
    return new FileLocation(fileName, 0, 0, 0, 0);
  }

  private FileLocation getFileLocation(Token token) {
    if (token != null) {
      return new FileLocation(fileName, 0, 0, 0, 0);
    }
    return new FileLocation(fileName, 0, 0, 0, 0);
  }

  @Override
  public Object visitModuleSpecification(ModuleSpecificationContext pCtx) {
    currentClocksOfModule = new HashSet<>();
    currentActionsOfModule = new HashSet<>();
    currentInitialStates = new HashSet<>();

    if (pCtx.initialConfigDefinition() != null) {
      visit(pCtx.initialConfigDefinition());
    }
    if (pCtx.variableDeclarationGroup(0) != null) {
      visit(pCtx.variableDeclarationGroup(0));
    }
    if (pCtx.automatonDefinition() != null) {
      visit(pCtx.automatonDefinition());
    }

    return null;
  }

  @Override
  public Object visitInitialConfigDefinition(InitialConfigDefinitionContext pCtx) {
    pCtx.stateNames.forEach(name -> currentInitialStates.add(name.getText()));

    return null;
  }

  @Override
  public Object visitVariableCondition(VariableConditionContext pCtx) {
    List<CExpression> expressions = new ArrayList<>(pCtx.expressions.size());
    for (var expression : pCtx.expressions) {
      expressions.add((CExpression) visit(expression));
    }

    var result = new TaVariableCondition(getFileLocation(pCtx), expressions);
    return result;
  }

  @Override
  public Object visitBinaryVariableExpression(BinaryVariableExpressionContext pCtx) {
    var fileLocation = getFileLocation(pCtx);
    var variable = createIdExpression(getFileLocation(pCtx.var), pCtx.var.getText());
    var constant =
        createFloatLiteralExpression(getFileLocation(pCtx.constant), pCtx.constant.getText());
    var operator = getOperatorFromString(pCtx.operator().getText());

    return createBinaryExpression(fileLocation, variable, constant, operator);
  }

  @Override
  public Object visitVariableDeclaration(VariableDeclarationContext ctx) {
    if (ctx.type.SYNC() != null) {
      currentActionsOfModule.add(ctx.name.getText());
    } else {
      currentClocksOfModule.add(ctx.name.getText());
    }
    return null;
  }

  @Override
  public Object visitAutomatonDefinition(AutomatonDefinitionContext pCtx) {
    currentParsedNodesByName = new HashMap<>();
    currentIdExpressionsByVariableName = new HashMap<>();

    var automatonName = pCtx.IDENTIFIER().getText();
    currentDeclaration =
        new TaDeclaration(
            getFileLocation(pCtx), automatonName, currentClocksOfModule, currentActionsOfModule);

    // unique entry and exit nodes are required for CFAs:
    var exitNode = new TCFAExitNode(currentDeclaration);
    var entryNode = new TCFAEntryNode(FileLocation.DUMMY, exitNode, currentDeclaration);
    exitNode.setEntryNode(entryNode);
    entryNodesByAutomaton.put(automatonName, entryNode);
    var edge = new BlankEdge("", FileLocation.DUMMY, entryNode, exitNode, "dummy edge");
    entryNode.addLeavingEdge(edge);
    exitNode.addEnteringEdge(edge);
    nodesByAutomaton.put(automatonName, exitNode); // exit node needs to be first
    nodesByAutomaton.put(automatonName, entryNode);

    // first parse all the nodes, before edges can be parsed
    for (var stateDefinition : pCtx.stateDefinition()) {
      var newNode = (TCFANode) visit(stateDefinition);
      nodesByAutomaton.put(automatonName, newNode);
      currentParsedNodesByName.put(newNode.getName(), newNode);
    }

    // process the edges which are defined by their source states
    for (var stateDefinition : pCtx.stateDefinition()) {
      currentTransitionSourceNode = currentParsedNodesByName.get(stateDefinition.name.getText());
      for (var transitionDefinition : stateDefinition.transitionDefinition()) {
        visit(transitionDefinition);
      }
    }

    // add edges from entry node to all initial states
    for (var entry : currentParsedNodesByName.entrySet()) {
      var node = entry.getValue();
      if (node.isInitialState()) {
        edge = new BlankEdge("", FileLocation.DUMMY, entryNode, node, "initial dummy edge");
        entryNode.addLeavingEdge(edge);
        node.addEnteringEdge(edge);
      }
    }

    return null;
  }

  @Override
  public Object visitStateDefinition(StateDefinitionContext pCtx) {
    var stateName = pCtx.name.getText();
    Optional<TaVariableCondition> invariant;
    if (pCtx.invariantDefinition() == null) {
      invariant = Optional.absent();
    } else {
      invariant = Optional.of((TaVariableCondition) visit(pCtx.invariantDefinition()));
    }

    return new TCFANode(
        stateName, invariant, currentDeclaration, currentInitialStates.contains(stateName));
  }

  @Override
  public Object visitInvariantDefinition(InvariantDefinitionContext pCtx) {
    return visit(pCtx.variableCondition());
  }

  @Override
  public Object visitTransitionDefinition(TransitionDefinitionContext pCtx) {
    var fileLocation = getFileLocation(pCtx);
    Optional<TaVariableCondition> guard;
    if (pCtx.guardDefinition() == null) {
      guard = Optional.absent();
    } else {
      guard = Optional.of((TaVariableCondition) visit(pCtx.guardDefinition()));
    }

    Set<CIdExpression> variablesToReset = null;
    if (pCtx.resetDefinition() == null) {
      variablesToReset = new HashSet<>();
    } else {
      variablesToReset = new HashSet<>(pCtx.resetDefinition().IDENTIFIER().size());
      for (var identifier : pCtx.resetDefinition().vars) {
        variablesToReset.add(createIdExpression(fileLocation, identifier.getText()));
      }
    }

    TCFANode source = currentTransitionSourceNode;
    TCFANode target = (TCFANode) visit(pCtx.gotoDefinition());

    Optional<String> action = Optional.absent();
    if (pCtx.syncMark != null) {
      action = Optional.of(pCtx.syncMark.getText());
      verify(
          currentActionsOfModule.contains(action.get()),
          "Sync action '%s' appears on a transition but is not declared in the module.",
          action.get());
    }

    var edge = new TCFAEdge(fileLocation, source, target, guard, variablesToReset, action);
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
    verify(
        currentParsedNodesByName.containsKey(pCtx.state.getText()),
        "Target state '%s' does not exist",
        pCtx.state.getText());
    return currentParsedNodesByName.get(pCtx.state.getText());
  }

  private static CBinaryExpression createBinaryExpression(
      FileLocation pFileLocation, AExpression op1, AExpression op2, BinaryOperator operator) {
    return new CBinaryExpression(
        pFileLocation,
        CNumericTypes.BOOL,
        CNumericTypes.BOOL,
        (CExpression) op1,
        (CExpression) op2,
        operator);
  }

  private static CType createFloatType() {
    return CNumericTypes.LONG_DOUBLE;
  }

  private CIdExpression createIdExpression(FileLocation pFileLocation, String name) {
    verify(
        currentClocksOfModule.contains(name),
        "Clock variable '%s' appears in expression but is not declared in the module.",
        name);
    if (!currentIdExpressionsByVariableName.containsKey(name)) {
      var declaration =
          new CVariableDeclaration(
              pFileLocation, false, CStorageClass.AUTO, createFloatType(), name, name, name, null);
      var idExpression = new CIdExpression(pFileLocation, createFloatType(), name, declaration);
      currentIdExpressionsByVariableName.put(name, idExpression);
    }

    return currentIdExpressionsByVariableName.get(name);
  }

  private static CFloatLiteralExpression createFloatLiteralExpression(
      FileLocation pFileLocation, String rawValue) {
    var value = new BigDecimal(rawValue);
    return new CFloatLiteralExpression(pFileLocation, createFloatType(), value);
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
