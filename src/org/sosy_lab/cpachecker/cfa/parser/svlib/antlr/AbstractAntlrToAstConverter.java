// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import java.nio.file.Path;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibBaseVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.IdentifierContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Qual_identiferContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SymbolContext;

class AbstractAntlrToAstConverter<T> extends SvLibBaseVisitor<T> {

  protected SvLibScope scope;
  private final Optional<Path> filePath;

  public AbstractAntlrToAstConverter(SvLibScope pScope, Path pFilePath) {
    scope = pScope;
    filePath = Optional.of(pFilePath);
  }

  public AbstractAntlrToAstConverter(SvLibScope pScope) {
    scope = pScope;
    filePath = Optional.empty();
  }

  protected FileLocation fileLocationFromContext(ParserRuleContext pContext) {
    return new FileLocation(
        filePath.orElse(Path.of("#none#")),
        pContext.getStart().getStartIndex(),
        pContext.getStop().getStartIndex() - pContext.getStart().getStartIndex(),
        pContext.getStart().getLine(),
        pContext.getStop().getLine(),
        pContext.getStart().getCharPositionInLine(),
        pContext.getStop().getCharPositionInLine());
  }

  protected FileLocation fileLocationFromContext(
      ParserRuleContext pInitialContext, ParserRuleContext pFinalContext) {
    return new FileLocation(
        filePath.orElse(Path.of("#none#")),
        pInitialContext.getStart().getStartIndex(),
        pFinalContext.getStop().getStartIndex() - pInitialContext.getStart().getStartIndex(),
        pInitialContext.getStart().getLine(),
        pFinalContext.getStop().getLine(),
        pInitialContext.getStart().getCharPositionInLine(),
        pFinalContext.getStop().getCharPositionInLine());
  }

  /** The name that a symbol denotes, without the vertical bars of a quoted symbol. */
  protected static String nameOf(SymbolContext pSymbol) {
    String text = pSymbol.getText();
    return pSymbol.quotedSymbol() == null ? text : text.substring(1, text.length() - 1);
  }

  /** The name of an identifier, which is the name of its symbol unless it has indices. */
  protected static String nameOf(IdentifierContext pIdentifier) {
    return pIdentifier.index().isEmpty() ? nameOf(pIdentifier.symbol()) : pIdentifier.getText();
  }

  /**
   * The name of a qualified identifier, which is the name of its identifier unless it has a sort.
   */
  protected static String nameOf(Qual_identiferContext pIdentifier) {
    return pIdentifier.GRW_As() == null ? nameOf(pIdentifier.identifier()) : pIdentifier.getText();
  }
}
