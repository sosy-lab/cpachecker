// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.PropertyChecker;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.propertychecker.PropertyCheckerBuilder;

@Options(prefix = "cpa.propertychecker")
public class PropertyCheckerCPA extends AbstractSingleWrapperCPA implements ProofChecker {

  @Option(
      secure = true,
      description =
          "Qualified name for class which checks that the computed abstraction adheres to the"
              + " desired property.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.pcc.propertychecker")
  private Class<? extends PropertyChecker> className =
      org.sosy_lab.cpachecker.pcc.propertychecker.DefaultPropertyChecker.class;

  @Option(
      secure = true,
      description =
          "List of parameters for constructor of propertychecker.className. Parameter values are"
              + " specified in the order the parameters are defined in the respective constructor."
              + " Every parameter value is finished with \",\". The empty string represents an"
              + " empty parameter list.")
  private String parameters = "";

  private final PropertyChecker propChecker;
  private final ProofChecker wrappedProofChecker;

  public PropertyCheckerCPA(ConfigurableProgramAnalysis pCpa, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);
    propChecker = PropertyCheckerBuilder.buildPropertyChecker(className, parameters);
    if (pCpa instanceof ProofChecker) {
      wrappedProofChecker = (ProofChecker) pCpa;
    } else {
      wrappedProofChecker = null;
    }
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PropertyCheckerCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return getWrappedCpa().getInitialState(pNode, pPartition);
  }

  public PropertyChecker getPropChecker() {
    return propChecker;
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Preconditions.checkNotNull(
        wrappedProofChecker, "Wrapped CPA must implement the ProofChecker interface");
    return wrappedProofChecker.areAbstractSuccessors(pState, pCfaEdge, pSuccessors);
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    Preconditions.checkNotNull(
        wrappedProofChecker, "Wrapped CPA must implement the ProofChecker interface");
    return wrappedProofChecker.isCoveredBy(pState, pOtherState);
  }
}
