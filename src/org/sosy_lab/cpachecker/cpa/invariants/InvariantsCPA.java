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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptAllVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * This is a CPA for collecting simple syntactic invariants about integer variables.
 */
public class InvariantsCPA extends AbstractCPA {

  @Options(prefix="cpa.invariants")
  public static class InvariantsOptions {

    @Option(values={"JOIN", "SEP"}, toUppercase=true,
        description="which merge operator to use for InvariantCPA")
    private String merge = "JOIN";

  }

  private final boolean useBitvectors;

  private final Configuration config;

  private final LogManager logManager;

  private final ReachedSetFactory reachedSetFactory;

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(InvariantsCPA.class).withOptions(InvariantsOptions.class);
  }

  public InvariantsCPA(Configuration config, LogManager logger, InvariantsOptions options,
      ReachedSetFactory pReachedSetFactory, CFA pCfa) throws InvalidConfigurationException {
    super(options.merge, "sep", InvariantsDomain.INSTANCE, InvariantsTransferRelation.INSTANCE);
    this.config = config;
    this.logManager = logger;
    this.reachedSetFactory = pReachedSetFactory;
    this.cfa = pCfa;
    this.useBitvectors = true;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    try {
      Configuration.Builder configurationBuilder = Configuration.builder().copyFrom(config);
      configurationBuilder.setOption("output.disable", "true");
      //configurationBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.invariants.variableselection.VariableSelectionCPA");
      configurationBuilder.setOption("specification", "config/specification/default.spc");

      ConfigurableProgramAnalysis cpa = new CPABuilder(configurationBuilder.build(), logManager, reachedSetFactory).buildCPAs(cfa);
      ReachedSet reached = reachedSetFactory.create();
      reached.add(cpa.getInitialState(pNode), cpa.getInitialPrecision(pNode));
      new CPAAlgorithm(cpa, logManager, config).run(reached);
      Set<CFAEdge> relevantEdges = new HashSet<>();
      for (AbstractState state : FluentIterable.from(reached).filter(AbstractStates.IS_TARGET_STATE)) {
        CFANode location = AbstractStates.extractLocation(state);
        Queue<CFANode> nodes = new ArrayDeque<>();
        nodes.add(location);
        while (!nodes.isEmpty()) {
          location = nodes.poll();
          for (int i = 0; i < location.getNumEnteringEdges(); ++i) {
            CFAEdge edge = location.getEnteringEdge(i);
            if (relevantEdges.add(edge)) {
              nodes.add(edge.getPredecessor());
            }
          }
        }
      }
      VariableSelection<CompoundState> variableSelection = new AcceptAllVariableSelection<>();
      return new InvariantsState(this.useBitvectors, variableSelection, ImmutableSet.copyOf(relevantEdges));
    } catch (InvalidConfigurationException | CPAException | InterruptedException e) {
      this.logManager.logException(Level.SEVERE, e, "Unable to select specific variables. Defaulting to selecting all variables.");
    }
    return new InvariantsState(this.useBitvectors, new AcceptAllVariableSelection<CompoundState>());
  }
}