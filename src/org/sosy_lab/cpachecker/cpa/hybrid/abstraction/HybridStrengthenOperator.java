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
package org.sosy_lab.cpachecker.cpa.hybrid.abstraction;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.hybrid.HybridAnalysisState;

public interface HybridStrengthenOperator {

    /**
     * 
     * @param stateToStrengthen The hybrid analysis state to strengthen
     * @param strengtheningInformationState The other state with strengthening information
     * @param edge The respective edge of the cfa
     * @return a HybridAnalysisState that is at least as strong as the incoming state
     */
    public HybridAnalysisState strengthen(
        HybridAnalysisState stateToStrengthen,
        AbstractState strengtheningInformationState,
        CFAEdge edge);

    /**
     * 
     * @param stateToStrengthen A state of another domain to strengthen with possible concrete values
     * @param strengtheningInformationState The HybridAnalysisState containing concrete values for strengthening
     * @param edge The respective dege of the cfa
     * @return A state of another domain with (possible) concrete values injected
     */
    default AbstractState injectStrengthening(
        AbstractState stateToStrengthen,
        HybridAnalysisState strengtheningInformationState,
        CFAEdge edge) {
        return stateToStrengthen; // examine, if this method is sensible in some cases
    }
    
}