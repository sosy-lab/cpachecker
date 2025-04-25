// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.LogicFunctionDefContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ParametersContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.PolyIdContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TypeVarBindersContext;

public class AntlrLogicalDefinitionToLogicalDefinitionConverter
    extends AntlrToInternalAbstractConverter<AcslLogicDefinition> {
  private final AntlrTermToTermConverter antlrTermToTermConverter;
  private final AntrlTypeExpressionToTypeConverter antrlTypeExpressionToTypeConverter;

  protected AntlrLogicalDefinitionToLogicalDefinitionConverter(AcslScope pAcslScope) {
    super(CProgramScope.empty(), pAcslScope);
    antlrTermToTermConverter = new AntlrTermToTermConverter(getCProgramScope(), getAcslScope());
    antrlTypeExpressionToTypeConverter =
        new AntrlTypeExpressionToTypeConverter(getCProgramScope(), getAcslScope());
  }

  @Override
  public AcslLogicDefinition visitLogicFunctionDef(LogicFunctionDefContext ctx) {
    // The context has the form:
    // [typeExpr, polymorphicId, parameters, '=', term]

    // Solving this through a visitor would be nicer, but seems like overkill
    PolyIdContext polyIdContext = ctx.polyId();
    String functionName = polyIdContext.id().getText();
    TypeVarBindersContext typeVarBindersContext = polyIdContext.typeVarBinders();
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
                        FileLocation.DUMMY,
                        false,
                        new AcslPolymorphicType(typeName),
                        typeName,
                        typeName);
                  })
              .toList();
    }

    for (AcslTypeVariableDeclaration polymorphicType : polymorphicTypes) {
      if (!getAcslScope().registerTypeDeclaration(polymorphicType)) {
        throw new RuntimeException("Polymorphic type " + polymorphicType + " already exists");
      }
    }

    AcslType returnType = antrlTypeExpressionToTypeConverter.visit(ctx.getChild(0));

    // Solving this through a visitor would be nicer, but seems like overkill
    ParametersContext parametersContext = ctx.parameters();
    List<@NonNull AcslParameterDeclaration> parameters =
        FluentIterable.from(parametersContext.children)
            .filter(
                elem ->
                    !(Objects.equals(elem.getText(), ",")
                        || Objects.equals(elem.getText(), "(")
                        || Objects.equals(elem.getText(), ")")))
            .transform(
                parameter -> {
                  String variableName = parameter.getChild(1).getText();
                  AcslType type = antrlTypeExpressionToTypeConverter.visit(parameter.getChild(0));
                  return new AcslParameterDeclaration(FileLocation.DUMMY, type, variableName);
                })
            .toList();

    AcslFunctionDeclaration functionDeclaration =
        new AcslFunctionDeclaration(
            FileLocation.DUMMY,
            new AcslFunctionType(
                returnType,
                FluentIterable.from(parameters)
                    .transform(AcslParameterDeclaration::getType)
                    .toList(),
                false),
            functionName,
            functionName,
            polymorphicTypes,
            parameters);

    for (AcslParameterDeclaration parameter : parameters) {
      getAcslScope().registerDeclaration(parameter);
    }

    getAcslScope().registerDeclaration(functionDeclaration);

    AcslTerm body = antlrTermToTermConverter.visit(ctx.getChild(4));
    return new AcslLogicFunctionDefinition(FileLocation.DUMMY, functionDeclaration, body);
  }
}
