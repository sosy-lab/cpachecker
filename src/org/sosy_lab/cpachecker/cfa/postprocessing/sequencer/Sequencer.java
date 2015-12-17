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
package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.util.Collection;
import java.util.HashSet;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.FunctionCallCollector;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThread;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThreadContainer;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFAFunctionUtils;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;

@Options
public class Sequencer {

  private LogManager logger;

  private MutableCFA cfa;

  public Sequencer(MutableCFA cfa, LogManager logger) throws InvalidConfigurationException {
    this.logger = logger;
    this.cfa = cfa;
  }


  // TODO replace declaration of function calls of stubbed functions!!!!!
  // TODO assert edges in threaedIdentificator were not replaced !!
  public void sequenceCFA() {
    StubDeclaration stubDeclaration = new StubDeclaration();
    SequencePreparator threadIdentificator = new SequencePreparator(stubDeclaration, cfa);
    CThreadContainer threads = threadIdentificator.traverseAndReplaceFunctions();
    ControlVariables controlVariables = new ControlVariables(threads);
    POSIXStubs posixStubs = new POSIXStubs(controlVariables, stubDeclaration, cfa, logger);

    //TODO this stub or the posixStub stub
//    StubPosixFunctions.stubThreadCreationIntoFunction(threadIdentificator, controlVariables, cfa, logger);

    // create context switches
    for(CThread thread : threads.getAllThreads()) {
      exploreThreadRecursivly(thread, thread.getThreadFunction(), new HashSet<FunctionEntryNode>());
    }

    // build scheduler simulation function and the corresponding context switch edges
    ControlCodeBuilder threadControlCodeInjector = new ControlCodeBuilder(controlVariables, cfa, threads, logger);

    threadControlCodeInjector.buildControlVariableDeclaration();
    posixStubs.buildPthreadCreateBody();
    posixStubs.buildPThreadJoinBody();

    threadControlCodeInjector.buildScheduleSimulationFunction();

    cfa.setThreads(threads);
  }

  private void exploreThreadRecursivly(CThread thread,
      FunctionEntryNode entryNode, Collection<FunctionEntryNode> alreadyVisited) {
    assert entryNode != null;
    alreadyVisited.add(entryNode);

    CFAVisitor contextswtichMarker = new ThreadContextSwitchMarker(thread, cfa);
    FunctionCallCollector collector = new FunctionCallCollector();
    CompositeCFAVisitor visitor = new CompositeCFAVisitor(contextswtichMarker,
        collector);
    CFATraversal.dfs().traverseOnce(entryNode, visitor);

    for (AStatementEdge statementEdge : collector.getFunctionCalls()) {
      if(!CFAFunctionUtils.isExternFunction(statementEdge, cfa)) {
        FunctionEntryNode nextNode = cfa.getFunctionHead(CFAFunctionUtils
            .getFunctionName(statementEdge));
        if(alreadyVisited.contains(nextNode)) {
          continue;
        }
        exploreThreadRecursivly(thread, nextNode, alreadyVisited);
      }
    }
  }


}
