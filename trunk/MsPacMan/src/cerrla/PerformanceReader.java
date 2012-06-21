package cerrla;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import rrlFramework.Config;

/**
 * Reads a performance file in and notes down the performance array, the last
 * readable generator and the last generator.
 * 
 * @author Sam Sarjant
 */
public class PerformanceReader {
	/** A recording of performance scores for each value. */
	private static SortedMap<Integer, Float[]> performanceMap_;

	/**
	 * Reads a raw numerical performance file and stores the values as
	 * accessible private values.
	 * 
	 * @param perfFile
	 *            The performance file to read.
	 * @return True if the file was read successfully, false otherwise.
	 */
	public static boolean readPerformanceFile(File perfFile, boolean byEpisode)
			throws Exception {
		performanceMap_ = new TreeMap<Integer, Float[]>();
		FileReader reader = new FileReader(perfFile);
		BufferedReader buf = new BufferedReader(reader);

		// For every value within the performance file
		String input = null;
		while ((input = buf.readLine()) != null) {
			String[] vals = input.split("\t");
			int episode = Integer.parseInt(vals[0]);
			float performance = Float.parseFloat(vals[1]);
			Float averageElite = null;
			Float bestElite = null;
			if (vals.length == 5 && !vals[3].equals("null")) {
				averageElite = Float.parseFloat(vals[3]);
				bestElite = Float.parseFloat(vals[4]);
			}
			Float[] perfs = { performance, averageElite, bestElite };
			performanceMap_.put(episode, perfs);
		}

		buf.close();
		reader.close();

		return true;
	}

	public static SortedMap<Integer, Float[]> getPerformanceArray() {
		return performanceMap_;
	}
}
