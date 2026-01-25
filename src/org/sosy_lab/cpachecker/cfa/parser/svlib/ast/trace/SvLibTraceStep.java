// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class SvLibTraceStep extends SvLibTraceComponent
    permits SvLibChoiceStep, SvLibHavocVariablesStep, SvLibLeapStep, SvLibInitProcVariablesStep {
  @Serial private static final long serialVersionUID = -8454696686234105859L;

  SvLibTraceStep(FileLocation pFileLocation) {
    super(pFileLocation);
  }
}
