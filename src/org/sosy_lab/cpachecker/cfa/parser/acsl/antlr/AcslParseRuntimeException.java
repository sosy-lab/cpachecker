// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import java.io.Serial;

/**
 * Signals an ACSL annotation that is invalid or not supported. The converters of the parse tree
 * throw it because ANTLR visitors cannot throw checked exceptions, and {@link AcslParser} reports
 * it as {@link AcslParser.AcslParseException}.
 */
final class AcslParseRuntimeException extends RuntimeException {

  @Serial private static final long serialVersionUID = 6014538431289071823L;

  AcslParseRuntimeException(String pMsg) {
    super(pMsg);
  }
}
