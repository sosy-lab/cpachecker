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
package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperTransferRelation;

public abstract class AbstractSingleWrapperTransferRelation implements WrapperTransferRelation {

  protected final TransferRelation transferRelation;

  protected AbstractSingleWrapperTransferRelation(TransferRelation pWrapped) {
    transferRelation = Preconditions.checkNotNull(pWrapped);
  }

  @Override
  @Nullable
  public <T extends TransferRelation> T retrieveWrappedTransferRelation(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(transferRelation.getClass())) {
      return pType.cast(transferRelation);
    } else if (transferRelation instanceof WrapperTransferRelation) {
      return ((WrapperTransferRelation) transferRelation).retrieveWrappedTransferRelation(pType);
    } else {
      return null;
    }
  }

  @Override
  public Iterable<TransferRelation> getWrappedTransferRelations() {
    return ImmutableList.of(transferRelation);
  }
}
