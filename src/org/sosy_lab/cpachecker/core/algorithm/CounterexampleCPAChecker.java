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

import static com.google.common.collect.Iterables.isEmpty;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix="counterexample.checker")
public class CounterexampleCPAChecker implements CounterexampleChecker {

  private final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  @Option(name="config",
      description="configuration file for counterexample checks with CPAchecker")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private File configFile = new File("config/explicitAnalysis-no-cbmc.properties");

  public CounterexampleCPAChecker(Configuration config, LogManager logger, ReachedSetFactory pReachedSetFactory, CFA pCfa) throws InvalidConfigurationException {
    this.logger = logger;
    config.inject(this);
    this.reachedSetFactory = pReachedSetFactory;
    this.cfa = pCfa;
  }

  @Override
  public boolean checkCounterexample(ARGState pRootElement,
      ARGState pErrorElement, Set<ARGState> pErrorPathElements)
      throws CPAException, InterruptedException {

    String automaton =
        produceGuidingAutomaton(pRootElement, pErrorPathElements);

    File automatonFile;
    try {
      automatonFile = Files.createTempFile("automaton", ".txt", automaton);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path automaton to file " + e.getMessage(), e);
    }

    CFAFunctionDefinitionNode entryNode = (CFAFunctionDefinitionNode)extractLocation(pRootElement);

    try {
      Configuration lConfig = Configuration.builder()
              .loadFromFile(configFile)
              .setOption("specification", automatonFile.getAbsolutePath())
              .build();

      CPABuilder lBuilder = new CPABuilder(lConfig, logger, reachedSetFactory);
      ConfigurableProgramAnalysis lCpas = lBuilder.buildCPAs(cfa);
      Algorithm lAlgorithm = new CPAAlgorithm(lCpas, logger, lConfig);
      PartitionedReachedSet lReached = new PartitionedReachedSet(TraversalMethod.DFS);
      lReached.add(lCpas.getInitialElement(entryNode), lCpas.getInitialPrecision(entryNode));

      lAlgorithm.run(lReached);

      if (isEmpty(filterTargetElements(lReached))) {
        return false; // target state is not reachable, counterexample is infeasible
      } else {
        return true;
      }

    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration in counterexample-check config: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);
    } finally {
      // delete temp file so it is gone even if JVM is killed
      automatonFile.delete();
    }
  }

  private String produceGuidingAutomaton(ARGState pRootElement,
      Set<ARGState> pPathElements) {
    StringBuilder sb = new StringBuilder();
    sb.append("CONTROL AUTOMATON AssumptionAutomaton\n\n");
    sb.append("INITIAL STATE ARG" + pRootElement.getElementId() + ";\n\n");

    for (ARGState e : pPathElements) {

      CFANode loc = AbstractStates.extractLocation(e);
      sb.append("STATE USEFIRST ARG" + e.getElementId() + " :\n");

      for (ARGState child : e.getChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringElement();
          assert !child.isCovered();
        }

        if (pPathElements.contains(child)) {
          CFANode childLoc = AbstractStates.extractLocation(child);
          CFAEdge edge = loc.getEdgeTo(childLoc);
          sb.append("    MATCH \"");
          escape(edge.getRawStatement(), sb);
          sb.append("\" -> ");

          if (child.isTarget()) {
            sb.append("ERROR");
          } else {
            sb.append("GOTO ARG" + child.getElementId());
          }
          sb.append(";\n");
        }
      }
      sb.append("    TRUE -> STOP;\n\n");
    }
    sb.append("END AUTOMATON\n");

    return sb.toString();
  }

  private static void escape(String s, StringBuilder appendTo) {
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