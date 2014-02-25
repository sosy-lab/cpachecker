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
package org.sosy_lab.cpachecker.core.interfaces;

import java.io.PrintStream;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * A class to output statistics and results of an analysis.
 *
 * You usually want to implement {@link StatisticsProvider} and register your
 * Statistics instances so that they are actually called after CPAchecker finishes.
 */
public interface Statistics {

    /**
     * Prints this group of statistics using the given PrintStream.
     *
     * This is also the correct place to write any output files the user may wish
     * to the disk. Please add a configuration option of the following form
     * in order to determine the file name for output files:
     * <code>
     * @Option(description="...", name="...)
     * @FileOption(FileOption.Type.OUTPUT_FILE)
     * private File outputFile = new File("Default Filename.txt");
     * </code>
     * Note that <code>outputFile</code> may be null because the user disabled
     * output files (do not write anything in this case).
     * Do not forget to obtain a {@link org.sosy_lab.common.configuration.Configuration}
     * instance and call <code>inject(this)</code> in your constructor as usual.
     *
     * @param out the PrintStream to use for printing the statistics
     * @param result the result of the analysis
     * @param reached the final reached set
     */
    public void printStatistics(PrintStream out, Result result, ReachedSet reached);

    /**
     * Define a name for this group of statistics.
     * May be null, in this case no headings is printed and
     * {@link #printStatistics(PrintStream, Result, ReachedSet)}
     * should not actually write to the PrintStream
     * (but may still write output files for example).
     * @return A String with a human-readable name or null.
     */
    public String getName();
}
