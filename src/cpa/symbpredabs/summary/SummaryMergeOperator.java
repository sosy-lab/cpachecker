/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs.summary;

import java.util.logging.Level;

import cmdline.CPAMain;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryMergeOperator implements MergeOperator {

  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2,
                               Precision prec) {
    CPAMain.logManager.log(Level.ALL, "DEBUG_4",
        "Trying to merge elements: ", element1,
        " and: ", element2);

    return element2;
  }


  public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
                                           AbstractElementWithLocation element2,
                                           Precision prec) {
    CPAMain.logManager.log(Level.ALL, "DEBUG_4",
        "Trying to merge elements: ", element1,
        " and: ", element2);

    return element2;
  }
}
