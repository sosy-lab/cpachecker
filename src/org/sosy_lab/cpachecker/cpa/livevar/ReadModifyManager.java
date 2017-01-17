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
package org.sosy_lab.cpachecker.cpa.livevar;

import com.google.common.base.Equivalence.Wrapper;
import java.util.BitSet;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.VariableClassification;

@Options(prefix="cpa.liveVar")
public class ReadModifyManager extends LiveVariablesManager {
  private boolean findModified = false;

  public ReadModifyManager(
      Optional<VariableClassification> pVarClass,
      Configuration pConfig,
      Language pLang,
      CFA pCFA,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pVarClass, pConfig, pLang, pCFA, pLogger);
    pConfig.inject(this, ReadModifyManager.class);
  }

  /**
   * @return All variables read by instructions associated with {@code pEdge}.
   */
  public Collection<Wrapper<ASimpleDeclaration>> getReadVars(CFAEdge pEdge)
      throws CPATransferException {

    // Assume that none of the variables are live,
    // see what variables become live after processing the statement.
    LiveVariablesState state = LiveVariablesState.empty(noVars, this);

    LiveVariablesState successor = getAbstractSuccessorsForEdge(
        state, SingletonPrecision.getInstance(), pEdge
    ).iterator().next();
    Collection<Wrapper<ASimpleDeclaration>> out = dataToVars(successor.getData());
    return out;
  }

  /**
   * @return All variables modified by instructions associated with {@code pEdge}.
   */
  public Collection<Wrapper<ASimpleDeclaration>> getKilledVars(CFAEdge pEdge)
      throws CPATransferException {
    findModified = true;
    try {
      BitSet full = new BitSet(noVars);

      // Assume that all variables are live, see what variables stop
      // being live after the statement.
      full.flip(0, noVars);
      LiveVariablesState state = LiveVariablesState.of(full, this);
      LiveVariablesState successor = getAbstractSuccessorsForEdge(
          state, SingletonPrecision.getInstance(), pEdge
      ).iterator().next();

      // Reverse the logic: get all bits associated with killed variables.
      BitSet data = successor.getData();
      data.flip(0, noVars);
      return dataToVars(data);
    } finally {
      findModified = false;
    }
  }

  @Override
  protected void handleExpression(AExpression expression, BitSet writeInto) {
    if (!findModified) {
      super.handleExpression(expression, writeInto);
    }
  }

  @Override
  protected boolean isLeftHandSideLive(ALeftHandSide expression) {

    // This is a hackish solution: for our purposes we need
    // to treat assignment LHS as if it was always live.
    return true;
  }

  @Override
  protected boolean getAssumeGlobalsAreAlwaysLive() {
    return false;
  }
}
