/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Set;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.Files.DeleteOnCloseFile;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix="counterexample.checker")
public class CounterexampleCPAChecker implements CounterexampleChecker {

  private final LogManager logger;
  private final CFA cfa;
  private final String filename;

  @Option(name="config",
      description="configuration file for counterexample checks with CPAchecker")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private File configFile = new File("config/explicitAnalysis-no-cbmc.properties");

  public CounterexampleCPAChecker(Configuration config, LogManager logger,
      CFA pCfa, String pFilename) throws InvalidConfigurationException {
    this.logger = logger;
    config.inject(this);
    this.cfa = pCfa;
    this.filename = pFilename;
  }


  @Override
  public boolean checkCounterexample(ARGState pRootState,
      ARGState pErrorState, Set<ARGState> pErrorPathStates)
      throws CPAException, InterruptedException {

    // This temp file will be automatically deleted when the try block terminates.
    try (DeleteOnCloseFile automatonFile = Files.createTempFile("automaton", ".txt")) {

      try (Writer w = Files.openOutputFile(automatonFile.toPath())) {
        produceGuidingAutomaton(w, pRootState, pErrorPathStates);
      }

      return checkCounterexample(pRootState, automatonFile.toPath());

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path automaton to file " + e.getMessage(), e);
    }
  }

  private boolean checkCounterexample(ARGState pRootState, Path automatonFile)
      throws CPAException, InterruptedException {

    FunctionEntryNode entryNode = (FunctionEntryNode)extractLocation(pRootState);

    try {
      Configuration lConfig = Configuration.builder()
              .loadFromFile(configFile)
              .setOption("specification", automatonFile.toAbsolutePath().toString())
              .build();

      CoreComponentsFactory factory = new CoreComponentsFactory(lConfig, logger);
      ConfigurableProgramAnalysis lCpas = factory.createCPA(cfa, null);
      Algorithm lAlgorithm = factory.createAlgorithm(lCpas, filename, cfa, null);
      ReachedSet lReached = factory.createReachedSet();
      lReached.add(lCpas.getInitialState(entryNode), lCpas.getInitialPrecision(entryNode));

      lAlgorithm.run(lReached);

      // counterexample is feasible if a target state is reachable
      return from(lReached).anyMatch(IS_TARGET_STATE);

    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration in counterexample-check config: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);
    }
  }

  private void produceGuidingAutomaton(Appendable sb, ARGState pRootState,
      Set<ARGState> pPathStates) throws IOException {
    sb.append("CONTROL AUTOMATON AssumptionAutomaton\n\n");
    sb.append("INITIAL STATE ARG" + pRootState.getStateId() + ";\n\n");

    for (ARGState s : pPathStates) {

      CFANode loc = AbstractStates.extractLocation(s);
      sb.append("STATE USEFIRST ARG" + s.getStateId() + " :\n");

      for (ARGState child : s.getChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        if (pPathStates.contains(child)) {
          CFANode childLoc = AbstractStates.extractLocation(child);
          CFAEdge edge = loc.getEdgeTo(childLoc);
          sb.append("    MATCH \"");
          escape(edge.getRawStatement(), sb);
          sb.append("\" -> ");

          if (child.isTarget()) {
            sb.append("ERROR");
          } else {
            sb.append("GOTO ARG" + child.getStateId());
          }
          sb.append(";\n");
        }
      }
      sb.append("    TRUE -> STOP;\n\n");
    }
    sb.append("END AUTOMATON\n");
  }

  private static void escape(String s, Appendable appendTo) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '\n':
        appendTo.append("\\n");
        break;
      case '\"':
        appendTo.append("\\\"");
        break;
      case '\\':
        appendTo.append("\\\\");
        break;
      default:
        appendTo.append(c);
        break;
      }
    }
  }
}