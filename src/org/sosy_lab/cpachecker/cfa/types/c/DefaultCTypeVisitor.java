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
package org.sosy_lab.cpachecker.cfa.types.c;


public abstract class DefaultCTypeVisitor<R, X extends Exception>
  implements CTypeVisitor<R, X> {

  public abstract R visitDefault(final CType t) throws X;

  @Override
  public R visit(final CArrayType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CCompositeType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CElaboratedType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CEnumType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CFunctionType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CPointerType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CProblemType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CSimpleType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CTypedefType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CVoidType t) throws X {
    return visitDefault(t);
  }
  
  @Override
  public R visit(final CBitFieldType t) throws X {
    return visitDefault(t);
  }
}

