(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (block a))
(assert (block b))
(assert (block c))
(assert (above a b))
(assert (above a c))
(assert (above b c))
(assert (precedes a b))
(assert (precedes a c))
(assert (precedes b c))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

;; rule definition


(run)

(facts)

(reset)
(run)
(facts)