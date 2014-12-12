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
package org.sosy_lab.cpachecker.cpa.wp;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


/**
 * Abstract domain. Used for inferring necessary and sufficient preconditions.
 */
@SuppressWarnings("unused")
public class WpAbstractDomain implements AbstractDomain {

  private final PathFormulaManager pathFmgr;
  private final BooleanFormulaManager boolFmgr;

  private WpAbstractState bottomState;
  private WpAbstractState topState;

  public WpAbstractDomain(PathFormulaManager pPathFormulaManager, FormulaManagerView pFormulaManagerView) {
    pathFmgr = pPathFormulaManager;
    boolFmgr = pFormulaManagerView.getBooleanFormulaManager();
    bottomState = null;
    topState = null;
  }

  /**
   * @see AbstractDomain#join()
   */
  @Override
  public AbstractState join(AbstractState s1, AbstractState s2) throws CPAException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see AbstractDomain#isLessOrEqual()
   */
  @Override
  public boolean isLessOrEqual(AbstractState s1, AbstractState s2) throws CPAException, InterruptedException {
    throw new RuntimeException("Not yet implemented");
  }

  public synchronized AbstractState getTopInstance() {
    throw new RuntimeException("Not yet implemented");
  }

  public synchronized AbstractState getBottomInstance() {
    throw new RuntimeException("Not yet implemented");
  }

}
