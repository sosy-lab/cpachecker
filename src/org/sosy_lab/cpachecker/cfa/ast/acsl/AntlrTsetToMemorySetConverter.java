// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TsetEmptyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TsetTermContext;

public class AntlrTsetToMemorySetConverter
    extends AntlrToInternalAbstractConverter<AcslMemoryLocationSet> {

  private final AntlrTermToTermConverter antrlToTermConverter;

  protected AntlrTsetToMemorySetConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    antrlToTermConverter = new AntlrTermToTermConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslMemoryLocationSet visitTsetEmpty(TsetEmptyContext ctx) {
    return new AcslMemoryLocationSetEmpty(
        FileLocation.DUMMY, new AcslSetType(AcslBuiltinLogicType.ANY));
  }

  @Override
  public AcslMemoryLocationSet visitTsetTerm(TsetTermContext ctx) {
    AcslTerm term = antrlToTermConverter.visit(ctx.term());
    return new AcslMemoryLocationSetTerm(FileLocation.DUMMY, term);
  }
}
