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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.ControlDependenceComputer;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.DominanceFrontier;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.LHOreduc;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.propertychecker.DefaultPropertyChecker;

@Options(prefix="pcc.arg")
public class ARG_CPAStrategy extends AbstractARGStrategy {

  @Option(secure=true,
      name = "checkPropertyPerElement",
      description = "Enable if used property checker implements satisfiesProperty(AbstractState) and checked property is violated for a set iff an element in this set exists for which violates the property")
  private boolean singleCheck = false;
  private List<AbstractState> visitedStates;
  private final StopOperator stop;
  private final TransferRelation transfer;
  private Configuration config;

  public ARG_CPAStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable PropertyCheckerCPA pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa == null ? new DefaultPropertyChecker() : pCpa.getPropChecker(), pShutdownNotifier, pProofFile);
    pConfig.inject(this);
    config = pConfig;
    if (pCpa == null) {
      stop = null;
      transfer = null;
    } else {
      if(!(pCpa.getWrappedCPAs().get(0) instanceof ARGCPA)) {
        throw new InvalidConfigurationException("Expect that the property checker cpa wraps an ARG cpa");
      }
      stop = ((ARGCPA)pCpa.getWrappedCPAs().get(0)).getWrappedCPAs().get(0).getStopOperator();
      transfer = ((ARGCPA)pCpa.getWrappedCPAs().get(0)).getWrappedCPAs().get(0).getTransferRelation();
    }
  }

  @Override
  protected void initChecking(final ARGState pRoot) {
    if (!singleCheck) {
      visitedStates = new ArrayList<>();
    }

  }

  @Override
  protected boolean checkCovering(final ARGState pCovered, final ARGState pCovering, final Precision pPrecision) throws CPAException, InterruptedException {
    return checkCoverWithStopOp(pCovered.getWrappedState(), Collections.singleton(pCovering.getWrappedState()), pPrecision);
  }

  @Override
  protected boolean isCheckSuccessful() {
    if (!singleCheck) {
      try {
        stats.getPropertyCheckingTimer().start();
        return propChecker.satisfiesProperty(visitedStates);
      } finally {
        stats.getPropertyCheckingTimer().stop();
      }
    }
    return true;
  }

  @Override
  protected boolean isCheckComplete() {
    return true;
  }

  @Override
  protected boolean checkForStatePropertyAndOtherStateActions(final ARGState pState) {
    if (!singleCheck) {
      visitedStates.add(pState);
    } else {
      return super.checkForStatePropertyAndOtherStateActions(pState);
    }
    return true;
  }

  @Override
  protected boolean prepareNextWaitlistIteration(final ReachedSet pReachedSet) {
    return true;
  }

  @Override
  protected boolean checkSuccessors(final ARGState pPredecessor, final Collection<ARGState> pSuccessors,
      final Precision pPrecision) throws InterruptedException, CPAException {
    Collection<AbstractState> wrappedSuccessors = new ArrayList<>(pSuccessors.size());
    for (ARGState succ: pSuccessors) {
      wrappedSuccessors.add(succ.getWrappedState());
    }

    Collection<? extends AbstractState> computedSuccessors =
        transfer.getAbstractSuccessors(pPredecessor.getWrappedState(), pPrecision);

     for (AbstractState succ : computedSuccessors) {
       if (!checkCoverWithStopOp(succ, wrappedSuccessors, pPrecision)) {
         return false;
       }
     }
    return true;
  }

  @Override
  protected boolean addSuccessors(final Collection<ARGState> pSuccessors, final ReachedSet pReachedSet,
      final Precision pPrecision) {
    for (ARGState argS : pSuccessors) {
      pReachedSet.add(argS, pPrecision);
    }
    return true;
  }

  @Override
  protected boolean treatStateIfCoveredByUnkownState(final ARGState pCovered, final ARGState pCoveringState,
      final ReachedSet pReachedSet,
      final Precision pPrecision) {
    pReachedSet.add(pCoveringState, pPrecision);
    return false;
  }

  private boolean checkCoverWithStopOp(final AbstractState pCovered, final Collection<AbstractState> pCoverElems,
      final Precision pPrecision) throws CPAException, InterruptedException {
    return stop.stop(pCovered, pCoverElems, pPrecision);
  }

  @Option(
    secure = true,
    name = "pcc.lhoReadFile",
    description = "file in which lho-Order was stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  protected Path lhoOrderFile =
      Paths.get("D:\\\\eclipse-workspace\\\\CPAchecker-IFC\\\\output\\\\lho.obj");

  private LogManager lLogger;

  // Reading
  // Variable for LhoOrder
  private ArrayList<CFANode> lhoOrder;
  // Post-Dominators
  private Map<CFANode, List<CFANode>> rDom;
  // temporary CFA
  private CFA cfa;

  // Building
  // Control-Dependencies

  // Intervals
  private Map<CFANode, Interval> ancestorRelation;

  private UnmodifiableReachedSet reachSet;
  private LHOreduc lhoreduc;

  private void initPDCert() {
    lhoreduc =
        LHOreduc.readLHOOrder(
            Paths.get("D:\\eclipse-workspace\\CPAchecker-IFC\\output\\lho.obj"),
            lLogger);
    lhoOrder = lhoreduc.getLHO();
    rDom = lhoreduc.getRDom();
    cfa = lhoreduc.getCFA();
  }

  private boolean checkDomisRootedTree() {
    if (!(cfa.getMainFunction().getExitNode().equals(lhoOrder.get(0)))) {
      return false;
    }
    if (!(lhoreduc.checkForTreeandCycle(cfa))) {
      return false;
    }
    return true;

  }


  private boolean doDFSIntervals() {
    if (!(lhoreduc.doDFS(cfa))) {
      return false;
    }
    ancestorRelation=lhoreduc.getAncestors();
    return true;
  }

  private boolean checkParentProperty() {
    for (CFANode v : cfa.getAllNodes()) {
      int vlength = v.getNumLeavingEdges();
      for (int j = 0; j < vlength; j++) {
        CFANode w = v.getLeavingEdge(j).getSuccessor();
        CFANode parentofV = lhoreduc.getDom().get(v);
        lLogger.log(Level.FINE, v + "," + w + "," + parentofV);
        Interval invw = ancestorRelation.get(w);
        Interval invpv = ancestorRelation.get(parentofV);
        lLogger.log(Level.FINE, "[]" + "," + invw + "," + invpv);
        boolean contained =
            (invpv.getHigh() >= invw.getHigh()) && (invpv.getLow() <= invw.getLow());
        if (!contained) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean checkLHOProperty() {
    for (CFANode v : cfa.getAllNodes()) {
      if (!(cfa.getMainFunction().getExitNode().equals(v))) {
        lLogger.log(Level.FINE, v);
        /* (t(v),v) */
        CFANode parentofV = lhoreduc.getDom().get(v);
        boolean one = lhoreduc.existsEdge(parentofV, v);

        /* (u,v), (w,v) lh(u)<lh(v)<lh(w) */
        int vlength = v.getNumLeavingEdges();
        TreeSet<Integer> positions = new TreeSet<>();
        for (int j = 0; j < vlength; j++) {
          CFANode uw = v.getLeavingEdge(j).getSuccessor();
          positions.add(lhoOrder.indexOf(uw));
        }
        LinkedList<Integer> positions2 = new LinkedList<>(positions);
        boolean two =
            ((vlength >= 2)
                && positions2.get(0) != lhoOrder.indexOf(v)
                && positions2.get(vlength - 1) != lhoOrder.indexOf(v));

        if ((one || two) == false) {
          lLogger.log(Level.FINE, one);
          lLogger.log(Level.FINE, two);
          return false;
        }
      }
    }
    return true;
  }

  // TODO
  @Override
  public boolean checkCertificate(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    // Initialize
    reachSet = pReachedSet;
    initPDCert();
    // Check rDom is a tree rooted in N0
    if (!checkDomisRootedTree()) {
      lLogger.log(Level.FINE, "DomisnotRootedTree");
      return false;
    }
    // Do DFS traversal
    if (!doDFSIntervals()) {
      lLogger.log(Level.FINE, "Intervals");
      return false;
    }
    // CheckParentProperty
    if (!checkParentProperty()) {
      lLogger.log(Level.FINE, "ParentProperty");
      return false;
    }
    // CheckLHOProperty
    if (!checkLHOProperty()) {
      lLogger.log(Level.FINE, "LHOProperty");
      return false;
    }
    // Compute PDF
    DominanceFrontier domfron = new DominanceFrontier(cfa, lhoreduc.getDom());
    domfron.execute();
    Map<CFANode, TreeSet<CFANode>> df = domfron.getDominanceFrontier();

    ControlDependenceComputer cdcom = new ControlDependenceComputer(cfa, df);
    cdcom.execute();
    Map<CFANode, TreeSet<CFANode>> cd = cdcom.getControlDependency();

    Map<CFANode, TreeSet<CFANode>> recd = cdcom.getReversedControlDependency();

    // Control Dependencies (Reversed)
    Map<CFANode, TreeSet<CFANode>> rcd = recd;

    // Check as normal
    boolean result = super.checkCertificate(pReachedSet);
    return result;
  }

}
