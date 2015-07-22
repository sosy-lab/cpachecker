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
/**
 *  This package defines a CPA that determines the level of (non-)determism of a program,
 *  by inspecting how many assume edges can be evaluated to a concrete result, and how many not
 *  This should be a good indicator for whether using refinement for the value analysis or not
 *  (e.g. product lines are quite deterministic, and work very good without refinement,
 *  while it is the other way round for most ldv* tasks)
 */
package org.sosy_lab.cpachecker.cpa.deterministic;