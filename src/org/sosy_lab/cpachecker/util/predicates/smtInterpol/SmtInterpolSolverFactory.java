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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.SolverFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

/**
 * Entry point for loading SmtInterpol.
 *
 * Do not access this class directly, it needs to be loaded via
 * {@link org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory}
 * because SmtInterpol needs to have it's own class loader.
 */
public class SmtInterpolSolverFactory implements SolverFactory {

  // Needs to have default constructor because FormulaManagerFactory
  // calls it reflectively

  @Override
  public FormulaManager create(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    return SmtInterpolFormulaManager.create(pConfig, pLogger, pShutdownNotifier);
  }

  @Override
  public ProverEnvironment createProver(FormulaManager pMgr) {
    return ((SmtInterpolFormulaManager)pMgr).createProver();
  }

  @Override
  public InterpolatingProverEnvironment<?> createInterpolatingProver(FormulaManager pMgr) {
    return ((SmtInterpolFormulaManager)pMgr).createInterpolator();
  }

}
