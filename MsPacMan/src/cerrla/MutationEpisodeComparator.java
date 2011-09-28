package cerrla;

import java.util.Comparator;

import relationalFramework.RelationalRule;
import util.Pair;

public class MutationEpisodeComparator implements Comparator<Pair<RelationalRule, Integer>> {
	private static Comparator<Pair<RelationalRule, Integer>> instance_;

	@Override
	public int compare(Pair<RelationalRule, Integer> o1,
			Pair<RelationalRule, Integer> o2) {
		int result = Double.compare(o1.objB_, o2.objB_);
		if (result == 0)
			return o1.objA_.compareTo(o2.objA_);
		return result;
	}
	
	public static Comparator<Pair<RelationalRule, Integer>> getInstance() {
		if (instance_ == null)
			instance_ = new MutationEpisodeComparator();
		return instance_;
	}
}
