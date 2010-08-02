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
/**
 *
 */
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

/**
 * @author holzera
 *
 */
public class FlatLatticeDomain implements AbstractDomain {
  private final AbstractElement mTopElement;
  private final AbstractElement mBottomElement;
  private final JoinOperator mJoinOperator;
  private final PartialOrder mPartialOrder;

  private static class BottomElement implements AbstractElement {
    @Override
    public String toString() {
      return "<BOTTOM>";
    }
  }

  private static class TopElement implements AbstractElement {
    @Override
    public String toString() {
      return "<TOP>";
    }
  }

  public FlatLatticeDomain(AbstractElement pTopElement, AbstractElement pBottomElement) {
    assert(pTopElement != null);
    assert(pBottomElement != null);

    this.mTopElement = pTopElement;
    this.mBottomElement = pBottomElement;

    this.mPartialOrder = new EqualityPartialOrder(this);
    this.mJoinOperator = new EqualityJoinOperator(this);
  }

  public FlatLatticeDomain() {
    this(new TopElement(), new BottomElement());
  }

  @Override
  public AbstractElement getBottomElement() {
    return this.mBottomElement;
  }

  @Override
  public AbstractElement getTopElement() {
    return this.mTopElement;
  }

  @Override
  public JoinOperator getJoinOperator() {
    return this.mJoinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return this.mPartialOrder;
  }

}
