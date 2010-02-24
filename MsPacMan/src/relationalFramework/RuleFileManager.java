package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A class handling the loading and saving of files within the system, boith of
 * generators and rules.
 * 
 * @author Samuel J. Sarjant
 */
public class RuleFileManager {
	/** The element delimiter between elements in the generator files. */
	public static final String ELEMENT_DELIMITER = ",";

	/** The delimiter character between rules within the same rule base. */
	public static final String RULE_DELIMITER = "@";

	/**
	 * Save rules to a file in the format
	 * 
	 * @param ruleBaseFile
	 *            The file to save the rules to.
	 */
	public static void saveRulesToFile(File ruleBaseFile) {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		try {
			if (!ruleBaseFile.exists())
				ruleBaseFile.createNewFile();

			FileWriter writer = new FileWriter(ruleBaseFile);
			BufferedWriter bf = new BufferedWriter(writer);

			bf.write(StateSpec.getInstance().getGoalState() + "\n");
			// For each of the rule bases
			for (Slot slot : policyGenerator) {
				// For each of the rules
				for (GuidedRule r : slot.getGenerator()) {
					bf.write(StateSpec.getInstance().encodeRule(r.toString())
							+ RULE_DELIMITER);
				}
				bf.write("\n");
			}

			System.out.println("Random rulebases saved to: " + ruleBaseFile);

			bf.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the rules from a file.
	 * 
	 * @param ruleBaseFile
	 *            The file to load the rules from.
	 * @param actGenerator
	 *            The actions generator.
	 * @param condGenerator
	 *            The conditions generator.
	 * @return The rules loaded in.
	 */
	public static ProbabilityDistribution<Slot> loadRulesFromFile(
			File ruleBaseFile) {
		ProbabilityDistribution<Slot> ruleBases = new ProbabilityDistribution<Slot>();

		try {
			FileReader reader = new FileReader(ruleBaseFile);
			BufferedReader bf = new BufferedReader(reader);

			// Checking the environment goals match.
			String input = bf.readLine();
			if (!input
					.equals(StateSpec.getInstance().getGoalState().toString())) {
				System.err
						.println("Environment goal does not match! Crashing...");
				return null;
			}

			// Read in a line of rules, all infering the same slot.
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				Slot slot = null;
				// Split the base into rules
				String[] split = input.split(RULE_DELIMITER);
				// For each rule, add it to the rulebase
				for (int i = 0; i < split.length; i++) {
					String rule = StateSpec.getInstance().parseRule(split[i]);
					if (slot == null) {
						slot = new Slot(StateSpec.splitFact(rule
								.split(StateSpec.INFERS_ACTION)[1].trim())[0]);
					}

					GuidedRule gr = new GuidedRule(rule, slot);
					slot.getGenerator().add(gr);
				}
				slot.getGenerator().normaliseProbs();
				ruleBases.add(slot);
			}

			bf.close();
			reader.close();

			return ruleBases;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Saves the generators/distributions to file. TODO Modify this method to
	 * save the generators in a more dynamic format.
	 * 
	 * @param output
	 *            The file to output the generator to.
	 * @throws Exception
	 *             Should something go awry.
	 */
	public static void saveGenerators(File output) throws Exception {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		// For each of the rule generators
		for (int i = 0; i < policyGenerator.size(); i++) {
			buf.write("(" + policyGenerator.getElement(i).toParsableString()
					+ ")" + ELEMENT_DELIMITER + policyGenerator.getProb(i)
					+ "\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Saves a human readable version of the generators out to file. Typically
	 * the generators are frozen before this method is called.
	 * 
	 * @param output
	 *            The file to output the human readable generators to.
	 */
	public static void saveHumanGenerators(File output) throws Exception {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		buf.write("A typical policy:\n");

		// Go through each slot, writing out those that fire
		ArrayList<Slot> probs = policyGenerator.getOrderedElements();
		for (Slot slot : probs) {
			// Output every non-zero rule
			boolean single = true;
			for (GuidedRule rule : slot.getGenerator().getNonZero()) {
				if (!single)
					buf.write("/ ");
				buf.write(StateSpec.getInstance().encodeRule(rule.toString()));
				single = false;
			}
			buf.write("\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Loads the generators/distributions from file.
	 * 
	 * @param file
	 *            The file to load from.
	 * @return The policy generator to load to.
	 */
	public static ProbabilityDistribution<Slot> loadGenerators(File input) {
		ProbabilityDistribution<Slot> dist = new ProbabilityDistribution<Slot>();
		try {
			FileReader reader = new FileReader(input);
			BufferedReader buf = new BufferedReader(reader);

			// Parse the slots
			String in = null;
			while ((in = buf.readLine()) != null) {
				// Get the slot string, ignoring the () brackets
				String slotString = in
						.substring(1, in.lastIndexOf(ELEMENT_DELIMITER) - 1);
				Slot slot = Slot.parseSlotString(slotString);
				Double prob = Double.parseDouble(in.substring(in
						.lastIndexOf(ELEMENT_DELIMITER) + 1));

				// Parse the rules
				dist.add(slot, prob);
			}

			buf.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return dist;
	}

}
