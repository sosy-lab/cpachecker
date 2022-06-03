// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.reachdef;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/*
 * Requires preprocessing with cil to get proper result because preprocessing guarantees that
 * 1) no two variables accessible in function f, have same name in function f
 * 2) all local variables are declared at begin of function body
 *
 * If function x is called from at least two distinct functions y and z, analysis must be done together
 * with CallstackCPA.
 */
@Options(prefix = "cpa.reachdef")
public class ReachingDefCPA extends AbstractCPA implements ProofCheckerCPA {

  private LogManager logger;

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN", "IGNORECALLSTACK"},
      description = "which merge operator to use for ReachingDefCPA")
  private String mergeType = "JOIN";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "IGNORECALLSTACK"},
      description = "which stop operator to use for ReachingDefCPA")
  private String stopType = "SEP";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ReachingDefCPA.class);
  }

  private ReachingDefCPA(LogManager logger, Configuration config, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    super(
        DelegateAbstractDomain.getInstance(),
        new ReachingDefTransferRelation(logger, shutdownNotifier, config));
    config.inject(this);
    this.logger = logger;
  }

  @Override
  public MergeOperator getMergeOperator() {
    switch (mergeType) {
      case "SEP":
        return MergeSepOperator.getInstance();
      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());
      case "IGNORECALLSTACK":
        return new MergeIgnoringCallstack();
      default:
        throw new AssertionError("unknown merge operator");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    switch (stopType) {
      case "SEP":
        return new StopSepOperator(getAbstractDomain());
      case "JOIN":
        return new StopJoinOperator(getAbstractDomain());
      case "IGNORECALLSTACK":
        return new StopIgnoringCallstack();
      default:
        throw new AssertionError("unknown stop operator");
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    logger.log(
        Level.FINE,
        "Start extracting all declared variables in program.",
        "Distinguish between local and global variables.");
    Pair<Set<MemoryLocation>, Map<FunctionEntryNode, Set<MemoryLocation>>> result =
        ReachingDefUtils.getAllVariables(pNode);
    logger.log(Level.FINE, "Extracted all declared variables.", "Create initial state.");
    ((ReachingDefTransferRelation) getTransferRelation())
        .provideLocalVariablesOfFunctions(result.getSecond());
    ((ReachingDefTransferRelation) getTransferRelation()).setMainFunctionNode(pNode);
    return new ReachingDefState(result.getFirst());
  }
}
