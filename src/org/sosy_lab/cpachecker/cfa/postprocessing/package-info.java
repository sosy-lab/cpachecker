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
 * Post-processings for the CFA that change the CFA structure,
 * executed (optionally) between parsing and returning the finished CFA.
 *
 * Be careful when you want to add something here.
 * If possible, do not change the CFA,
 * but write you analysis such that it handles the unprocessed CFA.
 * If your analysis depends on a specifically post-processed CFA,
 * it may not be possible to combine it with other CPAs.
 */
package org.sosy_lab.cpachecker.cfa.postprocessing;