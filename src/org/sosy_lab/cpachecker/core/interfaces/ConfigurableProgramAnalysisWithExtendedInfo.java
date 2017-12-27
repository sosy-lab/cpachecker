/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;


/**
 * This Cpa can enrich counterexample with additional information.
 */
public interface ConfigurableProgramAnalysisWithExtendedInfo
    extends ConfigurableProgramAnalysisWithConcreteCex{
  /**
   * Creates a {@link CFAPathWithAdditionalInfo} path, that contain the concrete values of
   * the given variables along the given {@link ARGPath}. The {@link ConcreteStatePath} path
   * is used to calculate the concrete values of the variables along the generated counterexample path.
   *
   *
   * @param path An {@link CFAPathWithAdditionalInfo} counterexample path, generated from the {@link ARGCPA}.
   * The concrete values of variables along this path should be calculated.
   * @param pPath
   * @return A {@link CFAPathWithAdditionalInfo} path along the {@link CFAEdge} edges of the {@link ARGPath} path that
   * contain concrete values for the variables along the path.
   */
  CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath);

}
