// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Preconditions.checkArgument;

public enum ClangFormatStyle {
  NONE(""),
  CHROMIUM("Chromium"),
  GNU("GNU"),
  GOOGLE("Google"),
  LLVM("LLVM"),
  MICROSOFT("Microsoft"),
  MOZILLA("Mozilla"),
  WEBKIT("WebKit");

  private final String command;

  ClangFormatStyle(String pCommand) {
    command = pCommand;
  }

  public String getCommand() {
    checkArgument(isEnabled(), "cannot get command, ClangFormatStyle is NONE");
    return "--style=" + command;
  }

  public boolean isEnabled() {
    return !this.equals(NONE);
  }
}
