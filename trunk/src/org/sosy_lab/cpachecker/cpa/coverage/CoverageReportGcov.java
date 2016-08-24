/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.coverage;

import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.FileCoverage;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.FileCoverage.FunctionInfo;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;

/**
 * Generate coverage information in Gcov format
 * (http://gcc.gnu.org/onlinedocs/gcc/Gcov.html).
 */
class CoverageReportGcov {

  //String constants from gcov format
  private final static String TEXTNAME = "TN:";
  private final static String SOURCEFILE = "SF:";
  private final static String FUNCTION = "FN:";
  private final static String FUNCTIONDATA = "FNDA:";
  private final static String LINEDATA = "DA:";

  private final LogManager logger;

  public CoverageReportGcov(LogManager pLogger) {
    this.logger = pLogger;
  }

  public void write(CoverageData pData, Path pTarget) {

    Map<String, FileCoverage> infos = pData.getInfosPerFile();

    try (Writer w = MoreFiles.openOutputFile(pTarget, Charset.defaultCharset())) {

      for (Map.Entry<String, FileCoverage> entry : infos.entrySet()) {
        String sourcefile = entry.getKey();
        FileCoverage fileInfos = entry.getValue();

        //Convert ./test.c -> /full/path/test.c
        w.append(TEXTNAME + "\n");
        w.append(SOURCEFILE + Paths.get(sourcefile).toAbsolutePath() + "\n");

        for (FunctionInfo info : fileInfos.allFunctions) {
          w.append(FUNCTION + info.firstLine + "," + info.name + "\n");
          //Information about function end isn't used by lcov, but it is useful for some postprocessing
          //But lcov ignores all unknown lines, so, this additional information can't affect on its work
          w.append("#" + FUNCTION + info.lastLine + "\n");
        }

        for (String name : fileInfos.visitedFunctions) {
          w.append(FUNCTIONDATA + "1," + name + "\n");
        }

        /* Now save information about lines
         */
        for (Integer line : fileInfos.allLines) {
          w.append(LINEDATA + line + "," + (fileInfos.visitedLines.get(line) ? 1 : 0) + "\n");
        }
        w.append("end_of_record\n");
      }

    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write coverage information to file");
    }

  }

}
