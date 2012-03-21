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
package org.sosy_lab.cpachecker.core.waitlist;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;

/**
 * Waitlist where elements are sorted using the provided comparator.
 */
public class ComparatorWaitlist implements Waitlist {

  private final TreeSet<AbstractElement> waitlist;

  public ComparatorWaitlist(Comparator<AbstractElement> comp){
    waitlist = new TreeSet<AbstractElement>(comp);
  }

  @Override
  public Iterator<AbstractElement> iterator() {
    return waitlist.iterator();
  }

  @Override
  public void add(AbstractElement pElement) {
    waitlist.add(pElement);
  }

  @Override
  public void clear() {
    waitlist.clear();
  }

  @Override
  public boolean contains(AbstractElement pElement) {
    return waitlist.contains(pElement);
  }

  @Override
  public boolean isEmpty() {
    return waitlist.isEmpty();
  }

  @Override
  public AbstractElement pop() {
    return waitlist.pollLast();
  }

  @Override
  public boolean remove(AbstractElement pElement) {
    return waitlist.remove(pElement);
  }

  @Override
  public int size() {
    return waitlist.size();
  }

  @Override
  public String toString(){
    return waitlist.toString();
  }


  public enum ComparatorWaitlistFactory implements WaitlistFactory {
    ARTID_MAX {
      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new ARTID_MAX());
      }
    },

    ENVAPP_MIN_TOP_MIN_DISTANCE_MIN {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new EnvAppMinTopMinDistMin());
      }
    },

    ENVAPP_LIMIT_TOP_MIN_DISTANCE_MIN {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new  EnvLimitMinTopMinDistMin());
      }
    },

    LBE_TEST {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new LBETest());
      }

    }

  }

  /*
   * Minimize env. applications and then minimizes topological numbers.

  public static class EnvAppMinTopMin implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (elem1 == elem2){
        return 0;
      }

      Set<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      Set<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      // compare the number of env. applications
      if (eapp1.size() < eapp2.size()){
        return 1;
      }

      if (eapp1.size() > eapp2.size()){
        return -1;
      }

      // compare applications recursivly
      for (int i=0; i<eapp1.size(); i++){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(i);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(i);

        // choose the one with lower topological application point
        int apptop1 = app1.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int apptop2 = app2.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (apptop1 < apptop2){
          return 1;
        }

        if (apptop1 > apptop2){
          return -1;
        }

        // compare the sources of env. transitions
        ARTElement source1 = app1.getSecond().getSourceARTElement();
        ARTElement source2 = app2.getSecond().getSourceARTElement();

        int result = compare(source1, source2);
        if (result != 0){
          return result;
        }
      }


      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top2 < top1){
        return -1;
      }

      // compre ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 < id2) ? -1 : 0;
    }
  }*/


  /*
   * Minimize env. applications and then minimizes topological numbers.

  public static class EnvAppMinTopMin2 implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (elem1 == elem2){
        return 0;
      }

      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      // compare the number of env. applications
      if (eapp1.size() < eapp2.size()){
        return 1;
      }

      if (eapp1.size() > eapp2.size()){
        return -1;
      }

      // compare topological numbers
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top1 > top2){
        return -1;
      }

      // compare applications recursivly
      for (int i=0; i<eapp1.size(); i++){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(i);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(i);

        // choose the one with lower topological application point
        int apptop1 = app1.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int apptop2 = app2.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (apptop1 < apptop2){
          return 1;
        }

        if (apptop1 > apptop2){
          return -1;
        }

        // compare the sources of env. transitions
        ARTElement source1 = app1.getSecond().getSourceARTElement();
        ARTElement source2 = app2.getSecond().getSourceARTElement();

        int result = compare(source1, source2);
        if (result != 0){
          return result;
        }
      }

      // compare ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 < id2) ? -1 : 0;
    }
  }*/


  /*
   * Minimize env. applications and then minimizes topological numbers.

  public static class Mix implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (aelem1.equals(aelem2)){
        return 0;
      }

      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      // compare the number of env. applications
      if (eapp1.size() < eapp2.size()){
        return 1;
      }

      if (eapp1.size() > eapp2.size()){
        return -1;
      }

      // compare topological numbers
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top1 > top2){
        return -1;
      }

      // compare applications recursivly
      for (int i=0; i<eapp1.size(); i++){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(i);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(i);

        // choose the one with lower topological application point
        int apptop1 = app1.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int apptop2 = app2.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (apptop1 > apptop2){
          return 1;
        }

        if (apptop1 < apptop2){
          return -1;
        }

        // compare the sources of env. transitions
        ARTElement source1 = app1.getSecond().getSourceARTElement();
        ARTElement source2 = app2.getSecond().getSourceARTElement();

        int result = compare(source1, source2);
        if (result != 0){
          return result;
        }
      }

      // compare ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 < id2) ? -1 : 0;
    }
  }*/




  public static class ARTID_MAX implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {

      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      int br1 = aelem1.getRefinementBranches();
      int br2 = aelem2.getRefinementBranches();

      if (br1 < br2){
        return 1;
      }

      if (br1 > br2){
        return -1;
      }

      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 == id2) ? 0 : -1;
    }
  }




  /*
   * Minimize env. applications and maximizes then topological numbers.
  public static class EnvAppMinTopMax implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (elem1 == elem2){
        return 0;
      }

      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      // compare the number of env. applications
      if (eapp1.size() < eapp2.size()){
        return 1;
      }

      if (eapp1.size() > eapp2.size()){
        return -1;
      }


      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 > top2){
        return 1;
      }

      if (top2 > top1){
        return -1;
      }

      // compare applications recursivly
      for (int i=0; i<eapp1.size(); i++){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(i);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(i);

        // choose the one with higher topological application point
        int apptop1 = app1.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int apptop2 = app2.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (apptop1 > apptop2){
          return 1;
        }

        if (apptop1 < apptop2){
          return -1;
        }

        // compare the sources of env. transitions
        ARTElement source1 = app1.getSecond().getSourceARTElement();
        ARTElement source2 = app2.getSecond().getSourceARTElement();

        int result = compare(source1, source2);
        if (result != 0){
          return result;
        }
      }



      // compre ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 < id2) ? -1 : 0;
    }
  }*/


  /**
   * Minimize env. app, minimize topological id, min. distance from root,
   *
   */
  public static class EnvAppMinTopMinDistMin implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {

      if (elem1.equals(elem2)){
        return 0;
      }


      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      Set<Pair<ARTElement, RGEnvTransition>> appbf1 = aelem1.getEnvApplied();
      Set<Pair<ARTElement, RGEnvTransition>> appbf2 = aelem2.getEnvApplied();


      if (appbf1.size() < appbf2.size()){
        return 1;
      }

      if (appbf1.size() > appbf2.size()){
        return -1;
      }

     /* if (!appbf1.isEmpty()){

        // max top of application point
        for (int i=0; i<appbf1.size(); i++){
          Pair<ARTElement, RGEnvTransition> pair1 = appbf1.get(i);
          Pair<ARTElement, RGEnvTransition> pair2 = appbf2.get(i);

          ARTElement app1 = pair1.getFirst();
          ARTElement app2 = pair2.getFirst();

          int appTop1 = app1.retrieveLocationElement().getLocationNode()
              .getTopologicalSortId();
          int appTop2 = app2.retrieveLocationElement().getLocationNode()
              .getTopologicalSortId();

          if (appTop1 > appTop2){
            return 1;
          }

          if (appTop1 < appTop2){
            return -1;
          }


          Integer appD1 = app1.getDistanceFromRoot();
          Integer appD2 = app2.getDistanceFromRoot();
          assert appD1 != null && appD2 != null;

          if (appD1 < appD2){
            return 1;
          }

          if (appD1 > appD2){
            return -1;
          }

          int envCmp = RGEnvTransitionComparator.ENVAPP_MIN_DISTANCE_MIN_TOP_MAX.
              compare(pair1.getSecond(), pair2.getSecond());

          if (envCmp != 0){
            return envCmp;
          }
        }
      }*/

      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top1 > top2){
        return -1;
      }

      // compare distance
      Integer d1 = aelem1.getDistanceFromRoot();
      Integer d2 = aelem2.getDistanceFromRoot();
      assert d1 != null && d2 != null;

      if (d1 < d2){
        return 1;
      }

      if (d1 > d2){
        return -1;
      }

      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      if (id1 < id2){
        return 1;
      }

      if (id1 > id2){
        return -1;
      }

      assert false;
      return 0;
    }

  }



  /**
   * Minimize env. app, maximize topological id, min. distance from root,
   *
   */
  public static class LBETest implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {

      if (elem1.equals(elem2)){
        return 0;
      }


      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      Set<ARTElement> appbf1 = aelem1.getEnvAppliedPoints();
      Set<ARTElement> appbf2 = aelem2.getEnvAppliedPoints();

      int br1 = aelem1.getRefinementBranches();
      int br2 = aelem2.getRefinementBranches();

      if (br1 < br2){
        return 1;
      }

      if (br1 > br2){
        return -1;
      }

      if (br1 > 0){

        // choose element with application points with lower topological ids
        int totalTop1 = 0;

        for (ARTElement app1 : appbf1){
          totalTop1 += app1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
        }

        int totalTop2 = 0;

        for (ARTElement app2 : appbf2){
          totalTop2 += app2.retrieveLocationElement().getLocationNode().getTopologicalSortId();
        }

        if (totalTop1 < totalTop2){
          return 1;
        }

        if (totalTop1 > totalTop2){
          return -1;
        }

      }

      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 > top2){
        return 1;
      }

      if (top1 < top2){
        return -1;
      }

      // compare distance
     /* Integer d1 = aelem1.getDistanceFromRoot();
      Integer d2 = aelem2.getDistanceFromRoot();
      assert d1 != null && d2 != null;

      if (d1 < d2){
        return 1;
      }

      if (d1 > d2){
        return -1;
      }*/

      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      if (id1 > id2){
        return 1;
      }

      if (id1 < id2){
        return -1;
      }

      assert false;
      return 0;
    }

  }

  /**
   * Limit env. app, minimize topological id, min. distance from root,
   *
   */
  public static class EnvLimitMinTopMinDistMin implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {

      if (elem1.equals(elem2)){
        return 0;
      }


      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      int itpSize1 = aelem1.getInterpolationSize();
      int itpSize2 = aelem2.getInterpolationSize();
      int diff = itpSize1 - itpSize2;

      if (diff < 50){
        return 1;
      }

      if (diff > 50){
        return -1;
      }

      Set<Pair<ARTElement, RGEnvTransition>> appbf1 = aelem1.getEnvApplied();
      Set<Pair<ARTElement, RGEnvTransition>> appbf2 = aelem2.getEnvApplied();

      /* compare ref branches
      for (int i=0; i<Math.max(appbf1.size(), appbf2.size()); i++){

        if (appbf2.size() <= i){
          return 1;
        }

        if (appbf1.size() <= i){
          return -1;
        }

        Pair<ARTElement, RGEnvTransition> pair1 = appbf1.get(i);
        Pair<ARTElement, RGEnvTransition> pair2 = appbf2.get(i);

        ARTElement app1 = pair1.getFirst();
        ARTElement app2 = pair2.getFirst();

        int appTop1 = app1.retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int appTop2 = app2.retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (appTop1 > appTop2){
          return 1;
        }

        if (appTop1 < appTop2){
          return -1;
        }


        Integer appD1 = app1.getDistanceFromRoot();
        Integer appD2 = app2.getDistanceFromRoot();
        assert appD1 != null && appD2 != null;

        if (appD1 < appD2){
          return 1;
        }

        if (appD1 > appD2){
          return -1;
        }

        int envCmp = RGEnvTransitionComparator.ENVAPP_MIN_DISTANCE_MIN_TOP_MAX.
            compare(pair1.getSecond(), pair2.getSecond());

        if (envCmp != 0){
          return envCmp;
        }

      }*/


      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top1 > top2){
        return -1;
      }

      // compare distance
      Integer d1 = aelem1.getDistanceFromRoot();
      Integer d2 = aelem2.getDistanceFromRoot();
      assert d1 != null && d2 != null;

      if (d1 < d2){
        return 1;
      }

      if (d1 > d2){
        return -1;
      }

      assert false;
      return 0;
    }

  }





}
