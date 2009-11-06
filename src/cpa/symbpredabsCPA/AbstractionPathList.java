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
package cpa.symbpredabsCPA;

import java.util.ArrayList;
import java.util.List;

/**
 * This list is used to keep the path of abstraction locations up to
 * an abstract element. This is not used as a stack, whenever we compute
 * a new abstraction, we add its node to the end of the list.
 * @author erkan
 *
 */
class AbstractionPathList {

  private List<Integer> parents;

  public AbstractionPathList(){
    parents = new ArrayList<Integer>();
  }

  public void addToList(int i){
    parents.add(i);
  }

  @Override
  public boolean equals(Object o){
    if (o == null) {
      return false;
    }

    AbstractionPathList otherParentsList = (AbstractionPathList) o;
    List<Integer> otherList = otherParentsList.parents;
    if (this.parents.size() != otherList.size()){
      return false;
    }
    else{
      for(int i=0; i<otherList.size(); i++){
        if(this.parents.get(i) != otherList.get(i)){
          return false;
        }
      }
    }
    return true;
  }

  public void copyFromExisting(AbstractionPathList parents2) {
    parents.addAll(parents2.parents);
  }

  @Override
  public String toString(){
    String s = "";
    for(int par:parents){
      s = s + "-"+ par;
    }
    return s;
  }

  @Override
  public int hashCode() {
    return parents.hashCode();
  }
}
