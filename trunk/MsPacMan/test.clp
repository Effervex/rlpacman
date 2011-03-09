(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (block a))
(assert (block b))
(assert (block c))
(assert (block d))
(assert (floor fl))
(assert (on a fl))
(assert (on b a))
(assert (on c fl))
(assert (on d b))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

(defrule unstack
    "Unstacked state"
    (floor ?Y) (forall (block ?X) (on ?X ?Y)) => (assert (goalMet TRUE)))

(defrule exists
    "Does X exist?"
    (onFloor ?) => (assert (floorexists TRUE)))

(run)

(facts)

(reset)
(run)
(facts)
