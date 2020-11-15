// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
