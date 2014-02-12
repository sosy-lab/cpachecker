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
package org.sosy_lab.cpachecker.cpa.defuse;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class DefUseDefinition implements AbstractState {
    private final String variableName;
    private final CFAEdge assigningEdge;

    public DefUseDefinition(String variableName, CFAEdge assigningEdge) {
        this.variableName = Preconditions.checkNotNull(variableName);
        this.assigningEdge = assigningEdge;
    }

    public String getVariableName() {
        return variableName;
    }

    public CFAEdge getAssigningEdge() {
        return assigningEdge;
    }

    @Override
    public int hashCode() {
        return variableName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DefUseDefinition)) {
          return false;
        }

        DefUseDefinition otherDef = (DefUseDefinition) other;
        return otherDef.variableName.equals(this.variableName)
            && Objects.equal(otherDef.assigningEdge, this.assigningEdge);
    }

}
