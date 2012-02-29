/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic.nodes;

import org.sosy_lab.cpachecker.cpa.seplogic.csif.CorestarInterface;


public abstract class Formula extends SeplogicNode implements Cloneable {
  public static String RETVAR = CorestarInterface.RETVAR;
  public static Formula TRUE = new Equality(new StringArgument("1"), new StringArgument("1"));
  public static Formula FALSE = new Equality(new StringArgument("1"), new StringArgument("0"));

  private String cachedRepr = null;

  @Override
  protected Object clone() throws CloneNotSupportedException {
    Formula f = (Formula) super.clone();
    f.cachedRepr = null;
    return f;
  }


  public String getRepr() {
    if (cachedRepr != null)
      return cachedRepr;
    cachedRepr = toString();
    return cachedRepr;
  }


  public void setCachedRepr(String pCachedRepr) {
    cachedRepr = pCachedRepr;
  }

}
