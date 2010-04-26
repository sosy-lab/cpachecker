/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults;


import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.UnmodifiableReachedElements;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;

/**
 * This class implements the PrecisionAdjustment operator for a CPA, where the
 * precision never changes. It does not make any assumptions about the precision,
 * even not that the precision is non-null.
 *  
 * @author wendler
 */
public class StaticPrecisionAdjustment implements PrecisionAdjustment {

  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement element,
        Precision precision, UnmodifiableReachedElements elements) {
    
    return new Pair<AbstractElement, Precision>(element, precision);
  }
  
  private static final PrecisionAdjustment instance = new StaticPrecisionAdjustment();
  
  public static PrecisionAdjustment getInstance() {
    return instance;
  }
}
