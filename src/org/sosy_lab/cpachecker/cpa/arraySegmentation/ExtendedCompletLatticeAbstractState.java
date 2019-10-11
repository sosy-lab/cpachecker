/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation;

import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public interface ExtendedCompletLatticeAbstractState<T extends ExtendedCompletLatticeAbstractState<T>>
    extends ClonableLatticeAbstractState<T> {

  /**
   * The specific empty element is not part of the original lattice, hence it will throw an error
   * when merged or compared to other lattice elements- The only purpose is to avoid null values,
   * since it is used at the end of each segmentation.
   *
   * @return a instance that represents the specific empty element. It needs to be able to return
   *         top, bottom and the meetOperator
   *
   */
  T constructEmptyInstance();

  /**
   *
   * @return the top element of the complete lattice
   */
  T getTopElement();

  /**
   *
   * @return the bottom element of the complete lattice
   */
  T getBottomElement();

  /**
   * Since this is a complete lattice, a meet operator exists. It will be used during unification of
   * array segmentations
   *
   * @return the meet operator
   */
  BinaryOperator<T> getMeetOperator();

  /**
   * If during the evaluation of a binary expression segment bounds need to removed, it need to be
   * specified, if removing stored analysis information leads to a unsound analysis result (e.g. the
   * analysis does not correctly over-approximate anymore). Hence, if the value is a default value
   * (in general the bottom element of the lattice), removing is ok. Otherwise, the computation is
   * aborted and a specific error symbol is returned, indicating that the analysis would compute a
   * unsound result
   *
   * @return true, if the value is default and can be removed
   */
  default Predicate<T> getIsDefaultValueAndCanBeRemoved() {
    return new Predicate<T>() {

      @Override
      public boolean test(T element) {
        return element.equals(getBottomElement());
      }
    };
  }

}
