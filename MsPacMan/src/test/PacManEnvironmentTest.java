package test;

import static org.junit.Assert.*;

import java.util.SortedSet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.junit.*;

import relationalFramework.StateSpec;
import rlPacMan.*;

/**
 * A testing class for the methods used by the PacMan environment.
 * 
 * @author Samuel J. Sarjant
 */
public class PacManEnvironmentTest {
	private PacManEnvironment sut_;

	@Before
	public void setUp() {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.OFF);
		StateSpec.initInstance("rlPacMan.PacMan");
		sut_ = new PacManEnvironment();
		sut_.env_init();
	}

	@Test
	public void testSearchMaze() {
		sut_.resetEnvironment();
		// Try every level
		for (int i = 0; i < 10; i++) {
			System.out.println("Level " + i);
			// Try every possible valid position
			int maxX = sut_.getModel().m_gameState.length;
			int maxY = sut_.getModel().m_gameState[0].length;
			Player player = sut_.getModel().getPlayer();
			int x = player.m_locX;
			int y = player.m_locY;

			SortedSet<Junction> points = sut_.searchMaze(player);
			// Always at least 2 junctions
			assertTrue(points.size() >= 2);

			// Distance grid
			int[][] distanceGrid = sut_.getDistanceGrid();
			assertEquals(distanceGrid[x][y], 0);
			for (int dy = 0; dy < distanceGrid[0].length; dy++) {
				for (int dx = 0; dx < distanceGrid.length; dx++) {
					if (distanceGrid[dx][dy] == Integer.MAX_VALUE) {
						System.out.print(".. ");
					} else {
						if (distanceGrid[dx][dy] >= 10)
							System.out.print(distanceGrid[dx][dy] + " ");
						else
							System.out.print(" " + distanceGrid[dx][dy] + " ");
						// There will be at least 2 locations
						// adjacent which are either +1 or -1
						// distance.
						int count = 0;
						int value = distanceGrid[(dx - 1 + maxX) % maxX][dy];
						if (value != Integer.MAX_VALUE) {
							// Always a difference of 1
							String message = "LEFT i:" + i + " x:" + x + " y:"
									+ y + " dx:" + dx + " dy:" + dy;
							assertEquals(message, Math.abs(value
									- distanceGrid[dx][dy]), 1);
							count++;
						}
						value = distanceGrid[(dx + 1 + maxX) % maxX][dy];
						if (value != Integer.MAX_VALUE) {
							// Always a difference of 1
							String message = "RIGHT i:" + i + " x:" + x + " y:"
									+ y + " dx:" + dx + " dy:" + dy;
							assertEquals(message, Math.abs(value
									- distanceGrid[dx][dy]), 1);
							count++;
						}
						value = distanceGrid[dx][(dy - 1 + maxY) % maxY];
						if (value != Integer.MAX_VALUE) {
							// Always a difference of 1
							String message = "UP i:" + i + " x:" + x + " y:"
									+ y + " dx:" + dx + " dy:" + dy;
							assertEquals(message, Math.abs(value
									- distanceGrid[dx][dy]), 1);
							count++;
						}
						value = distanceGrid[dx][(dy + 1 + maxY) % maxY];
						if (value != Integer.MAX_VALUE) {
							// Always a difference of 1
							String message = "DOWN i:" + i + " x:" + x + " y:"
									+ y + " dx:" + dx + " dy:" + dy;
							assertEquals(message, Math.abs(value
									- distanceGrid[dx][dy]), 1);
							count++;
						}

						assertTrue(count >= 2);
					}
				}
				System.out.println();
			}

			sut_.getModel().loadNextLevel();
			sut_.resetGridStart();
		}
	}
}
