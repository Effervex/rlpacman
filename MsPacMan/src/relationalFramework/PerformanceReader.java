package relationalFramework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Reads a performance file in and notes down the performance array, the
 * last readable generator and the last generator.
 * 
 * @author Sam Sarjant
 */
public class PerformanceReader {
	private static Float[] performanceArray_;
	private static float performanceSum_;
	private static String readableGenerator_;
	private static String generator_;

	/**
	 * Reads a performance file and stores the values as accessible private
	 * values.
	 * 
	 * @param perfFile
	 *            The performance file to read.
	 * @return True if the file was read successfully, false otherwise.
	 */
	public static boolean readPerformanceFile(File perfFile)
			throws Exception {
		performanceSum_ = 0;
		performanceArray_ = null;
		readableGenerator_ = null;
		generator_ = null;
		FileReader reader = new FileReader(perfFile);
		BufferedReader buf = new BufferedReader(reader);

		// For every value within the performance file
		ArrayList<Float> performances = new ArrayList<Float>();
		float val = 0;
		boolean noNote = false;
		String input = buf.readLine();
		while (input != null) {
			// Check for end of file
			if ((input == null) || (input.equals(LearningController.END_PERFORMANCE)))
				break;
			if (!noNote) {
				// First read in the readableGenerator
				readableGenerator_ = input;
				while (!(input = buf.readLine()).equals(""))
					readableGenerator_ += "\n" + input;

				// Then read the generator
				while (input.equals(""))
					input = buf.readLine();
				generator_ = input;
				while (!(input = buf.readLine()).equals(""))
					generator_ += "\n" + input;

				// Then read the performance
				while (input.equals(""))
					input = buf.readLine();

				val = Float.parseFloat(input);

				input = buf.readLine();
				while (input.equals(""))
					input = buf.readLine();

				// Some performance files may be cut off, so just use the
				// last recorded value.
				performances.add(val);
				performanceSum_ += val;
			}
		}

		performanceArray_ = performances.toArray(new Float[performances
				.size()]);

		buf.close();
		reader.close();

		return true;
	}

	public static Float[] getPerformanceArray() {
		return performanceArray_;
	}

	public static float getPerformanceSum() {
		return performanceSum_ / performanceArray_.length;
	}

	public static String getReadableGenerator() {
		return readableGenerator_;
	}

	public static String getGenerator() {
		return generator_;
	}
}
