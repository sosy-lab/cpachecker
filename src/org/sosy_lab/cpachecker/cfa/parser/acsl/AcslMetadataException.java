// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import java.io.Serial;

public class AcslMetadataException extends RuntimeException {
  @Serial private static final long serialVersionUID = 9113841098540412745L;

  public AcslMetadataException(String message) {
    super(message);
  }

  public static class AcslNodeMappingException extends AcslMetadataException {
    @Serial private static final long serialVersionUID = 480129679996868553L;

    public AcslNodeMappingException(String message) {
      super(message);
    }
  }

  public static class AcslMetadataCreationException extends AcslMetadataException {
    @Serial private static final long serialVersionUID = 1264216760515393605L;

    public AcslMetadataCreationException(String message) {
      super(message);
    }
  }
}
