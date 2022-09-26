// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence.SequenceBoundAnalysis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;

@Options(prefix = "cpa.sequenceCPA")
public class SequenceCPA extends AbstractCPA implements ProofCheckerCPA {


  @Option(
      secure = true,
      description =
          "The path for the input file to handle input/random values for the constant propagation"
              + " following the lower bound. If no path is given, there is no lower bound.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  protected Path path2Bound = null;

  @Option(
      secure = true,
      description = "Stop if the list of decisions taken is empty.")
  protected boolean stopIfUnderspecifiedTestcase = false;


  private List<Boolean> decisionNodes = new ArrayList<>();

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SequenceCPA.class);

  }

  SequenceCPA(Configuration pConfig, CFA pCFA, LogManager pLogger)
      throws InvalidConfigurationException {
    super("sep", "sep", new SequenceTransferRelation(pCFA, pLogger));
    pConfig.inject(this);
        if (path2Bound != null) {
          decisionNodes = readDecisionNodes(path2Bound);
        }
  }

  private List<Boolean> readDecisionNodes(Path pPath2Bound) throws InvalidConfigurationException {
    try {
      return Files.readAllLines(pPath2Bound, Charset.defaultCharset()).stream().map(line -> line.equals("true")).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    } catch (IOException pE) {
      throw new InvalidConfigurationException("Could not read decision nodes from file " + pPath2Bound, pE);
    }
  }


  @Override
  public SequenceTransferRelation getTransferRelation() {
    assert  super.getTransferRelation() instanceof SequenceTransferRelation;
    return (SequenceTransferRelation) super.getTransferRelation();
  }

  @Override
  public StopOperator getStopOperator() {
    return super.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new SequenceState(decisionNodes, stopIfUnderspecifiedTestcase);
  }
}
