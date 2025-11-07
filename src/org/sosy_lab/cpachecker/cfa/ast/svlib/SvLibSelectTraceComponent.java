// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class SvLibSelectTraceComponent implements SvLibAstNode
    permits SvLibTrace,
        SvLibTraceEntryCall,
        SvLibTraceSetGlobalVariable,
        SvLibTraceSetTag,
        SvLibTraceStep,
        SvLibViolatedProperty {
  @Serial private static final long serialVersionUID = -5924055290995494634L;
  private final FileLocation fileLocation;

  SvLibSelectTraceComponent(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  abstract <R, X extends Exception> R accept(SvLibTraceElementVisitor<R, X> v) throws X;
}
