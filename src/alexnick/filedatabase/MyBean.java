package alexnick.filedatabase;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import alexnick.CommonLib;

public class MyBean {
	private String one;
	private String two;
	private String three;
	private String four;
	private String fourApp;

	Boolean check;
	int serviceIntOne;
	int serviceIntTwo;
	String serviceString;

	Path binPath;
	Map<String, Integer> mapCountExt;
	Map<String, Set<String>> mapExtSignatures;

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

		this.check = false;
		this.serviceIntOne = 0;
		this.serviceIntTwo = 0;
		this.serviceString = null;

		this.binPath = null;
		this.mapCountExt = null;
		this.mapExtSignatures = null;
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
	String getNameLowerCaseFromFour() {
		var pos = four.lastIndexOf(File.separator);
		return pos >= 0 ? four.substring(pos + 1).toLowerCase() : four.toLowerCase();
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
	 * Finds 'substringInLowerCase' in MyBean.one
	 * 
	 * @param substringInLowerCase substring for finding, not must be null/empty,
	 *                             MUST BE IN LOWER CASE
	 * @param separatorInLowerCase if not null/empty, MUST BE IN LOWER CASE:
	 *                             'findInLowerCase' will be divided on separate
	 *                             strings, finding be in each
	 * 
	 * @return 'true' if found 'findLowerCase' in 'MyBean.one'
	 */
	boolean findInOneLowerCase(String substringInLowerCase, String separatorInLowerCase) {
		return findInLowerCase(0, one.toLowerCase(), substringInLowerCase, separatorInLowerCase);
	}

	/**
	 * Finds in 'stringInLowerCase', substring 'findInLowerCase'
	 * 
	 * @param findPosition         1:find in starts; 2:find in ends; else (example
	 *                             0): any place 'stringInLowerCase'
	 * @param stringInLowerCase    string for finding, not must be null/empty, MUST
	 *                             BE IN LOWER CASE
	 * @param substringInLowerCase substring for finding, not must be null/empty,
	 *                             MUST BE IN LOWER CASE
	 * 
	 * @param separatorInLowerCase if not null/empty, MUST BE IN LOWER CASE:
	 *                             'findInLowerCase' will be divided on separate
	 *                             strings, finding be in each
	 * 
	 * @return
	 */
	boolean findInLowerCase(int findPosition, final String stringInLowerCase, String substringInLowerCase,
			final String separatorInLowerCase) {
		if (CommonLib.nullEmptyString(substringInLowerCase) || stringInLowerCase.isEmpty()) {
			return false;
		}

		if (CommonLib.nullEmptyString(separatorInLowerCase)) {
			return stringInLowerCase.contains(substringInLowerCase);
		}

		int separatorLength = separatorInLowerCase.length();
		int limit = 1000; // just in case
		while (!substringInLowerCase.isEmpty() && limit > 0) {
			limit--;
			int posSeparatorInFind = substringInLowerCase.indexOf(separatorInLowerCase);
			var findSub = posSeparatorInFind >= 0 ? substringInLowerCase.substring(0, posSeparatorInFind)
					: substringInLowerCase;

			if (!findSub.isEmpty()) {
				if (findPosition == 1) {
					if (stringInLowerCase.startsWith(findSub)) { // 'findSub' not empty
						return true;
					}
				} else if (findPosition == 2) {
					if (stringInLowerCase.endsWith(findSub)) {
						return true;
					}
				} else if (stringInLowerCase.contains(findSub)) {
					return true;
				}
			}
			if (posSeparatorInFind < 0) {
				return false;
			}
			substringInLowerCase = substringInLowerCase.substring(posSeparatorInFind + separatorLength);
		}
		return false;
	}

}
