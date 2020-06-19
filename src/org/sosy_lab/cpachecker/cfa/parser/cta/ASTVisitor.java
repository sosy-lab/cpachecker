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
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.AutomatonDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.InitialConfigDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.ModuleInstantiationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.ModuleSpecificationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.NumericVariableExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.OperatorContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.ParametricVariableExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.ResetDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.SpecificationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.StateDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.TransitionDefinitionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableConditionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableDeclarationContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableDeclarationGroupContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableExpressionContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableTypeContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser.VariableVisibilityQualifierContext;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParserBaseVisitor;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.AutomatonSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.BooleanCondition;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.BooleanCondition.Operator;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.ModuleInstantiation;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.ModuleSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.StateSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.SystemSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.TransitionSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.VariableSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.VariableSpecification.VariableType;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.VariableSpecification.VariableVisibility;

class ASTVisitor extends CTAGrammarParserBaseVisitor<SystemSpecification> {

  @Override
  public SystemSpecification visitSpecification(SpecificationContext pCtx) {
    var modules =
        pCtx.modules.stream()
            .map(moduleCtx -> moduleCtx.accept(new ModuleSpecificationVisitor()))
            .collect(Collectors.toSet());
    var rootModules = modules.stream().filter(module -> module.isRoot).collect(Collectors.toSet());
    verify(
        rootModules.size() == 1,
        "Invalid number of root modules. Expected 1 but got %s",
        rootModules.size());

    return new SystemSpecification.Builder()
        .modules(modules)
        .instantiation(
            ModuleInstantiation.getDummyInstantiationForModule(rootModules.iterator().next()))
        .build();
  }

  public static class ModuleSpecificationVisitor
      extends CTAGrammarParserBaseVisitor<ModuleSpecification> {
    public static ModuleSpecificationVisitor getInstance() {
      return new ModuleSpecificationVisitor();
    }

    @Override
    public ModuleSpecification visitModuleSpecification(ModuleSpecificationContext pCtx) {
      var moduleName = pCtx.name.getText();
      var isRoot = pCtx.ROOT() != null;
      var initialCondition =
          Optional.fromNullable(pCtx.initialCondition)
              .transform(ic -> ic.accept(new InitialConditionVisitor()));
      var automatonSpecification =
          Optional.fromNullable(pCtx.automaton)
              .transform(
                  automatonSpec -> {
                    verify(
                        initialCondition.isPresent(),
                        "Module %s contains an automaton but no initial state definition.",
                        moduleName);
                    return automatonSpec.accept(
                        new AutomatonSpecificationVisitor(initialCondition.get()));
                  });
      var variables =
          pCtx.variables.stream()
              .flatMap(
                  varGroup -> varGroup.accept(new VariableSpecificationGroupVisitor()).stream())
              .collect(Collectors.toSet());
      var instantiations =
          pCtx.instantiations.stream()
              .map(inst -> inst.accept(new ModuleInstantiationVisitor()))
              .collect(Collectors.toSet());

      var builder = new ModuleSpecification.Builder();
      return builder
          .isRoot(isRoot)
          .moduleName(moduleName)
          .automaton(automatonSpecification)
          .instantiations(instantiations)
          .variables(variables)
          .build();
    }
  }

  private static class AutomatonSpecificationVisitor
      extends CTAGrammarParserBaseVisitor<AutomatonSpecification> {
    private final Set<String> initialStates;

    public AutomatonSpecificationVisitor(Set<String> pInitialStates) {
      initialStates = pInitialStates;
    }

    @Override
    public AutomatonSpecification visitAutomatonDefinition(AutomatonDefinitionContext pCtx) {
      var automatonName = pCtx.name.getText();
      var states =
          pCtx.states.stream()
              .map(
                  state ->
                      state.accept(
                          new StateSpecificationVisitor(
                              initialStates.contains(state.name.getText()))))
              .collect(Collectors.toSet());
      var transitions =
          pCtx.states.stream()
              .flatMap(
                  state -> state.accept(new TransitionFromStateSpecificationVisitor()).stream())
              .collect(Collectors.toSet());

      return new AutomatonSpecification.Builder()
          .automatonName(automatonName)
          .initialStates(initialStates)
          .stateSpecifications(states)
          .transitions(transitions)
          .build();
    }
  }

  private static class InitialConditionVisitor extends CTAGrammarParserBaseVisitor<Set<String>> {
    @Override
    public Set<String> visitInitialConfigDefinition(InitialConfigDefinitionContext pCtx) {
      return pCtx.stateNames.stream().map(Token::getText).collect(Collectors.toSet());
    }
  }

  public static class BooleanConditionVisitor
      extends CTAGrammarParserBaseVisitor<BooleanCondition> {
    private BooleanCondition.Builder builder;

    @Override
    public BooleanCondition visitVariableCondition(VariableConditionContext pCtx) {
      builder = new BooleanCondition.Builder();
      for (var exprCtx : pCtx.expressions) {
        handleExpression(exprCtx);
      }

      return builder.build();
    }

    private void handleExpression(VariableExpressionContext pCtx) {
      if (pCtx instanceof NumericVariableExpressionContext) {
        handleExpression((NumericVariableExpressionContext) pCtx);
      } else {
        handleExpression((ParametricVariableExpressionContext) pCtx);
      }
    }

    private void handleExpression(NumericVariableExpressionContext pCtx) {
      var variable = pCtx.var.getText();
      var operator = getOperator(pCtx.op);
      var constant = new BigDecimal(pCtx.constant.getText());
      builder.expression(variable, operator, constant);
    }

    private void handleExpression(ParametricVariableExpressionContext pCtx) {
      var variable = pCtx.var.getText();
      var operator = getOperator(pCtx.op);
      var constant = pCtx.constant.getText();
      builder.expression(variable, operator, constant);
    }

    private BooleanCondition.Operator getOperator(OperatorContext op) {
      switch (op.getText()) {
        case "<":
          return Operator.LESS;
        case "<=":
          return Operator.LESS_EQUAL;
        case ">":
          return Operator.GREATER;
        case ">=":
          return Operator.GREATER_EQUAL;
        case "=":
          return Operator.EQUAL;
        default:
          throw new VerifyException("Unknown binary operator " + op.getText());
      }
    }
  }

  public static class ModuleInstantiationVisitor
      extends CTAGrammarParserBaseVisitor<ModuleInstantiation> {
    @Override
    public ModuleInstantiation visitModuleInstantiation(ModuleInstantiationContext pCtx) {
      var builder =
          new ModuleInstantiation.Builder()
              .instanceName(pCtx.instanceName.getText())
              .specificationName(pCtx.specificationName.getText());

      for (var variableInstantiation : pCtx.variableInstantiations) {
        builder.variableMapping(
            variableInstantiation.instanceName.getText(), variableInstantiation.specName.getText());
      }

      return builder.build();
    }
  }

  public static class StateSpecificationVisitor
      extends CTAGrammarParserBaseVisitor<StateSpecification> {
    private boolean isInitialState;

    public StateSpecificationVisitor(boolean pIsInitialState) {
      isInitialState = pIsInitialState;
    }

    @Override
    public StateSpecification visitStateDefinition(StateDefinitionContext pCtx) {
      var invariant =
          Optional.fromNullable(pCtx.invariant)
              .transform(inv -> inv.condition.accept(new BooleanConditionVisitor()));
      return new StateSpecification.Builder()
          .name(pCtx.name.getText())
          .invariant(invariant)
          .isInitialState(isInitialState)
          .build();
    }
  }

  public static class TransitionFromStateSpecificationVisitor
      extends CTAGrammarParserBaseVisitor<Set<TransitionSpecification>> {
    @Override
    public Set<TransitionSpecification> visitStateDefinition(StateDefinitionContext pCtx) {
      return pCtx.transitions.stream()
          .map(
              transition ->
                  transition.accept(new TransitionSpecificationVisitor(pCtx.name.getText())))
          .collect(Collectors.toSet());
    }
  }

  public static class TransitionSpecificationVisitor
      extends CTAGrammarParserBaseVisitor<TransitionSpecification> {
    private final String sourceState;

    public TransitionSpecificationVisitor(String pSourceState) {
      sourceState = pSourceState;
    }

    @Override
    public TransitionSpecification visitTransitionDefinition(TransitionDefinitionContext pCtx) {
      var guard =
          Optional.fromNullable(pCtx.guard).transform(g -> g.accept(new BooleanConditionVisitor()));
      var syncMark = Optional.fromNullable(pCtx.syncMark).transform(Token::getText);
      var resetClocks =
          Optional.fromNullable(pCtx.resetDefinition())
              .transform(r -> r.accept(new ResetClocksVisitor()));
      var targetState = pCtx.gotoDefinition().state.getText();

      return new TransitionSpecification.Builder()
          .guard(guard)
          .resetClocks(resetClocks.or(ImmutableSet.of()))
          .source(sourceState)
          .syncMark(syncMark)
          .target(targetState)
          .build();
    }
  }

  public static class ResetClocksVisitor extends CTAGrammarParserBaseVisitor<Set<String>> {
    @Override
    public Set<String> visitResetDefinition(ResetDefinitionContext pCtx) {
      if (pCtx == null){ 
        return new HashSet<>();
      }
      return pCtx.vars.stream().map(Token::getText).collect(Collectors.toSet());
    }
  }

  public static class VariableSpecificationGroupVisitor
      extends CTAGrammarParserBaseVisitor<Set<VariableSpecification>> {
    @Override
    public Set<VariableSpecification> visitVariableDeclarationGroup(
        VariableDeclarationGroupContext pCtx) {
      var visibility = getVisibility(pCtx.visibility);
      return pCtx.declarations.stream()
          .map(declaration -> declaration.accept(new VariableSpecificationVisitor(visibility)))
          .collect(Collectors.toSet());
    }

    private VariableVisibility getVisibility(VariableVisibilityQualifierContext pCtx) {
      if (pCtx.INPUT() != null) {
        return VariableVisibility.INPUT;
      }
      if (pCtx.LOCAL() != null) {
        return VariableVisibility.LOCAL;
      }
      throw new VerifyException(
          "Unsupported variable visiblity: "
              + pCtx.getText()
              + ". Supported types are INPUT, and LOCAL.");
    }
  }

  public static class VariableSpecificationVisitor
      extends CTAGrammarParserBaseVisitor<VariableSpecification> {
    private final VariableVisibility visibility;

    public VariableSpecificationVisitor(VariableVisibility pVisibility) {
      visibility = pVisibility;
    }

    @Override
    public VariableSpecification visitVariableDeclaration(VariableDeclarationContext pCtx) {
      var type = getVariableType(pCtx.type);
      var initialization =
          Optional.fromNullable(pCtx.initialization)
              .transform(initToken -> new BigDecimal(initToken.getText()));
      verify(
          !initialization.isPresent() || type.equals(VariableType.CONST),
          "Initalizations are only allowed for constant variables.");

      return new VariableSpecification.Builder()
          .name(pCtx.name.getText())
          .visibility(visibility)
          .type(type)
          .initialization(initialization.orNull())
          .build();
    }

    private VariableSpecification.VariableType getVariableType(VariableTypeContext pCtx) {
      if (pCtx.CLOCK() != null) {
        return VariableType.CLOCK;
      }
      if (pCtx.CONST() != null) {
        return VariableType.CONST;
      }
      if (pCtx.SYNC() != null) {
        return VariableType.SYNC;
      }
      throw new VerifyException(
          "Unsupported variable type: "
              + pCtx.getText()
              + ". Supported types are CLOCK, CONST and SYNC.");
    }
  }
}
