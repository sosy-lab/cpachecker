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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;

@Options(prefix = "pcc.partial")
public class PartialCertificateTypeProvider {

  public enum PartialCertificateTypes {
    ALL,
    HEURISTIC,
    ARG,
    MONOTONESTOPARG
  }

  private final boolean withCMC;

  @Option(secure=true,
      description = "Selects the strategy used for partial certificate construction")
  private PartialCertificateTypes certificateType = PartialCertificateTypes.HEURISTIC;

  public PartialCertificateTypeProvider(final Configuration pConfig, final boolean pHeuristicAllowed)
      throws InvalidConfigurationException {
    this(pConfig, pHeuristicAllowed, false);
  }

  public PartialCertificateTypeProvider(final Configuration pConfig, final boolean pHeuristicAllowed,
      final boolean partialCertificateForCMC) throws InvalidConfigurationException {
    pConfig.inject(this);
    if (!pHeuristicAllowed && certificateType == PartialCertificateTypes.HEURISTIC) {
      certificateType = PartialCertificateTypes.ARG;
    }
    withCMC = partialCertificateForCMC;
  }

  public PartialReachedConstructionAlgorithm getPartialCertificateConstructor() {
    return getPartialCertificateConstructor(false);
  }

  private PartialReachedConstructionAlgorithm getPartialCertificateConstructor(boolean pKeepARGState) {
    switch (certificateType) {
    case ARG:
      return new ARGBasedPartialReachedSetConstructionAlgorithm(pKeepARGState);
    case MONOTONESTOPARG:
      return new MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm(pKeepARGState, withCMC);
    default:// HEURISTIC
      return new HeuristicPartialReachedSetConstructionAlgorithm();
    }
  }

  public PartialReachedConstructionAlgorithm getCertificateConstructor() {
    if (certificateType == PartialCertificateTypes.ALL) {
      return new CompleteCertificateConstructionAlgorithm();
    }
    return getPartialCertificateConstructor(true);
  }

}
