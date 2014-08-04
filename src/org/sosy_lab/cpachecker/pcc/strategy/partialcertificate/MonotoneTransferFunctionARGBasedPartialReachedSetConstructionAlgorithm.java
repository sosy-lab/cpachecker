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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm implements PartialReachedConstructionAlgorithm {

  private final boolean returnARGStates;

  public MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm(final boolean pReturnARGStatesInsteadOfWrappedStates){
    returnARGStates = pReturnARGStatesInsteadOfWrappedStates;
  }

  @Override
  public AbstractState[] computePartialReachedSet(final UnmodifiableReachedSet pReached)
      throws InvalidConfigurationException {
    if (!(pReached.getFirstState() instanceof ARGState)) { throw new InvalidConfigurationException(
        "May only compute partial reached set with this algorithm if an ARG is constructed and ARG is top level state."); }
    ARGState root = (ARGState) pReached.getFirstState();

    NodeSelectionARGPass argPass = getARGPass(pReached.getPrecision(root), root);
    argPass.passARG(root);

    List<? extends AbstractState> reachedSetSubset = argPass.getSelectedNodes();
    return reachedSetSubset.toArray(new AbstractState[reachedSetSubset.size()]);
  }

  protected NodeSelectionARGPass getARGPass(final Precision pRootPrecision, final ARGState pRoot)
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
      return pNode == root || pNode.getParents().size() > 1 || pNode.getCoveredByThis().size() > 0
          && !pNode.isCovered();
    }

    @Override
    public boolean stopPathDiscovery(final ARGState pNode) {
      return false;
    }

    public List<? extends AbstractState> getSelectedNodes(){
      if (returnARGStates) {
        return argStates;
      } else {
        return wrappedARGStates;
      }
    }

  }


}
