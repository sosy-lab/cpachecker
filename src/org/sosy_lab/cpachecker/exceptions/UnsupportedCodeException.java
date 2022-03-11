// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Exception thrown when a CPA cannot handle some code attached to a CFAEdge because it uses
 * features that are unsupported.
 */
public class UnsupportedCodeException extends UnrecognizedCodeException {

  private static final long serialVersionUID = -7693635256672813804L;

  public UnsupportedCodeException(String msg, @Nullable CFAEdge edge, @Nullable AAstNode astNode) {
    super("Unsupported feature", msg, edge, astNode);
  }

  public UnsupportedCodeException(String msg, @Nullable CFAEdge cfaEdge) {
    this(msg, cfaEdge, null);
  }
}
