/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation;

import com.google.common.collect.Lists;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.extenedArraySegmentationDomain.CExtendedArraySegmentationTransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.extenedArraySegmentationDomain.ExtendedArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.ArraySegmentationCPAHelper;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

@Options(prefix = "cpa.arrayContentCPA")
public class ExtendedLocationArrayContentCPA<T extends ExtendedCompletLatticeAbstractState<T>>
    extends AbstractCPA {

  @Option(
    secure = true,
    name = "merge",
    toUppercase = true,
    values = {"JOIN"},
    description = "which merge operator to use for UsageOfArrayElemensCPA")
  private String mergeType = "JOIN";

  @Option(
    secure = true,
    name = "stop",
    toUppercase = true,
    values = {"SEP"},
    description = "which stop operator to use for UsageOfArrayElemensCPA")
  private String stopType = "SEP";

  @Option(
    name = "arrayName",
    toUppercase = false,
    description = "The array that needs to be analyzed")
  private String varnameArray = "";

  protected final LogManager logger;
  protected final CFA cfa;
  protected final Configuration config;

  private AbstractCPA innerCPA;
  private ArraySegmentationCPAHelper<T> helper;
  private LocationStateFactory factory;

  private final LocationCPA locationCPA;

  public static final Level GENERAL_LOG_LEVEL = Level.INFO;

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param pConfig the configuration of the CPAinterval analysis CPA.
   */
  protected ExtendedLocationArrayContentCPA(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(
        "join",
        "sep",
        DelegateAbstractDomain.<ExtendedLocationArrayContentState<T>>getInstance(),
        null);

    pConfig.inject(this, ExtendedLocationArrayContentCPA.class);
    this.logger = pLogger;
    this.cfa = pCfa;
    this.config = pConfig;
    innerCPA = constructInnerCPA(pConfig, logger, pCfa, varnameArray, pShutdownNotifier);
    this.factory = new LocationStateFactory(pCfa, AnalysisDirection.FORWARD, pConfig);
    locationCPA = LocationCPA.create(this.cfa, this.config);
    helper =
        new ArraySegmentationCPAHelper<>(pCfa, pLogger, varnameArray, pShutdownNotifier, pConfig);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExtendedLocationArrayContentCPA.class);
  }

  @Override
  public TransferRelation getTransferRelation() {
    CExtendedArraySegmentationTransferRelation<T> transfer =
        new CExtendedArraySegmentationTransferRelation<>(
            innerCPA.getTransferRelation(),
            new LogManagerWithoutDuplicates(logger),
            cfa.getMachineModel(),
            getName());
    return new ExtendedLocationArrayContentTransferRelation<>(
        new LogManagerWithoutDuplicates(logger),
        this.factory,
        transfer);
  }

  @Override
  public ExtendedLocationArrayContentState<T>
      getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {

    ArraySegmentationState<T> initalSeg =
        helper.computeInitaleState(
            getInitialInnerState(pNode, pPartition),
            getPredicate(),
            getEmptyElement(),
            getName(),
            pNode);

    return new ExtendedLocationArrayContentState<>(
        locationCPA.getInitialState(pNode, pPartition),
        new ExtendedArraySegmentationState<>(Lists.newArrayList(initalSeg), this.logger),
        logger);
  }

  /**
   * Constructs the inner CPA analysis
   *
   * @param pConfig of the analysis
   * @param pLogger for logging
   * @param pCfa of the program
   * @param pVarnameArray the variable name of the array
   * @param pShutdownNotifier the shutdown notifier of the analysis
   * @throws InvalidConfigurationException if the path formula cannot be created
   */
  protected AbstractCPA constructInnerCPA(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      String pVarnameArray,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    throw new UnsupportedOperationException(
        "This needs to be overwritten by an extending analysis");
  }

  /**
   *
   * @return the name of the analysis (used e.g. for logging)
   */
  protected String getName() {
    throw new UnsupportedOperationException(
        "This needs to be overwritten by an extending analysis");
  }

  /**
   *
   * @return An instance of the empty element, that references top and bottom element of the lattice
   *         as well as the meet operator
   */
  protected T getEmptyElement() {
    throw new UnsupportedOperationException(
        "This needs to be overwritten by an extending analysis");
  }

  /**
   * @param pNode the initial note
   * @param pPartition the initial partition
   */
  protected T getInitialInnerState(CFANode pNode, StateSpacePartition pPartition) {
    throw new UnsupportedOperationException(
        "This needs to be overwritten by an extending analysis");
  }

  /**
   *
   * @return a predicate that determines, if the state computed at the end of the program is a
   *         Violation
   */
  protected Predicate<ArraySegmentationState<T>> getPredicate() {
    throw new UnsupportedOperationException(
        "This needs to be overwritten by an extending analysis");
  }

}
