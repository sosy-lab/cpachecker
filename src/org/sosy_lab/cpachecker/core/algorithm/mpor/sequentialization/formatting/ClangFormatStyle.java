// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.formatting;

public enum ClangFormatStyle {
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
    return "--style=" + command;
  }
}
