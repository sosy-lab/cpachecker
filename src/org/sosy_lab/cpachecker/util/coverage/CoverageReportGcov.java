// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
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
      w.append(SOURCEFILE).append(String.valueOf(Path.of(sourcefile).toAbsolutePath()))
          .append("\n");

      for (FunctionInfo info : fileInfos.allFunctions) {
        w.append(FUNCTION).append(String.valueOf(info.firstLine)).append(",").append(info.name)
            .append("\n");
        //Information about function end isn't used by lcov, but it is useful for some postprocessing
        //But lcov ignores all unknown lines, so, this additional information can't affect on its work
        w.append("#" + FUNCTION).append(String.valueOf(info.lastLine)).append("\n");
      }

      for (Multiset.Entry<String> functionEntry : fileInfos.visitedFunctions.entrySet()) {
        w.append(FUNCTIONDATA).append(String.valueOf(functionEntry.getCount())).append(",")
            .append(functionEntry.getElement()).append("\n");
      }

      /* Now save information about lines
       */
      for (Integer line : fileInfos.allLines) {
        w.append(LINEDATA).append(String.valueOf(line)).append(",")
            .append(String.valueOf(fileInfos.getVisitedLine(line))).append("\n");
      }
      w.append("end_of_record\n");
    }
  }
}
