// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

/** Super-class for all thrown exceptions that occur while parsing ltl-properties */
public class LtlParseException extends InvalidConfigurationException {

  private static final long serialVersionUID = -8907490649042996735L;

  public LtlParseException(String pMsg) {
    super(checkNotNull(pMsg));
  }

  public LtlParseException(String pMsg, Throwable pCause) {
    super(checkNotNull(pMsg), checkNotNull(pCause));
  }
}
