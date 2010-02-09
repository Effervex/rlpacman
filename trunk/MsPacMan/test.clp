;; Initial facts
(assert (highest a))
(assert (clear a))
(assert (on a b))
(assert (on b c))
(assert (on c d))
(assert (onFloor d))

;; rule definition
(defrule stacked
    ?c <- (accumulate (bind ?count 0)
        			  (bind ?count (+ ?count 1)
            		  ?count
            		  (highest ?)))
    (test (= ?c 1))
    =>
    (assert (terminal TRUE)))