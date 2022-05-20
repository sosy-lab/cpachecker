// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.arg.witness")
public class WitnessOptions {

  @Option(
      secure = true,
      description = "Verification witness: Include function calls and function returns?")
  private boolean exportFunctionCallsAndReturns = true;

  @Option(secure = true, description = "Verification witness: Include assumptions (C statements)?")
  private boolean exportAssumptions = true;

  @Option(
      secure = true,
      description = "Verification witness: Include the considered case of an assume?")
  private boolean exportAssumeCaseInfo = true;

  @Option(
      secure = true,
      description =
          "Verification witness: Include the (starting) line numbers of the operations on the"
              + " transitions?")
  private boolean exportLineNumbers = true;

  @Option(
      secure = true,
      description = "Verification witness: Include the sourcecode of the operations?")
  private boolean exportSourcecode = false;

  @Option(
      secure = true,
      description =
          "Verification witness: Include (not necessarily globally unique) thread names for"
              + " concurrent tasks for debugging?")
  private boolean exportThreadName = false;

  @Option(secure = true, description = "Verification witness: Include the offset within the file?")
  private boolean exportOffset = true;

  @Option(
      secure = true,
      description = "Verification witness: Include an thread-identifier within the file?")
  private boolean exportThreadId = false;

  @Option(secure = true, description = "Some redundant transitions will be removed")
  private boolean removeInsufficientEdges = true;

  @Option(
      secure = true,
      description = "Verification witness: Revert escaping/renaming of functions for threads?")
  private boolean revertThreadFunctionRenaming = false;

  @Option(
      secure = true,
      description =
          "Verification witness: Export labels for nodes in GraphML for easier visual"
              + " representation?")
  private boolean exportNodeLabel = false;

  @Option(secure = true, description = "Always export source file name, even default")
  private boolean exportSourceFileName = false;

  @Option(
      secure = true,
      description =
          "Produce an invariant witness instead of a correctness witness. Constructing an invariant"
              + " witness makes use of a different merge for quasi-invariants: Instead of computing"
              + " the disjunction of two invariants present when merging nodes, 'true' is ignored"
              + " when constructing the disjunction. This may be unsound in some situations, so be"
              + " careful when using this option.")
  private boolean produceInvariantWitnesses = false;

  boolean exportFunctionCallsAndReturns() {
    return exportFunctionCallsAndReturns;
  }

  boolean exportAssumptions() {
    return exportAssumptions;
  }

  boolean exportAssumeCaseInfo() {
    return exportAssumeCaseInfo;
  }

  boolean exportLineNumbers() {
    return exportLineNumbers;
  }

  boolean exportSourcecode() {
    return exportSourcecode;
  }

  boolean exportOffset() {
    return exportOffset;
  }

  boolean exportThreadId() {
    return exportThreadId;
  }

  boolean exportThreadName() {
    return exportThreadName;
  }

  boolean removeInsufficientEdges() {
    return removeInsufficientEdges;
  }

  boolean revertThreadFunctionRenaming() {
    return revertThreadFunctionRenaming;
  }

  public boolean exportNodeLabel() {
    return exportNodeLabel;
  }

  boolean exportSourceFileName() {
    return exportSourceFileName;
  }

  public boolean produceInvariantWitnesses() {
    return produceInvariantWitnesses;
  }
}
