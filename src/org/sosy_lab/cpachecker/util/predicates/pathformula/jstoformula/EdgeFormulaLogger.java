/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.java_smt.api.BooleanFormula;

class EdgeFormulaLogger {
  private final boolean logEdgeFormulas;
  private final LogManager warningLogger;
  private final Path logFile;

  EdgeFormulaLogger(final boolean pLogEdgeFormulas, final LogManager pWarningLogger) {
    logEdgeFormulas = pLogEdgeFormulas;
    warningLogger = pWarningLogger;
    logFile = Paths.get("./output/CPA_edge-formula-log.txt");
    if (logEdgeFormulas) {
      log(StandardOpenOption.CREATE, "");
      log(StandardOpenOption.TRUNCATE_EXISTING, "");
    }
  }

  void log(final CFAEdge pEdge, final BooleanFormula pEdgeFormula) {
    if (!logEdgeFormulas) {
      return;
    }
    log(
        StandardOpenOption.APPEND,
        String.format(
            "; %3d -> %3d:   %s%n%s%n",
            pEdge.getPredecessor().getNodeNumber(),
            pEdge.getSuccessor().getNodeNumber(),
            pEdge.getDescription(),
            pEdgeFormula));
  }

  private void log(final StandardOpenOption pOpenOption, final String pMessage) {
    try {
      Files.write(logFile, pMessage.getBytes(UTF_8), pOpenOption);
    } catch (IOException e) {
      warningLogger.log(Level.WARNING, "Edge formula could not be written to file: " + e);
    }
  }
}
