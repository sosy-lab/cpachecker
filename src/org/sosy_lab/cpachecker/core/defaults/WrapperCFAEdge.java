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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;

public class WrapperCFAEdge implements AbstractEdge {
  private final CFAEdge edge;

  public WrapperCFAEdge(CFAEdge pEdge) {
    Preconditions.checkNotNull(pEdge);
    edge = pEdge;
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  @Override
  public int hashCode() {
    return Objects.hash(edge);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof WrapperCFAEdge)) {
      return false;
    }
    WrapperCFAEdge other = (WrapperCFAEdge) obj;

    return edge == other.edge;
  }

  @Override
  public String toString() {
    return edge.toString();
  }
}
