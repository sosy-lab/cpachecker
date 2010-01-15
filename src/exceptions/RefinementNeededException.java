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
package exceptions;

import java.util.Collection;

import cpa.common.interfaces.AbstractElement;

/**
 * Exception thrown when an refinement is needed.
 * 
 * Because of the separation of CPAAlgorithm and CEGAR, this exception cannot
 * be used to signal the need of refinement any more, instead the program will
 * crash. 
 */
@Deprecated
public class RefinementNeededException extends CPATransferException {

    /**
     * auto generated
     */
    private static final long serialVersionUID = -141927893977460824L;

    private final Collection<AbstractElement> toUnreach;
    private final Collection<AbstractElement> toWaitlist;

    public RefinementNeededException(
            Collection<AbstractElement> toUnreach,
            Collection<AbstractElement> toWaitlist) {
        super();
        this.toUnreach = toUnreach;
        this.toWaitlist = toWaitlist;
    }

    public Collection<AbstractElement> getReachableToUndo() {
        return toUnreach;
    }

    public Collection<AbstractElement> getToWaitlist() {
        return toWaitlist;
    }

}
