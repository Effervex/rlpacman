package cerrla;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Reads a performance file in and notes down the performance array, the last
 * readable generator and the last generator.
 * 
 * @author Sam Sarjant
 */
public class PerformanceReader {
	private static SortedMap<Integer, Float> performanceMap_;
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
	public static boolean readPerformanceFile(File perfFile, boolean byEpisode)
			throws Exception {
		performanceMap_ = new TreeMap<Integer, Float>();
		readableGenerator_ = null;
		generator_ = null;
		FileReader reader = new FileReader(perfFile);
		BufferedReader buf = new BufferedReader(reader);
		
		// Read up to "A typical policy"
		String input = "";
		while (input != null && !input.equals("A typical policy:")) {
			input = buf.readLine();
		}

		// For every value within the performance file
		
		int regularCEcount = 1;
		while (input != null) {
			// Check for end of file
			if ((input == null)
					|| (input.equals(LearningController.END_PERFORMANCE)))
				break;
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

			// Then read the convergence
			buf.readLine();
			buf.readLine();
			buf.readLine();

			// Then read the performance
			while (input.equals(""))
				input = buf.readLine();

			String[] split = input.split("\t");
			int ep = 0;
			float val = 0;
			if (split.length == 2) {
				ep = Integer.parseInt(split[0]);
				val = Float.parseFloat(split[1]);
			} else if (split.length == 1) {
				ep = regularCEcount;
				val = Float.parseFloat(split[0]);
			}
			if (!byEpisode)
				ep = regularCEcount;

			input = buf.readLine();
			while ((input != null) && input.equals(""))
				input = buf.readLine();

			// Some performance files may be cut off, so just use the
			// last recorded value.
			performanceMap_.put(ep, val);
			regularCEcount++;
		}

		buf.close();
		reader.close();

		return true;
	}

	/**
	 * Extracts the performances from the input file and prints them out to the
	 * output file.
	 * 
	 * @param input
	 *            The input file.
	 * @param output
	 *            The output file.
	 */
	public static void extractPerformance(File input, File output) {
		try {
			readPerformanceFile(input, true);

			if (!output.exists())
				output.createNewFile();
			FileWriter writer = new FileWriter(output);
			BufferedWriter bf = new BufferedWriter(writer);

			SortedMap<Integer, Float> perfs = getPerformanceArray();
			for (Integer ep : perfs.keySet()) {
				bf.write(ep + "\t" + perfs.get(ep) + "\n");
			}

			bf.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Extracts the performances from the input file and prints them out to an
	 * output file which is named using the input file.
	 * 
	 * @param input
	 *            The input file.
	 */
	public static void extractPerformance(File input) {
		try {
			// Create the output file
			String outputPath = input.getAbsolutePath();
			int dotIndex = outputPath.lastIndexOf('.');
			outputPath = outputPath.substring(0, dotIndex) + "PerfOutput"
					+ outputPath.substring(dotIndex);
			File output = new File(outputPath);
			output.createNewFile();

			extractPerformance(input, output);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static SortedMap<Integer, Float> getPerformanceArray() {
		return performanceMap_;
	}

	public static String getReadableGenerator() {
		return readableGenerator_;
	}

	public static String getGenerator() {
		return generator_;
	}

	/**
	 * Main method calls the extract performance method.
	 * 
	 * @param args
	 *            The file to extract from and optionally the file to
	 *            destination.
	 */
	public static void main(String[] args) {
		if (args.length == 1)
			extractPerformance(new File(args[0]));
		else if (args.length >= 2) {
			if (args[1].contains("."))
				extractPerformance(new File(args[0]), new File(args[1]));
			else {
				String[] split = args[1].split("-");
				int start = Integer.parseInt(split[0]);
				int end = Integer.parseInt(split[1]);
				for (; start <= end; start++)
					extractPerformance(new File(args[0] + start));
			}
		} else
			System.err.println("No input/output files given!");
	}
}
