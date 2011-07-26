(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (block a))
(assert (block b))
(assert (block c))
(assert (block d))
(assert (onFloor a))
(assert (on b a))
(assert (onFloor c))
(assert (on d b))
(assert (goalArgs onAB b a))

;; define the terminal fact
(deftemplate goal (slot goalMet))

(defrule onAB
    (goalArgs onAB ?G_0 ?G_1) (on ?G_0 ?G_1)
	=> (assert (goal (goalMet TRUE))))


(run)

(facts)

(reset)
(run)
(facts)
