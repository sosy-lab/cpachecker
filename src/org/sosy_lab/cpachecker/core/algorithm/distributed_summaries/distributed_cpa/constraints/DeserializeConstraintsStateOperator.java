// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;

public class DeserializeConstraintsStateOperator implements DeserializeOperator {
  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    // SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    ContentReader constraintContent = pMessage.getAbstractStateContent(ConstraintsState.class);
    String serializedConstraints =
        constraintContent.get(SerializeConstraintsStateOperator.CONSTRAINTS_KEY);
    Preconditions.checkNotNull(serializedConstraints, "Constraints List must be provided");

    HashSet<?> deserializedObjects;
    HashSet<Constraint> constraints = new HashSet<>();
    try {
      deserializedObjects =
          DssSerializeObjectUtil.deserialize(serializedConstraints, HashSet.class);
      for (Object o : deserializedObjects) {
        constraints.add((Constraint) o);
      }
      return new ConstraintsState(constraints);

    } catch (ClassCastException e) {
      throw new AssertionError("Could not deserialize constraints", e);
    }
  }
}
