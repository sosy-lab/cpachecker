/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

import com.google.common.base.Preconditions;

/**
 * This class implements an AutomatonAnalysis as described in the related Documentation.
 * @author rhein
 */
@Options(prefix="cpa.automaton")
public class ControlAutomatonCPA implements ConfigurableProgramAnalysis {

  @Option(name="dotExport")
  private boolean export = false;
  
  @Option(name="dotExportFile", type=Option.Type.OUTPUT_FILE)
  private File exportFile = new File("automaton.dot");

  public static class AutomatonCPAFactory extends AbstractCPAFactory {

    private Automaton mAutomaton = null;
    
    public CPAFactory setAutomaton(Automaton pAutomaton) {
      Preconditions.checkNotNull(pAutomaton);
      Preconditions.checkState(mAutomaton == null, "setAutomaton called twice on AutomatonCPAFactory");
      
      mAutomaton = pAutomaton;
      return this;
    }
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      if (mAutomaton == null) {
        return new ControlAutomatonCPA(getConfiguration(), getLogger());
      } else {
        return new ControlAutomatonCPA(mAutomaton, getConfiguration(), getLogger()); 
      }
    }
  }

  public static AutomatonCPAFactory factory() {
    return new AutomatonCPAFactory();
  }

  @Option(required=false, type=Option.Type.OPTIONAL_INPUT_FILE)
  private File inputFile = null;
  
  @Option
  private boolean breakOnTargetState = true;

  private final Automaton automaton;
  private final AutomatonState topState = new AutomatonState.TOP(this);
  private final AutomatonState bottomState = new AutomatonState.BOTTOM(this);

  private final AbstractDomain automatonDomain = new FlatLatticeDomain(topState);
  private final StopOperator stopOperator = new StopSepOperator(automatonDomain);
  private final AutomatonTransferRelation transferRelation;
  private final PrecisionAdjustment precisionAdjustment;
  private final Statistics stats = new AutomatonStatistics(this);

  private ControlAutomatonCPA(Automaton automaton, Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, ControlAutomatonCPA.class);
    this.automaton = automaton;
    logger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");
    transferRelation = new AutomatonTransferRelation(this, logger);
    precisionAdjustment = breakOnTargetState ? new AutomatonPrecisionAdjustment() : StaticPrecisionAdjustment.getInstance();
    
    if (export && exportFile != null) {
      try {
        this.automaton.writeDotFile(new PrintStream(exportFile));
      } catch (FileNotFoundException e) {
        logger.log(Level.WARNING, "Could not create/write to the Automaton DOT file \"" + exportFile + "\"");
      }
    }
  }
  
  /**
   * Loads a Automaton from the argument DefinitionFile.
   * The argument mergeType is ignored.
   * @param mergeType
   * @param pStopType
   * @throws FileNotFoundException
   * @throws InvalidConfigurationException
   */
  protected ControlAutomatonCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, ControlAutomatonCPA.class);
    if (inputFile == null) {
      throw new InvalidConfigurationException("Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");
    }
    automaton = parseAutomatonFile(logger, config);
    logger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");
    transferRelation = new AutomatonTransferRelation(this, logger);
    precisionAdjustment = breakOnTargetState ? new AutomatonPrecisionAdjustment() : StaticPrecisionAdjustment.getInstance();

    if (export) {
      try {
        this.automaton.writeDotFile(new PrintStream(exportFile));
      } catch (FileNotFoundException e) {
        logger.log(Level.WARNING, "Could not create/write to the Automaton DOT file \"" + exportFile + "\"");
      }
    }
  }

  private Automaton parseAutomatonFile(LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    if (inputFile != null) {
      List<Automaton> lst = AutomatonParser.parseAutomatonFile(inputFile, config, pLogger);
      if (lst.size() == 1) {
        return lst.get(0);
      } else if (lst.size() > 1) {
        throw new InvalidConfigurationException("Found " + lst.size() + " automata in the File " + inputFile.getAbsolutePath() + " The CPA can only handle ONE Automaton!");
      } else { // lst.size == 0
        throw new InvalidConfigurationException("Could not find automata in the file " + inputFile.getAbsolutePath());
      }
    } else {
      throw new InvalidConfigurationException("No Specification file was given.");
    }
  }

  Automaton getAutomaton() {
    return this.automaton;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return automatonDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return AutomatonState.automatonStateFactory(automaton.getInitialVariables(), automaton.getInitialState(), this);
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public AutomatonTransferRelation getTransferRelation() {
    return transferRelation ;
  }

  public AutomatonState getBottomState() {
    return this.bottomState;
  }

  public AutomatonState getTopState() {
    return this.topState;
  }
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
