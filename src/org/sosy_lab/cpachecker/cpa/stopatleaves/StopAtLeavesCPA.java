// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.stopatleaves;

import java.util.Collections;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
/**
 * @see org.sosy_lab.cpachecker.cpa.targetreachability.TargetReachabilityCPA
 */
public class StopAtLeavesCPA extends AbstractCPA {
        private List<CFANode> leaves;

        StopAtLeavesRelation relation;

        private StopAtLeavesCPA(
                Configuration pConfig,
                ShutdownNotifier shutdownNotifier,
                LogManager pLogger,
                CFA pCfa,
                Specification pSpecification)
                throws InvalidConfigurationException {
                super(
                        "join",
                        "sep",
                        new FlatLatticeDomain(StopAtLeavesState.CONTINUE),
                        null /* never used */);
                //pConfig.inject(this);

                relation = new StopAtLeavesRelation(Collections.emptyList());
        }

        public static CPAFactory factory() {
                return AutomaticCPAFactory.forType(StopAtLeavesCPA.class);
        }

        @Override
        public AbstractState getInitialState(
                CFANode node, StateSpacePartition partition) throws InterruptedException {

                return StopAtLeavesState.CONTINUE;
        }

        public void setLeaves(List<CFANode> pLeaves) {
                leaves = pLeaves;
                relation.setLeaves(pLeaves);
        }

        @Override
        public TransferRelation getTransferRelation() {
                return relation;
        }
}
