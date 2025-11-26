// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;

public class AntlrAssertionToAssertionConverter
    extends AntlrToInternalAbstractConverter<AcslAssertion> {
  private final AntlrPredicateToPredicateConverter antlrPredicateToPredicateConverter;

  protected AntlrAssertionToAssertionConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    antlrPredicateToPredicateConverter =
        new AntlrPredicateToPredicateConverter(pCProgramScope, pAcslScope);
  }

  public AcslAssertion visitAssertion(AssertionContext ctx) {
    ParseTree predTree = ctx.getChild(1);
    AcslPredicate predicate = antlrPredicateToPredicateConverter.visit(predTree);
    AcslAssertion assertion = new AcslAssertion(FileLocation.DUMMY, predicate);

    return assertion;
  }
}
