package alexnick.filedatabase;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import alexnick.CommonLib;

public class MyBean {
	private String one;
	private String two;
	private String three;
	private String four;
	private String fourApp;

	boolean check = false;
	boolean selectedForSorting = false; //for SORT ONLY
	int serviceIntOne = 0;
	int serviceIntTwo = 0;
	int serviceIntThree = 0;
	long serviceLong = 0;
	String serviceStringOne = null;
	String serviceStringTwo = null;

	Path binPath = null;
	Map<String, Integer> mapCountExt = null;
	Map<String, Set<String>> mapExtSignatures = null;
	Set<Integer> serviceSet = null;

	/**
	 * MyBean for table with 4 columns, all strings must not be 'null'<br>
	 * If 'null', will be set as 'empty'
	 * 
	 * @param one
	 * @param two
	 * @param three
	 * @param four
	 * @param fourApp
	 */
	public MyBean(String one, String two, String three, String four, String fourApp) {
		this.one = one == null ? "" : one;
		this.two = two == null ? "" : two;
		this.three = three == null ? "" : three;
		this.four = four == null ? "" : four;
		this.fourApp = fourApp == null ? "" : fourApp;
	}

	Boolean getCheck() {
		return check;
	}

	String getOne() {
		return one;
	}

	String getOneLowerCase() {
		return one.toLowerCase();
	}

	void setOne(String one) {
		this.one = one;
	}

	String getTwo() {
		return two;
	}

	void setTwo(String two) {
		this.two = two;
	}

	String getThree() {
		return three;
	}

	void setThree(String three) {
		this.three = three;
	}

	String getFour(boolean removePrefixNoExists, boolean appendFourApp) {
		String fourTmp = removePrefixNoExists ? getFourNoFrefix() : four;
		return (fourApp.isEmpty() || !appendFourApp) ? fourTmp : fourTmp.concat(fourApp);
	}

	/**
	 * 'setFour' and 'setFourApp' must be set together;<br>
	 * However, can be set separately, if any of 'four' or 'fourApp' be set as
	 * 'null'<br>
	 * 
	 * @param four    if 'null', won't be changed
	 * @param fourApp if 'null', won't be changed
	 */
	void setFour(String four, String fourApp) {
		if (four != null) {
			this.four = four;
		}
		if (fourApp != null) {
			this.fourApp = fourApp;
		}
	}

	String getFourApp(boolean aliasIfEmpty, boolean deleteFirstDots) {
		var s = deleteFirstDots ? CommonLib.startOrEndDeleteAll(".", true, false, fourApp) : fourApp;
		if (s.isEmpty()) {
			return aliasIfEmpty ? Const.EXTEMPTY : "";
		}
		return s;
	}

	private String getFourNoFrefix() {
		return isFourPrefixNoExists() ? four.substring(Const.prefixInTableForNoExists.length()) : four;
	}

	String getFourLowerCase(boolean removePrefixNoExists, boolean withExt) {
		String fourTmp = removePrefixNoExists ? getFourNoFrefix() : four;
		return withExt ? fourTmp.concat(fourApp).toLowerCase() : fourTmp.toLowerCase();
	}

	/**
	 * Found last index '\' in 'four'; if finds, return ends 'four' after '\'
	 * 
	 * @return
	 */
	String getNameFromFour(boolean toLowerCase) {
		var pos = four.lastIndexOf(File.separator);
		String res = pos >= 0 ? four.substring(pos + 1) : four.toLowerCase();
		return toLowerCase ? res.toLowerCase() : res;
	}

	int getStartNumberFromFour() {
		var pos = four.indexOf(',');
		if (pos > 0) {
			try {
				return Integer.valueOf(four.substring(0, pos));
			} catch (Exception e) {
			}
		}
		return 0;
	}

	boolean isFourPrefixNoExists() {
		return four.startsWith(Const.prefixInTableForNoExists);
	}

	/**
	 * @param columnNumber set 1:'one', 2:'two', 3:'three', 4:'four'+'fourApp';
	 *                     5:'four only' <br>
	 *                     else return false
	 * @return
	 */
	String getStringByColumnNumberOrEmpty(int columnNumber) {
		return columnNumber == 1 ? one
				: columnNumber == 2 ? two
						: columnNumber == 3 ? three
								: columnNumber == 4 ? getFour(false, true)
										: columnNumber == 5 ? getFour(false, false) : "";
	}

	/**
	 * Finds 'subStrings' in MyBean.four<br>
	 * Find position will be set as 'any place'
	 * 
	 * @param columnNumber set 1:'one', 2:'two', 3:'three', 4:'four'+'fourApp';
	 *                     5:'four only' <br>
	 *                     else return false
	 * @param toLowerCase  1: 'string' from 'columnNumber' will be set to lower
	 *                     case<br>
	 *                     2: 'each from 'substrings' will be set to lower case<br>
	 *                     3: '1' and '2': all strings will be set to lower case<br>
	 *                     else (example 0): no action, comparing as is
	 * @param subStrings   substrings for finding, must not be null/empty
	 * 
	 * @return 'true' if found at least one 'subString' in defined 'columnNumber'
	 */
	boolean findSubstringsInColumn(int columnNumber, int toLowerCase, List<String> subStrings) {
		String s = getStringByColumnNumberOrEmpty(columnNumber);
		return s.isEmpty() ? false : FileDataBase.findSubStringsInString(0, toLowerCase, s, subStrings);
	}

}
