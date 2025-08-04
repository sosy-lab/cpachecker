(declare-var x Int)
(declare-var y Int)
(define-proc f1 ((x Int) (y Int)) () () (sequence (assume (= x y)) (! (assume true) :assert (= x y))))
(verify-call f1 (x y))