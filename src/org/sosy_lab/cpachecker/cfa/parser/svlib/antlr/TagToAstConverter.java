// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.CheckTruePropertyContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.EnsuresPropertyContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.InvariantPropertyContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.RequiresPropertyContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.TagAttributeContext;

class TagToAstConverter extends AbstractAntlrToAstConverter<SvLibTagAttribute> {
  private final TermToAstConverter termToAstConverter;

  public TagToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    termToAstConverter = new TermToAstConverter(pScope, pFilePath);
  }

  public TagToAstConverter(SvLibScope pScope) {
    super(pScope);
    termToAstConverter = new TermToAstConverter(pScope);
  }

  @Override
  public SvLibTagAttribute visitTagAttribute(TagAttributeContext pContext) {
    return new SvLibTagReference(
        pContext.symbol().getText(), fileLocationFromContext(pContext), scope.copy());
  }

  @Override
  public SvLibTagAttribute visitCheckTrueProperty(CheckTruePropertyContext pContext) {
    SvLibFinalRelationalTerm term = pContext.relationalTerm().accept(termToAstConverter);
    return new SvLibCheckTrueTag(term, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibTagAttribute visitInvariantProperty(InvariantPropertyContext pContext) {
    SvLibFinalRelationalTerm term = pContext.relationalTerm().accept(termToAstConverter);
    return new SvLibInvariantTag(term, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibTagAttribute visitEnsuresProperty(EnsuresPropertyContext pContext) {
    SvLibFinalRelationalTerm term = pContext.relationalTerm().accept(termToAstConverter);
    return new SvLibEnsuresTag(term, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibTagAttribute visitRequiresProperty(RequiresPropertyContext pContext) {
    SvLibTerm term = pContext.term().accept(termToAstConverter);
    return new SvLibRequiresTag(term, fileLocationFromContext(pContext));
  }
}
