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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode;

public abstract sealed class SvLibTraceComponent implements SvLibParsingAstNode
    permits SmtLibModel,
        SvLibTraceEntryProcedure,
        SvLibTraceSetGlobalVariable,
        SvLibTraceStep,
        SvLibTraceUsingAnnotation,
        SvLibViolatedProperty {
  @Serial private static final long serialVersionUID = -5924055290995494634L;
  private final FileLocation fileLocation;

  SvLibTraceComponent(FileLocation pFileLocation) {
    fileLocation = pFileLocation;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  abstract <R, X extends Exception> R accept(SvLibTraceComponentVisitor<R, X> v) throws X;
}
