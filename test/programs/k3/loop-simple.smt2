; This file is part of CPAchecker,
; a tool for configurable software verification:
; https://cpachecker.sosy-lab.org
;
; SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
;
; SPDX-License-Identifier: Apache-2.0

(set-logic LIA)

(declare-sort |c#ptr| 1)
(declare-sort |c#heap| 0)
(declare-var |c#heap| |c#heap|)
(declare-fun |c#bitxor| (Int Int) Int)
(declare-fun |c#bitand| (Int Int) Int)
(declare-fun |c#bitor| (Int Int) Int)
(define-proc
  main
  ()
  ((|c#result| Int))
  ((a Int))
  (sequence
    (assign (a 6))
    (assign (a 0))
    (! (while
      (< a 6)
      (assign (a (+ a 1)))) :tag while-loop)
    (if (not (= a 6)) (! (sequence) :assert false))
    (assign (|c#result| 1))
    (return)))

(verify-call main ())