package alexnick.filedatabase;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

import alexnick.CommonLib;
import alexnick.CopyMove;

import static alexnick.CommonLib.setInfo;
import static alexnick.CopyMove.*;

public class CompareFolders {
	private ArrayList<String> compareLog = new ArrayList<String>();
	private String sourceStartPathString; // set with '\' on end
	private Path sourceBinPath;
	private Path sourceStartPath;

	private String destStartPathString; // set with '\' on end
	private Path destBinPath;
	private Path destStartPath;

	private int copyMode;
	private int compareLogType;

	private boolean doBakToCopyMove;
	private Program program;

	/**
	 * @param program               class, not must be null
	 * @param compareLogType        0 (by default):new, newer and newerEqualSize in
	 *                              log; other lists in short format (no more limit,
	 *                              just like writes on console); 1: also be added
	 *                              before in full format, old and older; 2: also be
	 *                              added before in full format, equals and
	 *                              equalsSign;
	 * @param copyMode              0 (by default):no copying files; 1:copy by
	 *                              'backUp' method, 2: the same with offer creating
	 *                              new folder; 3: 'exchange' method, just
	 *                              'sourceStartPath' be changed on 'destStartPath';
	 *                              4: choose method;
	 * @param sourceStartPathString must be from exists Path;
	 * @param sourceBinPath         if 'null', be updated '*.bin' file; else: must
	 *                              be exists
	 * @param destStartPathString   must be from exists Path
	 * @param destBinPathFile       if 'null', be updated '*.bin' file; else: must
	 *                              be exists
	 * @throws Exception if any errors
	 */
//method called with  'copymode' > 0 -> for Const.MODE_STOP_FOUR (when 'bin created')
	public CompareFolders(Program program, int compareLogType, int copyMode, String sourceStartPathString,
			Path sourceBinPath, String destStartPathString, Path destBinPath) throws Exception {
		this.program = program;
		this.copyMode = copyMode < 0 || copyMode > 4 ? 0 : copyMode;
		this.compareLogType = compareLogType < 0 || compareLogType > 2 ? 0 : compareLogType;
		this.doBakToCopyMove = program.getOptions().contains(Const.OPTIONS_DO_BAK_TO_COPY_MOVE);
		// !!! for 'copyMode' == 0 -> no check exist start path
		Path[] paths = checkParametersOrThrow(sourceStartPathString, sourceBinPath);
		this.sourceStartPath = paths[0];
		this.sourceBinPath = paths[1];
		this.sourceStartPathString = CommonLib.fileSeparatorAddIfNeed(false, true, this.sourceStartPath.toString());

		paths = checkParametersOrThrow(destStartPathString, destBinPath);
		this.destStartPath = paths[0];
		this.destBinPath = paths[1];
		this.destStartPathString = CommonLib.fileSeparatorAddIfNeed(false, true, this.destStartPath.toString());
		boolean bNeedFullPaths = copyMode == 0; // copyMode == 0 when be click on button 'CompareTwoBin' in ViewTable
		if (bNeedFullPaths && program.getOptions().contains(Const.OPTIONS_COMPARETWOBIN_NOFULLPATHS)) {
			CommonLib.setInfo(2, "option", "CompareTwoBin NO_FULL_PATHS", null, null);
			bNeedFullPaths = false;
		}
		doCompareFolders(bNeedFullPaths);
	}

	// !!! for 'copyMode' == 0 -> no check exist start path; 'binPath' check only
	private Path[] checkParametersOrThrow(String startPathString, Path binPath) throws Exception {
		File binPathFile = null;
		File startPathFile = copyMode == 0 ? null
				: Path.of(startPathString).toAbsolutePath().toFile().getCanonicalFile();

		if (binPath == null && copyMode != 0) {
			if (!startPathFile.exists()) {
				throw new IllegalArgumentException("Path must exist " + startPathString);
			}

			// 'getUpdatedBinFile' writes start info on console
			binPathFile = program.getUpdatedBinFile(true, startPathFile);
			if (binPathFile == null) { // 'empty' is norm, be that copied
				System.out.println("ERROR OF UPDATE bin file, start path: " + startPathFile);
			}

		} else {
			binPathFile = binPath.toFile();
		}

		if (!binPathFile.exists()) {
			throw new IllegalArgumentException("Path of *.bin must exist " + binPathFile);
		}

		Path[] paths = new Path[2];
		paths[0] = (copyMode == 0) ? Path.of(startPathString) : startPathFile.toPath();
		paths[1] = binPathFile.toPath();

		if (paths[0] == null || paths[1] == null) {
			throw new IllegalArgumentException("Error of checking parameters for comparing");
		}

		return paths;
	}

	private void doCompareFolders(boolean bNeedFullPaths) { // arg trimmed, lowercase
		try {
			if (copyMode == 4) {
				var confirmList = new ArrayList<String>();
				confirmList.add("NO COPY");
				confirmList.add("'backUp method'");
				confirmList.add("'backUp method' with offer creating new folder");
				confirmList.add("'exchange method', 'source start path' be changed on 'dest start path");
				copyMode = CommonLib.pauseQueryList(confirmList, null);
				if (copyMode < 0 || copyMode > 3) {
					copyMode = 0;
				}
			}
			// !!! adding to 'compareLog': without '\n', need for correct line in 'contents'
			CommonLib.addLog("%date%", false, compareLog);
			CommonLib.addLog("Sourse info, folder: " + sourceStartPath + "; bin: " + sourceBinPath, false, compareLog);
			CommonLib.addLog("Destination info, folder: " + destStartPath + "; bin: " + destBinPath, false, compareLog);
			String type = copyMode == 3 ? "exchange" : copyMode == 0 ? "no copy" : "backUpCopy"; // for 1,2
			if (copyMode == 2) {
				type = type.concat(" with offer creanting new folder");
			}

			String logTypeInfo = CommonLib.NEW_LINE_UNIX + "Copy method: " + type;
			compareLog.add("");
			compareLog.add("Copy method: " + type);

			type = compareLogType == 2 ? "Full log" : compareLogType == 1 ? "Extended log" : "Standard log";

			logTypeInfo += CommonLib.NEW_LINE_UNIX + "Log format: " + type;
			compareLog.add("Log format: " + type);

			ArrayList<String> newList = new ArrayList<>();
			ArrayList<String> newerList = new ArrayList<>();
			ArrayList<String> oldList = new ArrayList<>();
			ArrayList<String> olderList = new ArrayList<>();
			ArrayList<String> equalList = new ArrayList<>();
			ArrayList<String> equalSignList = new ArrayList<>();

			String startSource = bNeedFullPaths ? sourceStartPathString : "";
			String startDest = bNeedFullPaths ? destStartPathString : "";

			if (!divideTwoBinOnSix(startSource, startDest, newList, newerList, oldList, olderList, equalList,
					equalSignList)) {
				CommonLib.addLog("%sep%", true, compareLog);
				CommonLib.addLog("error dividing *.bin lists ", true, compareLog);
				return;
			}

			var limit = 100;

			// compareLogType == 2, 'equalList', 'equalSignList' writes in full format
			List<String> contents = new ArrayList<String>();

			String caption = "equalList";
			var sizeForReturn = CommonLib.writeListToConsole(true, 0, limit, caption, compareLogType == 2, compareLog,
					equalList);
			contents.add(format(caption, sizeForReturn));

			// 'equalSignList' no sorting, there is formatting list
			caption = "equalSignList";
			sizeForReturn = CommonLib.writeListToConsole(false, 0, limit, "equalSignList", compareLogType == 2,
					compareLog, equalSignList);
			contents.add(format(caption, sizeForReturn));

			// compareLogType == 2 or 1, 'oldList', 'olderList' writes in full format
			caption = "oldList";
			sizeForReturn = CommonLib.writeListToConsole(true, 0, limit,
					"oldList : files is in 'destination' folder, but no found in 'source' folder", compareLogType > 0,
					compareLog, oldList);
			contents.add(format(caption, sizeForReturn));

			caption = "olderList";
			sizeForReturn = CommonLib.writeListToConsole(true, 0, limit,
					"olderList : files in 'destination' folder is older than in 'source' folder", compareLogType > 0,
					compareLog, olderList);
			contents.add(format(caption, sizeForReturn));

			List<MyBean> beans = copyMode > 0 ? new ArrayList<MyBean>() : null;
			// 'newList', 'newerList', 'newerListEqualSize' must be wrote in full format
			ArrayList<Path> extractPathNew = new ArrayList<>();

			caption = "newList";
			sizeForReturn = CommonLib.writeListToConsole(false, // is sorting in 'getTotalSizeSortAndGetPaths'
					getTotalSizeSortAndGetPaths(0, sourceStartPathString, "newList", newList, extractPathNew, beans),
					limit, "newList : files in 'source' folder no found in 'destination' folder", true, compareLog,
					newList);
			contents.add(format(caption, sizeForReturn));

			ArrayList<String> newerListEqualSize = new ArrayList<String>();

//from 'newerList' be extracted strings, that contains 'ProgrConst.EQUAL_SIZE';
//that is be in result, if in both lists: paths->equals and sizes->equals; but crc->noEquals and date->newer	
			divideNewerList(newerList, newerListEqualSize);

			ArrayList<Path> extractPathNewer = new ArrayList<>();

			caption = "newerList";
			sizeForReturn = CommonLib.writeListToConsole(false, // is sorting in 'getTotalSizeSortAndGetPaths'
					getTotalSizeSortAndGetPaths(1, sourceStartPathString, "newerList", newerList, extractPathNewer,
							beans),
					limit,
					"newerList : files in 'source' folder is newer than in 'destination' folder; EXCEPT EQUALS SIZE",
					true, compareLog, newerList);
			contents.add(format(caption, sizeForReturn));

			ArrayList<Path> extractPathNewerEqualSize = new ArrayList<>();

			caption = "newerList,equal_size";
			sizeForReturn = CommonLib.writeListToConsole(false, // is sorting in 'getTotalSizeSortAndGetPaths'
					getTotalSizeSortAndGetPaths(2, sourceStartPathString, "newerList,equal_size", newerListEqualSize,
							extractPathNewerEqualSize, beans),
					limit, "newerList,equal_size : extracted from 'newerList', files WITH EQUALS SIZE", true,
					compareLog, newerListEqualSize);
			contents.add(format(caption, sizeForReturn));

			var name = CommonLib.removeRestrictedWindowsChars(3, sourceStartPath.toString()) + Const.extensionLog;
			var pathLog = FileDataBase.getTempPath(name);
			if (pathLog == null) {
				pathLog = FileDataBase.getTempPath("compareFolders" + Const.extensionLog);
			}
			if (copyMode > 0) {
				if (backUpCopyFiles(logTypeInfo, pathLog, beans) <= 0) {
					// errors?
				} else {
					var s0 = "Updating destination *.bin, folder " + destStartPath;
					CommonLib.addLog(s0, true, compareLog);
					program.getUpdatedBinFile(true, destStartPath.toFile());
				}
			}

			if (compareLog != null && pathLog != null) {
				CommonLib.addLog("%sep%", false, compareLog);
				compareLog.add("Contents [list : string number in this file]:" + CommonLib.NEW_LINE_UNIX);
				compareLog.addAll(contents);
				CommonLib.addLog("%sep%", false, compareLog);
				CommonLib.addLog(CommonLib.NEW_LINE_UNIX + "comparing folders's finished", true, compareLog);

				// first parameter do 'true', to '.bak' create; copying's strange sometimes
				CommonLib.saveAndShowList(true, copyMode > 0 ? 3 : 1, pathLog, compareLog);
			}
			return;
		} catch (Exception e) {
			System.out.println(CommonLib.NEW_LINE_UNIX + "Error of comparing folder: " + e);
		}
		return;
	}

	private String format(String caption, int sizeForReturn) {
		return String.format("%30s : %d", caption, sizeForReturn);
	}

	private void divideNewerList(ArrayList<String> newerList, ArrayList<String> newerListEqualSize) {
		if (newerList.isEmpty()) {
			return;
		}
		ArrayList<String> tmp = new ArrayList<String>();
		for (var s : newerList) {
			if (s.contains(Const.EQUAL_SIZE)) {
				newerListEqualSize.add(s);
			} else {
				tmp.add(s);
			}
		}
		if (!newerListEqualSize.isEmpty()) {
			newerList.clear();
			newerList.addAll(tmp);
		}
	}

// been checked, what one as minimum of lists no empty returns -1, if cancelled;
// or count copied files; String sourceDisk, String destDisk,
	private int backUpCopyFiles(String logTypeInfo, Path pathLog, List<MyBean> beans) {
		if (copyMode <= 0 || beans == null) {
			return -1;
		}
		if (beans.isEmpty()) {
			CommonLib.addLog("no found new files for copying", true, compareLog);
			return -1;
		}

		if (beans.size() > 1) { // sort
			beans.sort(new Comparator<MyBean>() {
				@Override
				public int compare(MyBean o1, MyBean o2) {
//'countBinItems (serviceIntOne)': 0:newList; 1:newerList; 2: newerListEqualSize
					var c1 = o1.serviceIntOne;
					var c2 = o2.serviceIntOne;
					if (c1 != c2) {
						return c1 - c2;
					}
					return o1.getFourLowerCase(false, false).compareTo(o2.getFourLowerCase(false, false));
				}
			});
		}

		int countNew = 0;
		int countNewer = 0;
		int countNewerEqualSize = 0;
		for (var b : beans) {
			var c = b.serviceIntOne;
			if (c == 0) {
				countNew++;
			} else if (c == 1) {
				countNewer++;
			} else if (c == 2) {
				countNewerEqualSize++;
			} else {
				continue;
			}
			b.check = true;
		}

		String caption = "Check items for copying. Total count: " + beans.size() + getCountOrEmpty(countNew, "New list")
				+ getCountOrEmpty(countNewer, "Newer list")
				+ getCountOrEmpty(countNewerEqualSize, "Newer list equal size");

		System.out.println(logTypeInfo);
		CommonLib.setInfo(2, "path log", pathLog.toString(), null, null);

		FileDataBase.showFrameInfo("Compare table");
		var ft = new FutureTask<>(new CompareTable(countNew, countNewer, countNewerEqualSize, caption,
				destStartPath.toString(), null, beans));
		new Thread(ft).start();
		int copyCount = 0;
		int totalCountForCopy = 0;
		try {
			// <Path,Integer>,0:new; 1: newer; 2: newerEqualSize
			var map = ft.get();
			if (CommonLib.nullEmptyMap(map)) {
				CommonLib.addLog("no result for copying files", true, compareLog);
				return -1;
			}

//copyMode: 0: no copy; 1:backUpCopyMove method; 2:the same but offer new folder; 3:exchange method		
			Path newDestStartPath = (copyMode == 2) ? createNewFolder(destStartPath, compareLog) : destStartPath;

			int removeFromSourceForExchange = (copyMode == 3) ? sourceStartPathString.length() : 0;
			var bCM = new CopyMove(DeleteIfExists_ERROR, true, false);

			List<Path> list = getPathsFromMapOrEmpty(0, map);
			if (!list.isEmpty()) {
				totalCountForCopy = list.size();
				copyCount += bCM.backUpCopyMoveFiles(QUERY_CONFIRM_LIST, 3, true, "New files, copying",
						removeFromSourceForExchange, newDestStartPath, list, compareLog);
			}
			if (doBakToCopyMove) {
				setInfo(2, "mode", "OLD_EXISTS_RENAME_TO_BAK", null, null);
			}
// 1:replace old; 2: old be saved with 'bak'
			int deleteIfExistsMode = doBakToCopyMove ? DeleteIfExists_OLD_RENAME_TO_BAK : DeleteIfExists_OLD_DELETE;
			bCM.setDeleteIfExistsMode(deleteIfExistsMode);

			if (queryCopyMoveDefined(false)) { // that is, no been chosen 'NOT_FOR_ALL'
				list = getPathsFromMapOrEmpty(1, map);
				if (!list.isEmpty()) {
					totalCountForCopy += list.size();
					copyCount += bCM.backUpCopyMoveFiles(getQueryCopyMove(), 3, true, "Newer files, replace copying",
							removeFromSourceForExchange, newDestStartPath, list, compareLog);
				}
			}

			if (queryCopyMoveDefined(false)) { // that is, no been chosen 'NOT_FOR_ALL'
				list = getPathsFromMapOrEmpty(2, map);
				if (!list.isEmpty()) {
					totalCountForCopy += list.size();
					copyCount += bCM.backUpCopyMoveFiles(getQueryCopyMove(), 3, true,
							"Newer files EQUAL SIZE, replace copying", removeFromSourceForExchange, newDestStartPath,
							list, compareLog);
				}
			}
		} catch (Exception e) {
			CommonLib.addLog("error while backup copying files", true, compareLog);
		}

		CommonLib.addLog(CommonLib.NEW_LINE_UNIX + "Total copied files: " + copyCount + " from " + totalCountForCopy,
				true, compareLog);
		return copyCount;
	}

	private String getCountOrEmpty(int count, String caption) {
		return (count <= 0) ? "" : ". " + caption + ": " + count;
	}

	private List<Path> getPathsFromMapOrEmpty(int typeList, Map<Path, Integer> map) {
		List<Path> result = new ArrayList<Path>();
		for (Path path : map.keySet()) {
			int type = map.get(path);
			if (type == typeList) {
				result.add(path);
			}
		}
		return result;
	}

	private boolean divideTwoBinOnSix(String startSource, String startDest, ArrayList<String> newList,
			ArrayList<String> newerList, ArrayList<String> oldList, ArrayList<String> olderList,
			ArrayList<String> equalList, ArrayList<String> equalSignList) {
		var f1List = CommonLib.readFile(2, 0, sourceBinPath);
		var f2List = CommonLib.readFile(2, 0, destBinPath);
//need divide if empty...		if (f1List.isEmpty() || f2List.isEmpty()) {return false;}

		// get analog arrays with lower case
		String[] f1ArrLowerCase = fillArrayFromBin(f1List);
		String[] f2ArrLowerCase = fillArrayFromBin(f2List);

		System.out.println();
		System.out.println(CommonLib.PRINTDELIMITER);
		System.out.println("Comparing two *.bin files");
		System.out.println("First (new), size: " + f1List.size() + ", " + sourceBinPath);
		System.out.println("Second (old), size: " + f2List.size() + ", " + destBinPath);
		System.out.println(CommonLib.PRINTDELIMITER);

		for (int i = 0; i < f1List.size(); i++) {
			if (f1ArrLowerCase[i].isEmpty()) {
				continue;
			}
			for (int j = 0; j < f2List.size(); j++) {
				if (f2ArrLowerCase[j].isEmpty()) {
					continue;
				}
				// equals paths, in lower case, without 'startPath'
				if (f1ArrLowerCase[i].equals(f2ArrLowerCase[j])) {
					fillEqualsLists(startSource, startDest, f1List.get(i), f2List.get(j), newerList, olderList,
							equalList);
					f1ArrLowerCase[i] = "";
					f2ArrLowerCase[j] = "";
					break;
				}
			}
		}

		List<String> f1ListTrimmed = getListWithoutEmpty(f1ArrLowerCase, f1List);
		List<String> f2ListTrimmed = getListWithoutEmpty(f2ArrLowerCase, f2List);

		if (!f1ListTrimmed.isEmpty() && !f2ListTrimmed.isEmpty()) {
			extractDupSignatures(startSource, startDest, f1ListTrimmed, f2ListTrimmed, equalSignList);
		}
		ConverterBinFunc.fillBinList(startSource, "1", f1ListTrimmed, newList);
		ConverterBinFunc.fillBinList(startDest, "1", f2ListTrimmed, oldList);
		return true;
	}

// extract from no-empty sorted lists (new,old) equal signatures (size,crc) -> move it in equalSignList (created, empty)
	private void extractDupSignatures(String startSource, String startDest, List<String> fNew, List<String> fOld,
			List<String> equalSignList) {
		var fNewCur = 0;
		var sign = "";
		var isCaption = false;

		for (int i = 0; i < fOld.size(); i++) {
			var sOld = fOld.get(i);
			var pos = sOld.indexOf(')'); // aabbccdd(0) - minim pos must be more (10 + 1)
			if (pos <= 10) {
				continue;
			}

			if (isCaption && sign.equals(sOld.substring(0, pos + 1))) {
				equalSignList
						.add(ConverterBinFunc.getPathStringFromBinItem(null, startDest, sOld, "1", null, null, null));
				fOld.set(i, "");
				continue;
			}

			sign = sOld.substring(0, pos + 1);
			isCaption = false;
			for (int j = fNewCur; j < fNew.size(); j++) {
				var sNew = fNew.get(j);
				if (!sNew.startsWith(sign)) { // 'sign' not empty
					if (isCaption) {
						break;
					}
					continue;
				}
				if (!isCaption) { // service string must start in ' '
					isCaption = true;
					if (!equalSignList.isEmpty()) {
						equalSignList.add(" -----");
					}
					equalSignList.add(" ~signature: " + sign);
					equalSignList.add(" ~new list:");
				}
				equalSignList
						.add(ConverterBinFunc.getPathStringFromBinItem(null, startSource, sNew, "1", null, null, null));
				fNew.set(j, "");
				fNewCur = j + 1;
			}

			if (!isCaption) {
				continue;
			}

			equalSignList.add("");
			equalSignList.add(" ~old list:");
			equalSignList.add(ConverterBinFunc.getPathStringFromBinItem(null, startDest, sOld, "1", null, null, null));
			fOld.set(i, "");
		}
	}

// returns sorted, only no-empty lines from list to result, 'arr' and 'list' must have the same size
	private List<String> getListWithoutEmpty(String[] arr, List<String> list) throws IllegalArgumentException {
		if (arr.length != list.size()) {
			throw new IllegalArgumentException("error compare, no equals elements");
		}
		List<String> res = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			if (!arr[i].isEmpty() && !list.get(i).isEmpty()) {
				res.add(list.get(i));
			}
		}
		res.sort(null);
		return res;
	}

	// take two strings from bin, equals by full path, sorts to lists
	private void fillEqualsLists(String startSource, String startDest, String s1, String s2,
			ArrayList<String> newerList, ArrayList<String> olderList, ArrayList<String> equalList) {
		long[] arrDateSizeCrc1 = ConverterBinFunc.getDecodeDateSizeCrc(s1);
		long[] arrDateSizeCrc2 = ConverterBinFunc.getDecodeDateSizeCrc(s2);

		var sub = s2.substring(0, s2.indexOf('*'));
		if (arrDateSizeCrc1[0] == 0 || arrDateSizeCrc2[0] == 0 || sub.length() < 20) {
			equalList.add(
					"<error compare> " + ConverterBinFunc.getPathStringFromBinItem(null, "", s1, "", null, null, null));
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(Const.BRACE_START); // need for extract paths
		boolean bIsEqual = false;
		if (arrDateSizeCrc1[1] == arrDateSizeCrc2[1]) {
			sb.append(Const.EQUAL_SIZE); // ProgrConst.EQUAL_SIZE uses for extract 'equal sizes' from newer list
			if (arrDateSizeCrc1[2] == arrDateSizeCrc2[2]) {
				sb.append("; crc(").append(Long.toHexString(arrDateSizeCrc1[2])).append(")");
				bIsEqual = true;
			}
		} else {
			sb.append("diff bytes: ").append(arrDateSizeCrc1[1] - arrDateSizeCrc2[1]);
		}

		sb.append("; ");
		long diff = arrDateSizeCrc1[0] - arrDateSizeCrc2[0];
		long diffAbs = diff < 0 ? -diff : diff;

		// on flash disk, date modified without msec
		final int milliSecondRound = 2000;
		if (diffAbs < milliSecondRound) {
			sb.append("equal ");
		}
		sb.append("date, diff msec: ").append(diff);
		if (diffAbs >= 1000) { // more 'second'
			sb.append(",").append(CommonLib.getDifferentTime(diffAbs));
		}
		sb.append(Const.BRACE_END_WITH_SLASH_SPACE); // need for extract path
		sb.append(ConverterBinFunc.getPathStringFromBinItem(null, "", s1, sub, null, null, null));

		if (bIsEqual) {
			// refer to equals, if equals sizes and crc
			equalList.add(sb.toString());
		} else if (arrDateSizeCrc1[0] > arrDateSizeCrc2[0]) {
			newerList.add(startSource + sb.toString());
		} else {
			olderList.add(startDest + sb.toString());
		}
	}

// returns created string array in lower case from bin, size as list.size;
// list must be not null and no empty; that is, remains ends of '.bin' string with path
	private String[] fillArrayFromBin(List<String> list) {
		list.sort(null);
		String[] arr = new String[list.size()];
		Arrays.fill(arr, "");

		for (int i = 0; i < list.size(); i++) {
			String sub = ConverterBinFunc.getBinEndFromBinItemOrEmpty(true, list.get(i));
			if (!sub.isEmpty()) {
				arr[i] = sub;
			}
		}
		return arr;
	}

	// if 'beans' not null - be filling; 'numberList' need for filling 'beans'
	private long getTotalSizeSortAndGetPaths(int numberList, String startPath, String caption, ArrayList<String> list,
			ArrayList<Path> extractPath, List<MyBean> beans) {
		list.sort(null);
		if (copyMode == 0) {
			return 0; // no copying, no extracted; sort list only
		}
		extractPath.clear();
		long res = 0;
		String sub;
		for (var s : list) {
			if (s.isEmpty()) {
				continue;
			}

			String two_startSign = "";
			String three_diffSign = "";

			if (s.startsWith(Const.BRACE_START)) {
				var p2 = s.indexOf(Const.BRACE_END_WITH_SLASH_SPACE);
				if (p2 < 0) {
					continue;
				}
				p2 += Const.BRACE_END_WITH_SLASH_SPACE.length();
				two_startSign = s.substring(0, p2);
				sub = s.substring(p2);
			} else {
				sub = s;
			}

			var p = sub.indexOf(Const.BRACE_START);
			if (p >= 0) {
				three_diffSign = sub.substring(p);
				sub = sub.substring(0, p).trim();
			}
			try {
				Path path = Path.of(startPath, sub);
				if (path.toFile().exists() && !path.toFile().isDirectory()) { // check on 'directory' is redundant
					res += path.toFile().length();
					extractPath.add(path);
					if (beans != null) {
						var pathToString = path.toString();
						var pos = pathToString.lastIndexOf('.');
						var ext = (pos < 0) ? "" : pathToString.substring(pos);
						if (!ext.isEmpty()) {
							pathToString = pathToString.substring(0, pos);
						}
						var bean = new MyBean(caption, two_startSign, three_diffSign, pathToString, ext);
						bean.serviceIntOne = numberList;
						beans.add(bean);
					}
				}
			} catch (Exception e) {
			}
		}
		var s = caption + ", extracted paths: " + extractPath.size() + " from list size: " + list.size();
		CommonLib.addLog(s, true, compareLog);

		if (!extractPath.isEmpty() && compareLog != null) {
			compareLog.add("");
			for (var p : extractPath) {
				compareLog.add(p.toString());
			}
		}
		return res;
	}

}