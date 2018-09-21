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
package org.sosy_lab.cpachecker.cpa.hybrid.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionUtils
{

    /**
     * This method reduces a collection of some base type of the given type T
     * till it only contains objects of the sub-type T
     * 
     * This method may return an empty collection.
     * 
     * @param collection The collection to filter
     * @param clazz The class object of type T to 
     */
    public <T> Collection<T>  ofType(Collection<? super T> collection, Class<T> clazz)
    {
        return collection
            .stream()
            .filter(elem -> elem.getClass() == clazz)
            // the cast is safe due to filter operation
            .map(elem -> (T) elem)
            .collect(Collectors.toList());
            
    }

    public <T> boolean appliesToAtLeastOne(Iterable<T> collection, Predicate<T> pred)
    {
        return getApplyingElements(collection, pred).size() >= 1;
    }

    public <T> boolean appliesToAll(Iterable<T> collection, Predicate<T> pred)
    {
        return getApplyingElements(collection, pred).size() == count(collection);
    }

    public <T> Collection<T> getApplyingElements(Iterable<T> collection, Predicate<T> pred)
    {
        Collection<T> resultCollection = new LinkedList<>();

        for(T element : collection)
        {
            if(pred.test(element))
            {
                resultCollection.add(element);
            }
        }

        return resultCollection;
    }

    /**
     * @param collection The collection to calculate the element count for
     * @return The size of the collection
     */
    public <T> int count(Iterable<T> collection)
    {
        int count = 0;
        for(T elem : collection)
        {
            count++;
        }

        return count;
    }

    /**
     * 
     * @param collection The respective collection 
     * @return True, if the collection has any elements, else false
     */
    public <T> boolean any(Iterable<T> collection)
    {
        return count(collection) > 0;
    } 
}