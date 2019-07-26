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
package org.sosy_lab.cpachecker.pcc.strategy.arg;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.LHOreduc;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "arg")
public class LHOCheck_Strategy extends ARG_CPAStrategy {

  @Option(
    secure = true,
    name = "pcc.lhoReadFile",
    description = "file in which lho-Order was stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  Path lhoOrderFile1 = Paths.get("D:\\\\eclipse-workspace\\\\CPAchecker-IFC\\\\output\\\\lho.obj");

  private LogManager logger;
  private final StopOperator stop;
  private final TransferRelation transfer;

  // Reading
  // Variable for LhoOrder
  private ArrayList<CFANode> lhoOrder;
  // Post-Dominators
  private Map<CFANode, List<CFANode>> rDom;

  // Building
  // Control-Dependencies

  // Intervals
  private Map<CFANode, Interval> intervals;

  private UnmodifiableReachedSet reachSet;

  public LHOCheck_Strategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pProofFile, pCpa);
    pConfig.inject(this);
    logger = pLogger;
    if (pCpa == null) {
      stop = null;
      transfer = null;
    } else {
      if (!(pCpa.getWrappedCPAs().get(0) instanceof ARGCPA)) {
        throw new InvalidConfigurationException(
            "Expect that the property checker cpa wraps an ARG cpa");
      }
      stop = ((ARGCPA) pCpa.getWrappedCPAs().get(0)).getWrappedCPAs().get(0).getStopOperator();
      transfer =
          ((ARGCPA) pCpa.getWrappedCPAs().get(0)).getWrappedCPAs().get(0).getTransferRelation();
    }
  }

  private void initPDCert() {
    LHOreduc lhoreduc =
        LHOreduc.readLHOOrder(
            Paths.get("D:\\eclipse-workspace\\CPAchecker-IFC\\output\\lho.obj"),
            logger);
    lhoOrder = lhoreduc.getLHO();
    rDom = lhoreduc.getRDom();
  }

  private boolean checkDomisRootedTree() {
    ARGState root= (ARGState) reachSet.getLastState();
    CFANode cRoot=null;
    CompositeState wstate = (CompositeState) root.getWrappedState();
    for (AbstractState cstate : wstate.getWrappedStates()) {
      if (cstate instanceof LocationState) {
        LocationState lstate = (LocationState) cstate;
        cRoot = lstate.getLocationNode();
      }
    }
    return (cRoot.equals(lhoOrder.get(1)));

  }

  private boolean doDFSIntervals() {
    for (int i = 0; i < lhoOrder.size(); i++) {
      int j = i;

    }
    return true;
  }

  private boolean checkParentProperty() {
    return true;
  }

  private boolean checkLHOProperty() {
    return true;
  }

  // TODO
  @Override
  public boolean checkCertificate(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "HERE");
    // Initialize
    reachSet = pReachedSet;
    initPDCert();
    // Check rDom is a tree rooted in N0
    if (!checkDomisRootedTree()) {
      logger.log(Level.FINE, "DomisnotRootedTree");
      return false;
    }
    // Do DFS traversal
    if (!doDFSIntervals()) {

    }
    // CheckParentProperty
    if (!checkParentProperty()) {

    }
    // CheckLHOProperty
    if (!checkLHOProperty()) {

    }
    // Compute PDF

    // Check as normal
    boolean result = super.checkCertificate(pReachedSet);
    return result;
  }



}
