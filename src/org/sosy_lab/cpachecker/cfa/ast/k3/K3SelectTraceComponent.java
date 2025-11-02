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

public abstract sealed class K3SelectTraceComponent implements K3AstNode
    permits K3Trace,
        K3TraceEntryCall,
        K3TraceSetGlobalVariable,
        K3TraceSetTag,
        K3TraceStep,
        K3ViolatedProperty {
  @Serial private static final long serialVersionUID = -5924055290995494634L;
  private final FileLocation fileLocation;

  K3SelectTraceComponent(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  abstract <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X;
}
