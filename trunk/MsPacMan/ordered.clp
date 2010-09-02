(deftemplate validActions (multislot move) (multislot moveFloor))

;; Initial facts
(assert (tower t0))
(assert (tower t1))
(assert (tower t2))
(assert (tile a))
(assert (tile b))
(assert (tile c))
(assert (tile d))
(assert (towerBase TAU2))
(assert (tile TAU2))
(assert (clear a t2))
(assert (on a b t2))
(assert (on b c t2))
(assert (on c d t2))
(assert (on d e t2))

;; define the terminal fact
(deftemplate goalState (slot goalMet))

;; rule definition
(defrule moveA
    (clear ?X ?Ta) (clear ?Y ?Tb&:(neq ?Tb ?Ta)) (smaller ?X ?Y)
    =>
    (assert (move ?X ?Tb)))

(defrule moveB
    (clear ?X ?Ta) (tower ?Tb&:(neq ?Tb ?Ta)) (not (onFloor ?Y ?Tb))
    =>
    (assert (move ?X ?Tb)))

(defrule smaller1
    (tile ?X) (not (towerBase ?X)) (tile ?Y&:(< ?X ?Y))
    =>
    (assert (smaller ?X ?Y)))

(defrule smaller2
    (tile ?X) (not (towerBase ?X)) (towerBase ?Y)
    =>
    (assert (smaller ?X ?Y)))

(defrule goalRule
    (tower t2) (forall (tile ?X) (or (towerBase ?X) (on ?X ? t2)))
    =>
    (assert (goalMet true)))

(run)

(facts)

(reset)
(run)
(facts)