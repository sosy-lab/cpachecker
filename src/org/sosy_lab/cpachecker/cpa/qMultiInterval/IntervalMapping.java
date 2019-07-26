/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

/**
 * Class for IntervalMapping. It maps the specified start interval of possible vlaues to each
 * variable
 */
public class IntervalMapping implements Serializable {

  private static final long serialVersionUID = -5002071709574348525L;

  /** the map which saves the specified intervals */
  private TreeMap<Variable, IntervalExt> intervalMap;

  private LogManager logger;

  /**
   * Constructor which gets a path to the mapping file and reads it. Then it adds each
   * variable-interval pair to the map
   *
   * @param filePath Path to the mappingfile
   */
  public IntervalMapping(Path filePath, LogManager logger) {

    this.logger = logger;
    // this.logger.log(Level.INFO, "TEST");
    // Log.Log2("TEST");
    this.logger.logf(Level.CONFIG, "IntervallMapping input: %s", filePath);
    intervalMap = new TreeMap<>();

    List<String> lines = null;
    try {
      lines = Files.readAllLines(filePath, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logfException(Level.SEVERE, e, "Could not read File : %s", filePath);
      return;
    }

    for (String line : lines) {
      if (line.trim().isEmpty() || line.contains("//")) {
        continue;
      }
      if (line.contains("=") && line.contains("[") && line.contains(",") && line.contains("]")) {

        int var = line.indexOf("=");
        int startlow = line.indexOf("[");
        int starthigh = line.indexOf(",");
        int endhigh = line.indexOf("]");

        Variable varname = new Variable(line.substring(0, var).trim());
        IntervalExt interval =
            new IntervalExt(
                Long.parseLong(line.substring(startlow + 1, starthigh)),
                Long.parseLong(line.substring(starthigh + 1, endhigh)));

        intervalMap.put(varname, interval);
        logger.logf(
            Level.FINEST,
            "IntervalMapping mapped to Variable %s the interval %s.",
            varname,
            interval);

      } else if (line.contains("=") && line.contains("(") && line.contains("Bit")) {

        int var = line.indexOf("=");
        int start = line.indexOf("(");
        int end = line.indexOf("Bit");

        Variable varname = new Variable(line.substring(0, var).trim());
        int bit = Integer.parseInt(line.substring(start + 1, end));
        logger.logf(Level.FINEST, "IntervalMapping mapped to Variable %s %d Bit.", varname, bit);
        IntervalExt interval;
        if (bit == 64) {
          interval = IntervalExt.UNBOUND;
        } else {
          interval = new IntervalExt((long) -Math.pow(2, bit - 1), (long) Math.pow(2, bit - 1) - 1);
        }

        intervalMap.put(varname, interval);


      }
    }
  }

  /**
   * getter for the mapping
   *
   * @return the Intervalmapping to each Variable
   */
  public TreeMap<Variable, IntervalExt> getMap() {
    return intervalMap;
  }
}
