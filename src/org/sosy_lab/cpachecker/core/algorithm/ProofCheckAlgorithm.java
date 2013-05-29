/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options
public class ProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer transferTimer = new Timer();
    private Timer stopTimer = new Timer();
    private Timer readTimer = new Timer();
    private Timer preparationTimer = new Timer();
    private Timer propertyCheckingTimer = new Timer();

    private int countIterations = 0;

    @Override
    public String getName() {
      return "Proof Check algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println("Number of iterations:                     " + countIterations);
      out.println();
      out.println("Total time for proof check algorithm:     " + totalTimer);
      out.println("  Time for reading in proof:              " + readTimer);
      out.println("  Time for preparing proof for checking:          " + preparationTimer);
      out.println("  Time for abstract successor checks:     " + transferTimer + " (Calls: "
          + transferTimer.getNumberOfIntervals() + ")");
      out.println("  Time for covering checks:               " + stopTimer + " (Calls: "
          + stopTimer.getNumberOfIntervals()
          + ")");
      out.println(" Time for checking property:          "   + propertyCheckingTimer);
    }
  }

  private static final String CPA_CLASS_PREFIX ="org.sosy_lab.cpachecker";

  private final CPAStatistics stats = new CPAStatistics();
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;

  @Option(name = "pcc.proofFile", description = "file in which ARG representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File file = new File("arg.obj");

  @Option(
      name = "pcc.proofType",
      description = "defines proof representation, either abstract reachability graph or set of reachable abstract states",
      values = { "ARG", "SET", "PSET" })
  private String pccType = "ARG";

  @Option(
      name = "pcc.proofcheck.precision",
      description = "Flag indicating if analysis which constructed proof adjusted precisions during computation.")
  private boolean considerPrecisionAdjustment = true;

  @Option(
      name = "pcc.proofcheck.propertychecker",
      description = "Qualified name for class which checks that the computed abstraction adheres to the desired property.")
  private String checkerClass = "org.sosy_lab.cpachecker.pcc.propertychecker.DefaultPropertyChecker";
  @Option(
      name = "pcc.proofcheck.propertychecker.parameters",
      description = "List of parameters for constructor of pcc.proofcheck.propertychecker. Parameter values are " +
      		"specified in the order they parameters are defined in the respective constructor. Every parameter value is finished " +
      		"with \",\". The empty string represents an empty parameter list.")
  private String checkerParamList = "";

  private final Object proof;
  private Multimap<CFANode, AbstractState> statesPerLocation;
  private PropertyChecker propertyChecker;


  public ProofCheckAlgorithm(ConfigurableProgramAnalysis cpa, Configuration pConfig, LogManager logger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    this.cpa = cpa;
    this.logger = logger;

    Object proof = null;
    try {
      proof = readProof();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed reading proof.", e);
    }
    this.proof = prepareForChecking(proof);

    System.gc();
  }

  protected ProofCheckAlgorithm(ConfigurableProgramAnalysis cpa, Configuration pConfig, LogManager logger, ReachedSet pReachedSet)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    this.cpa = cpa;
    this.logger = logger;

    if (pReachedSet == null || pReachedSet.hasWaitingState()) { throw new IllegalArgumentException(
        "Parameter pReachedSet may not be null and may not have any states in its waitlist."); }

    Object proof;
    if (pccType.equals("ARG")) {
      proof = pReachedSet.getFirstState();
    } else if (pccType.equals("SET")) {
      proof = new AbstractState[pReachedSet.size()];
      pReachedSet.asCollection().toArray((AbstractState[]) proof);
    } else if (pccType.equals("PSET")) {
      proof = ProofGenerator.computePartialReachedSet(pReachedSet);
    } else {
      throw new InvalidConfigurationException("Undefined proof format. Only use predefined values for pcc.proofType.");
    }

    this.proof = prepareForChecking(proof);
  }

  private Object readProof() throws IOException, ClassNotFoundException {
    stats.totalTimer.start();
    stats.readTimer.start();

    InputStream fis = null;
    try {

      fis = new FileInputStream(file);
      ZipInputStream zis = new ZipInputStream(fis);

      ZipEntry entry = zis.getNextEntry();
      assert entry.getName().equals("Proof");
      zis.closeEntry();

      entry = zis.getNextEntry();
      assert entry.getName().equals("Helper");
      ObjectInputStream o = new ObjectInputStream(zis);
      //read helper storages
      int numberOfStorages = o.readInt();
      for (int i = 0; i < numberOfStorages; ++i) {
        Serializable storage = (Serializable) o.readObject();
        GlobalInfo.getInstance().addHelperStorage(storage);
      }
      zis.closeEntry();

      o.close();
      zis.close();
      fis.close();

      fis = new FileInputStream(file);
      zis = new ZipInputStream(fis);
      entry = zis.getNextEntry();
      assert entry.getName().equals("Proof");
      o = new ObjectInputStream(zis);
      return o.readObject();

    } finally {
      fis.close();
      stats.readTimer.stop();
      stats.totalTimer.stop();
    }
  }

  private Object prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    stats.totalTimer.start();
    stats.preparationTimer.start();

    createPropertyChecker();

    try {
      if (pccType.equals("ARG")) {
        if (considerPrecisionAdjustment && !(cpa instanceof ProofChecker)) { throw new InvalidConfigurationException(
            "ProofCheckAlgorithm needs a CPA that implements the ProofChecker interface."); }
        if (!(pReadProof instanceof ARGState)) { throw new InvalidConfigurationException(
            "Proof Type requires ARG."); }
      } else if (pccType.equals("SET")) {
        if (!(pReadProof instanceof AbstractState[])) { throw new InvalidConfigurationException(
            "Proof Type requires reached set as set of abstract states."); }
        orderReachedSetByLocation((AbstractState[]) pReadProof);
      } else if (pccType.equals("PSET")) {
        if (!(pReadProof instanceof AbstractState[])) { throw new InvalidConfigurationException(
            "Proof Type requires reached set as set of abstract states."); }
        if (CPAs.retrieveCPA(cpa, LocationCPABackwards.class) != null) { throw new InvalidConfigurationException(
            "Partial reached set not supported as certificate for backward analysis"); }
        orderReachedSetByLocation((AbstractState[]) pReadProof);
      }

    } finally {
      stats.preparationTimer.stop();
      stats.totalTimer.stop();
    }

    return pReadProof;
  }

  private void createPropertyChecker() throws InvalidConfigurationException {
    if (checkerClass == null) { throw new InvalidConfigurationException(
        "No property checker defined. Please specifiy a class which "); }

    Class<?> propertyCheckerClass;
    try {
      propertyCheckerClass = Classes.forName(checkerClass, CPA_CLASS_PREFIX);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "Option pcc.proofcheck.propertychecker is set to unknown PropertyChecker " + checkerClass, e);
    }

    if (!PropertyChecker.class.isAssignableFrom(propertyCheckerClass)) { throw new InvalidConfigurationException(
        "Option pcc.proofcheck.propertychecker must be set to a class implementing the PropertyChecker interface!"); }

    // get list of parameters
    String[] param;

    if (checkerParamList.equals("")) {
      param = new String[0];
    } else {
      String[] result = checkerParamList.split(",", -1);
      param = new String[result.length - 1];
      for (int i = 0; i < param.length; i++) {
        param[i] = result[i];
      }
    }
    // construct property checker instance
    try {
      Constructor<?>[] cons = propertyCheckerClass.getConstructors();

      Class<?>[] paramTypes;
      Constructor<?> constructor = null;
      for (Constructor<?> con : cons) {
        paramTypes = con.getParameterTypes();
        if (paramTypes.length != param.length)
          continue;
        else {
          for (Class<?> paramType : paramTypes) {
            if (paramType != String.class)
              continue;
          }
        }
        constructor = con;
        break;
      }

      if (constructor == null) { throw new UnsupportedOperationException(
          "Cannot create PropertyChecker " + checkerClass + " if it does not provide a constructor with "
              + param.length + " String parameters."); }

      propertyChecker = (PropertyChecker) constructor.newInstance((Object[]) param);
    } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      throw new UnsupportedOperationException(
          "Creation of specified PropertyChecker instance failed.", e);
    }
  }

  protected String getPCCType() {
    return pccType;
  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    if (proof == null)
      return false;

    stats.totalTimer.start();
// TODO put different check algorithms (methods) in own classes
    logger.log(Level.INFO, "Proof check algorithm started");
    boolean result = false;

    if (pccType.equals("ARG")) {
      if (considerPrecisionAdjustment)
        result = checkARG(reachedSet);
      else
        result = checkARG2(reachedSet);
    } else if (considerPrecisionAdjustment) {
      logger
          .log(
              Level.SEVERE,
              "Analysis with precision adjustment changing the state or precision of an element",
              "currently only supported for proof type ARG.");
    } else {
      if (pccType.equals("SET")) {
        result = checkReachedSet(reachedSet);
      } else if (pccType.equals("PSET")) {
        result = checkPartialReachedSet(reachedSet);
      } else {
        logger.log(Level.SEVERE, "Undefined proof format. No checking available.");
      }
    }

    stats.totalTimer.stop();

    return result;
  }

  private boolean checkARG(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    ProofChecker checker = (ProofChecker) cpa;
    ARGState rootState = (ARGState) proof;

    //TODO does not account for strengthen yet (proof check will fail if strengthen is needed to explain successor states)


    logger.log(Level.INFO, "Proof check algorithm started");

    AbstractState initialState = reachedSet.popFromWaitlist();
    Precision initialPrecision = reachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!(checker.isCoveredBy(initialState, rootState) && checker.isCoveredBy(rootState, initialState))) {
      logger.log(Level.WARNING, "Root state of proof is invalid.");
      return false;
    }

    reachedSet.add(rootState, initialPrecision);

    Set<ARGState> postponedStates = new HashSet<>();

    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    Set<ARGState> inWaitlist = new HashSet<>();
    inWaitlist.add(rootState);

    boolean unexploredParent;

    do {
      for (ARGState e : postponedStates) {
        if (!reachedSet.contains(e.getCoveringState())) {
          logger.log(Level.WARNING, "Covering state", e.getCoveringState(), "was not found in reached set");
          return false;
        }
        reachedSet.reAddToWaitlist(e);
      }
      postponedStates.clear();

      while (reachedSet.hasWaitingState()) {
        CPAchecker.stopIfNecessary();

        stats.countIterations++;
        ARGState state = (ARGState) reachedSet.popFromWaitlist();
        inWaitlist.remove(state);

        logger.log(Level.FINE, "Looking at state", state);

        stats.propertyCheckingTimer.start();
        if (!propertyChecker.satisfiesProperty(state)) {
          stats.propertyCheckingTimer.stop();
          return false;
        }
        stats.propertyCheckingTimer.stop();

        if (state.isCovered()) {

          logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
          ARGState coveringState = state.getCoveringState();

          if (!reachedSet.contains(coveringState)) {
            postponedStates.add(state);
            continue;
          }

          stats.stopTimer.start();
          if (!isCoveringCycleFree(state)) {
            stats.stopTimer.stop();
            logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
            return false;
          }
          if (!checker.isCoveredBy(state, coveringState)) {
            stats.stopTimer.stop();
            logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
            return false;
          }
          stats.stopTimer.stop();
        } else {
          stats.transferTimer.start();
          Collection<ARGState> successors = state.getChildren();
          logger.log(Level.FINER, "Checking abstract successors", successors);
          if (!checker.areAbstractSuccessors(state, null, successors)) {
            stats.transferTimer.stop();
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
          stats.transferTimer.stop();
          for (ARGState e : successors) {
            unexploredParent = false;
            for (ARGState p : e.getParents()) {
              if (!reachedSet.contains(p) || inWaitlist.contains(p)) {
                waitingForUnexploredParents.add(e);
                unexploredParent = true;
                break;
              }
            }
            if (unexploredParent) {
              continue;
            }
            if (reachedSet.contains(e)) {
              // state unknown parent of e
              logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
              return false;
            } else {
              waitingForUnexploredParents.remove(e);
              reachedSet.add(e, initialPrecision);
              inWaitlist.add(e);
            }
          }
        }
      }
    } while (!postponedStates.isEmpty());

    return waitingForUnexploredParents.isEmpty();
  }

  private boolean checkARG2(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    ARGState rootState = (ARGState) proof;
    StopOperator stop = ((ARGCPA) cpa).getWrappedCPAs().get(0).getStopOperator();
// TODO include property checking

    logger.log(Level.INFO, "Proof check algorithm started");

    ARGState initialState = (ARGState) reachedSet.popFromWaitlist();
    Precision initialPrecision = reachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!stop
        .stop(initialState.getWrappedState(), Collections.singleton(rootState.getWrappedState()), initialPrecision)) {
      logger.log(Level.WARNING, "Root state of proof is invalid.");
      return false;
    }

    reachedSet.add(rootState, initialPrecision);


    while (reachedSet.hasWaitingState()) {
      CPAchecker.stopIfNecessary();

      stats.countIterations++;
      ARGState state = (ARGState) reachedSet.popFromWaitlist();

      logger.log(Level.FINE, "Looking at state", state);


      if (state.isCovered()) {

        logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
        ARGState coveringState = state.getCoveringState();

        if (!reachedSet.contains(coveringState)) {
          reachedSet.add(coveringState, initialPrecision);
        }

        stats.stopTimer.start();
        if (!isCoveringCycleFree(state)) {
          stats.stopTimer.stop();
          logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
          return false;
        }
        if (!stop.stop(state.getWrappedState(), Collections.singleton(coveringState.getWrappedState()),
            initialPrecision)) {
          stats.stopTimer.stop();
          logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
          return false;
        }
        stats.stopTimer.stop();
      } else {
        stats.transferTimer.start();
        ArrayList<AbstractState> successors = new ArrayList<>(state.getChildren().size());
        for (ARGState argS : state.getChildren()) {
          successors.add(argS.getWrappedState());
          reachedSet.add(argS, initialPrecision);
        }
        Collection<? extends AbstractState> computedSuccessors =
            ((ARGCPA) cpa).getWrappedCPAs().get(0).getTransferRelation()
                .getAbstractSuccessors(state.getWrappedState(), initialPrecision, null);
        logger.log(Level.FINER, "Checking abstract successors", successors);
        for (AbstractState succ : computedSuccessors) {
          if (!stop.stop(succ, successors, initialPrecision)) {
            stats.transferTimer.stop();
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
        }
        stats.transferTimer.stop();
      }
    }

    return true;
  }

  private boolean isCoveringCycleFree(ARGState pState) {
    HashSet<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) { return false; }
    }
    return true;
  }

  private boolean checkReachedSet(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    AbstractState[] certificate = (AbstractState[]) proof;

    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = reachedSet.getPrecision(reachedSet.getFirstState());

    // check if initial element covered
    AbstractState initialState = reachedSet.popFromWaitlist();
    assert (initialState == reachedSet.getFirstState() && reachedSet.size() == 1);

    try {
      stats.stopTimer.start();
      if (!stop.stop(initialState, statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
        logger.log(Level.FINE, "Cannot check that initial element is covered by result.");
        return false;
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.stopTimer.stop();
    }


    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    for (AbstractState state : certificate) {

      CPAchecker.stopIfNecessary();
      stats.countIterations++;

      try {
        stats.transferTimer.start();
        successors = cpa.getTransferRelation().getAbstractSuccessors(state, initialPrec, null);
        stats.transferTimer.stop();

        for (AbstractState succ : successors) {
          try {
            stats.stopTimer.start();
            if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(Level.FINE, "Cannot check that result is transitive closure.", "Successor ", succ,
                  "of element ", state, "not covered by result.");
              return false;
            }
          } finally {
            stats.stopTimer.stop();
          }
        }
      } catch (CPATransferException | InterruptedException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        return false;
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        return false;
      }
    }
    stats.propertyCheckingTimer.start();
    try {
      return propertyChecker.satisfiesProperty(Arrays.asList(certificate));
    } finally {
      stats.propertyCheckingTimer.stop();
    }
  }

  private boolean checkPartialReachedSet(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    List<AbstractState> certificate = new ArrayList<>(((AbstractState[]) proof).length);
    for (AbstractState elem : (AbstractState[]) proof) {
      certificate.add(elem);
    }

    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Precision initialPrec = reachedSet.getPrecision(reachedSet.getFirstState());

    // check initial element
    AbstractState initialState = reachedSet.popFromWaitlist();
    assert (initialState == reachedSet.getFirstState() && reachedSet.size() == 1);

    try {
      stats.stopTimer.start();
      if (!stop.stop(initialState, statesPerLocation.get(AbstractStates.extractLocation(initialState)), initialPrec)) {
        logger.log(Level.FINE, "Initial element not in partial reached set.", "Add to elements whose successors ",
            "must be computed.");
        addElement(initialState, certificate);
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    } finally {
      stats.stopTimer.stop();
    }


    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    while (!certificate.isEmpty()) {

      CPAchecker.stopIfNecessary();
      stats.countIterations++;

      try {
        stats.transferTimer.start();
        successors =
            cpa.getTransferRelation().getAbstractSuccessors(certificate.remove(certificate.size() - 1), initialPrec,
                null);
        stats.transferTimer.stop();

        for (AbstractState succ : successors) {
          try {
            stats.stopTimer.start();
            if (!stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec)) {
              logger.log(Level.FINE, "Successor ", succ, " not in partial reached set.",
                  "Add to elements whose successors ",
                  "must be computed.");
              if (AbstractStates.extractLocation(succ).getNumEnteringEdges() > 1) {
                stop.stop(succ, statesPerLocation.get(AbstractStates.extractLocation(succ)), initialPrec);
              }
              addElement(succ, certificate);
            }
          } finally {
            stats.stopTimer.stop();
          }
        }
      } catch (CPATransferException | InterruptedException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        return false;
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        return false;
      }
    }
    stats.propertyCheckingTimer.start();
    try {
      return propertyChecker.satisfiesProperty(statesPerLocation.values());
    } finally {
      stats.propertyCheckingTimer.stop();
    }
  }

  private void addElement(AbstractState element, List<AbstractState> insertIn) {
    insertIn.add(insertIn.size(), element);
    CFANode node = AbstractStates.extractLocation(element);
    statesPerLocation.put(node, element);
  }

  private void orderReachedSetByLocation(AbstractState[] pReached) {
    statesPerLocation = HashMultimap.create();
    for (AbstractState state : pReached) {
      statesPerLocation.put(AbstractStates.extractLocation(state), state);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
