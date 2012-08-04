package jCloisterZoneAlt;

import java.util.Map;

import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;

import relationalFramework.RelationalPredicate;
import util.Pair;

public class CarcassonneRelationalWrapper extends
		jCloisterZone.CarcassonneRelationalWrapper {
	@Override
	protected Pair<Position, Rotation> extractPositionRotation(
			RelationalPredicate action, Map<String, Position> locationMap) {
		String[] args = action.getArguments();
		Rotation rot = null;
		Position pos = null;
		if (args.length == 4) {
			pos = locationMap.get(args[2]);
			rot = Rotation.valueOf(args[3]);
		} else if (args.length == 6) {
			pos = locationMap.get(args[4]);
			rot = Rotation.valueOf(args[5]);
		}

		return new Pair<Position, Rotation>(pos, rot);
	}
}
