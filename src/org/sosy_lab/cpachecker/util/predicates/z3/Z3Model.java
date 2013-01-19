/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import org.sosy_lab.cpachecker.util.predicates.Model.TermType;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_sort_kind;



public class Z3Model {

  private static TermType toTermType(long ctx, long sort) {
    try {
      switch (Z3_sort_kind.fromInt(Native.getSortKind(ctx, sort))) {
      case Z3_BOOL_SORT:
        return TermType.Boolean;
      case Z3_INT_SORT:
        return TermType.Integer;
      case Z3_REAL_SORT:
        return TermType.Real;
      case Z3_BV_SORT:
        return TermType.Bitvector;
      case Z3_UNINTERPRETED_SORT:
        return TermType.Uninterpreted;
      case Z3_ARRAY_SORT:
      case Z3_DATATYPE_SORT:
      case Z3_FINITE_DOMAIN_SORT:
      case Z3_RELATION_SORT:
      case Z3_UNKNOWN_SORT:
      default:
        throw new IllegalArgumentException("Unsupported TermType!"); // TODO: Really?
      }
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

}
