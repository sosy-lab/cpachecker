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
package org.sosy_lab.cpachecker.cpa.hybrid.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.hybrid.util.CollectionUtils;

public class CollectionUtilsTest{

    @Test
    public void ofTypeTest() {
        List<Number> numList = Arrays.asList(
            new Integer(1),
            new Integer(2),
            new Integer(3),
            new Double(2.0),
            new Double(3.44),
            new Float(2.5)
        );

        Collection<Integer> filteredList = CollectionUtils.ofType(numList, Integer.class); 
        Collection<Double> filteredDoubles = CollectionUtils.ofType(numList, Double.class); 
        Assert.assertEquals(3, CollectionUtils.count(filteredList));
        Assert.assertEquals(2, CollectionUtils.count(filteredDoubles));
    }

    @Test
    public void countTest() {
        List<Number> numList = Arrays.asList(1,2,3);
        Assert.assertEquals(3, CollectionUtils.count(numList));
    }

    @Test
    public void applyingElementsTest() {
        CType voidEl = CVoidType.create(true, false);
        CType intEl = CNumericTypes.INT;
        CType boolEl = CNumericTypes.BOOL;
        CType floatEl = CNumericTypes.FLOAT;
        CType arrayEl = new CArrayType(false, false, CNumericTypes.INT, null);
        Collection<CType> typeCollection = CollectionUtils.of(voidEl, intEl, boolEl, floatEl, arrayEl);

        Collection<CType> sutResult = CollectionUtils.getApplyingElements(typeCollection, exp -> exp instanceof CSimpleType);
        Assert.assertTrue(sutResult.size() == 3);
    }
}