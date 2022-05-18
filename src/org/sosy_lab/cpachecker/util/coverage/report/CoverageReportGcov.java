// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.report;

import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollector.FunctionInfo;

/** Generate coverage information in Gcov format (http://gcc.gnu.org/onlinedocs/gcc/Gcov.html). */
public class CoverageReportGcov {
  private static final String TEXT_NAME = "TN:";
  private static final String SOURCE_FILE = "SF:";
  private static final String FUNCTION = "FN:";
  private static final String FUNCTION_DATA = "FNDA:";
  private static final String LINE_DATA = "DA:";

  public static void write(CoverageCollector collector, Writer w) throws IOException {

    for (String sourcefile : collector.getVisitedLinesPerFile().keySet()) {
      // Convert ./test.c -> /full/path/test.c
      w.append(TEXT_NAME + "\n");
      w.append(SOURCE_FILE)
          .append(String.valueOf(Path.of(sourcefile).toAbsolutePath()))
          .append("\n");

      for (FunctionInfo info : collector.getAllFunctions()) {
        w.append(FUNCTION)
            .append(String.valueOf(info.getFirstLine()))
            .append(",")
            .append(info.getName())
            .append("\n");
        // Information about function end isn't used by lcov, but it is useful for some
        // postprocessing
        // But lcov ignores all unknown lines, so, this additional information can't affect on its
        // work
        w.append("#" + FUNCTION).append(String.valueOf(info.getLastLine())).append("\n");
      }

      for (Multiset.Entry<String> functionEntry :
          collector.getVisitedFunctionsPerFile().get(sourcefile).entrySet()) {
        w.append(FUNCTION_DATA)
            .append(String.valueOf(functionEntry.getCount()))
            .append(",")
            .append(functionEntry.getElement())
            .append("\n");
      }

      /* Now save information about lines
       */
      for (Integer line : collector.getExistingLinesPerFile().get(sourcefile)) {
        w.append(LINE_DATA)
            .append(String.valueOf(line))
            .append(",")
            .append(String.valueOf(collector.getVisitedLinesPerFile().get(sourcefile).count(line)))
            .append("\n");
      }
      w.append("end_of_record\n");
    }
  }
}
