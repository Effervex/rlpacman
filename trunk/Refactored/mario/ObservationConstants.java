package mario;

public class ObservationConstants {
	// Level constants
	public static final int LVL_COIN = 2; // 1
	public static final int LVL_BREAKABLE_BRICK = -20;
	public static final int LVL_UNBREAKABLE_BRICK = -22; // a rock with animated
															// question mark
	public static final int LVL_BRICK = -24; // a rock with animated question
												// mark
	public static final int LVL_BORDER_CANNOT_PASS_THROUGH = -60;
	public static final int LVL_CANNON_MUZZLE = -82;
	public static final int LVL_CANNON_TRUNK = -80;
	public static final int LVL_FLOWER_POT = -90;
	public static final int LVL_BORDER_HILL = -62;
	public static final int LVL_FLOWER_POT_OR_CANNON = -85;

	// Enemy constants
	public static final int ENMY_GOOMBA = 80;
	public static final int ENMY_GOOMBA_WINGED = 95;
	public static final int ENMY_RED_KOOPA = 82;
	public static final int ENMY_RED_KOOPA_WINGED = 97;
	public static final int ENMY_GREEN_KOOPA = 81;
	public static final int ENMY_GREEN_KOOPA_WINGED = 96;
	public static final int ENMY_BULLET_BILL = 84;
	public static final int ENMY_SPIKY = 93;
	public static final int ENMY_SPIKY_WINGED = 99;
	public static final int ENMY_ENEMY_FLOWER = 91;
	public static final int ENMY_SHELL = 13;
	public static final int ENMY_MUSHROOM = 2;
	public static final int ENMY_FIRE_FLOWER = 3;
	public static final int ENMY_FIREBALL = 25;
	public static final int ENMY_GENERAL_ENEMY = 1;

	/**
	 * Checks if a particular cell value is empty (or containing something that
	 * can be passed through.
	 * 
	 * @param cellValue The value of the cell.
	 * @return True if the cell is empty or safely passable.
	 */
	public static boolean isEmptyCell(int cellValue) {
		switch (cellValue) {
		case 0:
		case LVL_COIN:
			return true;
		}
		return false;
		
	}
}
