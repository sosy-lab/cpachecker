// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.Label_idContext;

class AntlrLabelToLabelConverter extends AntlrToInternalAbstractConverter<AcslLabel> {

  protected AntlrLabelToLabelConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslLabel visitLabel_id(Label_idContext ctx) {
    String identifierName = ctx.getText();
    if (FluentIterable.from(AcslBuiltinLabel.values())
        .transform(AcslBuiltinLabel::getLabel)
        .anyMatch(label -> label.equals(identifierName))) {
      return AcslBuiltinLabel.of(identifierName);
    }

    return new AcslProgramLabel(identifierName, FileLocation.DUMMY);
  }
}
