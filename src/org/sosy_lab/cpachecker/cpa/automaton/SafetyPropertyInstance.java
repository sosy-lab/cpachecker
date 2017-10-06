/*
 * CPAchecker is a tool for configurable software verification.
 *
 *  Copyright (C) 2016-2017  University of Passau
 *
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
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.PropertyInstance;

public class SafetyPropertyInstance implements PropertyInstance<CFANode, SafetyProperty> {

  private final SafetyProperty property;
  private final Optional<CFANode> targetLocation;

  private SafetyPropertyInstance(final SafetyProperty pProperty, final Optional<CFANode> pTargetLocation) {
    property = Preconditions.checkNotNull(pProperty);
    targetLocation = Preconditions.checkNotNull(pTargetLocation);
  }

  @Override
  public SafetyProperty getProperty() {
    return property;
  }

  @Override
  public Optional<CFANode> getInstanceIdentifier() {
    return targetLocation;
  }

  @SuppressWarnings("unused")
  public static <P extends SafetyProperty> SafetyPropertyInstance of(SafetyProperty pProperty,
      @Nullable CFANode pTargetLocation) {
    return new SafetyPropertyInstance(pProperty, Optional.ofNullable(pTargetLocation));
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    SafetyPropertyInstance that = (SafetyPropertyInstance) pO;

    if (!property.equals(that.property)) {
      return false;
    }
    if (!targetLocation.equals(that.targetLocation)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = property.hashCode();
    result = 31 * result + targetLocation.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s @ %s",
        getProperty().toString(),
        getInstanceIdentifier().toString());
  }
}
