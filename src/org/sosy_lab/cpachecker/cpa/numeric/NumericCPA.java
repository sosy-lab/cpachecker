// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericMergeOperator;
import org.sosy_lab.cpachecker.cpa.numeric.merge_operator.NumericMergeOperator.NumericMergeOperations;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.numericdomains.Manager;
import org.sosy_lab.numericdomains.NumericalLibrary;
import org.sosy_lab.numericdomains.NumericalLibraryLoader;

@Options(prefix = "cpa.numeric")
public class NumericCPA extends AbstractCPA implements StatisticsProvider, ProofCheckerCPA {

  @Option(
      secure = true,
      name = "numericLibrary",
      toUppercase = true,
      description = "Use this to switch the underlying numerical library.")
  private NumericalLibrary numericLibrary = NumericalLibrary.ELINA;

  @Option(
      secure = true,
      name = "numericDomain",
      toUppercase = true,
      description =
          "Use this to switch between domains of a library. Note that not all domains are accessible from each library.",
      values = {"POLYHEDRA", "OCTAGON", "ZONES", "BOX"})
  private String numericDomain = "OCTAGON";

  @Option(
      secure = true,
      name = "mergeOperator",
      toUppercase = true,
      description = "MergeOperation that should be used by the domain.")
  private NumericMergeOperator.NumericMergeOperations mergeOp = NumericMergeOperations.SEP;

  @Option(
      secure = true,
      name = "startingPrecision",
      toUppercase = true,
      description = "Use this to set the initial precision type of the CPA.")
  private PrecisionType startingPrecision = PrecisionType.FULL;

  @Option(
      secure = true,
      description =
          "Use this to select the file containing the initial precision, if the starting precision is set to be read from file.")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path startingPrecisionFile = null;

  @Option(
      secure = true,
      description = "Use this to select the file to which the precision will be exported.")
  @FileOption(Type.OUTPUT_FILE)
  private Path precisionExportFile = null;

  private final Precision precision;
  private final Supplier<Manager> managerSupplier;
  private final LogManager logger;

  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;

  private final Configuration config;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;

  public NumericCPA(
      Configuration pConfig, CFA pCfa, ShutdownNotifier pShutdownNotifier, LogManager pLogManager)
      throws InvalidConfigurationException {
    super(DelegateAbstractDomain.getInstance(), new NumericTransferRelation(pLogManager));
    pConfig.inject(this);
    logger = pLogManager;
    cfa = pCfa;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;

    precision = createPrecision(pConfig, pCfa);
    logger.log(
        Level.INFO,
        "Set precision to:",
        startingPrecision,
        (startingPrecision == PrecisionType.REFINABLE_FROM_FILE
            ? " " + startingPrecisionFile
            : ""));

    NumericalLibraryLoader.loadLibrary(numericLibrary);
    managerSupplier = chooseDomain(numericDomain, numericLibrary);
    Manager manager = Manager.createManager(managerSupplier);
    logger.log(
        Level.INFO,
        "Using",
        manager.getDomainLibrary(),
        "version",
        manager.getDomainVersion(),
        "from",
        numericLibrary,
        "as numerical domain.");
    mergeOperator = NumericMergeOperator.getMergeOperator(manager, mergeOp);
    manager.dispose();

    stopOperator = new StopSepOperator(getAbstractDomain());

    // If necessary adjust the transfer relation to use loop information
    if (mergeOperator instanceof NumericMergeOperator
        && ((NumericMergeOperator) mergeOperator).usesLoopInformation()) {
      ((NumericTransferRelation) getTransferRelation()).setUseLoopInformation();
    }
  }

  private Precision createPrecision(Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    VariableTrackingPrecision baselinePrecison =
        VariableTrackingPrecision.createStaticPrecision(
            pConfig, pCfa.getVarClassification(), getClass());
    VariableTrackingPrecision refinablePrecision =
        VariableTrackingPrecision.createRefineablePrecision(pConfig, baselinePrecison);
    switch (startingPrecision) {
      case FULL:
        return baselinePrecison;
      case REFINABLE_EMPTY:
        return refinablePrecision;
      case REFINABLE_FROM_FILE:
        if (startingPrecisionFile == null) {
          throw new InvalidConfigurationException("No starting precision file was specified.");
        } else {
          return refinablePrecision.withIncrement(
              restoreMappingFromFile(pCfa, startingPrecisionFile, logger));
        }
      default:
        throw new AssertionError("Unhandled precision type: " + startingPrecision);
    }
  }

  private static Multimap<CFANode, MemoryLocation> restoreMappingFromFile(
      CFA pCfa, Path precisionFile, LogManager pLogger) throws InvalidConfigurationException {
    Multimap<CFANode, MemoryLocation> mapping = HashMultimap.create();

    List<String> contents = null;
    try {
      contents = Files.readAllLines(precisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      // In my opinion this should throw an exception and not default to something that wasn't
      // requested.
      pLogger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + precisionFile);
      throw new InvalidConfigurationException(
          "Could not read precision from file named: " + precisionFile, e.getCause());
    }

    Map<Integer, CFANode> idToCfaNode = createMappingForCFANodes(pCfa);
    final Pattern CFA_NODE_PATTERN = Pattern.compile("N([0-9][0-9]*)");

    // Set the initial value of the location to the default location
    CFANode location = idToCfaNode.values().iterator().next();
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;

      } else if (currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFA_NODE_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        }

      } else {
        mapping.put(location, MemoryLocation.valueOf(currentLine));
      }
    }

    return mapping;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
            if (precisionExportFile != null) {
              exportPrecision(pReached);
            }
          }

          @Override
          public @Nullable String getName() {
            return NumericCPA.this.getClass().getSimpleName();
          }
        });
  }

  /**
   * This method exports the precision to file.
   *
   * @param reached the set of reached states.
   */
  private void exportPrecision(UnmodifiableReachedSet reached) {
    VariableTrackingPrecision consolidatedPrecision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(reached);
    try (Writer writer = IO.openOutputFile(precisionExportFile, Charset.defaultCharset())) {
      consolidatedPrecision.serialize(writer);
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not write numeric-analysis precision to file");
    }
  }

  private static Map<Integer, CFANode> createMappingForCFANodes(CFA pCfa) {
    Map<Integer, CFANode> idToNodeMap = new HashMap<>();
    for (CFANode n : pCfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }

  private Supplier<Manager> chooseDomain(String domainName, NumericalLibrary pLibrary) {
    switch (pLibrary) {
      case APRON:
        return chooseApronDomain(domainName);
      case ELINA:
        return chooseElinaDomain(domainName);
      default:
        throw new IllegalStateException("Unhandled library: " + pLibrary);
    }
  }

  private Supplier<Manager> chooseApronDomain(String domain) {
    switch (domain) {
      case "OCTAGON":
        return org.sosy_lab.numericdomains.apron.OctagonManager::createDefaultOctagonManager;
      case "POLYHEDRA":
        return org.sosy_lab.numericdomains.apron.PolyhedraManager::createDefaultPolyhedraManager;
      case "BOX":
        return org.sosy_lab.numericdomains.apron.BoxManager::createDefaultBoxManager;
      default:
        logger.log(Level.SEVERE, "Unknown domain for APRON: " + domain);
        throw new AssertionError("Unknown domain for APRON: " + domain);
    }
  }

  private Supplier<Manager> chooseElinaDomain(String domain) {
    switch (domain) {
      case "OCTAGON":
        return org.sosy_lab.numericdomains.elina.OctagonManager::createDefaultOctagonManager;
      case "POLYHEDRA":
        return org.sosy_lab.numericdomains.elina.PolyhedraManager::createDefaultPolyhedraManager;
      case "ZONES":
        return org.sosy_lab.numericdomains.elina.ZonesManager::createDefaultZonesManager;
      default:
        logger.log(Level.SEVERE, "Unknown domain for ELINA: " + domain);
        throw new AssertionError("Unknown domain for ELINA: " + domain);
    }
  }

  /**
   * This method returns a CPAfactory for the numeric analysis CPA.
   *
   * @return the CPAfactory for the numeric analysis CPA
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(NumericCPA.class);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new NumericState(Manager.createManager(managerSupplier), logger);
  }

  /*@Override
  public boolean areAbstractSuccessors(
      AbstractState pState, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    try {
      Collection<? extends AbstractState> computedSuccessors =
          getTransferRelation().getAbstractSuccessorsForEdge(pState, precision, pCfaEdge);
      boolean found;
      for (AbstractState comp : computedSuccessors) {
        found = false;
        for (AbstractState e : pSuccessors) {
          if (isCoveredBy(comp, e)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    } catch (CPAException e) {
      throw new CPATransferException("Cannot compare abstract successors", e);
    }
    return true;
  }*/

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition) {
    return precision;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public CFA getCFA() {
    return cfa;
  }

  public Supplier<Manager> getManagerSupplier() {
    return managerSupplier;
  }

  private enum PrecisionType {
    FULL,
    REFINABLE_FROM_FILE,
    REFINABLE_EMPTY;
  }
}
