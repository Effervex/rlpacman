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
 *    src/test/CERRLATest.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package test;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.junit.Before;
import org.junit.Test;

import relationalFramework.StateSpec;
import relationalFramework.agentObservations.LocalAgentObservations;
import rrlFramework.Config;
import rrlFramework.RRLObservations;
import util.MultiMap;

import cerrla.CERRLA;
import cerrla.modular.GoalCondition;

public class CERRLATest {
	private CERRLA sut_;

	@Before
	public void setUp() {
		sut_ = new CERRLA();
		StateSpec.initInstance("blocksWorldMove.BlocksWorld", "onab");

		LocalAgentObservations.loadAgentObservations(GoalCondition
				.parseGoalCondition(StateSpec.getInstance().getGoalName()),
				null);
		Config.newInstance(new String[] { "blocksMoveArguments.txt" });
		sut_.initialise(0);
	}

	@Test
	public void testStartEpisode() {
		MultiMap<String, String[]> validActions = MultiMap.createListMultiMap();
		RRLObservations obs = new RRLObservations(StateSpec.getInstance()
				.getRete(), validActions, new double[] { 0, 0 },
				new DualHashBidiMap(), 0);
		sut_.startEpisode(obs);
	}

}
