// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;

public class AcslMetadataException extends RuntimeException {
  @Serial private static final long serialVersionUID = -3184259442448755810L;

  public AcslMetadataException(String message) {
    super(message);
  }
}
