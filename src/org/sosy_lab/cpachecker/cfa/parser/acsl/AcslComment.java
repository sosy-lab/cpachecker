// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser;

public record AcslComment(FileLocation fileLocation, String commentString) {

  public String getComment() {
    return commentString;
  }

  @Override
  public String toString() {
    return "'" + AcslParser.blankCommentMarkers(commentString).strip() + "' at " + fileLocation;
  }

  public enum AcslCommentType {
    ASSERTION,
    LOOP_ANNOTATION,
    FUNCTION_CONTRACT,
    STATEMENT_CONTRACT,
    LOGIC_DEF,
    UNKNOWN
  }
}
