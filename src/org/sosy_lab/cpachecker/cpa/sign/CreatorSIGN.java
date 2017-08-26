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

public class CreatorSIGN implements Creator{

    @Override
    //I am not sure if it has good performance???
    public NumberInterface factoryMethod(Object pO) {
        Integer enumNumber = (Integer) pO;
        switch(enumNumber.intValue()){
       // EMPTY(0), PLUS(1), MINUS(2), ZERO(4), PLUSMINUS(3), PLUS0(5), MINUS0(6), ALL(7);


        case 1: return SIGN.PLUS;
        case 2: return SIGN.MINUS;
        case 3: return SIGN.PLUSMINUS;
        case 4: return SIGN.ZERO;
        case 5: return SIGN.PLUS0;
        case 6: return SIGN.MINUS0;
        case 7: return SIGN.ALL;
        default: return SIGN.EMPTY; //TODO is it correct?
        }

    }

}
