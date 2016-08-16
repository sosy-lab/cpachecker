/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.util.cmc;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

@Options(prefix = "pcc.cmc")
public class AssumptionAutomatonGenerator {

  private final LogManager logger;

  @Option(secure = true, name = "file", description = "write collected assumptions to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumptionsFile = Paths.get("AssumptionAutomaton.txt");

  public AssumptionAutomatonGenerator(final Configuration config, final LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  public Set<ARGState> getAllAncestorsFor(final Collection<ARGState> nodes) {
    TreeSet<ARGState> uncoveredAncestors = new TreeSet<>();
    Deque<ARGState> toAdd = new ArrayDeque<>(nodes);

    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (uncoveredAncestors.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }
    return uncoveredAncestors;
  }

  public void writeAutomaton(final ARGState root, final List<ARGState> incompleteNodes) throws CPAException {
    assert(notCovered(incompleteNodes));

    try (Writer w = MoreFiles.openOutputFile(assumptionsFile, Charset.defaultCharset())) {
      logger.log(Level.FINEST, "Write assumption automaton to file ", assumptionsFile);
      AssumptionCollectorAlgorithm.writeAutomaton(w, root, getAllAncestorsFor(incompleteNodes),
          new HashSet<AbstractState>(incompleteNodes), 0, true);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not write assumption automaton for next partial ARG checking");
      throw new CPAException("Assumption automaton writing failed", e);
    }
  }

  private boolean notCovered(final List<ARGState> nodes){
    for(ARGState state: nodes) {
      if(state.isCovered()){
        return false;
      }
    }
    return true;
  }

}
