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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;

/**
 * Class for MinMaxMapping. It specifies three things. First: A map for the minimum allowed
 * remaining entropy respectively for all specified variables Second: A map for the maximum allowed
 * entropy loss on average respectively for all specified variables Third: A map for the minimum
 * allowed min-Entropy respectively for all specified variables
 */
public class MinMaxMapping {
  private LogManager logger;
  /** map for minimum remaining entropy */
  private TreeMap<Variable, Double> minRemEnt;
  /** map for maximum entropy loss on average */
  private TreeMap<Variable, Double> maxLoss;
  /** map for minimum allowed min-entropy */
  private TreeMap<Variable, Double> minminEnt;

  /**
   * Constructor which gets a path to the mapping file and reads it. Then it fills all three maps
   * with the mappings
   *
   * @param filePath the path to the mapping file
   */
  public MinMaxMapping(Path filePath, LogManager logger) {


    this.logger = logger;
    this.logger.logf(Level.CONFIG, "MinMaxMapping input: %s", filePath);
    minRemEnt = new TreeMap<>();
    maxLoss = new TreeMap<>();
    minminEnt = new TreeMap<>();

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

      if (line.contains("minRemEnt")) {

        int bra = line.indexOf("(");
        int sep = line.indexOf(",");
        int cket = line.indexOf(")");

        Variable varname = new Variable(line.substring(bra + 1, sep).trim());
        double entleft = Double.parseDouble(line.substring(sep + 1, cket).trim());
        minRemEnt.put(varname, entleft);
        logger.logf(Level.FINEST, "Minimum entropy for %s set to %f .", varname, entleft);

      } else if (line.contains("maxLoss")) {

        int bra = line.indexOf("(");
        int sep = line.indexOf(",");
        int cket = line.indexOf(")");

        Variable varname = new Variable(line.substring(bra + 1, sep).trim());
        double maxLos = Double.parseDouble(line.substring(sep + 1, cket).trim());

        maxLoss.put(varname, maxLos);
        // Log.Log("Maximum entropyloss for " + varname + " set to " + maxLos);
        logger.logf(Level.FINEST, "Maximum entropyloss for %s set to %f .", varname, maxLos);

      } else if (line.contains("minminEnt")) {

        int bra = line.indexOf("(");
        int sep = line.indexOf(",");
        int cket = line.indexOf(")");

        Variable varname = new Variable(line.substring(bra + 1, sep).trim());
        double minent = Double.parseDouble(line.substring(sep + 1, cket).trim());
        minminEnt.put(varname, minent);
        // Log.Log("Minimum min-entropy for " + varname + " set to " + minent);
        logger.logf(Level.FINEST, "Minimum Min-Entropy for %s set to %f .", varname, minent);
      }
    }
  }

  /** @return the minimum allowed entropy map */
  public TreeMap<Variable, Double> getMinRemEntMap() {
    return minRemEnt;
  }

  /** @return the maximum allowed entropy loss */
  public TreeMap<Variable, Double> getMaxLossMap() {
    return maxLoss;
  }

  /** @return the minumum allowed min-Entropy */
  public TreeMap<Variable, Double> getMinMinEntMap() {
    return minminEnt;
  }
}
