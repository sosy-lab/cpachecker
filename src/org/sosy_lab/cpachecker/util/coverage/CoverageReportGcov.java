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
package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Map;
import org.sosy_lab.cpachecker.util.coverage.FileCoverageInformation.FunctionInfo;

/** Generate coverage information in Gcov format (http://gcc.gnu.org/onlinedocs/gcc/Gcov.html). */
public class CoverageReportGcov {

  //String constants from gcov format
  private final static String TEXTNAME = "TN:";
  private final static String SOURCEFILE = "SF:";
  private final static String FUNCTION = "FN:";
  private final static String FUNCTIONDATA = "FNDA:";
  private final static String LINEDATA = "DA:";

  public static void write(CoverageData pCoverage, Writer w) throws IOException {

    for (Map.Entry<String, FileCoverageInformation> entry :
        pCoverage.getInfosPerFile().entrySet()) {
      String sourcefile = entry.getKey();
      FileCoverageInformation fileInfos = entry.getValue();

      //Convert ./test.c -> /full/path/test.c
      w.append(TEXTNAME + "\n");
      w.append(SOURCEFILE + Paths.get(sourcefile).toAbsolutePath() + "\n");

      for (FunctionInfo info : fileInfos.allFunctions) {
        w.append(FUNCTION + info.firstLine + "," + info.name + "\n");
        //Information about function end isn't used by lcov, but it is useful for some postprocessing
        //But lcov ignores all unknown lines, so, this additional information can't affect on its work
        w.append("#" + FUNCTION + info.lastLine + "\n");
      }

      for (Multiset.Entry<String> functionEntry : fileInfos.visitedFunctions.entrySet()) {
        w.append(FUNCTIONDATA + functionEntry.getCount() + "," + functionEntry.getElement() + "\n");
      }

      /* Now save information about lines
       */
      for (Integer line : fileInfos.allLines) {
        w.append(LINEDATA + line + "," + fileInfos.getVisitedLine(line) + "\n");
      }
      w.append("end_of_record\n");
    }
  }
}
