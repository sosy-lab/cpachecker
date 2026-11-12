// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSetMultimap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This CPA provides the ACSL predicates that hold at a location as assumptions, e.g., for the
 * predicate analysis.
 */
public class ACSLCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  private final ImmutableSetMultimap<CFANode, AcslPredicate> invariants;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ACSLCPA.class);
  }

  private ACSLCPA(CFA pCFA, LogManager pLogManager) {
    super("sep", "sep", null);
    if (pCFA.getAcslMetadata() == null) {
      pLogManager.log(Level.WARNING, "No ACSL annotations in CFA, ACSLCPA is useless.");
    }
    invariants = AcslInvariants.of(pCFA);
  }

  /** Returns the state for the predicates that hold at the given node. */
  ACSLState getStateForNode(CFANode pNode) {
    return new ACSLState(invariants.get(pNode));
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new ACSLTransferRelation(this);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return getStateForNode(node);
  }
}
