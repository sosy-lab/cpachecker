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

@Options(prefix = "pcc")
public class PartialCertificateTypeProvider {

  public enum PartialCertificateTypes {
    HEURISTIC,
    ARG,
    MONOTONESTOPARG;
  }

  @Option(
      description = "Selects the strategy used for partial certificate construction")
  private PartialCertificateTypes certificateType = PartialCertificateTypes.HEURISTIC;

  public PartialCertificateTypeProvider(final Configuration pConfig, final boolean pHeuristicAllowed)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    if (!pHeuristicAllowed && certificateType == PartialCertificateTypes.HEURISTIC) {
      certificateType = PartialCertificateTypes.ARG;
    }
  }

  public PartialCertificateTypes getCertificateType() {
    return certificateType;
  }

}
