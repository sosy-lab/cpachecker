// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import javax.management.RuntimeErrorException;

/** Helpers for javax.management */
public class ManagementUtils {

  public static RuntimeErrorException handleRuntimeErrorException(RuntimeErrorException e) {
    if (e.getTargetError() != null) {
      throw e.getTargetError();
    }
    throw e;
  }
}
