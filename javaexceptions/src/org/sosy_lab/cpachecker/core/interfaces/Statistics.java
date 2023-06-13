// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

/**
 * A class to output statistics and results of an analysis.
 *
 * <p>You usually want to implement {@link StatisticsProvider} and register your Statistics
 * instances so that they are actually called after CPAchecker finishes.
 */
public interface Statistics {

  /**
   * Prints this group of statistics using the given PrintStream.
   *
   * <p>This is also the correct place to write any output files the user may wish to the disk.
   * Please add a configuration option of the following form in order to determine the file name for
   * output files: <code>
   * {@literal @}Option(secure=true, description="...", name="...)
   * {@literal @}FileOption(FileOption.Type.OUTPUT_FILE)
   * private File outputFile = new File("Default Filename.txt");
   * </code> Note that <code>outputFile</code> may be null because the user disabled output files
   * (do not write anything in this case). Do not forget to obtain a {@link
   * org.sosy_lab.common.configuration.Configuration} instance and call <code>inject(this)</code> in
   * your constructor as usual.
   *
   * @param out the PrintStream to use for printing the statistics
   * @param result the result of the analysis
   * @param reached the final reached set
   */
  void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached);

  /**
   * Define a name for this group of statistics. May be null, in this case no headings is printed
   * and {@link #printStatistics(PrintStream, Result, UnmodifiableReachedSet)} should not actually
   * write to the PrintStream (but may still write output files for example).
   *
   * @return A String with a human-readable name or null.
   */
  @Nullable String getName();

  /**
   * Write result files related to this group of statistics.
   *
   * <p>Note that this method may be called in parallel with the statistics of other components, so
   * it should not modify any externally visible state, e.g., in abstract states.
   *
   * @param pResult the result of the analysis
   * @param pReached the final reached set
   */
  default void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {}

  int DEFAULT_OUTPUT_NAME_COL_WIDTH = 50;

  /**
   * Pretty print with zero indentation
   *
   * @see #put(PrintStream, int, String, Object)
   */
  default void put(PrintStream pTarget, String pName, Object pValue) {
    put(pTarget, 0, pName, pValue);
  }

  /**
   * Print a statistics line in a "pretty" fashion.
   *
   * @param target Write to this stream
   * @param indentLevel Indentation level (0 = no indentation)
   * @param name Left hand side (name/description)
   * @param value Right hand side (value)
   */
  default void put(PrintStream target, int indentLevel, String name, Object value) {
    StatisticsUtils.write(target, indentLevel, DEFAULT_OUTPUT_NAME_COL_WIDTH, name, value);
  }

  default void put(PrintStream target, int indentLevel, AbstractStatValue stat) {
    StatisticsUtils.write(target, indentLevel, DEFAULT_OUTPUT_NAME_COL_WIDTH, stat);
  }
}
