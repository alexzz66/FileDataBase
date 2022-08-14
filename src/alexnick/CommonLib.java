package alexnick;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFileChooser;

/**
 * @author alexey
 *
 */

public class CommonLib {
	public static final String NEW_LINE_WINDOWS = "\r\n";
	public static final String NEW_LINE_UNIX = "\n";

	public static final int SIGN_FILE = 1;
	public static final int SIGN_FOLDER = 2;
	public static final int SIGN_FILE_OR_FOLDER = 0;

	public static final String PRINTDELIMITER = "-------";
	public static final String ADDLOG_SEP = "%sep%";
	public static final String ADDLOG_DATE = "%date%";

	// formatter 'yyyy.MM.dd_HH:mm:ss' no change, this is constant for extract date
	public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss (E)");
	private static SimpleDateFormat formatterFileName = null;// uses in 'getFormatDateForFileName' only

//when 'start java' in power shell, need set focus if is command 'pause' below
	public static final String FOCUS_INFO = "Press ANY TIMES on SPACE and ENTER, if not focused." + NEW_LINE_UNIX;

	/**
	 * Writes on console 'Program's finished', then System.exit(exitCode)
	 * 
	 * @param exitCode
	 * @param finalPause if 'true' will first be written on console 'FINAL PAUSE'
	 *                   and wait for key press
	 */
	public static void finalProgramExit(int exitCode, boolean finalPause) {
		if (finalPause) {
			addLog(ADDLOG_SEP, true, null);
			pause("FINAL PAUSE");
		}
		System.out.println("Programm's finished");
		System.exit(exitCode);
	}

	/**
	 * Throws 'IllegalArgumentException'
	 * 
	 * @param message
	 */
	public static void errorArgument(String message) {
		throw new IllegalArgumentException(message);
	}

	/**
	 * @param value
	 * @return true, if 'value' null or empty
	 */
	synchronized public static boolean nullEmptyString(String value) {
		return value == null || value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' not null and not empty
	 */
	synchronized public static boolean notNullEmptyString(String value) {
		return value != null && !value.isEmpty();
	}

	/**
	 * if 'string' not null and not empty, be append to StringBuilder
	 * 
	 * @param string be append to 'sb'
	 * @param sb     must not be null
	 * @return 'true' if anything was appended to 'sb'
	 */
	synchronized public static boolean appendNotNullEmpty(String string, StringBuilder sb) {
		if (nullEmptyString(string)) {
			return false;
		}
		sb.append(string);
		return true;
	}

	/**
	 * if 'string' not null, will be trimmed, if defined, and appended to
	 * StringBuilder;<br>
	 * 'new line' types:<br>
	 * 0 (or default): no append 'new line';<br>
	 * 1: append, if 'string' NO EMPTY;<br>
	 * 2: append even if 'string' empty
	 * 
	 * @param string           will be append to 'sb'; if 'string' is null - return
	 *                         false; if 'string' is empty, be append 'newLines'
	 *                         only, if defined
	 * @param needTrim         if 'true', 'string' will be trimmed
	 * @param addNewLineBefore 'new line' before
	 * @param addNewLineAfter  'new line' after
	 * @param sb               must not be null
	 * @return 'true' if anything was appended to 'sb'
	 */
	synchronized public static boolean appendNotNullWithTrim(String string, boolean needTrim, int addNewLineBefore,
			int addNewLineAfter, StringBuilder sb) {
		if (string == null) {
			return false;
		}
		if (needTrim) {
			string = string.trim();
		}

		boolean res = false;
		if (addNewLineBefore == 1 && !string.isEmpty()) {
			addNewLineBefore = 2;
		}
		if (addNewLineBefore == 2) {
			sb.append(NEW_LINE_UNIX);
			res = true;
		}

		if (!string.isEmpty()) {
			sb.append(string);
			res = true;
		}

		if (addNewLineAfter == 1 && !string.isEmpty()) {
			addNewLineAfter = 2;
		}
		if (addNewLineAfter == 2) {
			sb.append(NEW_LINE_UNIX);
			res = true;
		}
		return res;
	}

	/**
	 * @param caption     if not null, will be written in start message
	 * @param yes         if not null, will be written after 'YES'
	 * @param no          if not null, will be written after 'NO'
	 * @param postMessage if not null, will be written down one line from 'NO'
	 * @return
	 */
	synchronized public static String formatConfirmYesNoMessage(String caption, String yes, String no,
			String postMessage) {
		var sb = new StringBuilder();
		appendNotNullWithTrim(caption, true, 0, 1, sb);
		sb.append("<YES> ");
		appendNotNullWithTrim(yes, true, 0, 2, sb);
		sb.append("<NO>  ");
		appendNotNullWithTrim(no, true, 0, 2, sb);
		appendNotNullWithTrim(postMessage, true, 1, 0, sb);
		return sb.toString();
	}

	/**
	 * @param value
	 * @return true, if 'value' null or empty
	 */
	synchronized public static boolean nullEmptyList(List<?> value) {
		return value == null || value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' not null and not empty
	 */
	synchronized public static boolean notNullEmptyList(List<?> value) {
		return value != null && !value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' null or empty
	 */
	synchronized public static boolean nullEmptySet(Set<?> value) {
		return value == null || value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' not null and not empty
	 */
	synchronized public static boolean notNullEmptySet(Set<?> value) {
		return value != null && !value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' null or empty
	 */
	synchronized public static boolean nullEmptyMap(Map<?, ?> value) {
		return value == null || value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' not null and not empty
	 */
	synchronized public static boolean notNullEmptyMap(Map<?, ?> value) {
		return value != null && !value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' null or empty
	 */
	synchronized public static boolean nullEmptyProperties(Properties value) {
		return value == null || value.isEmpty();
	}

	/**
	 * @param value
	 * @return true, if 'value' not null and not empty
	 */
	synchronized public static boolean notNullEmptyProperties(Properties value) {
		return value != null && !value.isEmpty();
	}

	private static void initFormatterFileName() {
		if (formatterFileName == null) {
			formatterFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
		}
	}

	/**
	 * @param date Date to format
	 * @return formatted string for file name, as "yyyy-MM-dd_HH-mm-ss", example
	 *         "2000-12-31_01-01-01"
	 */
	synchronized public static String getFormatDateForFileName(long date) {
		initFormatterFileName();
		return formatterFileName.format(date);
	}

	/**
	 * 
	 * @return formatted string for file name, as "yyyy-MM-dd_HH-mm-ss", example
	 *         "2000-12-31_01-01-01"; will be formatted current dateTime
	 */
	synchronized public static String getFormatDateNowForFileName() {
		initFormatterFileName();
		return formatterFileName.format(new Date());
	}

	/**
	 * @param date File date modified as type 'long' in milliseconds
	 * @return formatted string by specified formatter
	 */
	synchronized public static String dateModifiedToString(long date) {
		return formatter.format(date);
	}

	// s as '2022.08.13_15:03:56 (ñá)'; formatter 'yyyy.MM.dd_HH:mm:ss'
	synchronized public static LocalDateTime getLocalDateFromFormatterOrNull(String s) {
		if (nullEmptyString(s)) {
			return null;
		}
		int[] dt = new int[6];
		Arrays.fill(dt, -1);
		int index = 0;
		String sub = "";
		try {
			for (var i = 0; i < s.length(); i++) {
				char c = getDigitOrMinus(s.charAt(i));
				if (c != '-') {
					sub += c;
					continue;
				}

				if (sub.isEmpty()) {
					continue;
				}

				dt[index] = Integer.valueOf(sub);
				sub = "";
				index++;
				if (index >= dt.length) {
					break;
				}
			}

			if (index < dt.length) { // no fill array
				return null;
			}

			return LocalDateTime.of(dt[0], dt[1], dt[2], dt[3], dt[4], dt[5]);

		} catch (Exception e) {
		}
		return null;
	}

	synchronized public static long LocalDateToEpochSecondOrNil(LocalDateTime ldt) {
		long result = 0;
		try {
			result = ldt.toEpochSecond(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
		} catch (Exception e) {
			result = 0;
		}
		return result < 0 ? 0 : result;
	}

	synchronized public static char getDigitOrMinus(char charAt) {
		return switch (charAt) {
		case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> charAt;
		default -> '-';
		};
	}

	/**
	 * Returns different string (sec,min,hours), from mSec
	 * 
	 * @param mSec if less '999' - be added '-' in result; if (-999..999) -> returns
	 *             empty string
	 * @return formatted string "h:m:s"
	 */
	synchronized public static String getDifferentTimeOrEmpty(long mSec) {
		StringBuilder sb = new StringBuilder();
		if (mSec < 0) {
			mSec = -mSec;
			sb.append("-");
		}

		long sec = mSec / 1000;
		if (sec <= 0) {
			return "";
		}

		long min = sec < 60 ? 0 : sec / 60;
		long hours = min < 60 ? 0 : min / 60;

		sb.append(hours).append(":").append(sec < 60 ? min : min % 60).append(":").append(sec < 60 ? sec : sec % 60);
		return sb.toString();
	}

	/**
	 * Formats seconds to string, 25 second, like '0:25'; but if 'withHour defined:
	 * '0:00:25'
	 * 
	 * @param needDays 0 (by default): no write days<br>
	 *                 1: write days, if time >= 86400 <br>
	 *                 2: write days anyway<br>
	 *                 If written days, withHour will be set as 'true'
	 * 
	 * @param withHour if true AND time < 3600, will be as '0:mm:ss'
	 * @param time     count of second to transform
	 * @param prefix   if not null/empty, will be added before result
	 * @param postfix  if not null/empty, will be added after result
	 * @return 'prefix' + 'formatted seconds' + 'postfix'
	 */
	synchronized public static String secondsToString(int needDays, boolean withHour, int time, String prefix,
			String postfix) {
		if (time == Integer.MIN_VALUE) {
			time++; // no correct for 'Integer.MIN_VALUE'
		}

		if (needDays < 0 || needDays > 2) {
			needDays = 0;
		}

		final int daySeconds = 86400;

		var sb = new StringBuilder();
		appendNotNullEmpty(prefix, sb);

		if (time < 0) {
			sb.append("-");
			time = -time;
		}

		if ((needDays == 1 && time >= daySeconds) || (needDays == 2)) {
			withHour = true;
			int days = 0;
			if (time >= daySeconds) {
				days = time / daySeconds;
				time = time % daySeconds;
			}
			sb.append("Days ").append(days).append("; ");
		}

		int s = time % 60; // get seconds
		time = time / 60; // time in minute now
		int m = time % 60; // get minutes

		time = time / 60; // time in hours now
		boolean needCheckMinuteNil = false;

		if (withHour || time > 0) {
			sb.append(time).append(":");
			needCheckMinuteNil = true;
		}

		if (needCheckMinuteNil && m < 10) {
			sb.append("0");
		}

		sb.append(m).append(":");

		if (s < 10) {
			sb.append("0");
		}

		sb.append(s);
		appendNotNullEmpty(postfix, sb);
		return sb.toString();
	}

	/**
	 * @param needSortList if 'true': 'list' WILL BE SORTED
	 * @param totalSize    if more '0', write converted to bytes, KBytes
	 * @param limit        for writing on console, less or equals 0 or more than
	 *                     'LIMIT' (200), be equals 'LIMIT'
	 * @param caption      what 'list' is named; must be empty, but not null
	 * @param fullLog      if 'log' not null: if 'true' be write full log; if
	 *                     'false' no more 'limit'
	 * @param log          if not null, all inform and ALL items (without 'limit')
	 *                     be added to 'log'
	 * @param list         for writing on console, not must be null
	 * @return string in 'log' where be written 'caption'; returns '0' if error
	 */
	synchronized public static int writeListToConsole(boolean needSortList, long totalSize, int limit, String caption,
			boolean fullLog, List<String> log, List<String> list) {
		if (needSortList && list.size() > 1) {
			list.sort(null);
		}
		addLog(ADDLOG_SEP, true, log);
		var t = (totalSize > 0) ? " ; total size: " + bytesToKBMB(false, 0, totalSize) : "";
		var t2 = caption + " size: " + list.size() + t;

		addLog(t2, true, log);
		int sizeForReturn = log == null ? 0 : log.size();
		if (!list.isEmpty()) {
			addLog("", true, log);
		}

		final int LIMIT = 200;
		if (limit <= 0 || limit > LIMIT) {
			limit = LIMIT;
		}

		for (int i = 0; i < Math.min(limit, list.size()); i++) {
			addLog(list.get(i), true, log);
		}
		int dif = list.size() - limit;
		if (dif > 0) {
			String s = "...and other elements: " + dif;
			System.out.println(s);
			if (log != null) {
				if (fullLog) {
					for (int i = limit; i < list.size(); i++) {
						log.add(list.get(i));
					}
				} else {
					log.add(s);
				}
			}
		}
		addLog(ADDLOG_SEP, true, log);
		return sizeForReturn;
	}

	/**
	 * 
	 * @param s              string for adding to 'log';<br>
	 *                       case CommonLib.ADDLOG_DATE - add current date to
	 *                       'log';<br>
	 *                       case CommonLib.ADDLOG_SEP - add 'separator string' to
	 *                       'log'
	 * @param writeOnConsole if true, 's' be wrote on console
	 * @param log            list of string; if null - string 's' will not be added
	 */
	synchronized public static void addLog(String s, boolean writeOnConsole, List<String> log) {
		String res = "";
		boolean bAddedInLog = false;
		try {
			if (s.equals(ADDLOG_DATE)) {
				res = formatter.format(new Date());// "Date: " no need;writes in table
				return;
			}
			if (s.equals(ADDLOG_SEP)) {
				res = NEW_LINE_UNIX + PRINTDELIMITER + NEW_LINE_UNIX;
				bAddedInLog = true;
				if (log != null) { // need for correct size of 'log
					log.add("");
					log.add(PRINTDELIMITER);
					log.add("");
				}
				return;
			}
			res = s;
		} finally {
			if (writeOnConsole) {
				System.out.println(res);
			}
			if (!bAddedInLog && log != null) {
				log.add(res);
			}
		}
	}

	/**
	 * @param needSpaces if 'true', will be added three spaces before KB, MB, GB
	 * @param shortType  example for size 1025 bytes:<br>
	 *                   0 (by default): full format, size in bytes + short1, '1025
	 *                   (KB: 1,00)';<br>
	 *                   1: short1 only, 'KB: 1,00';<br>
	 *                   2: short2, '1,0kb' <br>
	 *                   3: full, swap => KB: 1,00 (1025) <br>
	 *                   4: the same as '3', with append spaces, KB:___1,00 (1025)
	 * @param size       long size (example of file)
	 * @return formatted string from @param sz, rounded to near bytes,KB,MB,GB (div
	 *         1024, not 1000)
	 */
	synchronized public static String bytesToKBMB(boolean needSpaces, int shortType, long size) {
		final String[] ar = { "bytes", "KB", "MB", "GB" };
		final String[] arSpaces = { "", "   ", "   ", "   " };
		if (shortType < 0 || shortType > 4) {
			shortType = 0;
		}
		String formatLength = shortType == 2 ? "%.1f" : shortType == 4 ? "%6.2f" : "%.2f";

		if (shortType == 4) {
			shortType = 3; // for return result
		}

		long sz = size;
		long x = 1 << 10;
		String res = "", tmp;
		int pos = 0;
		long md = 0;
		while (sz > 0) {
			if ((sz < x) || (pos == ar.length - 1)) {
				tmp = (md == 0) ? String.valueOf(sz) : String.format(formatLength, sz + md / 1024.0);
				String s = (needSpaces) ? arSpaces[pos] + tmp : tmp;
				res = shortType == 2 ? s + ar[pos].toLowerCase() : ar[pos] + ": " + s;
				break;
			}
			md = sz % x;
			sz >>>= 10;
			pos++;
		}
		if (res.isEmpty()) {// sz <= 0
			res = ar[0] + ": " + arSpaces[0] + 0;
		}
		return shortType == 3 ? res + " (" + size + ")" : shortType == 0 ? size + " (" + res + ")" : res;
	}

	/**
	 * @param bStore              if true - saving; if false - load properties
	 * @param commentForStore     need for 'bStore' == true; otherwise may be 'null'
	 * @param path                Path of property file
	 * @param prop                must be created for store or saving, be returned
	 *                            filling for 'bStore' == false
	 * @param pathDoubleForSaving if not null, for 'bStore' be saved copy of
	 *                            properties
	 * @return result of operation 'Properties -> store/load'
	 */
	synchronized public static void loadOrStoreProperties(boolean bStore, String commentForStore, Path path,
			Path pathDoubleForSaving, Properties prop) {
		try {
			CreateDirectoriesIfNeed(path);

			if (bStore) {
				var stream = new FileOutputStream(path.toString());
				prop.store(stream, commentForStore);
				stream.close();
				if (pathDoubleForSaving != null) {
					CreateDirectoriesIfNeed(pathDoubleForSaving);
					stream = new FileOutputStream(pathDoubleForSaving.toString());
					prop.store(stream, commentForStore);
					stream.close();
				}
			} else {
				if (!path.toFile().exists()) {
					return;
				}
				var stream = new FileInputStream(path.toString());
				if (path.toFile().exists()) {
					prop.load(stream);
					stream.close();
				}
			}
		} catch (Exception e) {
			System.out.println(
					"Error " + (bStore ? "save" : "load") + " properties: " + e.getMessage() + "; file " + path);
		}
	}

	/**
	 * @param skipEmpty        0 (by default) read all lines;<br>
	 *                         1: empty lines be skipped;<br>
	 *                         2: TRIMMED empty lines be skipped
	 * @param inLowerUpperCase if 1: each item will be set to lower case; 2: to
	 *                         UPPER CASE; else (0) : no action
	 * @param path             Path for file, where be read
	 * @return created (not null) array list of String; in case error, list be empty
	 */
	synchronized public static List<String> readFile(int skipEmpty, int inLowerUpperCase, Path path) {
		ArrayList<String> list = new ArrayList<>();
		if (!path.toFile().exists()) {
			return list;
		}
		if (skipEmpty < 0 || skipEmpty > 2) {
			skipEmpty = 0;
		}
		String line;
		try (var in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			while ((line = in.readLine()) != null) {
				if (skipEmpty > 0) {
					if (line.isEmpty() || (skipEmpty == 2 && line.trim().isEmpty())) {
						continue;
					}
				}

				if (inLowerUpperCase == 1) {
					list.add(line.toLowerCase());
				} else if (inLowerUpperCase == 2) {
					list.add(line.toUpperCase());
				} else {
					list.add(line);
				}
			}
		} catch (Exception e) {
			list.clear();
			System.out.println("error read file: " + path + NEW_LINE_UNIX + e.getMessage());
		}
		return list;
	}

	private static String getLineFromScanner(Scanner scanner) {
		String line = "";
		while (scanner.hasNext()) {
			line = scanner.nextLine().trim();
			if (!line.isEmpty()) {
				break;
			}
		}
		return line;
	}

	private static String selectedNumber(String numberInfo) {
		return FOCUS_INFO + "Type '" + numberInfo + "' then ENTER to continue or ANY SYMBOL instead, to cancel";
	}

	// where is 'scanner' - method starts with 'pause..' (recommended)
	/**
	 * @param caption if not empty, will be written on console
	 * @return 'false' if user cancelled operation
	 */
	public static boolean pauseQueryOne(String caption) {
		Scanner scanner = new Scanner(System.in);
		System.out.println();
		if (!caption.isEmpty()) {
			System.out.println(caption);
			System.out.println();
		}

		System.out.println(selectedNumber("1"));
		String line = getLineFromScanner(scanner);

		if (!line.equals("1")) {
			System.out.println("...cancelled");
			return false;
		}
		return true;
	}

	/**
	 * @param list it's created and no empty list of items, one of them will be
	 *             chosen;<br>
	 *             Number of item method writes, as index that in list
	 * @param log  may be null, otherwise be added to inform about working this
	 *             method, in console will be wrote obligatory
	 * @return correct number of list OR '-1'
	 */
	public static int pauseQueryList(List<String> list, List<String> log) {
		if (nullEmptyList(list)) {
			return -1;
		}
		Scanner scanner = new Scanner(System.in);
		var s = NEW_LINE_UNIX + selectedNumber("CHOOSED NUMBER (digit)");
		addLog(s, true, log);

		for (int i = 0; i < list.size(); i++) {
			s = i + ":  " + list.get(i);
			addLog(s, true, log);
		}
		String line = getLineFromScanner(scanner);
		int res;
		try {
			int x = Integer.parseInt(line);
			res = (x < 0 || x >= list.size()) ? -1 : x;
		} catch (NumberFormatException e) {
			res = -1;
		}
		if (res < 0) {
			s = "...cancelled";
		} else {
			s = "Chosen number: " + res;
		}
		addLog(s, true, log);
		return res;
	}

	@SuppressWarnings("resource")
	public static void pause(String caption) {
		System.out.println(); // need?
		if (!caption.isEmpty()) {
			System.out.println(caption.concat(NEW_LINE_UNIX));
		}
		System.out.println("Press ENTER to continue");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
	}

	/**
	 * Saves 'list' to 'path'; if 'list' is null/empty, will be created empty file
	 * 
	 * @param writeConsole        writes result of saving on console
	 * @param skipEmpty           0 (by default) save all lines;<br>
	 *                            1: empty lines will be skipped; <br>
	 *                            2: TRIMMED empty lines will be skipped
	 * @param deleteIfExistsMode  if 'path' file is exists:
	 *                            <p>
	 * 
	 *                            DeleteIfExists_ERROR (by default):error operation;
	 *                            <p>
	 * 
	 *                            DeleteIfExists_OLD_DELETE:deletes 'path' file
	 *                            before operation;
	 *                            <p>
	 * 
	 *                            DeleteIfExists_OLD_RENAME_TO_BAK:old file of
	 *                            'path' will be renamed with add '.bak';<br>
	 *                            WARNING: will be error if 'path' ends on'.bak';
	 * 
	 * @param path                full path, to saving 'list'
	 * @param pathDoubleForSaving if not null, in start this method, will be called
	 *                            recursively, with 'path' as
	 *                            'pathDoubleForSaving'<br>
	 * @param list                String list for saving to 'path'; if 'list' is
	 *                            null, will be saved empty file
	 * @return saving result for 'path' in 'list'
	 */
	synchronized public static boolean saveToFile(boolean writeConsole, int skipEmpty, int deleteIfExistsMode,
			Path path, Path pathDoubleForSaving, List<String> list) {
		if (pathDoubleForSaving != null) {
			saveToFile(writeConsole, skipEmpty, deleteIfExistsMode, pathDoubleForSaving, null, list);
		}
		if (skipEmpty < 0 || skipEmpty > 2) {
			skipEmpty = 0;
		}
		boolean bRes = false;
		if (deleteIfExistsMode != CopyMove.DeleteIfExists_ERROR
				&& deleteIfExistsMode != CopyMove.DeleteIfExists_OLD_DELETE
				&& deleteIfExistsMode != CopyMove.DeleteIfExists_OLD_RENAME_TO_BAK) {
			deleteIfExistsMode = CopyMove.DeleteIfExists_ERROR;
		}
		try { // try--finally
			if (path.toFile().exists()) {
				if (deleteIfExistsMode == 0) {
					System.out.println("<error, file exists>: " + path);
					return false;
				}

				try { // deleting
					if (deleteIfExistsMode == 2) {
						CopyMove.renameToBack(path); // 'path' not be exists after; or error
					} else {
						Files.delete(path);
					}
				} catch (Exception e) {
					if (writeConsole) {
						System.out.println("<error deleting of old exist file>: " + e.getMessage());
					}
					return false;
				}
			} else {
				try {
					CreateDirectoriesIfNeed(path);
				} catch (Exception e) {
					return false;
				}
			}

			try (var out = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				if (list != null) {
					for (var n : list) {
						if (skipEmpty > 0) {
							if (n.isEmpty() || (skipEmpty == 2 && n.trim().isEmpty())) {
								continue;
							}
						}
						out.write(n + NEW_LINE_UNIX);
					}
				}
				bRes = true;
				return true;
			} catch (Exception e) {
				if (writeConsole) {
					System.out.println("<error saving>: " + e.getMessage());
				}
				return false;
			}
		} finally {
			if (writeConsole) {
				String sv = (!bRes) ? "error save to" : "result saved to";
				System.out.println(sv + " file: " + path);
			}
		}
	}

	/**
	 * @param list if null/empty, return null
	 * @return array of string, size equals list.size, filled by items from 'list'
	 */
	synchronized public static String[] getArrayFromListOrNull(List<String> list) {
		if (nullEmptyList(list)) {
			return null;
		}

		String[] result = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	/**
	 * @param startIndex must be 0 or more, but no greater 'endIndex'
	 * @param endIndex   must be end list index (size-1) or less
	 * @param list       if null/empty, return null
	 * @return array of string, size equals list.size, filled by items from 'list'
	 */
	synchronized public static String[] getArrayFromListOrNullByIndexes(int startIndex, int endIndex,
			List<String> list) {
		if (nullEmptyList(list) || startIndex < 0 || endIndex >= list.size() || startIndex > endIndex) {
			return null;
		}

		String[] result = new String[endIndex - startIndex + 1];
		for (int i = startIndex; i <= endIndex; i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	/**
	 * @param toLowerCase if 'true', each element in 'list' will be set to result in
	 *                    lowerCase
	 * @param list        String list, not null, hash for empty items no created
	 * @return hash set from 'list'
	 */
	synchronized public static Set<String> getSetFromList(boolean toLowerCase, List<String> list) {
		Set<String> set = new HashSet<String>();
		for (var s : list) {
			if (s.isEmpty()) {
				continue;
			}
			set.add(toLowerCase ? s.toLowerCase() : s);
		}
		return set;
	}

	/**
	 * @param inLowerUpperCase if 1: each item will be set to lower case;<br>
	 *                         2: to UPPER CASE;<br>
	 *                         else (0) : no action
	 * @param path             where strings will be read from
	 * @return filling or empty (if error) set
	 */
	synchronized public static Set<String> getSetFromFileOrEmptySet(int inLowerUpperCase, Path path) {
		Set<String> set = new HashSet<String>();
		try {
			var list = readFile(2, inLowerUpperCase, path);
			if (!list.isEmpty()) {
				set.addAll(list);
			}
		} catch (Exception e) {
			set.clear();
		}
		return set;
	}

	/**
	 * @param needSortedList 0 (by default) no sort result list; 1: sort by string;
	 *                       2: sort by string in lower case
	 * @param list
	 * @return
	 */
	synchronized public static List<String> sortList(int needSortedList, List<String> list) {
		if (nullEmptyList(list)) {
			return list;
		}
		if (needSortedList == 1) {
			list.sort(null);
		} else if (needSortedList == 2) {
			list.sort(Comparator.comparing(String::toLowerCase));
		}
		return list;
	}

	/**
	 * @param needSortedList 0 (by default) no sort result list; 1: sort by string;
	 *                       2: sort by string in lower case
	 * @param set            not must be null
	 * @return
	 */
	synchronized public static List<String> getListFromSet(int needSortedList, Set<String> set) {
		List<String> list = new ArrayList<String>(set);
		return sortList(needSortedList, list);
	}

	/**
	 * @param needSortedList 0 (by default) no sort result list; 1: sort by string;
	 *                       2: sort by string in lower case
	 * @param set            not must be null
	 * @return
	 */
	synchronized public static List<String> getListFromPathsSet(int needSortedList, Set<Path> set) {
		List<String> list = new ArrayList<String>();
		for (var path : set) {
			list.add(path.toString());
		}
		return sortList(needSortedList, list);
	}

	/**
	 * @param needSortedList 0 (by default) no sort result list; 1: sort by string;
	 *                       2: sort by string in lower case
	 * @param paths          not must be null
	 * @return
	 */
	synchronized public static List<String> getListFromPathsList(int needSortedList, List<Path> paths) {
		List<String> list = new ArrayList<String>();
		for (var path : paths) {
			list.add(path.toString());
		}
		return sortList(needSortedList, list);
	}

	/**
	 * Creates hash map from no empty items in 'list' , items will be set to map in
	 * lower case, if defined 'toLowerCase';
	 * 
	 * @param toLowerCase if 'true', items in map will be set to lower case
	 * @param list        source string list, not null
	 * @return map in format "original string, number string in 'list'(last number,
	 *         if been several string)"
	 */
	synchronized public static Map<String, Integer> getMapFromList(boolean toLowerCase, List<String> list) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < list.size(); i++) {
			var s = list.get(i);
			if (s.isEmpty()) {
				continue;
			}
			map.put(toLowerCase ? s.toLowerCase() : s, i);
		}
		return map;
	}

	/**
	 * try start process in operation system for 'path'; that is open file/directory
	 * 
	 * @param waitEnter if 'true' required press 'Enter' before start process
	 * @param path      must be exists file/directory
	 */
	synchronized public static void startProcess(boolean waitEnter, Path path) {
		Desktop desktop = null;
		if (!Desktop.isDesktopSupported()) {
			return;
		}
		desktop = Desktop.getDesktop();
		try {
			if (waitEnter) {
				pause(NEW_LINE_UNIX + "will be open file: " + path);
			}
			desktop.open(path.toFile());
		} catch (Exception e) {
			System.out.println("error open process: " + path);
		}
	}

	/**
	 * 
	 * @return null if error or selected user directory
	 */
	synchronized public static Path getDestPathGUI(boolean inviteOnConsole) {
		JFileChooser chooser = new JFileChooser("/");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (inviteOnConsole) {
			System.out.println("Choose directory...");
		}
		int result = chooser.showDialog(null, "Choose directory");
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		try {
			var f = chooser.getSelectedFile().getCanonicalFile();
			if (!f.exists() || !f.isDirectory()) {
				return null;
			}
			return f.toPath();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Added File.separator to 's', IF NEED ONLY, that is, if is one, not be added;
	 * example: "\\s\\" returns without change
	 * 
	 * @param toStart -> added to start 's'
	 * @param toEnd   -> added to end 's'
	 * @param s       source string; if empty - returns File.separator IF DEFINED
	 *                'toStart' or 'toEnd'
	 * @return 's' with File.separator, if need; or 's' otherwise
	 */
	synchronized public static String fileSeparatorAddIfNeed(boolean toStart, boolean toEnd, String s) {
		return startOrEndAddIfNeed(File.separator, toStart, toEnd, s);
	}

	/**
	 * Added 'substr' to 's', IF NEED ONLY, means, if is one, will not be added;
	 * example: for 'toStart' and 'substr' == "a": "aAB" -> "aAB"; "AaB" -> "aAaB";
	 * 
	 * @param toStart -> added to start 's'
	 * @param toEnd   -> added to end 's'
	 * @param s       source string; if empty - returns 'substr' IF DEFINED
	 *                'toStart' or 'toEnd'
	 * @return 's' with 'substr', if need; or 's' otherwise
	 */
	synchronized public static String startOrEndAddIfNeed(String substr, boolean toStart, boolean toEnd, String s) {
		if (s.isEmpty()) {
			return (toStart || toEnd) ? substr : "";
		}
		if (substr.isEmpty()) {
			return s;
		}
		if (toStart) {
			if (!s.startsWith(substr)) {
				s = substr.concat(s);
			}
		}
		if (toEnd) {
			if (!s.endsWith(substr)) {
				s = s.concat(substr);
			}
		}
		return s;
	}

	/**
	 * Deletes ALL File.separator in 's'; "\\\\ab\\\\" -> "ab"
	 * 
	 * @param inStart -> deletes in start 's'
	 * @param inEnd   -> deletes in end 's'
	 * @param s       source string
	 * @return 's' with deleted File.separators on start/end
	 */
	synchronized public static String fileSeparatorDeleteAll(boolean inStart, boolean inEnd, String s) {
		return startOrEndDeleteAll(File.separator, inStart, inEnd, s);
	}

	/**
	 * Deletes ALL 'subStringToDelete' from 'source';<br>
	 * for 'subStringToDelete' == "a", "aaAB" -> "AB"
	 * 
	 * @param subStringToDelete
	 * @param inStart           -> deletes in start 'source'
	 * @param inEnd             -> deletes in end 'source'
	 * @param source            string to deleting 'subStringToDelete'
	 * @return 'source' with deleted 'subStringToDelete' on start or end; OR EMPTY
	 *         if error
	 */
	synchronized public static String startOrEndDeleteAll(String subStringToDelete, boolean inStart, boolean inEnd,
			String source) {
		if (nullEmptyString(source)) {
			return "";
		}

		if (nullEmptyString(subStringToDelete)) {
			return source;
		}

		int len = subStringToDelete.length();

		if (inStart) {
			while (source.startsWith(subStringToDelete)) {
				source = source.substring(len);
			}
			if (source.isEmpty()) {
				return "";
			}
		}
		if (inEnd) {
			while (source.endsWith(subStringToDelete)) {
				source = source.substring(0, source.length() - len);
			}
		}
		return source;
	}

	/**
	 * Deletes all chars, defined in 'charsToDelete' from 'source'; example, set
	 * 'a,b,c' and 'source' is 'ababccABC', result be 'ABC'
	 * 
	 * @param charsToDelete set of chars, to delete from start/end 'source'
	 * @param inStart       -> deletes in start 'source'
	 * @param inEnd         -> deletes in end 'source'
	 * @param source        source string
	 * @return 'source' with deleted chars on start or end; OR EMPTY if error
	 */
	synchronized public static String startOrEndDeleteAllChars(Set<Character> charsToDelete, boolean inStart,
			boolean inEnd, String source) {
		if (nullEmptyString(source)) {
			return "";
		}

		if (nullEmptySet(charsToDelete)) {
			return source;
		}

		if (inStart) {
			while (!source.isEmpty()) {
				char c = source.charAt(0);
				if (!charsToDelete.contains(c)) {
					break;
				}
				source = source.substring(1);
			}

			if (source.isEmpty()) {
				return "";
			}
		}

		if (inEnd) {
			while (!source.isEmpty()) {
				var endIndex = source.length() - 1;
				char c = source.charAt(endIndex);
				if (!charsToDelete.contains(c)) {
					break;
				}
				source = source.substring(0, endIndex);
			}
		}
		return source;
	}

	/**
	 * Saves 'list' and starts it in 'process', if 'needStartProcess' > 0;
	 * 
	 * @param doBackIfExists   if true and file of 'path' exists, old file will be
	 *                         moved with add '.bak'; if false, old file will be
	 *                         replaced
	 * @param needStartProcess 1:start,<br>
	 *                         2:start and wait press Enter; otherwise:no action,
	 *                         saving only; <br>
	 *                         3: confirm at start this method, may cancel saving
	 *                         and start; <br>
	 *                         0 (by default): save 'path' to 'list' only, with
	 *                         given 'doBackIfExists'
	 * @param path             where will be saved 'list', not must be null
	 * @param list             for saving, must be not null
	 */
	synchronized public static void saveAndShowList(boolean doBackIfExists, int needStartProcess, Path path,
			List<String> list) {
		if (needStartProcess == 3) {
			String infSizeList = (list == null) ? "null" : "" + list.size();
			if (!pauseQueryOne("Confirm saving information to file, then open this file:" + NEW_LINE_UNIX + path
					+ NEW_LINE_UNIX + "Count of string for saving: " + infSizeList)) {
				return;
			}
		}
		int deleteIfExistsMode = doBackIfExists ? CopyMove.DeleteIfExists_OLD_RENAME_TO_BAK
				: CopyMove.DeleteIfExists_OLD_DELETE;
		if (!saveToFile(true, 0, deleteIfExistsMode, path, null, list)) {
			return;
		}

		if (needStartProcess > 0) {
			startProcess(needStartProcess == 2, path);
		}
	}

	/**
	 * Try create directories to 'dest', if no need else
	 * 
	 * @param dest file or folder, be checked path to him
	 * @throws IOException if error
	 */
	public static void CreateDirectoriesIfNeed(Path dest) throws IOException {
		var parent = dest.getParent();
		if (parent != null && !parent.toFile().isDirectory()) {
			Files.createDirectories(parent);
		}
	}

	/**
	 * Each string from list, which length >= 3, will be converted to Path, then
	 * deleted.
	 * 
	 * @param needFileDirectory SIGN_FILE:must be file;<br>
	 *                          SIGN_FOLDER:must be directory; <br>
	 *                          else (SIGN_FILE_OR_FOLDER):no matter
	 * @param saveResultTo      if null: no saving 'log'; else 'log' will be trying
	 *                          save and show result
	 * @param list              paths of files, will be deleted
	 * @param log               filling by delete results; if null, will be created
	 * @return deleted count
	 */
	synchronized public static int deleteFiles(int needFileDirectory, Path saveResultTo, List<String> list,
			List<String> log) {
		List<File> fileList = getFileListOrNull(false, needFileDirectory, list);
		if (nullEmptyList(fileList)) {
			System.out.println("error: no found correct paths for deleting");
			return 0;
		}
		int deletedCount = 0;
		long deletedTotalSize = 0;

		if (log == null) {
			log = new ArrayList<String>();
		} else if (!log.isEmpty()) {
			addLog(ADDLOG_SEP, true, log);
		}
		addLog(ADDLOG_DATE, false, log);
		addLog("Will be DELETED, found in 'list', count: " + fileList.size(), true, log);
		System.out.println();
		for (var s : fileList) {
			addLog(s.toString(), true, log);
		}

		String queryResult = "";
		addLog(ADDLOG_SEP, true, log);
		addLog("start deleting..." + NEW_LINE_UNIX, true, log);

		CopyMove.setQueryCopyMove(CopyMove.QUERY_CONFIRM_LIST);
		Set<Integer> deletedNumbers = new HashSet<Integer>();

		for (int i = 0; i < fileList.size(); i++) {
			File f = fileList.get(i);
			addLog("try delete: " + f, true, log);
			if (CopyMove.queryCopyMoveDefined(true)) {
				System.out.println(NEW_LINE_UNIX + PRINTDELIMITER);

				queryResult = CopyMove.getQueryResult(NEW_LINE_UNIX + "CONFIRM DELETING, size: "
						+ bytesToKBMB(false, 2, f.length()) + "; file: " + f);

				if (!CopyMove.checkQueryResultYesOrYesToAll(queryResult)) {
					addLog("...cancelled by user: " + queryResult, true, log);
					if (CopyMove.getQueryCopyMove() == CopyMove.QUERY_NO_TO_ALL) {// queryResult.equals(CopyMove.CONFIRM_NO_TO_ALL)
						break;
					}
					continue;
				}
			}

			var len = f.isDirectory() ? 0 : f.length();
			boolean deleted = false;
			try {
				Files.delete(f.toPath());
				deleted = true;
				deletedCount++;
				deletedTotalSize += len;
				deletedNumbers.add(i);
			} catch (Exception e) {
			}

			addLog("=>result: " + deleted, true, log);
		}

		addLog(ADDLOG_SEP, true, log);
		addLog("Delete result: " + deletedCount + " of " + fileList.size() + ", total size of deleted: "
				+ bytesToKBMB(false, 0, deletedTotalSize), true, log);

		if (deletedCount < fileList.size()) {
			addLog(ADDLOG_SEP, false, log);
			addLog(">>> NO DELETED FILES: " + (fileList.size() - deletedCount), false, log);
			for (int i = 0; i < fileList.size(); i++) {
				if (!deletedNumbers.contains(i)) {
					addLog(fileList.get(i).toString(), false, log);
				}
			}
			addLog(ADDLOG_SEP, false, log);
		}

		if (saveResultTo != null) {
			saveAndShowList(false, 1, saveResultTo, log);
		}
		return deletedCount;
	}

	/**
	 * @param file there list of paths, for deleting
	 * @param log  if not null, will be filling; if null, will be created
	 * @return '-1' if error, otherwise count of deleted files
	 */
	synchronized public static int deleteFilesFromFile(File file, List<String> log) {
		if (!existsNotNullFile(true, file)) {
			return -1;
		}
		List<String> list = readFile(2, 0, file.toPath());
		Path saveResultTo = Path.of(file.toString().concat(".deleteResult.txt"));
		return deleteFiles(SIGN_FILE, saveResultTo, list, log);
	}

	/**
	 * @param writeOnConsoleIfFalse if 'true', and result of method == 'false', on
	 *                              console be written message about it
	 * @param file
	 * @return true, if file exists, and not directory, and length == 0
	 */
	private static boolean existsNotNullFile(boolean writeOnConsoleIfFalse, File file) {
		if (!file.exists() || file.isDirectory() || file.length() <= 0) {
			if (writeOnConsoleIfFalse) {
				System.out.println("file must exist, be not directory and be not empty: " + file);
			}
			return false;
		}
		return true;
	}

	/**
	 * @param needCanonicalFile if 'true', result file try transform to 'canonical'
	 *                          name, with correct symbols register
	 * @param needFileDirectory SIGN_FILE:must be file;<br>
	 *                          SIGN_FOLDER:must be directory;<br>
	 *                          else (SIGN_FILE_OR_FOLDER):no matter
	 * @param list              if null/empty, returns 'null';<br>
	 *                          from each string will be removed symbols after "*",
	 *                          "?", "\"", "<", ">", "|"; starts of string must be
	 *                          minimum 2 symbols, and will be attempt to convert to
	 *                          file
	 * @return list of existing file in absolute path
	 */
	private static List<File> getFileListOrNull(final boolean needCanonicalFile, final int needFileDirectory,
			List<String> list) {
		if (nullEmptyList(list)) {
			System.out.println("error: list of paths is empty");
			return null;
		}
		return list.stream().unordered()
				.map(fileString -> getCorrectFileOrNull(needCanonicalFile, 2, needFileDirectory, fileString))
				.filter(Objects::nonNull).toList();
	}

	/**
	 * Creates and checks existing file in absolute path (no canonical)
	 * 
	 * @param needCanonicalFile   if 'true', result file try transform to
	 *                            'canonical' name, with correct symbols register
	 * @param minLengthFileString minimal length 'fileString' for create file; if
	 *                            less than '1', will be set as '1'; recommended
	 *                            '2', example '/x' <br>
	 *                            or '4', example 'C:/x'
	 * @param needFileDirectory   SIGN_FILE:must be file; SIGN_FOLDER:must be
	 *                            directory; else (SIGN_FILE_OR_FOLDER):no matter
	 * @param fileString          string for create result file; must not be
	 *                            null/empty
	 * @return existing file path; or null
	 */
	synchronized public static File getCorrectFileOrNull(boolean needCanonicalFile, int minLengthFileString,
			final int needFileDirectory, String fileString) {
		try {
			fileString = removeEndStringAfterSpecificRestrictedWindowsChars(fileString);
			if (minLengthFileString < 1) {
				minLengthFileString = 1;
			}
			if (fileString.length() >= minLengthFileString) {
				File f = Path.of(fileString).toAbsolutePath().toFile();
				if (!f.exists()) {
					return null;
				}
				if (needCanonicalFile) {
					f = f.getCanonicalFile();
				}
				if (needFileDirectory == SIGN_FILE) {
					return f.isDirectory() ? null : f;
				}
				if (needFileDirectory == SIGN_FOLDER) {
					return f.isDirectory() ? f : null;
				}
				return f;
			} // 'else' no need
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * @param needCanonicalFile  if 'true', result file try transform to 'canonical'
	 *                           name, with correct symbols register
	 * @param needFileDirectory: SIGN_FILE:must be file;<br>
	 *                           SIGN_FOLDER:must be directory; <br>
	 *                           else (SIGN_FILE_OR_FOLDER):no matter
	 * @param file               must be existing file for reading path strings
	 * @return created (but may be empty) list of existing path or null
	 */
	synchronized public static List<File> getFileListFromFileOrNull(final boolean needCanonicalFile,
			final int needFileDirectory, File file) {
		if (!existsNotNullFile(true, file)) {
			return null;
		}
		List<String> list = readFile(2, 0, file.toPath());
		return getFileListOrNull(needCanonicalFile, needFileDirectory, list);
	}

	/**
	 * If 'source' ignore case ends on 'endSource', will be removed it, and added
	 * 'endDest'; Method can use for change extension if file name
	 * 
	 * @param source    string for change
	 * @param endSource 'source' must be ends on 'endSource', then it will be
	 *                  removed
	 * @param endDest   if removed 'endSource', will be added it
	 * @return 'source' with changed 'endSource' or empty if error
	 */
	synchronized public static String changeEndOfStringOrEmpty(String source, String endSource, String endDest) {
		if (nullEmptyString(source) || nullEmptyString(endSource) || nullEmptyString(endDest)) {
			return "";
		}

		var lenEndSource = endSource.length();
		if (source.length() < lenEndSource) {
			return "";
		}
		var posSeparator = source.length() - lenEndSource;
		if (!source.substring(posSeparator).equalsIgnoreCase(endSource)) {
			return "";
		}
		return source.substring(0, posSeparator).concat(endDest);
	}

	/**
	 * Writes information on console in specified format, between string delimiters,
	 * if defined
	 * 
	 * @param betweenStringDelimeters 0(by default): no string delimiters before and
	 *                                after info;<br>
	 *                                1:will be set delimiters as empty string; <br>
	 *                                2:will be set delimiters as PRINTDELIMITER,
	 *                                "-------"
	 * @param first                   must not be null/empty
	 * @param second                  if not null/empty, will be added as format
	 *                                ">>>_SET_'first'_:_'second'"
	 * @param appendString            if not null/empty, will be added at next
	 *                                string
	 * @param log                     if not null, all inform will be also added to
	 */
	synchronized public static void setInfo(int betweenStringDelimeters, String first, String second,
			String appendString, List<String> log) {
		if (first == null || first.isEmpty()) {
			return;
		}
		String result = second == null || second.isEmpty() ? first : first.concat(" : ").concat(second);
		addDelimiters(betweenStringDelimeters, log);
		addLog(">>> SET " + result, true, log);
		if (appendString != null && !appendString.isEmpty()) {
			addLog(appendString, true, log);
		}
		addDelimiters(betweenStringDelimeters, log);
	}

	private static void addDelimiters(int betweenStringDelimeters, List<String> log) {
		if (betweenStringDelimeters == 2) {
			addLog(ADDLOG_SEP, true, log);
		} else if (betweenStringDelimeters == 1) {
			addLog("", true, log);
		}
	}

	/**
	 * Sort string list
	 * 
	 * @param sortType  0 (by default):no sort list; 1:sort; 2: sort ignore case
	 * @param caption   if not null/empty, will be added as first item of 'list', in
	 *                  format: "==='caption'_(listSize)==="
	 * @param list      must not be null, be sorted
	 * @param listTotal if not null or equal to 'list', will be added 'list' to one
	 */
	synchronized public static void sortFillingList(int sortType, String caption, List<String> list,
			List<String> listTotal) {
		if (nullEmptyList(list)) {
			return;
		}
		if (sortType < 0 || sortType > 2) {
			sortType = 0;
		}
		var size = list.size();
		if (sortType != 0) {
			if (size > 1) {
				if (sortType == 2) {
					list.sort(Comparator.comparing(String::toLowerCase));
				} else {
					list.sort(null);
				}
			}
		}

		if (notNullEmptyString(caption)) {
			list.add(0, "===" + caption + " (" + size + ")===");
		}

		if (listTotal != null && listTotal != list) {
			listTotal.addAll(list);
		}
	}

	/**
	 * Extract root disk from 'sourceDisk' and creates string list from exists
	 * disks; if 'sourceFolder' defined, result list be filling that only
	 * 'disks/folder', which be found
	 * 
	 * @param excludeSourceDisk if 'true', in result list will not be added source
	 *                          disk/folder;<br>
	 *                          if 'false', to start result list will be added, if
	 *                          exists, source 'disk/folder';
	 * @param sourceDisk        may be disk or any folder, starts with need disk
	 * @param sourceFolder      if null/empty, result list will be filling disks
	 * @return not null string list of equal folders
	 * @throws IOException
	 */
	synchronized public static List<String> getEqualFoldersOnOtherDisks(boolean excludeSourceDisk, String sourceDisk,
			String sourceFolder) throws IOException {
		List<String> equalFolders = new ArrayList<>();
		sourceFolder = (nullEmptyString(sourceFolder)) ? "" : fileSeparatorDeleteAll(true, true, sourceFolder);

		sourceDisk = Path.of(sourceDisk).getRoot().toFile().toString().toUpperCase();
		if (nullEmptyString(sourceDisk)) {
			throw new IllegalArgumentException("source disk not must be null or empty");
		}

		sourceDisk = fileSeparatorAddIfNeed(false, true, sourceDisk);
		if (!excludeSourceDisk) {
			Path sourceFolderPath = (sourceFolder.isEmpty()) ? Path.of(sourceDisk)
					: Path.of(sourceDisk, sourceFolder).toAbsolutePath();

			if (sourceFolderPath.toFile().isDirectory()) {
				equalFolders.add(sourceFolderPath.toFile().getCanonicalFile().toString());
			} else {
				System.out.println("Source directory not found and not be added to result list: " + sourceFolderPath);
			}
		}

		for (File currentRoot : File.listRoots()) {
			if (currentRoot.toString().equalsIgnoreCase(sourceDisk)) {
				continue;
			}

			var cur = currentRoot.toString();
			if (sourceFolder.isEmpty()) {
				equalFolders.add(cur);
				continue;
			}

			File f = Path.of(cur, sourceFolder).toAbsolutePath().toFile();
			if (!f.isDirectory()) {
				continue;
			}
			equalFolders.add(f.getCanonicalFile().toString());
		}
		return equalFolders;
	}

	synchronized public static List<Map.Entry<String, Integer>> getSortedListFromMap(Map<String, Integer> map) {
		List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(map.entrySet());
		if (map.size() < 2) {
			return sortedList;
		}
		sortedList.sort((e1, e2) -> {
			var o1 = e1.getValue();
			var o2 = e2.getValue();
			if (o1 > o2)
				return -1;
			if (o1 < o2)
				return 1;
			return e1.getKey().compareTo(e2.getKey());
		});
		return sortedList;
	}

	/**
	 * Removes or replaces restricted symbols Windows / \ : * ? " < > |
	 * 
	 * @param type   0 (by default): checking only, returns 'source' or empty (if
	 *               found);
	 *               <p>
	 * 
	 *               1: removing: will be replaced all restricted symbols on "";
	 *               <p>
	 * 
	 *               2: replacing that symbols on " ";
	 *               <p>
	 * 
	 *               3: replacing that symbols on "-";
	 *               <p>
	 * 
	 *               4: replacing that symbols on "_";
	 * @param source not null string, for search restricted symbols
	 * @return for 'type'= 0 : 'source' as is or empty; for other - replaced string
	 */
	synchronized public static String removeRestrictedWindowsChars(int type, String source) {
		if (nullEmptyString(source)) {
			return "";
		}

		Set<Character> set = Set.of('/', '\\', ':', '*', '?', '\"', '<', '>', '|');

		if (type < 0 || type > 4) {
			type = 0;
		}

		String replaced = (type == 2) ? " " : (type == 3) ? "-" : (type == 4) ? "_" : "";

		for (char c : set) {
			if (source.isEmpty()) {
				return "";
			}

			if (source.indexOf(c) >= 0) {
				if (type == 0) {
					return "";
				}
				source = source.replace("" + c, replaced);
			}
		}
		return source;
	}

	/**
	 * Formats 'new name' to correct file name
	 * 
	 * @param limit   must be from 10 to 200 symbols; by default will be set 100
	 * @param newName must be not null/empty, must be without extension
	 * @return formatted string, or empty string, if error
	 */
	synchronized public static String getCorrectFileNameToRename(int limit, String newName) {
		if (nullEmptyString(newName)) {
			return "";
		}

		limit = (limit < 10 || limit > 200) ? 100 : limit;
		newName = removeRestrictedWindowsChars(1, newName);
		newName = trimWithDot(newName);

		if (newName.isEmpty()) {
			return "";
		}

		if (newName.length() > limit) {
			newName = newName.substring(0, limit);
			newName = trimWithDot(newName);
		}
		return newName;
	}

	private static String trimWithDot(String result) {
		result = result.trim();
		return startOrEndDeleteAllChars(Set.of(' ', '.', '\t'), true, true, result);
	}

	/**
	 * Method separates string from 'source' for creating Path; 'source' will be
	 * trimmed, then will be found from start 'source', specific restricted file
	 * name symbols: "*","?","\"","<",">","|"
	 * <p>
	 * 
	 * NB: symbols "/","\\",":" : no checked;
	 * 
	 * @param source string for creating Path, must contains after specific
	 *               restricted symbol, append info, as "D:/1.txt <appInfo>", then
	 *               that info will be removed
	 * @return trimmed 'source' with removed end after first found specific symbol
	 */
	synchronized public static String removeEndStringAfterSpecificRestrictedWindowsChars(String source) {
		source = source.trim();
		if (source.isEmpty()) {
			return "";
		}
		String[] arSymbols = { "*", "?", "\"", "<", ">", "|" };
		int minPos = source.length();
		for (int i = 0; i < arSymbols.length; i++) {
			var pos = source.indexOf(arSymbols[i]);
			if (pos >= 0 && pos < minPos) {
				minPos = pos;
			}
		}
		if (minPos < source.length()) {
			if (minPos <= 0) {
				return "";
			}
			source = source.substring(0, minPos).trim();
		}
		return source;
	}

	/**
	 * Creates list to string with specified format
	 * 
	 * @param limit     if less than 'list' size, will be append '...and other: ' +
	 *                  count not written 'list' items
	 * 
	 * @param prefix    if not null/empty, writes in start result
	 * @param postfix   if not null/empty, writes in end result
	 * @param separator if null/empty, will be set as ', '
	 * @param list      string list to write
	 * @return result string
	 */
	synchronized public static String ListToString(int limit, String prefix, String postfix, String separator,
			List<String> list) {
		var sb = new StringBuilder();
		appendNotNullEmpty(prefix, sb);
		if (notNullEmptyList(list)) {
			limit = limit < 0 ? 0 : Math.min(limit, list.size());
			separator = nullEmptyString(separator) ? ", " : separator;
			boolean beenApp = false;
			for (int i = 0; i < limit; i++) {
				if (beenApp) {
					sb.append(separator);
				}
				beenApp = appendNotNullEmpty(list.get(i), sb);
			}
		}
		appendNotNullEmpty(postfix, sb);
		var tail = list.size() - limit;
		if (tail > 0) {
			sb.append(" ...and other: ").append(tail);
		}
		return sb.toString();
	}

	/**
	 * Concatenates all lines in 'list' into one, using separator '\n'
	 * 
	 * @param list not null/empty string list
	 * @return empty if error; or text
	 */
	synchronized public static String listToStringOrEmpty(List<String> list) {
		if (nullEmptyList(list)) {
			return "";
		}
		var sb = new StringBuilder();
		final int lastIndex = list.size() - 1;

		for (int i = 0; i < list.size(); i++) {
			String s = list.get(i);
			if (notNullEmptyString(s)) {
				if (s.contains("\r")) {
					s = s.replace("\r", "");
				}
				if (s.contains("\n")) { // may be?
					s = s.replace("\n", "");
				}
				sb.append(s);
			}

			if (i < lastIndex) {
				sb.append(NEW_LINE_UNIX);
			}
		}
		return sb.toString();
	}

	synchronized public static List<String> stringToList(String s) {
		List<String> list = new ArrayList<String>();
		if (nullEmptyString(s)) {
			return list;
		}
		if (s.contains("\r")) {
			s = s.replace("\r", "");
		}

		boolean needAddEmpty = s.endsWith("\n");
		while (!s.isEmpty()) {
			var pos = s.indexOf('\n');
			if (pos < 0) {
				list.add(s);
				break;
			}
			String sub = pos < 0 ? s : s.substring(0, pos);
			list.add(sub);
			if (pos < 0) {
				break;
			}
			s = s.substring(pos + 1);
		}

		if (needAddEmpty) {
			list.add("");
		}
		return list;
	}

	/**
	 * @param code char code
	 * @return true, if code of char 'A'..'Z' or 'a'..'z'
	 */
	synchronized public static boolean englishLetterCharCode(int code) {
		return (code >= 65 && code <= 90) || (code >= 97 && code <= 122);
	}

	/**
	 * Checks start of 'pathString' on correct in OS Windows, but not network path
	 * names
	 * 
	 * @param pathString
	 * @return true, if 'pathString' starts with english letter, then ':'. If
	 *         'pathString' length more 2, next after ':' symbol, must be '\' or '/'
	 */
	synchronized public static boolean correctWindowsStartPath(String pathString) {
		if (pathString.length() < 2 && !englishLetterCharCode(pathString.codePointAt(0))
				&& pathString.charAt(1) != ':') {
			return false;
		}
		return pathString.length() == 2 ? true : pathString.charAt(2) == '/' || pathString.charAt(2) == '\\';
	}

	/**
	 * Set all items in array of boolean to equals values. Rules: if any item is
	 * 'true': all set in 'false'; else: all set 'true'
	 * 
	 * @param array array of boolean; if null/empty: error
	 * @return 0: all items set to 'false'; 1: all items set to 'true'; else (-1):
	 *         error
	 */
	synchronized public static int resetArrayBoolean(boolean[] array) {
		if (array == null || array.length == 0) {
			return -1;
		}
		boolean b = true;
		for (int i = 0; i < array.length; i++) {
			if (array[i]) {
				b = false;
				break;
			}
		}
		Arrays.fill(array, b);
		return b ? 1 : 0;
	}

	/**
	 * Generates random string of defined length, filled with digits. Result may
	 * starts with '0', but not '-'
	 * 
	 * @param length must be from 1 to 200; else will be set to minimal (1) /
	 *               maximal(200) border accordingly
	 * @return
	 */
	synchronized public static String getRandomDigitString(int length) {
		int min = 1;
		int max = 200;
		length = length < min ? min : length > max ? max : length;

		var random = new Random();
		String res = "";
		while (res.length() <= length) {
			int i = random.nextInt();
			res += String.valueOf(i < 0 ? -i : i);
		}
		return res.substring(1, length + 1); // means, result may starts with '0' sometimes
	}

	/**
	 * Creates 'number string' from 'value' with specified length; to result may be
	 * added prefix/postfix;
	 * 
	 * @param value   source integer, if less than 0, before 'number string' will be
	 *                added '-'
	 * @param length  required length (1..8) 'number string', if less than '1' will
	 *                be set as '1';<br>
	 *                if more than '8' will be set as '8';<br>
	 *                for example: value=42 and length=5, result 'number string'
	 *                will be '00042'
	 * @param prefix  if not null/empty, will be added before result
	 * @param postfix if not null/empty, will be added after result
	 * @return formatted string
	 */
	synchronized public static String formatInt(int value, int length, String prefix, String postfix) {
		var sb = new StringBuilder();
		length = length < 1 ? 1 : length > 8 ? 8 : length;
		appendNotNullEmpty(prefix, sb);
		if (value < 0 && value != Integer.MIN_VALUE) {
			sb.append("-");
			value = -value;
		}
		var s = String.valueOf(value);
		int diff = length - s.length();
		while (diff > 0) {
			sb.append("0");
			diff--;
		}
		sb.append(s);
		appendNotNullEmpty(postfix, sb);
		return sb.toString();
	}

	/**
	 * Extracts name from 'file', return array[2]:<br>
	 * index 0: name without extension;<br>
	 * index 1: extension with a leading dot, or an empty string if the extension is
	 * empty
	 * 
	 * @param file which name, extension will be extracted
	 * @return null if error; else filling array, 2 elements
	 */
	synchronized public static String[] extractFileNameExtensionOrNull(File file) {
		try {
			String[] ar = new String[2];
			var name = file.getName();
			if (nullEmptyString(name)) {
				return null;
			}
			var pos = name.lastIndexOf('.');
			ar[0] = pos < 0 ? name : name.substring(0, pos);
			ar[1] = pos < 0 ? "" : name.substring(pos);
			return ar;
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Add "item" to "list" if "item" is not equal to adjacent
	 * 
	 * @param toEnd if 'true' - try add to end; if 'false' - to start
	 * @param item
	 * @param list
	 * @return add result
	 */
	synchronized public static boolean addItemtoList(boolean toEnd, String item, List<String> list) {
		if (list == null || item == null) {
			return false;
		}

		if (list.isEmpty()) {
			list.add(item);
			return true;
		}

		var index = toEnd ? list.size() - 1 : 0;
		if (item.equals(list.get(index))) {
			return false;
		}

		if (toEnd) {
			list.add(item);
		} else {
			list.add(0, item);
		}

		return true;

	}

	synchronized public static String getSubstringFromIndexOrEmpty(String source, int index) {
		if (nullEmptyString(source) || index >= source.length()) {
			return "";
		}
		return source.substring(index);
	}

}
