// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.CPAs;

public class MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm
    implements PartialReachedConstructionAlgorithm {

  private final boolean returnARGStates;
  private final boolean withCMC;

  public MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm(
      final boolean pReturnARGStatesInsteadOfWrappedStates, final boolean pWithCMC) {
    returnARGStates = pReturnARGStatesInsteadOfWrappedStates;
    withCMC = pWithCMC;
  }

  @Override
  public AbstractState[] computePartialReachedSet(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    if (!(pReached.getFirstState() instanceof ARGState)) {
      throw new InvalidConfigurationException(
          "May only compute partial reached set with this algorithm if an ARG is constructed and"
              + " ARG is top level state.");
    }
    ARGCPA argCpa =
        CPAs.retrieveCPAOrFail(
            pCpa, ARGCPA.class, ARGBasedPartialReachedSetConstructionAlgorithm.class);
    ARGState root = (ARGState) pReached.getFirstState();

    NodeSelectionARGPass argPass = getARGPass(pReached.getPrecision(root), root, argCpa);
    argPass.passARG(root);

    List<? extends AbstractState> reachedSetSubset = argPass.getSelectedNodes();
    return reachedSetSubset.toArray(new AbstractState[0]);
  }

  /**
   * TODO write comment
   *
   * @param pRootPrecision the root precision
   * @param pRoot the root state
   * @param pCpa the CPA that corresponds to these states
   * @throws InvalidConfigurationException may be thrown in subclasses
   */
  protected NodeSelectionARGPass getARGPass(
      final Precision pRootPrecision, final ARGState pRoot, final ARGCPA pCpa)
      throws InvalidConfigurationException {
    return new NodeSelectionARGPass(pRoot);
  }

  protected class NodeSelectionARGPass extends AbstractARGPass {

    private final ARGState root;

    public NodeSelectionARGPass(final ARGState pRoot) {
      super(false);
      root = pRoot;
    }

    private List<AbstractState> wrappedARGStates = new ArrayList<>();
    private List<ARGState> argStates = new ArrayList<>();

    @Override
    public void visitARGNode(final ARGState pNode) {
      if (isToAdd(pNode)) {
        if (returnARGStates) {
          argStates.add(pNode);
        } else {
          wrappedARGStates.add(pNode.getWrappedState());
        }
      }
    }

    protected boolean isToAdd(final ARGState pNode) {
      return Objects.equals(pNode, root)
          || pNode.getParents().size() > 1
          || (!pNode.getCoveredByThis().isEmpty() && !pNode.isCovered())
          || (withCMC
              && (pNode.getChildren().size() > 1
                  || (!pNode.isCovered()
                      && (pNode.getChildren().isEmpty()
                          || pNode.getParents().iterator().next().getChildren().size() > 1))));
    }

    @Override
    public boolean stopPathDiscovery(final ARGState pNode) {
      return false;
    }

    public List<? extends AbstractState> getSelectedNodes() {
      if (returnARGStates) {
        return argStates;
      } else {
        return wrappedARGStates;
      }
    }
  }
}
