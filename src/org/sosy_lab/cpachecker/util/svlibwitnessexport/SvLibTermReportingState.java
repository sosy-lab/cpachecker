// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface SvLibTermReportingState extends AbstractState {

  /**
   * Returns the SvLibFinalRelationalTerm representation of the abstract state.
   *
   * @return the SvLibFinalRelationalTerm representation of the abstract state
   */
  SvLibTerm asSvLibTerm(SvLibScope pScope);
}
