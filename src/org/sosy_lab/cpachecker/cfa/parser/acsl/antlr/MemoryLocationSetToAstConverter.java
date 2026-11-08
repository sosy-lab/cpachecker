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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSetEmpty;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSetTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TsetEmptyContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.TsetTermContext;

public class MemoryLocationSetToAstConverter
    extends AbstractAntlrToAstConverter<AcslMemoryLocationSet> {

  private final TermToAstConverter termConverter;

  protected MemoryLocationSetToAstConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    super(pCProgramScope, pAcslScope, pInitialFileLocation);
    termConverter = new TermToAstConverter(pCProgramScope, pAcslScope, pInitialFileLocation);
  }

  @Override
  public AcslMemoryLocationSet visitTsetEmpty(TsetEmptyContext ctx) {
    return new AcslMemoryLocationSetEmpty(FileLocation.DUMMY);
  }

  @Override
  public AcslMemoryLocationSet visitTsetTerm(TsetTermContext ctx) {
    AcslTerm term = termConverter.visit(ctx.term());
    return new AcslMemoryLocationSetTerm(FileLocation.DUMMY, term);
  }
}
