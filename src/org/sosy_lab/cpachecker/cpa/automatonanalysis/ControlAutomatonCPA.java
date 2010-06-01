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
package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.EqualityPartialOrder;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements an AutomatonAnalysis as described in the related Documentation.
 * @author rhein
 */
@Options(prefix="automatonAnalysis")
public class ControlAutomatonCPA implements ConfigurableProgramAnalysis {

  @Option(name="dotExportFile")
  protected String exportFile = "";

  private static class AutomatonCPAFactory extends AbstractCPAFactory {

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new ControlAutomatonCPA(getConfiguration(), getLogger());
    }
  }

  public static CPAFactory factory() {
    return new AutomatonCPAFactory();
  }

  @Option(required=true)
  protected String inputFile = "";

  private final Automaton automaton;
  private final AutomatonTransferRelation transferRelation;
  private final Statistics stats = new AutomatonStatistics(this);

  private final AutomatonState topState = new AutomatonState.TOP(ControlAutomatonCPA.this);
  private final AutomatonState bottomState = new AutomatonState.BOTTOM(ControlAutomatonCPA.this);


  private final AutomatonDomain automatonDomain = new AutomatonDomain();
  private final PartialOrder partialOrder = new EqualityPartialOrder(automatonDomain);
  private final StopOperator stopOperator = new StopSepOperator(partialOrder);
  private final JoinOperator joinOperator = new JoinOperator() {
    @Override
    public AbstractElement join(AbstractElement pElement1,
                                AbstractElement pElement2) throws CPAException {
      if (pElement1 == pElement2) {
        return pElement1;
      } else {
        return topState;
      }
    }
  };

  private class AutomatonDomain implements AbstractDomain {
    @Override
    public AbstractElement getTopElement() {
      return topState;
    }

    @Override
    public PartialOrder getPartialOrder() {
      return partialOrder;
    }

    @Override
    public JoinOperator getJoinOperator() {
      return joinOperator;
    }

    @Override
    public AbstractElement getBottomElement() {
      return bottomState;
    }
  };

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
    automaton = parseAutomatonFile(logger);
    logger.log(Level.FINEST, "Automaton", automaton.getName(), "loaded.");
    transferRelation = new AutomatonTransferRelation(automaton, logger);
    logger.log(Level.FINER, "loaded the Automaton " + automaton.getName() );

    if (this.exportFile != "") {
      try {
        this.automaton.writeDotFile(new PrintStream(exportFile));
      } catch (FileNotFoundException e) {
        logger.log(Level.WARNING, "Could not create/write to the Automaton DOT file \"" + exportFile + "\"");
      }
    }
  }

  private Automaton parseAutomatonFile(LogManager pLogger) throws InvalidConfigurationException {
    SymbolFactory sf = new ComplexSymbolFactory();
    FileInputStream input = null;
    try {
      input = new FileInputStream(inputFile);
      Symbol symbol = new AutomatonParser(new AutomatonScanner(input, sf),sf,pLogger).parse();
      return (Automaton)symbol.value;
    } catch (Exception e) {
      pLogger.logException(Level.FINER, e, "Could not load automaton from file " + inputFile);
      throw new InvalidConfigurationException("Could not load automaton from file " + inputFile
          + " (" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + ")");
    } finally {
      if (input!= null) {
        try {
          input.close();
        } catch (IOException e) {
          throw new InvalidConfigurationException(
              "IO Exception when closing the FileStream to \"" + inputFile + "\": "+e.getMessage());
        }
      }
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
    return null;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
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
