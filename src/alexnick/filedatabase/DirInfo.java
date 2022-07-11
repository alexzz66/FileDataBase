package alexnick.filedatabase;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DirInfo {

	final String fullPathCanonical; // with file separator on end
	int countTotalFiles;
	long sizeTotalFiles;
	Set<String> transitFoldersNames;// means subfolders in this folder
	Map<String, FileInfo> filesMap;// means files in this folder
	Map<String, Integer> extsInfoMap; // <ext:count> in all subfolders

	/**
	 * @param fullNameCanonical
	 * 
	 */
	public DirInfo(String fullPathCanonical) {
		this.fullPathCanonical = fullPathCanonical;
		this.countTotalFiles = 0;
		this.sizeTotalFiles = 0L;
		this.transitFoldersNames = new TreeSet<String>();
		this.filesMap = new TreeMap<String, FileInfo>();

		this.extsInfoMap = new TreeMap<String, Integer>();
	}

}
