(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (pacman player))
(assert (dot a4))
(assert (distance player a4 4))
(assert (dot a1))
(assert (distance player a1 2.5))
(assert (dot a2))
(assert (distance player a2 3))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

(deffunction betweenRange (?val ?low ?high)
    (if (and (>= ?val ?low) (<= ?val ?high)) then 
        return TRUE))

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
(defrule testtest
    (pacman ?X) (dot ?Y) (distance ?X ?Y ?YDist&:(betweenRange ?YDist 2 3))
    =>
    (assert (closeEnough ?X ?Y)))

(run)

(facts)

(reset)
(run)
(facts)