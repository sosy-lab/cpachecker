// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicFunctionDefContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicPredicateDefContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ParametersContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.PolyIdContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.TypeVarBindersContext;

public class AntlrLogicalDefinitionToLogicalDefinitionConverter
    extends AntlrToInternalAbstractConverter<AcslLogicDefinition> {
  private final AntlrTermToTermConverter antlrTermToTermConverter;
  private final AntrlTypeExpressionToTypeConverter antrlTypeExpressionToTypeConverter;
  private final AntlrPredicateToPredicateConverter antlrPredicateToPredicateConverter;

  protected AntlrLogicalDefinitionToLogicalDefinitionConverter(
      AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    super(CProgramScope.empty(), pAcslScope, pInitialFileLocation);
    antlrTermToTermConverter =
        new AntlrTermToTermConverter(getCProgramScope(), getAcslScope(), pInitialFileLocation);
    antrlTypeExpressionToTypeConverter =
        new AntrlTypeExpressionToTypeConverter(getCProgramScope(), getAcslScope());
    antlrPredicateToPredicateConverter =
        new AntlrPredicateToPredicateConverter(
            getCProgramScope(), getAcslScope(), pInitialFileLocation);
  }

  private String parsePolyIdName(PolyIdContext ctx) {
    return ctx.id().getText();
  }

  private List<@NonNull AcslTypeVariableDeclaration> parsePolyTypes(PolyIdContext ctx) {
    TypeVarBindersContext typeVarBindersContext = ctx.typeVarBinders();
    List<@NonNull AcslTypeVariableDeclaration> polymorphicTypes = ImmutableList.of();
    if (typeVarBindersContext != null) {
      polymorphicTypes =
          FluentIterable.from(typeVarBindersContext.children)
              .skip(1)
              .limit(typeVarBindersContext.getChildCount() - 2)
              .transform(
                  child -> {
                    String typeName = Objects.requireNonNull(child).getText();
                    return new AcslTypeVariableDeclaration(
                        fileLocationFromContext(ctx),
                        false,
                        new AcslPolymorphicType(typeName),
                        typeName,
                        typeName);
                  })
              .toList();
    }
    return polymorphicTypes;
  }

  private List<@NonNull AcslParameterDeclaration> parseParameters(
      ParametersContext ctx, String functionName) {
    List<@NonNull AcslParameterDeclaration> parameters =
        FluentIterable.from(ctx.children)
            .filter(
                elem ->
                    !(Objects.equals(elem.getText(), ",")
                        || Objects.equals(elem.getText(), "(")
                        || Objects.equals(elem.getText(), ")")))
            .transform(
                parameter -> {
                  String variableName = parameter.getChild(1).getText();
                  String qualifiedName = functionName + "::" + variableName;
                  AcslType type = antrlTypeExpressionToTypeConverter.visit(parameter.getChild(0));
                  return new AcslParameterDeclaration(
                      fileLocationFromContext(ctx), type, variableName, qualifiedName);
                })
            .toList();

    return parameters;
  }

  @Override
  public AcslLogicDefinition visitLogicPredicateDef(LogicPredicateDefContext ctx) {
    // The context has the form:
    // [polymorphicId, parameters, '=', pred]

    // Solving this through a visitor would be nicer, but seems like overkill
    PolyIdContext polyIdContext = ctx.polyId();
    List<@NonNull AcslTypeVariableDeclaration> polymorphicTypes = parsePolyTypes(polyIdContext);
    String functionName = parsePolyIdName(polyIdContext);

    for (AcslTypeVariableDeclaration polymorphicType : polymorphicTypes) {
      if (!getAcslScope().registerTypeDeclaration(polymorphicType)) {
        throw new RuntimeException("Polymorphic type " + polymorphicType + " already exists");
      }
    }

    // Solving this through a visitor would be nicer, but seems like overkill
    ParametersContext parametersContext = ctx.parameters();
    List<@NonNull AcslParameterDeclaration> parameters =
        parseParameters(parametersContext, functionName);

    AcslPredicateDeclaration predicateDeclaration =
        new AcslPredicateDeclaration(
            fileLocationFromContext(ctx),
            new AcslPredicateType(
                transformedImmutableListCopy(parameters, AcslParameterDeclaration::getType), false),
            functionName,
            functionName,
            polymorphicTypes,
            parameters);

    for (AcslParameterDeclaration parameter : parameters) {
      getAcslScope().registerDeclaration(parameter);
    }

    getAcslScope().registerDeclaration(predicateDeclaration);

    AcslPredicate body = antlrPredicateToPredicateConverter.visit(ctx.pred());
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

    for (AcslTypeVariableDeclaration polymorphicType : polymorphicTypes) {
      if (!getAcslScope().registerTypeDeclaration(polymorphicType)) {
        throw new RuntimeException("Polymorphic type " + polymorphicType + " already exists");
      }
    }

    AcslType returnType = antrlTypeExpressionToTypeConverter.visit(ctx.typeExpr());

    // Solving this through a visitor would be nicer, but seems like overkill
    ParametersContext parametersContext = ctx.parameters();
    List<@NonNull AcslParameterDeclaration> parameters =
        parseParameters(parametersContext, functionName);

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

    for (AcslParameterDeclaration parameter : parameters) {
      getAcslScope().registerDeclaration(parameter);
    }

    getAcslScope().registerDeclaration(functionDeclaration);

    AcslTerm body = antlrTermToTermConverter.visit(ctx.term());
    return new AcslLogicFunctionDefinition(fileLocationFromContext(ctx), functionDeclaration, body);
  }
}
