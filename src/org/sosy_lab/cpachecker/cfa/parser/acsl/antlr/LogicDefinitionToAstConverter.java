// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LogicFunctionDefContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LogicPredicateDefContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.ParametersContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.PolyIdContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TypeVarBindersContext;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslFunctionType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

class LogicDefinitionToAstConverter extends AbstractAntlrToAstConverter<AcslLogicDefinition> {

  LogicDefinitionToAstConverter(AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    super(CProgramScope.empty(), pAcslScope, pInitialFileLocation);
  }

  /** Returns the scope with the type variables of a definition, in which its types are parsed. */
  private AcslScope scopeWithTypeVariables(List<AcslTypeVariableDeclaration> pTypeVariables) {
    if (transformedImmutableSetCopy(pTypeVariables, AcslTypeVariableDeclaration::getName).size()
        != pTypeVariables.size()) {
      throw new AcslParseRuntimeException("A type variable is declared twice in " + pTypeVariables);
    }
    return getAcslScope().withDeclarations(pTypeVariables);
  }

  private String parsePolyIdName(PolyIdContext ctx) {
    return ctx.id().getText();
  }

  private List<@NonNull AcslTypeVariableDeclaration> parsePolyTypes(PolyIdContext ctx) {
    TypeVarBindersContext typeVarBindersContext = ctx.typeVarBinders();
    if (typeVarBindersContext == null) {
      return ImmutableList.of();
    }
    return FluentIterable.from(typeVarBindersContext.children)
        .skip(1)
        .limit(typeVarBindersContext.getChildCount() - 2)
        .transform(
            child -> {
              String typeName = child.getText();
              return new AcslTypeVariableDeclaration(
                  fileLocationFromContext(ctx),
                  false,
                  new AcslPolymorphicType(typeName),
                  typeName,
                  typeName);
            })
        .toList();
  }

  private List<@NonNull AcslParameterDeclaration> parseParameters(
      ParametersContext ctx, String functionName, TypeToAstConverter typeConverter) {
    return FluentIterable.from(ctx.children)
        .filter(
            elem ->
                !(Objects.equals(elem.getText(), ",")
                    || Objects.equals(elem.getText(), "(")
                    || Objects.equals(elem.getText(), ")")))
        .transform(
            parameter -> {
              String variableName = parameter.getChild(1).getText();
              AcslType type = typeConverter.visit(parameter.getChild(0));
              return new AcslParameterDeclaration(
                  fileLocationFromContext(ctx), type, variableName, functionName);
            })
        .toList();
  }

  @Override
  public AcslLogicDefinition visitLogicPredicateDef(LogicPredicateDefContext ctx) {
    // The context has the form:
    // [polymorphicId, parameters, '=', pred]

    // Solving this through a visitor would be nicer, but seems like overkill
    PolyIdContext polyIdContext = ctx.polyId();
    List<@NonNull AcslTypeVariableDeclaration> polymorphicTypes = parsePolyTypes(polyIdContext);
    String functionName = parsePolyIdName(polyIdContext);

    AcslScope typeScope = scopeWithTypeVariables(polymorphicTypes);

    // Solving this through a visitor would be nicer, but seems like overkill
    ParametersContext parametersContext = ctx.parameters();
    List<@NonNull AcslParameterDeclaration> parameters =
        parseParameters(
            parametersContext, functionName, new TypeToAstConverter(getCProgramScope(), typeScope));

    AcslPredicateDeclaration predicateDeclaration =
        new AcslPredicateDeclaration(
            fileLocationFromContext(ctx),
            new AcslPredicateType(
                transformedImmutableListCopy(parameters, AcslParameterDeclaration::getType), false),
            functionName,
            functionName,
            polymorphicTypes,
            parameters);

    // The parameters and the predicate itself, for recursion, are visible in the body
    AcslPredicate body =
        new PredicateToAstConverter(
                getCProgramScope(),
                typeScope.withDeclarations(parameters).withDeclaration(predicateDeclaration),
                getInitialFileLocation())
            .visit(ctx.pred());
    return new AcslLogicPredicateDefinition(
        fileLocationFromContext(ctx), predicateDeclaration, body);
  }

  @Override
  public AcslLogicDefinition visitLogicFunctionDef(LogicFunctionDefContext ctx) {
    // The context has the form:
    // [typeExpr, polymorphicId, parameters, '=', term]

    // Solving this through a visitor would be nicer, but seems like overkill
    PolyIdContext polyIdContext = ctx.polyId();
    List<@NonNull AcslTypeVariableDeclaration> polymorphicTypes = parsePolyTypes(polyIdContext);
    String functionName = parsePolyIdName(polyIdContext);

    AcslScope typeScope = scopeWithTypeVariables(polymorphicTypes);
    TypeToAstConverter typeConverter = new TypeToAstConverter(getCProgramScope(), typeScope);

    AcslType returnType = typeConverter.visit(ctx.typeExpr());

    // Solving this through a visitor would be nicer, but seems like overkill
    ParametersContext parametersContext = ctx.parameters();
    List<@NonNull AcslParameterDeclaration> parameters =
        parseParameters(parametersContext, functionName, typeConverter);

    AcslFunctionDeclaration functionDeclaration =
        new AcslFunctionDeclaration(
            fileLocationFromContext(ctx),
            new AcslFunctionType(
                returnType,
                transformedImmutableListCopy(parameters, AcslParameterDeclaration::getType),
                false),
            functionName,
            functionName,
            polymorphicTypes,
            parameters);

    // The parameters and the function itself, for recursion, are visible in the body
    AcslTerm body =
        new TermToAstConverter(
                getCProgramScope(),
                typeScope.withDeclarations(parameters).withDeclaration(functionDeclaration),
                getInitialFileLocation())
            .visit(ctx.term());
    return new AcslLogicFunctionDefinition(fileLocationFromContext(ctx), functionDeclaration, body);
  }
}
