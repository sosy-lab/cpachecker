/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import org.sosy_lab.cpachecker.cpa.interval.Creator;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;

public class SIGNCreator implements Creator{
    public final static int PLUS = 1;
    public final static int MINUS = 2;
    public final static int PLUSMINUS = 3;
    public final static int ZERO = 4;
    public final static int PLUS0 = 5;
    public final static int MINUS0 = 6;
    public final static int ALL = 7;
    public final static int EMPTY = 0;

    @Override
    public NumberInterface factoryMethod(Object pO) {
        Integer enumNumber = (Integer) pO;
        switch(enumNumber.intValue()){
       // EMPTY(0), PLUS(1), MINUS(2), ZERO(4), PLUSMINUS(3), PLUS0(5), MINUS0(6), ALL(7);


        case PLUS: return SIGN.PLUS;
        case MINUS: return SIGN.MINUS;
        case PLUSMINUS: return SIGN.PLUSMINUS;
        case ZERO: return SIGN.ZERO;
        case PLUS0: return SIGN.PLUS0;
        case MINUS0: return SIGN.MINUS0;
        case ALL: return SIGN.ALL;
        default: return SIGN.EMPTY;
        }

    }

}
