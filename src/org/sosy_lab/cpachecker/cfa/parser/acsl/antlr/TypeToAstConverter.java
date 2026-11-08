// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.AbstractDeclaratorContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.BuiltInLogicTypeContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.LogicTypeExprContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.SpecifierQualifierListContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TypeExprContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TypeNameContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TypeSpecifierContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TypeVarContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TypedefNameContext;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

class TypeToAstConverter extends AbstractAntlrToAstConverter<AcslType> {
  TypeToAstConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope, FileLocation.DUMMY);
  }

  @Override
  public AcslType visitBuiltInLogicType(BuiltInLogicTypeContext ctx) {
    return AcslBuiltinLogicType.of(ctx.getText());
  }

  @Override
  public AcslType visitTypeVar(TypeVarContext ctx) {
    String typeName = ctx.getText();
    AcslType type = getTypeForName(typeName);
    return type;
  }

  @Override
  public AcslType visitTypedefName(TypedefNameContext ctx) {
    return getAcslScope().lookupType(ctx.getText());
  }

  @Override
  public AcslType visitTypeSpecifier(TypeSpecifierContext ctx) {
    TypedefNameContext typedefNameContext = ctx.typedefName();
    if (typedefNameContext != null) {
      return typedefNameContext.accept(this);
    }

    throw new AcslParseRuntimeException("TypeSpecifierContext is not fully implemented yet");
  }

  @Override
  public AcslType visitSpecifierQualifierList(SpecifierQualifierListContext ctx) {
    if (ctx.getChildCount() == 1) {
      TypeSpecifierContext typeSpecifierContext = ctx.typeSpecifier();
      if (typeSpecifierContext != null) {
        return typeSpecifierContext.accept(this);
      }
    }

    throw new AcslParseRuntimeException(
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

    throw new AcslParseRuntimeException("AbstractDeclaratorContext is not fully implemented yet");
  }

  @Override
  public AcslType visitLogicTypeExpr(LogicTypeExprContext ctx) {
    if (ctx.getChildCount() == 1) {
      return ctx.getChild(0).accept(this);
    }

    throw new AcslParseRuntimeException("LogicTypeExprContext is not fully implemented yet");
  }

  @Override
  public AcslType visitTypeExpr(TypeExprContext ctx) {
    if (ctx.getChildCount() == 1) {
      return ctx.getChild(0).accept(this);
    }

    throw new AcslParseRuntimeException("TypeExprContext is not fully implemented yet");
  }
}
