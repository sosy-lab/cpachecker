// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParser.AntlrToInternalNotImplementedException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.AbstractDeclaratorContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.BuiltInLogicTypeContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.LogicTypeExprContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.SpecifierQualifierListContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TypeExprContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TypeNameContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TypeSpecifierContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TypeVarContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TypedefNameContext;

public class AntrlTypeExpressionToTypeConverter extends AntlrToInternalAbstractConverter<AcslType> {
  protected AntrlTypeExpressionToTypeConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslType visitBuiltInLogicType(BuiltInLogicTypeContext ctx) {
    return AcslBuiltinLogicType.of(ctx.getText());
  }

  @Override
  public AcslType visitTypeVar(TypeVarContext ctx) {
    String typeName = ctx.getText();
    AcslType type = Objects.requireNonNull(getAcslScope().lookupType(typeName));
    return type;
  }

  @Override
  public AcslType visitTypedefName(TypedefNameContext ctx) {
    return getAcslScope().lookupTypedef(ctx.getText());
  }

  @Override
  public AcslType visitTypeSpecifier(TypeSpecifierContext ctx) {
    TypedefNameContext typedefNameContext = ctx.typedefName();
    if (typedefNameContext != null) {
      return typedefNameContext.accept(this);
    }

    throw new AntlrToInternalNotImplementedException(
        "TypeSpecifierContext is not fully implemented yet");
  }

  @Override
  public AcslType visitSpecifierQualifierList(SpecifierQualifierListContext ctx) {
    if (ctx.getChildCount() == 1) {
      TypeSpecifierContext typeSpecifierContext = ctx.typeSpecifier();
      if (typeSpecifierContext != null) {
        return typeSpecifierContext.accept(this);
      }
    }

    throw new AntlrToInternalNotImplementedException(
        "SpecifierQualifierListContext is not fully implemented yet");
  }

  @Override
  public AcslType visitTypeName(TypeNameContext ctx) {
    AcslType nestedType = ctx.getChild(0).accept(this);
    AbstractDeclaratorContext abstractDeclaratorContext = ctx.abstractDeclarator();

    if (abstractDeclaratorContext == null) {
      return nestedType;
    }

    // One child means we are restricted to a pointer
    if (abstractDeclaratorContext.getChildCount() == 1
        && abstractDeclaratorContext.pointer() != null) {
      return new AcslPointerType(nestedType);
    }

    throw new AntlrToInternalNotImplementedException(
        "AbstractDeclaratorContext is not fully implemented yet");
  }

  @Override
  public AcslType visitLogicTypeExpr(LogicTypeExprContext ctx) {
    if (ctx.getChildCount() == 1) {
      return ctx.getChild(0).accept(this);
    }

    throw new AntlrToInternalNotImplementedException(
        "LogicTypeExprContext is not fully implemented yet");
  }

  @Override
  public AcslType visitTypeExpr(TypeExprContext ctx) {
    if (ctx.getChildCount() == 1) {
      return ctx.getChild(0).accept(this);
    }

    throw new AntlrToInternalNotImplementedException(
        "TypeExprContext is not fully implemented yet");
  }
}
