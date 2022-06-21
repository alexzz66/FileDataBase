package alexnick.filedatabase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MarkInfo {
	Set<Integer> countSet;
	int countTotal;
	Map<String, Integer> mapCountExt;
	Map<String, Set<String>> mapExtSignatures;

	public MarkInfo() {
		countSet = new HashSet<Integer>();
		this.countTotal = 0;
		this.mapCountExt = new TreeMap<String, Integer>();
		this.mapExtSignatures = new HashMap<String, Set<String>>();
	}

	public Set<String> addAll(String key, Set<String> value) {
		this.mapExtSignatures.get(key).addAll(value);
		return mapExtSignatures.get(key);
	}

}
