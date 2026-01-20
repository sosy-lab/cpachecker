// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings("unused")
public class AcslTermToFormulaConverter {

  public static @NonNull Formula convertAcslTerm(AcslTerm term, FormulaManagerView fmgr) {

    return switch (term) {
      case AcslArraySubscriptTerm t ->
          throw new UnsupportedOperationException("Not yet implemented");
      case AcslAtTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslBinaryTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslFunctionCallTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslIdTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslLiteralTerm t -> convertLiteral(t, fmgr);
      case AcslOldTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslResultTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslTernaryTerm t -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslUnaryTerm t -> throw new UnsupportedOperationException("Not yet implemented");
    };
  }

  private static @NonNull Formula convertLiteral(
      AcslLiteralTerm literalTerm, FormulaManagerView fmgr) {
    return switch (literalTerm) {
      case AcslBooleanLiteralTerm bT -> fmgr.getBooleanFormulaManager().makeBoolean(bT.getValue());
      case AcslCharLiteralTerm cT -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslIntegerLiteralTerm iT -> fmgr.getIntegerFormulaManager().makeNumber(iT.getValue());
      case AcslRealLiteralTerm rT -> fmgr.getRationalFormulaManager().makeNumber(rT.getValue());
      case AcslStringLiteralTerm sT ->
          throw new UnsupportedOperationException("Not yet implemented");
    };
  }
}
