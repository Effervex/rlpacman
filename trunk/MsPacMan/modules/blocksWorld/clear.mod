(declare (variables ?_MOD_a))
(above ?X ?_MOD_a) (clear ?X) => (moveFloor ?X)
(above ?X ?_MOD_a) (clear ?X) (clear ?Y) => (move ?X ?Y)