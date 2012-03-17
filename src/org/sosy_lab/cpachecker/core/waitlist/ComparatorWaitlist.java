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
import java.util.List;
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

    MIN_ENVAPP_MIN_TOP {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new EnvAppMinTopMin());
      }
    },

    MIN_ENVAPP_MIN_TOP2 {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new EnvAppMinTopMin2());
      }
    },

    MIX {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new Mix());
      }
    },

    MIX2 {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new Mix2());
      }
    },

    MIX3 {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new Mix3());
      }
    },

    MIN_ENVAPP_MAX_TOP {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new EnvAppMinTopMax());
      }
    }


  }

  /**
   * Minimize env. applications and then minimizes topological numbers.
   */
  public static class EnvAppMinTopMin implements Comparator<AbstractElement>{

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
  }


  /**
   * Minimize env. applications and then minimizes topological numbers.
   */
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
  }


  /**
   * Minimize env. applications and then minimizes topological numbers.
   */
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
  }


  /**
   * Minimize env. applications and then minimizes topological numbers.
   */
  public static class Mix2 implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (aelem1.equals(aelem2)){
        return 0;
      }

      int branches1 = aelem1.getRefinementBranches();
      int branches2 = aelem2.getRefinementBranches();

      // compare the number of env. applications
      if (branches1 < branches2){
        return 1;
      }

      if (branches1 > branches2){
        return -1;
      }

      // compare topological numbers, children before parents
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top1 > top2){
        return -1;
      }

      // compare first application point
      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      if (eapp1.size() > 0){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(0);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(0);

        // compare application points - parents before children
        int t1top = app1.getFirst().retrieveLocationElement().getLocationNode().getTopologicalSortId();
        int t2top = app2.getFirst().retrieveLocationElement().getLocationNode().getTopologicalSortId();

        if (t1top > t1top){
          return 1;
        }

        if (t1top < t1top){
          return -1;
        }

        // compare generating points - parents before children
        int s1top = app1.getSecond().getSourceARTElement().retrieveLocationElement().
            getLocationNode().getTopologicalSortId();

        int s2top = app2.getSecond().getSourceARTElement().retrieveLocationElement().
            getLocationNode().getTopologicalSortId();

        if (s1top > s2top){
          return 1;
        }

        if (s1top < s2top){
          return -1;
        }


      }
      // compare ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : -1;
    }

    private int countBranches(ARTElement elem){
      int size = 0;

      List<Pair<ARTElement, RGEnvTransition>> envApplied = elem.getEnvApplied();

      for (Pair<ARTElement, RGEnvTransition> pair : envApplied){
        size += countBranches(pair.getFirst()) + 1;
      }

      return size;
    }
  }



  public static class Mix3 implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (aelem1.equals(aelem2)){
        return 0;
      }

      int branches1 = aelem1.getRefinementBranches();
      int branches2 = aelem2.getRefinementBranches();

      // compare the number of env. applications
      if (branches1 < branches2){
        return 1;
      }

      if (branches1 > branches2){
        return -1;
      }

      // compare first application point
      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      if (eapp1.size() > 0){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(0);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(0);

        int appId1 = app1.getFirst().getElementId();
        int appId2 = app2.getFirst().getElementId();

        if (appId1 > appId2){
          return 1;
        }

        if (appId1 < appId2){
          return -1;
        }

        // compare application points - parents before children
        /*int t1top = app1.getFirst().retrieveLocationElement().getLocationNode().getTopologicalSortId();
        int t2top = app2.getFirst().retrieveLocationElement().getLocationNode().getTopologicalSortId();

        if (t1top > t1top){
          return 1;
        }

        if (t1top < t1top){
          return -1;
        }*/

        // compare generating points - parents before children
        /*int s1top = app1.getSecond().getSourceARTElement().retrieveLocationElement().
            getLocationNode().getTopologicalSortId();

        int s2top = app2.getSecond().getSourceARTElement().retrieveLocationElement().
            getLocationNode().getTopologicalSortId();

        if (s1top > s2top){
          return 1;
        }

        if (s1top < s2top){
          return -1;
        }*/


        int srcId1 = app1.getSecond().getSourceARTElement().getElementId();
        int srcId2 = app2.getSecond().getSourceARTElement().getElementId();

        if (srcId1 > srcId2){
          return 1;
        }

        if (srcId1 < srcId2){
          return -1;
        }


      }
      // compare ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : -1;
    }
  }



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




  /**
   * Minimize env. applications and maximizes then topological numbers.
   */
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
  }





}
