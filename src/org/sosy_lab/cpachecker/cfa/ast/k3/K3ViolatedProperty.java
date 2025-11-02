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

public abstract sealed class K3ViolatedProperty extends K3SelectTraceComponent
    permits K3IncorrectTagProperty {
  @Serial private static final long serialVersionUID = -4859233865865266662L;

  K3ViolatedProperty(FileLocation pFileLocation) {
    super(pFileLocation);
  }
}
