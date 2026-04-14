// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.ProgramTransformationInformation;
import org.sosy_lab.cpachecker.cfa.transformation.SubCFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class LocationCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofCheckerCPA {

  private final LocationStateFactory stateFactory;
  private final LocationPrecision initialPrecision;

  private LocationCPA(LocationStateFactory pStateFactory, CFA pCFA) {
    super("sep", "sep", new LocationTransferRelation(pStateFactory));
    stateFactory = pStateFactory;
    boolean hasProgramTransformations =
        pCFA.getMetadata().getNodesToProgramTransformations().isPresent();
    Builder<SubCFA> programTransformations = ImmutableSet.builder();
    if(hasProgramTransformations){
      Collection<ProgramTransformationInformation> programTransformationInformations = pCFA.getMetadata().getNodesToProgramTransformations().get().values();
      for(ProgramTransformationInformation programTransformation : programTransformationInformations){
        programTransformations.add(programTransformation.subCFA());
      }
    }
    initialPrecision = new LocationPrecision(programTransformations.build());
  }

  public static CPAFactory factory() {
    return new LocationCPAFactory(AnalysisDirection.FORWARD);
  }

  public static LocationCPA create(CFA pCFA, Configuration pConfig)
      throws InvalidConfigurationException {
    return new LocationCPA(new LocationStateFactory(pCFA, AnalysisDirection.FORWARD, pConfig), pCFA);
  }

  @Override
  public LocationState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return stateFactory.getState(pNode);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    ImmutableSet<? extends AbstractState> successors = ImmutableSet.copyOf(pSuccessors);
    ImmutableSet<? extends AbstractState> actualSuccessors =
        ImmutableSet.copyOf(
            getTransferRelation()
                .getAbstractSuccessorsForEdge(
                    pElement, SingletonPrecision.getInstance(), pCfaEdge));
    return successors.equals(actualSuccessors);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new LocationPrecisionAdjustment();
  }

  @Override
  public  Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return initialPrecision;
  }

  public LocationStateFactory getStateFactory() {
    return stateFactory;
  }
}
