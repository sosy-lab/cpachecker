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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

@Options(prefix="cpa.bdd")
public class BDDCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BDDCPA.class);
  }

  private final NamedRegionManager manager;
  private final BitvectorManager bvmgr;
  private final PredicateManager predmgr;
  private VariableTrackingPrecision precision;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

  @Option(secure=true, description="mergeType")
  private String merge = "join";

  @Option(secure = true, name = "logfile", description = "Dump tracked variables to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("BDDCPA_tracked_variables.log");

  @Option(secure = true, description = "max bitsize for values and vars, initial value")
  private int bitsize = 64;

  @Option(
    secure = true,
    description = "use a smaller bitsize for all vars, that have only intEqual values"
  )
  private boolean compressIntEqual = true;

  private BDDCPA(CFA pCfa, Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config            = pConfig;
    logger            = pLogger;
    cfa               = pCfa;
    shutdownNotifier  = pShutdownNotifier;

    RegionManager rmgr = new BDDManagerFactory(config, logger).createRegionManager();

    precision         = VariableTrackingPrecision.createStaticPrecision(config, cfa.getVarClassification(), getClass());

    manager           = new NamedRegionManager(rmgr);
    bvmgr             = new BitvectorManager(rmgr);
    predmgr           = new PredicateManager(config, manager, cfa);
  }

  public void injectRefinablePrecision() throws InvalidConfigurationException {
      precision = VariableTrackingPrecision.createRefineablePrecision(config, precision);
  }

  public NamedRegionManager getManager() {
    return manager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<BDDState>getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return (merge.equals("sep"))
        ? MergeSepOperator.getInstance()
        : new MergeJoinOperator(getAbstractDomain());
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new BDDTransferRelation(manager, bvmgr, predmgr, cfa, bitsize, compressIntEqual);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new BDDState(manager, bvmgr, manager.makeTrue());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {
            VariableClassification varClass = cfa.getVarClassification().get();
            final Set<Partition> intBool = varClass.getIntBoolPartitions();
            int numOfBooleans = varClass.getIntBoolVars().size();

            int numOfIntEquals = 0;
            final Set<Partition> intEq = varClass.getIntEqualPartitions();
            for (Partition p : intEq) {
              numOfIntEquals += p.getVars().size();
            }

            int numOfIntAdds = 0;
            final Set<Partition> intAdd = varClass.getIntAddPartitions();
            for (Partition p : intAdd) {
              numOfIntAdds += p.getVars().size();
            }

            Collection<String> trackedIntBool =
                new TreeSet<>(); // TreeSet for nicer output through ordering
            Collection<String> trackedIntEq = new TreeSet<>();
            Collection<String> trackedIntAdd = new TreeSet<>();
            for (String var : predmgr.getTrackedVars().keySet()) {
              if (varClass.getIntBoolVars().contains(var)) {
                trackedIntBool.add(var);
              } else if (varClass.getIntEqualVars().contains(var)) {
                trackedIntEq.add(var);
              } else if (varClass.getIntAddVars().contains(var)) {
                trackedIntAdd.add(var);
              } else {
                // ignore other vars, they are either function_return_vars or tmp_vars
              }
            }

            if (dumpfile != null) { // option -noout
              try (Writer w = MoreFiles.openOutputFile(dumpfile, Charset.defaultCharset())) {
                w.append("Boolean\n\n");
                w.append(trackedIntBool.toString());
                w.append("\n\nIntEq\n\n");
                w.append(trackedIntEq.toString());
                w.append("\n\nIntAdd\n\n");
                w.append(trackedIntAdd.toString());
              } catch (IOException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not write tracked variables for BDDCPA to file");
              }
            }

            out.println(
                String.format(
                    "Number of boolean vars:           %d (of %d)",
                    trackedIntBool.size(), numOfBooleans));
            out.println(
                String.format(
                    "Number of intEqual vars:          %d (of %d)",
                    trackedIntEq.size(), numOfIntEquals));
            out.println(
                String.format(
                    "Number of intAdd vars:            %d (of %d)",
                    trackedIntAdd.size(), numOfIntAdds));
            out.println(
                String.format(
                    "Number of all vars:               %d",
                    trackedIntBool.size() + trackedIntEq.size() + trackedIntAdd.size()));
            out.println("Number of intBool partitions:     " + intBool.size());
            out.println("Number of intEq partitions:       " + intEq.size());
            out.println("Number of intAdd partitions:      " + intAdd.size());
            out.println("Number of all partitions:         " + varClass.getPartitions().size());
            manager.printStatistics(out);
          }

          @Override
          public String getName() {
            return "BDDCPA";
          }
        });
  }

  @Override
  public Reducer getReducer() {
    return new BDDReducer(predmgr);
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public CFA getCFA() {
    return cfa;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }


}
