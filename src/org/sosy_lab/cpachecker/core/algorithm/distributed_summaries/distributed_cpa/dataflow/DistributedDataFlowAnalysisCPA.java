// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import java.util.Map;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedDataFlowAnalysisCPA
        implements ForwardingDistributedConfigurableProgramAnalysis {

    private final InvariantsCPA invariantsCPA;
    private final SerializeOperator serializeOperator;
    private final DeserializeOperator deserializeOperator;

    private final BlockNode blockNode;

    public DistributedDataFlowAnalysisCPA(
            InvariantsCPA pInvariantsCPA,
            BlockNode pNode,
            CFA pCFA,
            Configuration pConfiguration,
            org.sosy_lab.common.log.LogManager pLogManager,
            ShutdownNotifier pShutdownNotifier,
            Map<Integer, CFANode> pIdToNodeMap)
            throws InvalidConfigurationException {
        invariantsCPA = pInvariantsCPA;
        blockNode = pNode;
        serializeOperator = new SerializeDataflowAnalysisStateOperator(blockNode);
        deserializeOperator = new DeserializeDataflowAnalysisStateOperator(invariantsCPA, pCFA);
    }

    @Override
    public SerializeOperator getSerializeOperator() {
        return serializeOperator;
    }

    @Override
    public DeserializeOperator getDeserializeOperator() {
        return deserializeOperator;
    }

    @Override
    public ProceedOperator getProceedOperator() {
        return ProceedOperator.always();
    }

    @Override
    public Class<? extends AbstractState> getAbstractStateClass() {
        return InvariantsState.class;
    }

    @Override
    public ConfigurableProgramAnalysis getCPA() {
        return invariantsCPA;
    }

    @Override
    public boolean isTop(AbstractState pAbstractState) {
        throw new UnsupportedOperationException("Unimplemented method 'isTop'");
    }

    @Override
    public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
            throws CPATransferException, InterruptedException, VerificationConditionException,
            SolverException {
        throw new UnsupportedOperationException(
                "Unimplemented method 'computeVerificationCondition'");
    }

}
