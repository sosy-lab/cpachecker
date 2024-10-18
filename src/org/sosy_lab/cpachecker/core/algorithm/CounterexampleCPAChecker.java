/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import static org.sosy_lab.cpachecker.util.AbstractElements.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Option.Type;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;

@Options(prefix="counterexample.checker")
public class CounterexampleCPAChecker implements CounterexampleChecker {

  private final LogManager logger;

  @Option(name="config",
      type=Type.REQUIRED_INPUT_FILE,
      description="configuration file for counterexample checks with CPAchecker")
  private File configFile = new File("test/config/explicitAnalysisInf.properties");

  public CounterexampleCPAChecker(Configuration config, LogManager logger) throws InvalidConfigurationException {
    this.logger = logger;
    config.inject(this);
  }

  @Override
  public boolean checkCounterexample(ARTElement pRootElement,
      ARTElement pErrorElement, Set<ARTElement> pErrorPathElements)
      throws CPAException, InterruptedException {

    String automaton =
        produceGuidingAutomaton(pRootElement, pErrorPathElements);

    File automatonFile;
    try {
      automatonFile = Files.createTempFile("automaton", ".txt", automaton);
    } catch (IOException e) {
      throw new CPAException("Could not write automaton for explicit analysis check (" + e.getMessage() + ")");
    }

    CFAFunctionDefinitionNode cfa = (CFAFunctionDefinitionNode)extractLocation(pRootElement);

    try {
      Configuration lConfig = Configuration.builder()
              .loadFromFile(configFile)
              .setOption("specification", automatonFile.getAbsolutePath())
              .build();

      CPABuilder lBuilder = new CPABuilder(lConfig, logger);
      ConfigurableProgramAnalysis lCpas = lBuilder.buildCPAs();
      Algorithm lAlgorithm = new CPAAlgorithm(lCpas, logger);
      PartitionedReachedSet lReached = new PartitionedReachedSet(TraversalMethod.DFS);
      lReached.add(lCpas.getInitialElement(cfa), lCpas.getInitialPrecision(cfa));

      lAlgorithm.run(lReached);

      if (isEmpty(filterTargetElements(lReached))) {
        return false; // target state is not reachable, counterexample is infeasible
      } else {
        return true;
      }

    } catch (InvalidConfigurationException e) {
      throw new CPAException("Invalid configuration for counterexample check: " + e.getMessage());
    } catch (IOException e) {
      throw new CPAException("Error during counterexample check: " + e.getMessage());
    }
  }

  private String produceGuidingAutomaton(ARTElement pRootElement,
      Set<ARTElement> pPathElements) {
    StringBuilder sb = new StringBuilder();
    sb.append("CONTROL AUTOMATON AssumptionAutomaton\n\n");
    sb.append("INITIAL STATE ART" + pRootElement.getElementId() + ";\n\n");

    for (ARTElement e : pPathElements) {

      CFANode loc = AbstractElements.extractLocation(e);
      sb.append("STATE USEFIRST ART" + e.getElementId() + " :\n");

      for (ARTElement child : e.getLocalChildren()) {
        if (child.isCovered()) {
          child = child.getCoveringElement();
          assert !child.isCovered();
        }

        if (pPathElements.contains(child)) {
          CFANode childLoc = AbstractElements.extractLocation(child);
          CFAEdge edge = loc.getEdgeTo(childLoc);
          sb.append("    MATCH \"");
          escape(edge.getRawStatement(), sb);
          sb.append("\" -> ");

          if (child.isTarget()) {
            sb.append("ERROR");
          } else {
            sb.append("GOTO ART" + child.getElementId());
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
