// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.llvm;

import org.sosy_lab.llvm_j.binding.LLVMLibrary;

public class LlvmUtils {

  /**
   * Extract the currently used version number from llvm-j.
   *
   * @return the currently used LLVM version number
   */
  public static String extractVersionNumberFromLlvmJ() {
    return LLVMLibrary.JNA_LIBRARY_NAME.substring("LLVM-".length());
  }
}
