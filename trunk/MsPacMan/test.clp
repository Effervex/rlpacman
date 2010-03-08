(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (on a b))
(assert (block a))
(assert (onFloor b))
(assert (block b))
(assert (on c h))
(assert (block c))
(assert (on d c))
(assert (block d))
(assert (on e a))
(assert (block e))
(assert (onFloor f)) 
(assert (block f))
(assert (on g e))
(assert (block g))
(assert (onFloor h))
(assert (block h))
(assert (highest g))
(assert (clear g))
(assert (above g e))
(assert (clear f))
(assert (above e a))
(assert (above g a))
(assert (clear d))
(assert (above d c))
(assert (above c h))
(assert (above d h))
(assert (above a b))
(assert (above e b))
(assert (above g b))
(assert (validActions (moveFloor "g" "d") (move "g f" "g d" "f g" "f d" "d g" "d f")))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

;; rule definition
(defrule stacked
    (clear ?X) (not (exists (clear ?Y &:(<> ?Y ?X))))
    =>
    (assert (goalState (goalMet TRUE))))

(run)

(facts)

(reset)
(run)
(facts)