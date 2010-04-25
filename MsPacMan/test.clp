(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (pacman player))
(assert (dot a4))
(assert (distance player a4 4))
(assert (dot a1))
(assert (distance player a1 4))
(assert (dot a2))
(assert (distance player a2 4))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

;; rule definition
(defrule closestDot1
    (pacman ?X) (dot ?Y) (distance ?X ?Y ?YDist)
    (not (and (dot ?Z &:(<> ?Z ?Y)) (distance ?X ?Z ?ZDist &:(< ?ZDist ?YDist))))
    =>
    (assert (closest ?X ?Y)))
(defrule closestDot2
    (pacman ?X) (dot ?Y) (not (dot ?Z &:(<> ?Z ?Y)))
    =>
    (assert (closest ?X ?Y)))

(run)

(facts)

(reset)
(run)
(facts)