/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ErrorSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.FinalSegment;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.UnreachableSegmentation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

public class SegmentationUnifier<T extends ExtendedCompletLatticeAbstractState<T>> {
  private BiPredicate<Boolean, Boolean> curleyVee = new BiPredicate<Boolean, Boolean>() {
    @Override
    public boolean test(Boolean pArg0, Boolean pArg1) {
      return pArg0 || pArg1;
    }
  };

  BiPredicate<Boolean, Boolean> curleyWedge = new BiPredicate<Boolean, Boolean>() {
    @Override
    public boolean test(Boolean pArg0, Boolean pArg1) {
      return pArg0 && pArg1;
    }
  };

  BinaryOperator<T> sqcup = new BinaryOperator<T>() {

    @Override
    public T apply(T pArg0, T pArg1) {
      try {
        return pArg0.join(pArg1);
      } catch (CPAException | InterruptedException e) {
        // TODO: Extend error handling!
        throw new IllegalArgumentException(e);
      }
    }
  };

  public Pair<ArraySegmentationState<T>, ArraySegmentationState<T>> unifyMerge(
      ArraySegmentationState<T> d1,
      ArraySegmentationState<T> d2,
      T il,
      T ir,
      boolean pIsLoopHead)
      throws CPAException {
    Pair<ArraySegmentationState<T>, ArraySegmentationState<T>> res =
        this.unifyGeneric(d1, d2, il, ir, sqcup, sqcup, curleyVee, curleyVee, !pIsLoopHead);
    return res;
  }

  public Pair<ArraySegmentationState<T>, ArraySegmentationState<T>> unifyCompare(
      ArraySegmentationState<T> d1,
      ArraySegmentationState<T> d2,
      T il,
      T ir,
      BinaryOperator<T> sqcap)
      throws CPAException {

    // _|_, _|_, sqcup, sqcap, v , ^
    return this.unifyGeneric(d1, d2, il, ir, sqcup, sqcap, curleyVee, curleyWedge, false);
  }

  public Pair<ArraySegmentationState<T>, ArraySegmentationState<T>> unifyGeneric(
      ArraySegmentationState<T> pD1,
      ArraySegmentationState<T> pD2,
      T il,
      T ir,
      BinaryOperator<T> ol,
      BinaryOperator<T> or,
      BiPredicate<Boolean, Boolean> hatl,
      BiPredicate<Boolean, Boolean> hatr,
      boolean isMergeAndNoLoopHead)
      throws CPAException {

    Language language = pD1.getLanguage();
    ArraySegmentationState<T> d1 = pD1.getDeepCopy();
    ArraySegmentationState<T> d2 = pD2.getDeepCopy();

    // Case 1:
    if (d1 instanceof ErrorSegmentation
        || d1 instanceof UnreachableSegmentation
        || d2 instanceof ErrorSegmentation
        || d2 instanceof UnreachableSegmentation) {
      return Pair.of(d1, d2);
    }

    if (d1.getSegments().isEmpty() || d2.getSegments().isEmpty()) {
      throw new IllegalArgumentException("Cannot unify empty segments");
    }

    // Setup some vars and pointer needed:
    List<ArraySegment<T>> segs1 = new ArrayList<>(d1.getSegments());
    List<ArraySegment<T>> segs2 = new ArrayList<>(d2.getSegments());
    ArraySegment<T> b1 = segs1.get(0);
    ArraySegment<T> b2 = segs2.get(0);

    // Create resultLists, the concatenation will be done in the end
    List<ArraySegment<T>> res1 = new ArrayList<>();
    List<ArraySegment<T>> res2 = new ArrayList<>();

    // The algorithm terminates, if the left and the right segment bound are reached with the
    // pointer

    while (!(b1 instanceof FinalSegment) && !(b2 instanceof FinalSegment)) {
      // Case 2: Both segment bounds are equal
      if (b1.getSegmentBound().containsAll(b2.getSegmentBound())
          && b2.getSegmentBound().containsAll(b1.getSegmentBound())) {
        res1.add(b1);
        res2.add(b2);
        b1 = b1.getNextSegment();
        b2 = b2.getNextSegment();
        continue;
      }
      // Needed in all other cases:
      List<AExpression> b1SegBounds = new ArrayList<>(b1.getSegmentBound());
      List<AExpression> b2SegBounds = new ArrayList<>(b2.getSegmentBound());
      List<AExpression> b1Bar =
          d2.getSegmentBounds(b2.getNextSegment())
              .parallelStream()
              .filter(b -> b1SegBounds.contains(b))
              .collect(Collectors.toList());
      List<AExpression> b2Bar =
          d1.getSegmentBounds(b1.getNextSegment())
              .parallelStream()
              .filter(b -> b2SegBounds.contains(b))
              .collect(Collectors.toList());
      // Compute the expressions, that are present in both segment bounds
      List<AExpression> subsetOfB1_B2 = new ArrayList<>();
      b1SegBounds.parallelStream()
          .filter(e -> b2SegBounds.contains(e))
          .forEach(e -> subsetOfB1_B2.add(e));

      // Case 3:
      if (b1SegBounds.containsAll(b2SegBounds)) {
        // Case 3.1
        // replace the segment bounds from b1 with them of B2, since the SB of B1 are a superset of
        // B2
        if (b1Bar.isEmpty()) {
          b1.setSegmentBound(b2SegBounds);
          continue;
        }
        // Case 3.2
        // To avoid confuse, crate two new elements, where the first is temp1 = B1\B1Bar I_l ?
        // and the second temp2 = B1Bar p1 ?1
        ArraySegment<T> temp2 =
            new ArraySegment<>(
                b1Bar,
                b1.getAnalysisInformation(),
                b1.isPotentiallyEmpty(),
                b1.getNextSegment(),
                language);
        ArraySegment<T> temp1 = new ArraySegment<>(subsetOfB1_B2, il, true, temp2, language);
        b1 = temp1;
        continue;
      }

      // Case 4:
      if (b2.getSegmentBound().containsAll(b1.getSegmentBound())) {
        // Case 4.1
        // replace the segment bounds from b1 with them of B2, since the SB of B1 are a superset of
        // B2
        if (b2Bar.isEmpty()) {
          b2.setSegmentBound(b1SegBounds);
          continue;
        }
        // Case 4.2
        // To avoid confuse, crate two new elements, where the first is temp1 = B2\B2Bar I_r ?
        // and the second temp2 = B2Bar p2 ?2
        ArraySegment<T> temp2 =
            new ArraySegment<>(
                b2Bar,
                b2.getAnalysisInformation(),
                b2.isPotentiallyEmpty(),
                b2.getNextSegment(),
                language);
        ArraySegment<T> temp1 = new ArraySegment<>(subsetOfB1_B2, ir, true, temp2, language);
        b2 = temp1;
        continue;
      }

      // Case 5:
      // Firstly, check if there is an expression in B1 present in B2 and vice versa

      if (b1SegBounds.parallelStream().anyMatch(b -> b2SegBounds.contains(b))
          && b2SegBounds.parallelStream().anyMatch(b -> b1SegBounds.contains(b))) {

        // Case 5.1 B1Bar = B2Bar = emptyset
        if (b1Bar.isEmpty() && b2Bar.isEmpty()) {
          // Reassign b1 and b2
          b1.setSegmentBound(subsetOfB1_B2);
          b2.setSegmentBound(subsetOfB1_B2);
          continue;
        } else if (b1Bar.isEmpty()) {
          // Case 5.2
          b1.setSegmentBound(subsetOfB1_B2);
          // To avoid confuse, crate two new elements, where the first is temp1 = B2\B2Bar I_r ?
          // and the second temp2 = B2Bar p2 ?2
          ArraySegment<T> temp2 =
              new ArraySegment<>(
                  b2Bar,
                  b2.getAnalysisInformation(),
                  b2.isPotentiallyEmpty(),
                  b2.getNextSegment(),
                  language);
          ArraySegment<T> temp1 = new ArraySegment<>(subsetOfB1_B2, ir, true, temp2, language);
          b2 = temp1;
          continue;
        } else if (b2Bar.isEmpty()) {
          // Case 5.3
          // To avoid confuse, crate two new elements, where the first is temp1 = B1\B1Bar I_l ?
          // and the second temp2 = B1Bar p1 ?1
          ArraySegment<T> temp2 =
              new ArraySegment<>(
                  b1Bar,
                  b1.getAnalysisInformation(),
                  b1.isPotentiallyEmpty(),
                  b1.getNextSegment(),
                  language);
          ArraySegment<T> temp1 = new ArraySegment<>(subsetOfB1_B2, il, true, temp2, language);
          b1 = temp1;
          b2.setSegmentBound(subsetOfB1_B2);
          continue;
        } else {
          // Firstly, remove b1Bar from B1 for the second argument named b1Temp, than use this
          // element and create a new one pointing to this
          ArraySegment<T> b1Temp = b1.removeExprFromBound(b1Bar);
          // Create B1Bar Il ? B1\B1Bar
          b1 = new ArraySegment<>(b1Bar, il, true, b1Temp, language);
          // Firstly, remove b2Bar from B2 for the second argument named b2Temp, than use this
          // element
          // and create a new one pointing to this
          ArraySegment<T> b2Temp = b2.removeExprFromBound(b2Bar);
          // Create B2Bar Ir ? B2\B2Bar
          b2 = new ArraySegment<>(b2Bar, ir, true, b2Temp, language);
          continue;
        }
      }

      // Load the last unified element (needed for cases 6-8):
      if (res1.isEmpty() || res2.isEmpty()) {
        throw new CPAException(
            "The unififcation failed for the elements "
                + pD1.toDOTLabel()
                + " and "
                + pD2.toDOTLabel());
      }
      ArraySegment<T> b0 = res1.get(res1.size() - 1);
      ArraySegment<T> b0Prime = res2.get(res2.size() - 1);
      // Since they will be re-added later on, remove tb0 and b0' from res1 and res2
      res1.remove(b0);
      res2.remove(b0Prime);

      // Case 6: Ensure that there is no intersection of B1 and B2
      if (!(b1.getSegmentBound().parallelStream().anyMatch(b -> b2SegBounds.contains(b))
          || b2.getSegmentBound().parallelStream().anyMatch(b -> b1SegBounds.contains(b)))) {
        List<AExpression> b1Hat = computeHat(b1, pD1);
        List<AExpression> b2Hat = computeHat(b2, pD2);
        if (isMergeAndNoLoopHead) {
          if (!b1Bar.isEmpty() && b2Bar.isEmpty() && b2.getSegmentBound().size() == 1) {
            // Case 6.2.1
            // add B2 to d1 and continue with the newly added segments
            ArraySegment<T> copyOfB2 =
                new ArraySegment<>(
                    new ArrayList<>(b2.getSegmentBound()),
                    il,
                    true,
                    b1,
                    b1.getLanguage());
            d1.addSegment(copyOfB2, b0);
            b1 = b0;
            b2 = b0Prime;
            continue;
          } else if (b1Bar.isEmpty() && !b2Bar.isEmpty() && b2.getSegmentBound().size() == 1) {
            // Case 6.2.2
            // add B1 to d2 and continue with the newly added segments
            ArraySegment<T> copyOfB1 =
                new ArraySegment<>(
                    new ArrayList<>(b1.getSegmentBound()),
                    ir,
                    true,
                    b2,
                    b2.getLanguage());
            d2.addSegment(copyOfB1, b0Prime);
            b1 = b0;
            b2 = b0Prime;
            continue;
          }
        }

        if (!b1Bar.isEmpty() && b2Bar.isEmpty()) {
          if (!b2Hat.isEmpty()) {

            // Case 6.1.1
            // Continue with B0 P0 ?0 b2Hat P0?0 B1Bar p1 ?1 B1'
            ArraySegment<T> secondNextSegment =
                new ArraySegment<>(
                    b1Bar,
                    b1.getAnalysisInformation(),
                    b1.isPotentiallyEmpty(),
                    b1.getNextSegment(),
                    language);
            b0.setNextSegment(
                new ArraySegment<>(
                    b2Hat,
                    b0.getAnalysisInformation(),
                    b0.isPotentiallyEmpty(),
                    secondNextSegment,
                    language));
            b1 = b0;

            // Continue with B0' P0' ?_0' b2Hat P0' ?_0' B_2'
            b0Prime.setNextSegment(
                new ArraySegment<>(
                    b2Hat,
                    b2.getAnalysisInformation(),
                    b2.isPotentiallyEmpty(),
                    b2.getNextSegment(),
                    language));
            b2 = b0Prime;
            continue;
          } else {

            // Case 6.1.2
            b0.setNextSegment(
                new ArraySegment<>(
                    b1Bar,
                    b1.getAnalysisInformation(),
                    b1.isPotentiallyEmpty(),
                    b1.getNextSegment(),
                    language));
            b1 = b0;
            // Merge the analysis information from B2 into B0' and remove the segment B2
            b0Prime.setAnalysisInformation(
                or.apply(b0Prime.getAnalysisInformation(), b2.getAnalysisInformation()));
            b0Prime.setPotentiallyEmpty(
                hatr.test(b0Prime.isPotentiallyEmpty(), b2.isPotentiallyEmpty()));
            b0Prime.setNextSegment(b2.getNextSegment());
            b2 = b0Prime;
            continue;
          }
        } else if (b1Bar.isEmpty() && !b2Bar.isEmpty()) {

          if (!b1Hat.isEmpty()) {
            // Case 6.1.3
            // Continue with B0 P0 ?0 B1Bar p1 ?1 B1'
            b0.setNextSegment(
                new ArraySegment<>(
                    b1Hat,
                    b1.getAnalysisInformation(),
                    b1.isPotentiallyEmpty(),
                    b1.getNextSegment(),
                    language));
            b1 = b0;
            ArraySegment<T> secondNextSegment =
                new ArraySegment<>(
                    b2Bar,
                    b2.getAnalysisInformation(),
                    b2.isPotentiallyEmpty(),
                    b2.getNextSegment(),
                    language);
            // Continue with B0' P0' ?0' B1Hat P0' ?0 B2Bar p2 ?2 B2'
            b0Prime.setNextSegment(
                new ArraySegment<>(
                    b1Hat,
                    b0Prime.getAnalysisInformation(),
                    b0Prime.isPotentiallyEmpty(),
                    secondNextSegment,
                    language));
            b2 = b0Prime;
            continue;

          } else {
            // Case 6.1.4
            // Merge the analysis information from B1 into B0 and remove the segment B1
            b0.setAnalysisInformation(
                ol.apply(b0.getAnalysisInformation(), b1.getAnalysisInformation()));
            b0.setPotentiallyEmpty(hatl.test(b0.isPotentiallyEmpty(), b1.isPotentiallyEmpty()));
            b0.setNextSegment(b1.getNextSegment());
            b1 = b0;
            b0Prime.setNextSegment(
                new ArraySegment<>(
                    b2Bar,
                    b2.getAnalysisInformation(),
                    b2.isPotentiallyEmpty(),
                    b2.getNextSegment(),
                    language));
            b2 = b0Prime;
            continue;

          }
        } else {

          if (!b1Bar.isEmpty() && !b2Bar.isEmpty()) {
            // Case 6.3:
            if (!b1Hat.isEmpty() && b2Hat.isEmpty()) {
              b0.setNextSegment(
                  new ArraySegment<>(
                      b1Bar,
                      b1.getAnalysisInformation(),
                      b1.isPotentiallyEmpty(),
                      b1.getNextSegment(),
                      language));
              b1 = b0;
              // Merge the analysis information from B2 into B0' and remove the segment B2
              b0Prime.setAnalysisInformation(
                  or.apply(b0Prime.getAnalysisInformation(), b2.getAnalysisInformation()));
              b0Prime.setPotentiallyEmpty(
                  hatr.test(b0Prime.isPotentiallyEmpty(), b2.isPotentiallyEmpty()));
              b0Prime.setNextSegment(b2.getNextSegment());
              b2 = b0Prime;
              continue;
            } else if (b1Hat.isEmpty() && !b2Hat.isEmpty()) {
              // Case 6.4
              // Merge the analysis information from B1 into B0 and remove the segment B1
              b0.setAnalysisInformation(
                  ol.apply(b0.getAnalysisInformation(), b1.getAnalysisInformation()));
              b0.setPotentiallyEmpty(hatl.test(b0.isPotentiallyEmpty(), b1.isPotentiallyEmpty()));
              b0.setNextSegment(b1.getNextSegment());
              b1 = b0;
              b0Prime.setNextSegment(
                  new ArraySegment<>(
                      b2Bar,
                      b2.getAnalysisInformation(),
                      b2.isPotentiallyEmpty(),
                      b2.getNextSegment(),
                      language));
              b2 = b0Prime;
              continue;
            }
          } else if (b1Bar.isEmpty() && b2Bar.isEmpty()) {

            // Case 6.6
            if (!b1Hat.isEmpty() && b2Hat.isEmpty()) {
              b0.setNextSegment(
                  new ArraySegment<>(
                      b1Hat,
                      b1.getAnalysisInformation(),
                      b1.isPotentiallyEmpty(),
                      b1.getNextSegment(),
                      language));
              b1 = b0;
              // Merge the analysis information from B2 into B0'
              T p0p2 = or.apply(b0Prime.getAnalysisInformation(), b2.getAnalysisInformation());
              boolean qm0qm2 = hatr.test(b0Prime.isPotentiallyEmpty(), b2.isPotentiallyEmpty());
              // Set the analysis information for B0' and B2
              b0Prime.setAnalysisInformation(p0p2);
              b0Prime.setPotentiallyEmpty(qm0qm2);

              b2.setAnalysisInformation(p0p2);
              b2.setPotentiallyEmpty(qm0qm2);
              // Replace the segment bounds from B2 by B1Hat
              b2.setSegmentBound(b1Hat);
              b2 = b0Prime;
              continue;

            } else if (b1Hat.isEmpty() && !b2Hat.isEmpty()) {
              // Case 6.7
              b0Prime.setNextSegment(
                  new ArraySegment<>(
                      b2Hat,
                      b2.getAnalysisInformation(),
                      b2.isPotentiallyEmpty(),
                      b2.getNextSegment(),
                      language));
              b2 = b0Prime;

              // Merge the analysis information from B1 into B0
              T p0p1 = ol.apply(b0.getAnalysisInformation(), b1.getAnalysisInformation());
              boolean qm0qm1 = hatl.test(b0.isPotentiallyEmpty(), b1.isPotentiallyEmpty());
              // Set the analysis information for B0 and B1
              b0.setAnalysisInformation(p0p1);
              b0.setPotentiallyEmpty(qm0qm1);

              b1.setAnalysisInformation(p0p1);
              b1.setPotentiallyEmpty(qm0qm1);
              // Replace the segment bounds from B1 by B2Hat
              b1.setSegmentBound(b2Hat);
              b1 = b0;
              continue;
            } else {
              // Case 6.5 and 6.8
              // Merge the analysis information from B1 into B0 and remove the segment B1
              b0.setAnalysisInformation(
                  ol.apply(b0.getAnalysisInformation(), b1.getAnalysisInformation()));
              b0.setPotentiallyEmpty(hatl.test(b0.isPotentiallyEmpty(), b1.isPotentiallyEmpty()));
              b0.setNextSegment(b1.getNextSegment());
              b1 = b0;
              // Merge the analysis information from B2 into B0' and remove the segment B2
              b0Prime.setAnalysisInformation(
                  or.apply(b0Prime.getAnalysisInformation(), b2.getAnalysisInformation()));
              b0Prime.setPotentiallyEmpty(
                  hatr.test(b0Prime.isPotentiallyEmpty(), b2.isPotentiallyEmpty()));
              b0Prime.setNextSegment(b2.getNextSegment());
              b2 = b0Prime;
              continue;
            }
          }
        }
      }

      // Case 7: Right limit reached
      if (!(b1.getNextSegment() instanceof FinalSegment)
          && b2.getNextSegment() instanceof FinalSegment) {
        // Merge the analysis information from B1 into B0 and remove the segment B1
        b0.setAnalysisInformation(
            ol.apply(b0.getAnalysisInformation(), b1.getAnalysisInformation()));
        b0.setPotentiallyEmpty(hatl.test(b0.isPotentiallyEmpty(), b1.isPotentiallyEmpty()));
        b0.setNextSegment(b1.getNextSegment());
        b1 = b0;
        continue;

      }
      // Case 8: Left limit reached
      if (b1.getNextSegment() instanceof FinalSegment
          && !(b2.getNextSegment() instanceof FinalSegment)) {
        // Merge the analysis information from B2 into B0' and remove the segment B2
        b0Prime.setAnalysisInformation(
            or.apply(b0Prime.getAnalysisInformation(), b2.getAnalysisInformation()));
        b0Prime
            .setPotentiallyEmpty(hatr.test(b0Prime.isPotentiallyEmpty(), b2.isPotentiallyEmpty()));
        b0Prime.setNextSegment(b2.getNextSegment());
        b2 = b0Prime;
        continue;

      }
      // Case 9:
      if (b1.getNextSegment() instanceof FinalSegment
          && b2.getNextSegment() instanceof FinalSegment) {
        // Termination, hence break loop (should happen anyway
        break;
      }

    }
    d1.setSegments(conc(res1, d1.gettEmptyElement()));
    d2.setSegments(conc(res2, d2.gettEmptyElement()));
    return Pair.of(d1, d2);
    // new ArraySegmentationState<>(
    // conc(res1, d1.gettEmptyElement()),
    // d1.gettBottom(),
    // d1.gettTop(),
    // d1.gettEmptyElement(),
    // d1.gettMeet(),
    // d1.gettLisOfArrayVariables(),
    // d1.gettArray(),
    // d1.getLogger()),
    // new ArraySegmentationState<>(
    // conc(res2, d2.gettEmptyElement()),
    // d2.gettBottom(),
    // d2.gettTop(),
    // d2.gettEmptyElement(),
    // d2.gettMeet(),
    // d1.gettLisOfArrayVariables(),
    // d1.gettArray(),
    // d2.getLogger()));

  }

  private List<AExpression> computeHat(ArraySegment<T> pB, ArraySegmentationState<T> pD) {
    List<AExpression> res = new ArrayList<>();
    for (AExpression e : pB.getSegmentBound()) {
      if (e instanceof AIntegerLiteralExpression) {
        res.add(e);
      }
      if (contains(e, pD.getSizeVar())) {
        res.add(e);
      }
    }

    return res;
  }

  private boolean contains(AExpression pToSearchIn, AExpression pToSeachFor) {
    if (pToSearchIn instanceof ABinaryExpression) {
      return (contains(((ABinaryExpression) pToSearchIn).getOperand1(), pToSeachFor)
          && onlyContainsIntegers(((ABinaryExpression) pToSearchIn).getOperand2()))
          || (contains(((ABinaryExpression) pToSearchIn).getOperand2(), pToSeachFor)
              && onlyContainsIntegers(((ABinaryExpression) pToSearchIn).getOperand1()));
    }
    return pToSearchIn.equals(pToSeachFor);
  }

  private boolean onlyContainsIntegers(AExpression pEx) {
    if (pEx instanceof ABinaryExpression) {
      return onlyContainsIntegers(((ABinaryExpression) pEx).getOperand1())
          && onlyContainsIntegers(((ABinaryExpression) pEx).getOperand2());
    }
    return pEx instanceof CIntegerLiteralExpression;
  }

  public List<ArraySegment<T>> conc(List<ArraySegment<T>> pCopiedElements, T pEmptyElement) {
    if (pCopiedElements.isEmpty()) {
      throw new IllegalArgumentException("Cannot concatinate an empty list of elements");
    }
    for (int i = 0; i < pCopiedElements.size() - 1; i++) {
      pCopiedElements.get(i).setNextSegment(pCopiedElements.get(i + 1));
    }
    pCopiedElements.get(pCopiedElements.size() - 1)
        .setNextSegment(new FinalSegment<>(pEmptyElement));
    return pCopiedElements;
  }

}
