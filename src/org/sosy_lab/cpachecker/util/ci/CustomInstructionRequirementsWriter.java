/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class CustomInstructionRequirementsWriter {

  private final String filePrefix;
  private int fileID;
  private final Class<?> requirementsState;

  public CustomInstructionRequirementsWriter(final String pFilePrefix, final Class<?> pClass) {
    filePrefix = pFilePrefix;
    fileID = 0;
    requirementsState = pClass;
  }

  // TODO to be continued
  public void writeCIRequirement(final ARGState pState, final Collection<ARGState> pSet,
      final AppliedCustomInstruction pACI) throws IOException {

    fileID++;
    try (Writer br = new BufferedWriter(Files.openOutputFile(Paths.get(filePrefix+fileID+".smt")))) {
      // TODO to be continued
    }
  }
}
