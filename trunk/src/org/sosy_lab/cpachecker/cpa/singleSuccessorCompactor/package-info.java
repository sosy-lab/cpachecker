/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
/**
 * This CPA should be used as wrapper-CPA.
 * It's location in the CPA-hierarchy should be between ARG- and Composite-CPA.
 * The transfer-relation calls the wrapped transfer-relation
 * as long as there is only/exactly one succeeding abstract state.
 * This can be beneficial for very special programs like RERS/ECA,
 * where long chains of abstract states are part of the CFA.
 *
 * The behavior is comparable with MultiEdges, but in contrast to them,
 * this CPA is not depending on chains in the CFA,
 * because the wrapped transfer-relation might be sufficient to exclude branches
 * and only traverse a chain of CFA-nodes.
 */
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;
