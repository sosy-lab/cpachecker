// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/** Generate coverage information in Gcov format (https://gcc.gnu.org/onlinedocs/gcc/Gcov.html).
 *  The .gcov files contain the : separated fields along with program source code.  The format is
 *
 *    execution_count:line_number:source line text
 *
 *  Some lines of information at the start have line_number of zero. These preamble lines are of the form
 *
 *    -:0:tag:value
 *
 *  The additional block information is of the form
 *
 *    tag information
 **/
public class CoverageReportGcov {

  // String constants from gcov format
  private final static String SOURCEFILE = "\t-:\t0:Source:";
  private final static String WORKING_DIRECTORY = "\t-:\t0:Working directory:";
  private final static String ADDITIONAL_TAG = "info ";



  public static void write(CoverageData pCoverage, Writer w) throws IOException {

    for (Map.Entry<String, FileCoverageInformation> entry :
        pCoverage.getInfosPerFile().entrySet()) {
      String sourcefile = entry.getKey();
      FileCoverageInformation fileInfos = entry.getValue();

      w.append(SOURCEFILE + Paths.get(sourcefile) + "\n");
      Path root = Paths.get(sourcefile).getRoot();
      if (root != null) {
        w.append(WORKING_DIRECTORY + root + "\n");
      }

//      for (FunctionInfo info : fileInfos.allFunctions) {
//        w.append(FUNCTION + info.firstLine + "," + info.name + "\n");
        //Information about function end isn't used by lcov, but it is useful for some postprocessing
        //But lcov ignores all unknown lines, so, this additional information can't affect on its work
//        w.append("#" + FUNCTION + info.lastLine + "\n");
//      }

//      for (Multiset.Entry<String> functionEntry : fileInfos.visitedFunctions.entrySet()) {
//        w.append(FUNCTIONDATA + functionEntry.getCount() + "," + functionEntry.getElement() + "\n");
//      }

      /* Now save information about lines
       */
      for (Integer line : fileInfos.allLines) {
        w.append("\t" + fileInfos.getVisitedLine(line) + ":\t" + line + ":" + fileInfos.getSourceCode(line) + "\n");
        String additionalInfo = fileInfos.getAdditionalInfo(line);
        if (!additionalInfo.isEmpty()) {
          w.append(ADDITIONAL_TAG + additionalInfo + "\n");
        }
      }
    }
  }
}
