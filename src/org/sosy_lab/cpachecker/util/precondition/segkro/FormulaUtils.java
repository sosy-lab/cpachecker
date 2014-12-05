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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

import com.google.common.collect.Lists;


public class FormulaUtils {

  public static boolean equalFormula(BooleanFormula f1, BooleanFormula f2) {
    // TODO: Implement a cache!
    return f1.equals(f2); // TODO: Test this!!
  }

  public static boolean containsFormulasFrom(List<BooleanFormula> pToCheck, Collection<BooleanFormula> pFrom) {
    for (BooleanFormula f: pFrom) {
      if (pToCheck.contains(f)) {
        return true; // TODO: Test this!!
      }
    }
    return false;
  }

  public static List<BooleanFormula> substractEqualFromulasFrom(Collection<BooleanFormula> pSubstractFrom, Collection<BooleanFormula> pSubstract) {
    // TODO: This might not be the optimal solution!

    List<BooleanFormula> result = Lists.newLinkedList(pSubstractFrom);
    Iterator<BooleanFormula> it = result.iterator();

    while (it.hasNext()) {
      BooleanFormula element = it.next();
      for (BooleanFormula s: pSubstract) {
        if (equalFormula(s, element)) {
          it.remove();
          break;
        }
      }
    }

    return result;
  }

}
