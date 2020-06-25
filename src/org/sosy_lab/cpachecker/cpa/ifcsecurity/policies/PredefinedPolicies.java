/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.AggregationFlow;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.PolicyAlgebra;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class, that offers some common Policies and SecurityClasses.
 */
public class PredefinedPolicies{

  /*
   * Low, High- Policies
   */
  final public static SecurityClasses LOW=new EnumSecurityClass(SecurityClassesSet.low);
  final public static SecurityClasses HIGH=new EnumSecurityClass(SecurityClassesSet.high);
  final public static SecurityClasses UNIMPORTANT=new EnumSecurityClass(SecurityClassesSet.any);
  final public static ConglomeratePolicy<SecurityClasses> HILO;
  final public static ConglomeratePolicy<SecurityClasses> HILOANY;

  /*
   * Military- Policy
   */
  final public static SecurityClasses UNCONFIDENTIAL=new EnumSecurityClass(SecurityClassesSet.unconfidential);
  final public static SecurityClasses CONFIDENTIAL=new EnumSecurityClass(SecurityClassesSet.confidential);
  final public static SecurityClasses SECRET=new EnumSecurityClass(SecurityClassesSet.secret);
  final public static SecurityClasses TOPSECRET=new EnumSecurityClass(SecurityClassesSet.topsecret);
  final public static ConglomeratePolicy<SecurityClasses> MILITARY;

  /**
   * Utility for computation of Policy-Algebra-Operations.
   */
  private static PolicyAlgebra<SecurityClasses> alg;


  /**
   * Initialization
   */
  static{
    alg=new PolicyAlgebra<>();
    HILO=createHiloPol();
    HILOANY=createHiloAnyPol();
    MILITARY=createMilitary();
  }

  /**
   * Constructs a classical high-low Policy
   * @return the  high-low Policy
   */
  private static ConglomeratePolicy<SecurityClasses> createHiloPol(){
    //Specify Classes
    SecurityClasses l1=new EnumSecurityClass(SecurityClassesSet.low);
    SecurityClasses h1=new EnumSecurityClass(SecurityClassesSet.high);
    //Specify Edges
    SortedSet<SecurityClasses> set=new TreeSet<>();
    set.add(l1);
    set.add(h1);
    //Set Policy
    ConglomeratePolicy<SecurityClasses> pol=new AggregationFlow<>(h1,set);
    return pol;
  }

  /**
   * Constructs a high(high-external)-low Policy which contains also unimportant high-elements (high-internal)
   * @return the  high-low-any Policy
   */
  private static ConglomeratePolicy<SecurityClasses> createHiloAnyPol(){
    //Specify Classes
    SecurityClasses u=new EnumSecurityClass(SecurityClassesSet.any);
    //Specify Edges
    SortedSet<SecurityClasses> set=new TreeSet<>();
    set.add(u);
    //Set Policy
    ConglomeratePolicy<SecurityClasses> pol=new AggregationFlow<>(u,set);
    //Join-Operation with HILO
    ConglomeratePolicy<SecurityClasses> retpol=alg.join(pol,HILO);
    return retpol;
  }

  /**
   * Constructs the classical Military Policy
   *  @return the Military-Policy
   */
  private static ConglomeratePolicy<SecurityClasses> createMilitary(){
    //Specify Classes
    SecurityClasses u=new EnumSecurityClass(SecurityClassesSet.unconfidential);
    SecurityClasses c=new EnumSecurityClass(SecurityClassesSet.confidential);
    SecurityClasses s=new EnumSecurityClass(SecurityClassesSet.secret);
    SecurityClasses t=new EnumSecurityClass(SecurityClassesSet.topsecret);
    //Set Policy
    //u->{u}
    SortedSet<SecurityClasses> set=new TreeSet<>();
    set.add(u);
    ConglomeratePolicy<SecurityClasses> pol=new AggregationFlow<>(u,set);
    //c->{u,c}
    set=new TreeSet<>(set);
    set.add(c);
    ConglomeratePolicy<SecurityClasses> tmppol=new AggregationFlow<>(c,set);
    pol=alg.union(pol,tmppol);
    //s->{u,c,s}
    set=new TreeSet<>(set);
    set.add(s);
    tmppol=new AggregationFlow<>(s,set);
    pol=alg.union(pol,tmppol);
    //s->{u,c,s,t}
    set=new TreeSet<>(set);
    set.add(t);
    tmppol=new AggregationFlow<>(t,set);
    pol=alg.union(pol,tmppol);
    return pol;
  }
}
