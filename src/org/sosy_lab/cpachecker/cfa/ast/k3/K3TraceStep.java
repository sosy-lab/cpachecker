// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class K3TraceStep extends K3SelectTraceComponent
    permits K3ChoiceStep, K3HavocVariablesStep, K3LeapStep, K3LocalVariablesStep {
  @Serial private static final long serialVersionUID = -8454696686234105859L;

  K3TraceStep(FileLocation pFileLocation) {
    super(pFileLocation);
  }
}
