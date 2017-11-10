/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.arg.witness")
class WitnessOptions {

  @Option(
    secure = true,
    description = "Verification witness: Include function calls and function returns?"
  )
  private boolean exportFunctionCallsAndReturns = true;

  @Option(secure = true, description = "Verification witness: Include assumptions (C statements)?")
  private boolean exportAssumptions = true;

  @Option(
    secure = true,
    description = "Verification witness: Include the considered case of an assume?"
  )
  private boolean exportAssumeCaseInfo = true;

  @Option(
    secure = true,
    description =
        "Verification witness: Include the (starting) line numbers of the operations on the transitions?"
  )
  private boolean exportLineNumbers = true;

  @Option(
    secure = true,
    description = "Verification witness: Include the sourcecode of the operations?"
  )
  private boolean exportSourcecode = false;

  @Option(
      secure = true,
      description = "Verification witness: Include (not necessarily globally unique) thread names for concurrent tasks for debugging?")
  private boolean exportThreadName = false;

  @Option(secure = true, description = "Verification witness: Include the offset within the file?")
  private boolean exportOffset = true;

  @Option(
    secure = true,
    description = "Verification witness: Include an thread-identifier within the file?"
  )
  private boolean exportThreadId = false;

  @Option(secure = true, description = "Some redundant transitions will be removed")
  private boolean removeInsufficientEdges = true;

  @Option(
    secure = true,
    description = "Verification witness: Revert escaping/renaming of functions for threads?"
  )
  private boolean revertThreadFunctionRenaming = false;

  @Option(
    secure = true,
    description =
        "Verification witness: Export labels for nodes in GraphML for easier visual representation?"
  )
  private boolean exportNodeLabel = false;

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

  boolean exportNodeLabel() {
    return exportNodeLabel;
  }
}
