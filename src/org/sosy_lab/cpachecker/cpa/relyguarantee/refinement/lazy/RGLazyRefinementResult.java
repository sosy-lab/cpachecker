/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.lazy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

public class RGLazyRefinementResult {

  /** Elements to be removed from ART. */
  private final SetMultimap<Integer, ARTElement> elementsToDrop;
  /** Precision of elements to be adjusted. */
  private final Map<Pair<Integer, ARTElement>, Precision> precisionToAdjust;

  public RGLazyRefinementResult(){
    this.elementsToDrop     = LinkedHashMultimap.create();
    this.precisionToAdjust  = new LinkedHashMap<Pair<Integer, ARTElement>, Precision>();
  }

  public SetMultimap<Integer, ARTElement> getElementsToDrop() {
    return elementsToDrop;
  }

  public Map<Pair<Integer, ARTElement>, Precision> getPrecisionToAdjust() {
    return precisionToAdjust;
  }


  public boolean checkCorrectness() {

    // check that every element covered by the subtree of an element to drop belongs
    // to the subtree of some element to drop
    for (Integer tid : elementsToDrop.keySet()){
      for (ARTElement elem : elementsToDrop.get(tid)){
        Set<ARTElement> subtree = elem.getLocalSubtree();
        for (ARTElement subelem : subtree){
          for (ARTElement covered : subelem.getCoveredByThis()){
            boolean existPivot = false;
            for (ARTElement check : elementsToDrop.get(tid)){
              if (check.getLocalSubtree().contains(covered)){
                existPivot = true;
                break;
              }
            }

            if (!existPivot){
              return false;
            }
          }
        }
      }
    }

    // check that for every element to be dropped, there is precision for its parents
    for (Integer tid : elementsToDrop.keySet()){
      for (ARTElement elem : elementsToDrop.get(tid)){
        for (ARTElement parent : elem.getLocalParents()){
          Pair<Integer, ARTElement> key = Pair.of(tid, parent);
          if (!precisionToAdjust.containsKey(key)){
            return false;
          }
        }
      }
    }

    // check that for every element with precision to be adjusted, there
    // is at least one child to be removed
    for (Pair<Integer, ARTElement> pair : precisionToAdjust.keySet()){
      Integer tid   = pair.getFirst();
      ARTElement elem  = pair.getSecond();
      boolean existChild = false;

      for (ARTElement child : elem.getLocalChildren()){
        if (elementsToDrop.containsEntry(tid, child)){
          existChild = true;
          break;
        }
      }

      if (!existChild){
        return false;
      }
    }

    return true;
  }

  public boolean addElementsToDrop(SetMultimap<Integer, ARTElement> map) {
    return elementsToDrop.putAll(map);
  }

  public void addPrecisionToAdjust(
      Map<Pair<Integer, ARTElement>, Precision> map) {
    precisionToAdjust.putAll(map);
  }


  public String toString(){
    StringBuilder bldr = new StringBuilder();

    bldr.append("Elements to be dropped by thread\n");
    for (Integer tid : elementsToDrop.keySet()){
      bldr.append("\t- tid "+tid+" : ");

      for (ARTElement elem : elementsToDrop.get(tid)){
        bldr.append(elem.getElementId()+", ");
      }
      bldr.append("\n");
    }

    bldr.append("Precision to be adjusted by thread and element id\n");
    for (Pair<Integer, ARTElement>  pair: this.precisionToAdjust.keySet()){
      Integer tid = pair.getFirst();
      int elementId = pair.getSecond().getElementId();
      bldr.append("\t-("+tid+","+elementId+"): "+precisionToAdjust.get(pair)+"\n");
    }

    return bldr.toString();
  }

  @Override
  public int hashCode(){
    return elementsToDrop.hashCode() + 11 * precisionToAdjust.hashCode();
  }
}
