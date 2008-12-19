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
package cpa.itpabs;

import cpa.itpabs.ItpAbstractElement;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

/**
 * An ItpAbstractElementManager is an object that know how to create and
 * manipulate generic ItpAbstractElement. This is used to encapsulate the
 * differences between explicit-state and symbolic (with summaries) versions
 * of the analysis.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface ItpAbstractElementManager {
    public ItpAbstractElement create(CFANode location);

    public boolean isFunctionEnd(ItpAbstractElement e, ItpAbstractElement succ);
    public boolean isFunctionStart(ItpAbstractElement e);
    public boolean isRightEdge(ItpAbstractElement e, CFAEdge edge,
                               ItpAbstractElement succ);
    public void pushContextFindRetNode(ItpAbstractElement e,
                                       ItpAbstractElement succ);
}
