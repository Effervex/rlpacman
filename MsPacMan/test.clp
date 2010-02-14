(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (block a))
(assert (block b))
(assert (block c))
(assert (block d))
(assert (block e))
(assert (highest a))
(assert (clear a))
(assert (on a b))
(assert (on b c))
(assert (on c d))
(assert (onFloor d))
(assert (clear e))
(assert (onFloor e))
(assert (validActions (move "a e" "e a") (moveFloor "a")))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

;; rule definition
(defrule stacked
    (highest ?X) (not (exists (highest ?Y &:(neq ?Y ?X))))
    =>
    (assert (goalState (goalMet TRUE))))

(defrule inequal
    (block ?X) (block ?Y) (block ?Z) (block ?W)
    (test (<> ?X ?Y ?Z ?W)) (test (<> ?Y ?Z ?W)) (test (<> ?Z ?W))
    (on ?X ?Y) (on ?Z ?W)
    =>
    (printout t ?X " != " ?Y " != " ?Z " != " ?W crlf))

(run)

(facts)

(reset)
(run)
(facts)