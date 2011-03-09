package rlPacManGeneral;

/**
 * A simple class combining weight and direction.
 * 
 * @author Sam Sarjant
 */
public class WeightedDirection {
	private byte direction_;
	private double weight_;

	public WeightedDirection(byte direction, double weight) {
		direction_ = direction;
		weight_ = weight;
	}

	/**
	 * @return the direction_
	 */
	public byte getDirection() {
		return direction_;
	}

	/**
	 * @return the weight_
	 */
	public double getWeight() {
		return weight_;
	}

	@Override
	public String toString() {
		String buffer = null;
		switch (direction_) {
		case Thing.UP:
			buffer = "UP";
			break;
		case Thing.DOWN:
			buffer = "DOWN";
			break;
		case Thing.LEFT:
			buffer = "LEFT";
			break;
		case Thing.RIGHT:
			buffer = "RIGHT";
			break;
		}
		return buffer + ": " + weight_;
	}
}