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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.policies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.Edge;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.PolicyAlgebra;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.AllTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.InternalSetComparator;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;

public class ConstellationOperators {

  public static boolean subset(Constellation<SecurityClasses, SecurityClasses> cons1, Constellation<SecurityClasses, SecurityClasses> cons2){
    for(Constellation<SecurityClasses, SecurityClasses>.Pair<SecurityClasses, SecurityClasses> pair:cons1.internalSet){
      SecurityClasses first=pair.first;
      SecurityClasses second=pair.second;
      if(!(cons2.containsPair(first, second))){
        return false;
      }
    }
    return true;
  }

  public static boolean subset(Constellation<SecurityClasses, SecurityClasses> cons,ArrayList<Constellation<SecurityClasses, SecurityClasses>> max ){
    for(Constellation<SecurityClasses, SecurityClasses> maxcon:max){
      if(subset(cons,maxcon)){
        return true;
      }
    }
    return false;
  }

  public static Constellation<SecurityClasses, SecurityClasses> computeConstellation(AllTrackingPrecision prec_pol_1,AllTrackingPrecision prec_pol_2){
    Constellation<SecurityClasses, SecurityClasses> result=new Constellation<>();
    Map<Variable, SecurityClasses> scmap1 = prec_pol_1.getScmap();
     Map<Variable, SecurityClasses> scmap2 = prec_pol_2.getScmap();
    assert(scmap1.keySet().equals(scmap2.keySet()));
    for(Variable var: scmap1.keySet()){
      SecurityClasses s1=scmap1.get(var);
      SecurityClasses s2=scmap2.get(var);
      result.addPair(s1, s2);
    }
    return result;
  }

  public static ArrayList<Constellation<SecurityClasses, SecurityClasses>> getMaximal(ConglomeratePolicy<SecurityClasses> pol_1, ConglomeratePolicy<SecurityClasses> pol_2) throws CloneNotSupportedException{

    PolicyAlgebra<SecurityClasses> alg1=new PolicyAlgebra<>();
    SortedSet<SecurityClasses>  sec_1= alg1.getDomain(pol_1);
    SortedSet<SecurityClasses> sec_2 = alg1.getDomain(pol_2);
    ArrayList<Constellation<SecurityClasses, SecurityClasses>> f_max=new ArrayList<>();
    Constellation<SecurityClasses, SecurityClasses> f=new Constellation<>();
    ArrayList<Constellation<SecurityClasses, SecurityClasses>> w=new ArrayList<>();
    w.add(f);
    while(w.size()!=0){
      f=w.remove(0);
      ArrayList<Constellation<SecurityClasses, SecurityClasses>> f_new=new ArrayList<>();
      for(SecurityClasses s1:sec_1){
        for(SecurityClasses s2:sec_2){
          if(!(f.containsPair(s1, s2))){
            @SuppressWarnings("unchecked")
            Constellation<SecurityClasses, SecurityClasses> fprime=(Constellation<SecurityClasses, SecurityClasses> )f.clone();
            fprime.addPair(s1, s2);
            if(valid(fprime,pol_1,pol_2)){
              f_new.add(fprime);
            }
          }
        }
      }
      if(f_new.size()==0){
        f_max.add(f);
      }
      else{
        for(Constellation<SecurityClasses, SecurityClasses> fprime:f_new){
          @SuppressWarnings("unchecked")
          List<Constellation<SecurityClasses, SecurityClasses>> clone = (List<Constellation<SecurityClasses, SecurityClasses>>)w.clone();
          for(Constellation<SecurityClasses, SecurityClasses> fsecond:clone){
            if(subset(fsecond,fprime)){
              w.remove(fsecond);
            }
          }
          w.add(fprime);

        }
      }
    }
    return f_max;
  }

  public static boolean valid(Constellation<SecurityClasses, SecurityClasses> cons,ConglomeratePolicy<SecurityClasses> pol_1, ConglomeratePolicy<SecurityClasses> pol_2){
    PolicyAlgebra<SecurityClasses> alg1=new PolicyAlgebra<>();
    SortedSet<SecurityClasses> sec_2 = alg1.getDomain(pol_2);
    /*
     * s_1 elem F^{-1}(S_2)
     */
    for(SecurityClasses s1:domain(cons, sec_2)){
      /*
       * tmp1 = {s_1}
       */
      SortedSet<SecurityClasses> tmp1=new TreeSet<>();
      tmp1.add(s1);
      /*
       * set1 elem POW(F^{-1}(S_2))-empty
       */
      SortedSet<SortedSet<SecurityClasses>> powerset1 = SetUtil.getPowerSet(domain(cons, sec_2));
      powerset1.remove(new TreeSet<>());
      for(SortedSet<SecurityClasses> set1:powerset1){
        /*
         * map:  Sec -> 2^{2^Sec}
         */
        TreeMap<SecurityClasses,SortedSet<SortedSet<SecurityClasses>>> map=new TreeMap<>();
        /*
         * set1_elem elem set1
         */
        for(SecurityClasses set1_elem:set1){
          /*
           * tmp_set1 = {set1_elem}
           */
          SortedSet<SecurityClasses> tmp_set1=new TreeSet<>();
          tmp_set1.add(set1_elem);
          /*
           * RANGE
           */
          /*
           * rng_set1=F(tmp_set1)
           */
          SortedSet<SecurityClasses> rng_set1 = range(cons, tmp_set1);
          SortedSet<SortedSet<SecurityClasses>> powerSet = SetUtil.getPowerSet(rng_set1);
          powerSet.remove(new TreeSet<>());
          map.put(set1_elem, powerSet);
        }

        SortedSet<SortedSet<SecurityClasses>> realmap=new TreeSet<>();
        int i=0;
        for(SecurityClasses tmp:set1){
          if(i==0){
            realmap = map.get(tmp);
          }
          else{
            SortedSet<SortedSet<SecurityClasses>> newrealmap = new TreeSet<>(new InternalSetComparator<SecurityClasses>());
            for(SortedSet<SecurityClasses> set: realmap){
              for(SortedSet<SecurityClasses> tet: map.get(tmp)){
                SortedSet<SecurityClasses> newset = SetUtil.union(set,tet);
                newrealmap.add(newset);
              }
            }
            realmap=newrealmap;
          }
          i++;
        }


        for(SecurityClasses s2:range(cons, tmp1)){


          for(SortedSet<SecurityClasses> set2:realmap){
              Edge<SecurityClasses> edge1 = new Edge<>(s1, set1);
              Edge<SecurityClasses> edge2 = new Edge<>(s2, set2);
              if(!(pol_1.getEdges().contains(edge1)) ){
                if(!pol_2.getEdges().contains(edge2)){

                }
                else{
                  return false;
                }
              }
                else{

                }
            }
        }
      }
    }
    return true;
  }

  public static SortedSet<SecurityClasses> range(Constellation<SecurityClasses, SecurityClasses> cons, SortedSet<SecurityClasses> set1){
    SortedSet<SecurityClasses> result=new TreeSet<>();
     for(Constellation<SecurityClasses, SecurityClasses>.Pair<SecurityClasses, SecurityClasses> elem: cons.internalSet){
        if(set1.contains(elem.getFirst())){
          result.add(elem.getSecond());
        }
      }
    return result;
  }

  public static SortedSet<SecurityClasses> domain(Constellation<SecurityClasses, SecurityClasses> cons, SortedSet<SecurityClasses> set2){
    SortedSet<SecurityClasses> result=new TreeSet<>();
    for(Constellation<SecurityClasses, SecurityClasses>.Pair<SecurityClasses, SecurityClasses> elem: cons.internalSet){
      if(set2.contains(elem.getSecond())){
        result.add(elem.getFirst());
      }
    }
    return result;
  }

}
