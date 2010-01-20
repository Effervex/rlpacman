package relationalFramework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.mandarax.kernel.Rule;

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
					bf
							.write(StateSpec.encodeRule(r.getRule())
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
			File ruleBaseFile, Collection<GuidedPredicate> condGenerator,
			Collection<GuidedPredicate> actGenerator) {
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

			// Read the rules in.
			Map<String, Object> constants = StateSpec.getInstance()
					.getConstants();
			while (((input = bf.readLine()) != null) && (!input.equals(""))) {
				Slot slot = null;
				// Split the base into rules
				String[] split = input.split(RULE_DELIMITER);
				// For each rule, add it to the rulebase
				for (int i = 0; i < split.length; i++) {
					Rule rule = StateSpec.getInstance().parseRule(split[i],
							constants);
					ArrayList<GuidedPredicate> condsAct = PolicyGenerator
							.getInstance().inferGuidedPreds(rule);
					GuidedPredicate action = condsAct
							.remove(condsAct.size() - 1);
					if (slot == null) {
						slot = new Slot(action.getPredicate());
					}

					GuidedRule gr = new GuidedRule(rule, condsAct, action, slot);
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

		buf.write(policyGenerator.generatorString(ELEMENT_DELIMITER) + "\n");

		// For each of the rule generators
		for (Slot slot : policyGenerator) {
			buf.write(slot.getGenerator().generatorString(ELEMENT_DELIMITER)
					+ "\n");
		}

		buf.close();
		wr.close();
	}

	/**
	 * Saves a frozen, human readable version of the generators out
	 * 
	 * @param output
	 *            The file to output the human readable generators to.
	 */
	public static void saveHumanGenerators(File output) throws Exception {
		ProbabilityDistribution<Slot> policyGenerator = PolicyGenerator
				.getInstance().getGenerator();

		FileWriter wr = new FileWriter(output);
		BufferedWriter buf = new BufferedWriter(wr);

		// Go through each slot, writing out those that fire
		ArrayList<Slot> probs = policyGenerator.getOrderedElements();
		for (Slot slot : probs) {
			// Output every non-zero rule
			boolean single = true;
			for (GuidedRule rule : slot.getGenerator().getNonZero()) {
				if (!single)
					buf.write("/ ");
				buf.write(StateSpec.encodeRule(rule.getRule()));
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
	 * @param policyGenerator
	 *            The policy generator to load to.
	 * @throws Exception
	 *             Should something go awry.
	 */
	public static void loadGenerators(File input,
			ProbabilityDistribution<Slot> policyGenerator) {
		// TODO Modify this method
		try {
			FileReader reader = new FileReader(input);
			BufferedReader buf = new BufferedReader(reader);

			// Parse the slots
			String[] split = buf.readLine().split(ELEMENT_DELIMITER);
			for (int i = 0; i < split.length; i++) {
				policyGenerator.set(i, Double.parseDouble(split[i]));
			}

			// Parse the rules
			// RuleBase.getInstance().readGenerators(buf);

			buf.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
