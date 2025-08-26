// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssertTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3EnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3InvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.AssertPropertyContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.EnsuresPropertyContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.InvariantPropertyContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.RequiresPropertyContext;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.TagAttributeContext;

class TagToAstConverter extends AbstractAntlrToAstConverter<K3TagAttribute> {
  private final TermToAstConverter termToAstConverter;

  public TagToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    termToAstConverter = new TermToAstConverter(pScope, pFilePath);
  }

  public TagToAstConverter(K3Scope pScope) {
    super(pScope);
    termToAstConverter = new TermToAstConverter(pScope);
  }

  @Override
  public K3TagAttribute visitTagAttribute(TagAttributeContext pContext) {
    return new K3TagReference(pContext.tagName().getText(), fileLocationFromContext(pContext));
  }

  @Override
  public K3TagAttribute visitAssertProperty(AssertPropertyContext pContext) {
    K3RelationalTerm term = pContext.relationalTerm().accept(termToAstConverter);
    return new K3AssertTag(term, fileLocationFromContext(pContext));
  }

  @Override
  public K3TagAttribute visitInvariantProperty(InvariantPropertyContext pContext) {
    K3RelationalTerm term = pContext.relationalTerm().accept(termToAstConverter);
    return new K3InvariantTag(term, fileLocationFromContext(pContext));
  }

  @Override
  public K3TagAttribute visitEnsuresProperty(EnsuresPropertyContext pContext) {
    K3RelationalTerm term = pContext.relationalTerm().accept(termToAstConverter);
    return new K3EnsuresTag(term, fileLocationFromContext(pContext));
  }

  @Override
  public K3TagAttribute visitRequiresProperty(RequiresPropertyContext pContext) {
    K3Term term = pContext.term().accept(termToAstConverter);
    return new K3RequiresTag(term, fileLocationFromContext(pContext));
  }
}
