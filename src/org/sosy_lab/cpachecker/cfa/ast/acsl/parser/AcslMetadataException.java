// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import java.io.Serial;

public class AcslMetadataException extends RuntimeException {
  @Serial private static final long serialVersionUID = -3184259442448755810L;

  public AcslMetadataException(String message) {
    super(message);
  }

  public static class AcslNodeMappingException extends AcslMetadataException {
    @Serial private static final long serialVersionUID = -2422260384913137786L;

    public AcslNodeMappingException(String message) {
      super(message);
    }
  }

  public static class AcslMetadataCreationException extends AcslMetadataException {
    @Serial private static final long serialVersionUID = 2641299193651871955L;

    public AcslMetadataCreationException(String message) {
      super(message);
    }
  }
}
