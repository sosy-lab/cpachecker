// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public enum AcslBuiltinLabel implements AcslLabel {
  HERE("Here"),
  OLD("Old"),
  PRE("Pre"),
  POST("Post"),
  LOOP_ENTRY("LoopEntry"),
  LOOP_EXIT("LoopExit"),
  INIT("Init"),
  ;

  @Serial private static final long serialVersionUID = 701126361956900L;

  private final String label;
  private final FileLocation location;

  AcslBuiltinLabel(String pLabel) {
    label = pLabel;
    location = FileLocation.DUMMY;
  }

  public static AcslBuiltinLabel of(String pLabel) {
    for (AcslBuiltinLabel builtinLabel : values()) {
      if (builtinLabel.label.equals(pLabel)) {
        return builtinLabel;
      }
    }
    throw new IllegalArgumentException("Unknown builtin label: " + pLabel);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return location;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return label;
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
