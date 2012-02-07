(reset)

;; Initial facts
(assert (block a))
(assert (block b))
(assert (block c))
(assert (block d))
(assert (floor floor))
(assert (clear floor))
(assert (on b d))
(assert (on d floor))
(assert (on a floor))
(assert (on c floor))

;; define the terminal fact
(deftemplate goal (slot goalMet))

(defrule clearRule
    (logical (block ?Y) (not (on ? ?Y)))
    =>
    (assert (clear ?Y)))

(defrule moveAction
    ?action <- (move ?X ?Y) (clear ?X) (clear ?Y) ?oldOn <- (on ?X ?Z)
    =>
    (assert (on ?X ?Y)) (retract ?oldOn))

(run)

(facts)

(assert (move a b))

(run)

(facts)