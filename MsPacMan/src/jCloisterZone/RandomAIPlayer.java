/*
 *    This file is part of the CERRLA algorithm
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/jCloisterZone/RandomAIPlayer.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package jCloisterZone;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.action.CaptureAction;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;

/**
 * The ultimate in AI technology has produced this AI of incomprehensible
 * knowledge and power. None can read it's complex strategies. None expect its
 * next move. It is, in a word, chaotic.
 * 
 * @author Sam Sarjant
 */
public class RandomAIPlayer extends AiPlayer {
	private Random random_ = new Random();
	
	public static EnumSet<Expansion> supportedExpansions() {
		return EnumSet.of(
			Expansion.BASIC,
			Expansion.INNS_AND_CATHEDRALS,
			Expansion.TRADERS_AND_BUILDERS,
			Expansion.PRINCESS_AND_DRAGON,
			Expansion.KING_AND_SCOUT,
			Expansion.RIVER,
			Expansion.RIVER_II,
			Expansion.GQ11,
			Expansion.CATAPULT,
			//only cards
			Expansion.CROP_CIRCLES,
			Expansion.PLAGUE
		);
	}

	@Override
	public void selectAbbeyPlacement(Set<Position> positions) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void selectTilePlacement(Map<Position, Set<Rotation>> placements) {
		int selected = random_.nextInt(placements.size());
		for (Position pos : placements.keySet()) {
			if (selected == 0) {
				Set<Rotation> rots = placements.get(pos);
				selected = random_.nextInt(rots.size());
				for (Rotation rot : rots) {
					if (selected == 0) {
						getServer().placeTile(rot, pos);
						return;
					} else
						selected--;
				}
			} else
				selected--;
		}
	}

	@Override
	public void selectAction(List<PlayerAction> actions) {
		if (random_.nextBoolean()) {
			getServer().placeNoFigure();
			return;
		}
		int selected = random_.nextInt(actions.size());
		MeepleAction ma = (MeepleAction) actions.get(selected);
		Tile currTile = getGame().getTilePack().getCurrentTile();
		Position pos = currTile.getPosition();

		Set<Location> locations = ma.getSites().get(pos);
		selected = random_.nextInt(locations.size());
		for (Location loc : locations) {
			if (selected == 0) {
				ma.perform(getServer(), pos, loc);
				return;
			} else
				selected--;
		}
	}

	@Override
	public void selectTowerCapture(CaptureAction action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void selectDragonMove(Set<Position> positions, int movesLeft) {
		throw new UnsupportedOperationException();
	}

}
