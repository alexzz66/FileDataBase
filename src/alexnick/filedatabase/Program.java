package alexnick.filedatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.FutureTask;

import alexnick.CopyMove;

import static alexnick.CopyMove.*;
import static alexnick.CommonLib.*;
import static alexnick.filedatabase.ConverterBinFunc.*;

public class Program {
	private String options;
	Set<String> extsNeedListSet = new HashSet<String>();
	Set<String> extsNoNeedListSet = new HashSet<String>();
	// private String

	/**
	 * @param mode       must be defined as 'Const.MODE_ ...'
	 * @param options    defined in 'Const.OPTIONS_ ...', separator ';'
	 * @param parameters not null, may be empty, parameters of command line
	 * @throws Exception
	 */
	public Program(final int mode, final String options, List<String> parameters) throws Exception {
		if (SetInfoMode(mode) == Const.MODE_NO_DEFINED) {
			errorArgument("'mode' not defined");
		}

		var f = Path.of("").toFile().getCanonicalFile();
		var arrDiskAndBinFolder = checkAndCreateBinFolderOrNull(false, f);
		if (arrDiskAndBinFolder == null) {
			errorArgument("no defined disk of program");
		}
		if (FileDataBase.diskMain == null) {
			FileDataBase.diskMain = arrDiskAndBinFolder[0];
		}

		this.options = options;
		int needCalcID3 = readGlobalOptions(mode == Const.MODE_EXTRACT);

		if (!checkRepositoryPath(mode == Const.MODE_VIEW, this.options.contains(Const.OPTIONS_DOUBLE_REPO))) {
			errorArgument("Error checking repository and options directory on disk " + FileDataBase.diskMain);
		}

		if (mode == Const.MODE_EXTRACT) {// before 'isTemp'
			showExtract(parameters);
			return;
		}

		if (!FileDataBase.isTEMP) {
			fillPropertyList(FileDataBase.getPathPropertyExtsNeed(false), extsNeedListSet);
			fillPropertyList(FileDataBase.getPathPropertyExtsNoNeed(false), extsNoNeedListSet);
		}

		if (mode == Const.MODE_PATHSLIST) {
			initShowPathsListTable(parameters, null);
			return;
		}

		if (mode == Const.MODE_VIEW) {
			var showView = new InitShowViewTable(this);
			if (showView.getShowViewResult() == Const.MR_COPY_MOVE) {
				doCopyMoveNew();
			}
			return;
		}

		if (parameters.isEmpty()) {
			errorArgument("no found parameter for create *.bin");
		}

		createBin(needCalcID3, mode, parameters.get(0), null);
	}

	/**
	 * Checks binFolder and creates this, if not found; !!!WARNING: first calling on
	 * start program, before init global parameters
	 * 
	 * @param checkOnly need for updating all base, start checking
	 * @param folder    folder
	 * @return array of String, size==2, with correct 'disk' and 'binFolder'; 'null'
	 *         if error
	 */
	private String[] checkAndCreateBinFolderOrNull(boolean checkOnly, File folder) {
		try {
			var root = folder.toPath().getRoot();
			if (root == null) {
				return null;
			}
			final String disk = root.toString();
			if (disk.isEmpty()) { // may be?
				return null;
			}

			String diskSignPrefix = Path.of(disk, Const.binFolderStartSignature).toString().toLowerCase(); // disk+signature
			int prefLen = diskSignPrefix.length();
			var lst = Files.list(Path.of(disk)).filter(e -> e.toFile().isDirectory()).filter(
					e -> (e.toString().toLowerCase().startsWith(diskSignPrefix) && e.toString().length() != prefLen))
					.toList();
			if (lst.size() > 1) {
				return null;
			}

			Path diskBinFolder = (lst.size() == 1) ? lst.get(0) : null;
			if (diskBinFolder == null) {
				if (checkOnly) {
					return null;
				}

				Date d = new Date();
				long l = d.getTime();
				var f1 = Path.of(disk).toFile();
				var x = disk.concat(bytesToKBMB(false, 1, f1.getTotalSpace())).concat("-");
				x = x.replace(" ", "");
				if (x.contains(File.separator)) {
					x = x.replace(File.separator, "");
				}
				x = removeRestrictedWindowsChars(3, x);

				System.out.println(NEW_LINE_UNIX + "type description of new disk...");
				var inf = new InputTextGUI(null, false,
						"Type less 30 symbols, english letters, '_'(no start/end); or remain empty",
						"Input short label for disk " + disk);
				diskBinFolder = Path.of(disk,
						Const.binFolderStartSignature + x + Long.toHexString(l) + getPostfix(inf.result));
				Files.createDirectory(diskBinFolder);
			}

			if (diskBinFolder.toFile().isDirectory()) {
				var binFolder = diskBinFolder.toString().substring(disk.length());

				String[] arr = new String[2];
				arr[0] = disk;
				arr[1] = binFolder;
				return arr;
			}

		} catch (Exception e) {
		}
		return null;
	}

	private String getPostfix(String s) {
		if (nullEmptyString(s)) {
			return "";
		}
		final int MAX_LENGTH = 30;
		String result = "";
		for (int i = 0; i < s.length(); i++) {
			int code = s.codePointAt(i); // code == 95 >> '_'
			if (code == 95 || englishLetterCharCode(code)) {
				result += s.charAt(i);
				if (result.length() >= 200) {
					break;
				}
			}
		}

		while (result.contains("__")) {
			result = result.replace("__", "_");
		}

		if (result.length() > MAX_LENGTH) {
			result = result.substring(0, MAX_LENGTH);
		}

		result = startOrEndDeleteAll("_", true, true, result);

		return result.isEmpty() ? result : "-" + result;
	}

	/**
	 * @param needCalcID3    ID3_EXTRACT_NO: no action; ID3_EXTRACT_NEW:new ID3;
	 *                       ID3_EXTRACT_ALL:all ID3 for 'MP3'
	 * @param modeStop       MODE_ONE:*.txt list only; MODE_TWO:*.bin and *.dat
	 *                       list; MODE_THREE:after 'MODE_TWO',duplicate table;
	 *                       MODE_FOUR:after 'MODE_THREE',copyToFolder
	 * @param rowStartFolder string for try creating 'startFolder'
	 * @param pathForReturn  if not null, be filling path of *.bin
	 * @return true, if been created *.bin
	 * @throws IOException
	 */
	private boolean createBin(int needCalcID3, int modeStop, String rowStartFolder, File[] pathForReturn)
			throws Exception {
		if (Const.checkModeStop(modeStop) == Const.MODE_NO_DEFINED) {
			errorArgument("no defined 'mode' for creating *.bin");
		}

		File startFolderFile = getCorrectFileOrNull(true, 2, SIGN_FOLDER, rowStartFolder);
		if (startFolderFile == null) {
			errorArgument("not defined start folder for creating *.bin");
		}

		List<String> binInf = new ArrayList<String>();
		boolean resultSavingBin = false;
		var arrDiskAndBinFolder = checkAndCreateBinFolderOrNull(false, startFolderFile);
		if (arrDiskAndBinFolder == null) {
			System.out.println("Error checking searching path: " + startFolderFile);
			return false;
		}

		String binDisk = arrDiskAndBinFolder[0];
		String binFolder = arrDiskAndBinFolder[1];
		var binFolderPathToString = FileDataBase.getPathInRepo(true, binFolder, false).toString();
		var binFolderPathDouble = FileDataBase.getPathInRepo(true, binFolder, true);

		// 'nameForResultFiles' creates from start searching path, uses 'dirSeparator'
		var nameForResultFiles = getResultName(startFolderFile.toPath(), binFolder, binFolderPathToString);
		if (nameForResultFiles.isEmpty()) {
			System.out.println("Error creating folder name for results");
			return false;
		}

		if (FileDataBase.isTEMP) {
			setInfo(0, "repository", "~TMP~bin~data~repository", null, binInf);
		}

		if (FileDataBase.repositoryPathCurrent != null) {
			addLog("Current repository: " + FileDataBase.repositoryPathCurrent, true, binInf);
		}

		addLog("", true, binInf);
		binInf.add("<bin>");
		addLog(Const.ALIAS_DATE, false, binInf);
		addLog(ADDLOG_DATE, true, binInf);
		addLog(Const.ALIAS_START_SEARCH, false, binInf);
		addLog(startFolderFile.toString(), true, binInf);

		binInf.add(Const.ALIAS_FOUND_EXT);
		binInf.add("</bin>");

		Map<String, FileCntSize> hmExtsInfoIncluded = new HashMap<String, FileCntSize>();
		Map<String, FileCntSize> hmExtsInfoExcluded = new HashMap<String, FileCntSize>();

		List<File> listFullPaths = modeStop == Const.MODE_STOP_ONE ? new ArrayList<File>() : null;
		var listPathsToEndBin = getListPathsToEndBin(startFolderFile.toPath(), hmExtsInfoIncluded, hmExtsInfoExcluded,
				listFullPaths);

		var name = nameForResultFiles.concat(Const.extensionBinList);
		Path binPath = Path.of(binFolderPathToString, name);
		if (pathForReturn != null) {
			pathForReturn[0] = binPath.toFile();
		}

		Path binPathDouble = (binFolderPathDouble == null) ? null : Path.of(binFolderPathDouble.toString(), name);

		if (listPathsToEndBin.isEmpty()) { // not early this check -> need 'pathForReturn' init
			System.out.println("error: NO FOUND FILES in " + startFolderFile);
			if (binPath.toFile().exists()) {
				Files.delete(binPath);
			}

//need this?  if (binPathDouble != null && binPathDouble.toFile().exists()) {
//				Files.delete(binPathDouble);
//			  }

			return false;
		}

		if (!hmExtsInfoIncluded.isEmpty()) { // hmExtsInfo not must be empty
//there ExtensionFrame, may be System.exit(); all parameters except 'startPath', may be changed; there need 'ProgrConst.extsNeedList'
			if (!workWithHmExtsInfo(startFolderFile.toPath(), hmExtsInfoExcluded, hmExtsInfoIncluded, listPathsToEndBin,
					listFullPaths)) {
				return false;
			}
		}

		if (modeStop == Const.MODE_STOP_ONE) {
			confirmAndSavingSearchResults(startFolderFile.toString(), nameForResultFiles, listPathsToEndBin,
					listFullPaths);
			return false;
		}

//!!! included BEFORE excluded, need for 'view mode' for parse '*.dat'
		if (!hmExtsInfoIncluded.isEmpty()) {
			ArrayList<Map.Entry<String, FileCntSize>> sortedHmExtsList = FileDataBase
					.getSortedHmExt(hmExtsInfoIncluded);
			addLog(ADDLOG_SEP, true, binInf);
			addBinInfFromHmExtsList("Found", Const.ALIAS_FOUND_FILES, sortedHmExtsList, binInf);
		}

		if (!hmExtsInfoExcluded.isEmpty()) {
			ArrayList<Map.Entry<String, FileCntSize>> sortedHmExtsList = FileDataBase
					.getSortedHmExt(hmExtsInfoExcluded);
			addLog(ADDLOG_SEP, true, binInf);
			addBinInfFromHmExtsList("Excluded", "Total excluded files:", sortedHmExtsList, binInf);
		}

// ==============BIN START==================
		System.out.println(NEW_LINE_UNIX + "start of creating *.bin...");
		var binCreator = new BinCreator(needCalcID3, startFolderFile.toPath(), binPath, listPathsToEndBin);
		List<String> binList = binCreator.getBinList();

		if (needCalcID3 != Const.ID3_EXTRACT_NO) {
			FileDataBase.saveID3Property();
		}

		int noFoundNewFiles = binList.isEmpty() ? 1 : binList.get(0).equals(Const.EXTEMPTY) ? 2 : 0;
		if (noFoundNewFiles != 0) {
			System.out.println("NO FOUND NEW FILES for updating '*.bin' " + binPath);
			if (noFoundNewFiles == 2) {
				binList.remove(0);
			}
			if ((modeStop == Const.MODE_STOP_ONE || modeStop == Const.MODE_STOP_TWO) || binList.isEmpty()) {
				return false;
			}
		}

		System.out.println("sorting...");
		binList.sort(null);
		if (noFoundNewFiles == 0) {
			System.out.println("saving...");
			resultSavingBin = saveToFile(true, 1, DeleteIfExists_OLD_DELETE, binPath, binPathDouble, binList);
		}

		System.out.println("adding duplicates bin...");
		List<MyBean> duplicatesBeans = new ArrayList<MyBean>();

		// there be filling 'duplicatesBeans'
		int[] groupsCountForReturn = new int[1];
		Arrays.fill(groupsCountForReturn, 0);
		binInf.addAll(
				addDuplicateInfToBinInf(startFolderFile.toString(), binList, groupsCountForReturn, duplicatesBeans));

		name = nameForResultFiles.concat(Const.extensionBinData);
		Path binInfPath = Path.of(binFolderPathToString, name);
		Path binInfPathDouble = (binFolderPathDouble == null) ? null : Path.of(binFolderPathDouble.toString(), name);

		if (noFoundNewFiles == 0 || !binInfPath.toFile().exists()) {
			saveToFile(true, 0, CopyMove.DeleteIfExists_OLD_DELETE, binInfPath, binInfPathDouble, binInf);
		}
// ==============BIN END==================

		if (modeStop == Const.MODE_STOP_ONE || modeStop == Const.MODE_STOP_TWO) {
			return resultSavingBin;
		}

		// show duplicate window
		if (!duplicatesBeans.isEmpty()) {
			name = nameForResultFiles.concat(Const.extensionTxt);
			// for saving near '.bin' and '.dat' -> Path.of(binFolderPathToString, name)
			int deletedCount = ShowDuplicatesFrame(groupsCountForReturn[0], FileDataBase.getTempPath(name),
					duplicatesBeans);
			if (deletedCount > 0) { // need update *.bin
				addLog(ADDLOG_SEP, true, null);
				System.out.println("updating *.bin after delete files...");
				createBin(0, Const.MODE_STOP_TWO, startFolderFile.toString(), null);
			}
		}

		if (modeStop == Const.MODE_STOP_THREE) {
			return resultSavingBin;
		}

		addLog(ADDLOG_SEP, true, null);

//for '4' comparing with the same 'startpath.bin', but created on other disk		
		System.out.println("Choose variant of 'comparing mode'." + NEW_LINE_UNIX
				+ "If 'SELECT FOLDER', be need choose method copying;" + NEW_LINE_UNIX
				+ "otherwise be set 'exchange' copying method");
		List<String> list = new ArrayList<String>();
		list.add(
				"'Standard log': 'newList', 'newerList', 'newerEqualSizeList' be in full format; all others list - in short format");// 0
		list.add("'Extended log': 'oldList', 'olderList' also be in full format");// 1
		list.add("'Full log': all lists in full format"); // 2
		list.add("SELECT FOLDER, 'standard log'"); // 3
		list.add("SELECT FOLDER, 'extended log'"); // 4
		list.add("SELECT FOLDER, 'full log'"); // 5
		list.add("cancel: select something other than 0..5");

		int compareLogType = pauseQueryList(list, null);
		if (compareLogType < 0 || compareLogType > 5) {
			return resultSavingBin;
		}
		compareFolders(compareLogType, binDisk, startFolderFile.toString(), binPath);
		return resultSavingBin;
	}

	// calls if created and no empty 'duplicatesPath', for shows it in table for
	// deleting, returns count of delete files
	// 'duplicatesBeans' be changed - be checked items for deleting
	private int ShowDuplicatesFrame(int groupsCount, Path saveResultTo, List<MyBean> duplicatesBeans) {
		int deletedCount = 0;
		FileDataBase.showFrameInfo("Duplicate table");
		var ft = new FutureTask<>(new DuplicateTable(null, "Checking items will be deleted. Groups count: "
				+ groupsCount + "; total count: " + duplicatesBeans.size(), duplicatesBeans));
		new Thread(ft).start();
		ArrayList<String> delList = new ArrayList<String>();
		try {
			var list = ft.get();
			if (nullEmptyList(list)) {
				addLog("no result for choose files", true, null);
			} else {
				if (duplicatesBeans != null) {
					delList.add("===ALL DUBLICATES INFO:===");
					for (var d : duplicatesBeans) {
						var sb = new StringBuilder();
						sb.append(d.getTwo()).append(" (").append(d.getOne()).append(") ")
								.append(d.getFour(true, true));
						if (d.check) {
							sb.append(" <CHECKED FOR DELETING>");
						}
						delList.add(sb.toString());
					}
				}
				if (list.isEmpty()) {
					addLog("no found files for deleting", true, delList);
				} else {
					deletedCount = deleteFiles(null, list, delList);
				}
			}
		} catch (Exception e) {
			addLog("Error of define duplicate paths", true, delList);
		} finally {
			if (!delList.isEmpty()) {
				saveAndShowList(true, 1, saveResultTo, delList);
			}
		}
		return deletedCount;
	}

	// 'duplicatesBeans' must be created; will be empty and returns filling;
	private List<String> addDuplicateInfToBinInf(String startPath, List<String> bin, int[] groupsCountForReturn,
			List<MyBean> duplicatesBeans) {
		// example 00000c66(2329d167)17db1319c5c*<1\new>txt
		duplicatesBeans.clear();

		final String SEP = NEW_LINE_UNIX + "-------------" + NEW_LINE_UNIX;
		List<String> dupInfoList = new ArrayList<>();
		List<String> dupGroupPathsList = new ArrayList<>();
		startPath = fileSeparatorAddIfNeed(false, true, startPath);
		int group = 0;
		String subPrev = "";
		boolean bNew = true;

		for (int i = 0; i < bin.size(); i++) {
			String s = bin.get(i);
			if (s.isEmpty() || s.startsWith("00000000"))
				continue;
			var p1 = s.indexOf('(');
			var p2 = s.indexOf(')');
			if (p1 < 8 || p2 < 10 || (p2 - p1) < 2 || ((p2 - p1) == 2 && s.indexOf(p2 - 1) == '0'))
				continue;
			String sub = s.substring(0, p2);
			if (sub.equals(subPrev)) {
				if (bNew) {
					String t = sub.substring(0, p1);
					try {
						t = ",  " + bytesToKBMB(false, 0, Long.valueOf(t, 16));
					} catch (Exception e) {
						t = "";
					}
					if (group == 0) {
						dupInfoList.add(SEP + "Found duplicates by crc:" + SEP);
					}
					group++;
					dupInfoList.add("Group: " + group + t);
					String prev = bin.get(i - 1);
					dupInfoList.add(prev);
					// if returns empty?
					dupGroupPathsList.add(NEW_LINE_UNIX
							+ getPathStringFromBinItem(null, startPath, prev, "", null, null, duplicatesBeans));
					bNew = false;
				}
				dupInfoList.add(s);
				dupGroupPathsList.add(getPathStringFromBinItem(null, startPath, s, "", null, null, duplicatesBeans));
			} else {
				if (!bNew) {
					dupInfoList.addAll(dupGroupPathsList);
					dupGroupPathsList.clear();
					dupInfoList.add(SEP);
					bNew = true;
				}
				subPrev = sub;
			}
		}

		if (!dupGroupPathsList.isEmpty()) {
			dupInfoList.addAll(dupGroupPathsList);
		}
		if (groupsCountForReturn != null && groupsCountForReturn.length > 0) {
			groupsCountForReturn[0] = group;
		}
		return dupInfoList;
	}

	// 'startPath' may be disk, may be with subfolders, may ends on File.separator
	// method called on 'Const.MODE_STOP_FOUR' in 'create bin'
	void compareFolders(int compareLogType, String binDisk, String startPath, Path binPath) throws Exception {
		int copyMode = compareLogType <= 2 ? 3 : 4;
		if (compareLogType < 0 || compareLogType > 5) {
			compareLogType = 0;
		}
		String destStartPathString = "";
		if (compareLogType <= 2) { // 0,1,2
			String sourceFolder = binDisk.length() >= startPath.length() ? "" : startPath.substring(binDisk.length());
			List<String> equalFolders = getEqualFoldersOnOtherDisks(true, binDisk, sourceFolder);

			if (equalFolders.isEmpty()) {
				System.out.println("no found equals folders on other disks for " + startPath);
				return;
			}
			// choosing destination directory
			System.out.println("Current updated folder: " + startPath);
			var choosed = pauseQueryList(equalFolders, null);
			if (choosed < 0) {
				return;
			}
			destStartPathString = equalFolders.get(choosed);
		} else { // 3,4,5
			compareLogType -= 3;
			Path destFolder = getDestPathGUI(true);
			if (destFolder == null) {
				return;
			}
			destStartPathString = destFolder.toString();
		}

		if (!destStartPathString.isEmpty()) {
			new CompareFolders(this, compareLogType, copyMode, startPath, binPath, destStartPathString, null, false);
		}
	}

	// 'binInf' must be created
	private void addBinInfFromHmExtsList(String prefix, String alias,
			ArrayList<Map.Entry<String, FileCntSize>> sortedHmExtsList, List<String> binInf) {
		addLog(prefix + " extensions, sorted by count and total size", true, binInf);// this_string_must_be_any,but_no_empty
		long sz = 0;
		long count = 0;
		for (var e : sortedHmExtsList) {
			var v = e.getValue();
			var sz0 = v.getSize();
			sz += sz0;
			var count0 = v.getCount();
			count += count0;
			var sb0 = new StringBuilder();
			sb0.append(e.getKey()).append(Const.extSeparator).append(count0).append(",  ")
					.append(bytesToKBMB(false, 0, sz0));
			addLog(sb0.toString(), true, binInf);
		}

		var sb = new StringBuilder();
		sb.append(NEW_LINE_UNIX).append(alias).append(NEW_LINE_UNIX).append(count).append(", ")
				.append(bytesToKBMB(false, 0, sz)).append(" [");

		int x = 0;
		for (var e : sortedHmExtsList) {
			if (++x > 30) {
				break;
			}
			if (x > 1) {
				sb.append(";");
			}
			sb.append(e.getKey());
		}
		sb.append("]");
		addLog(sb.toString(), true, binInf);
	}

// 'listFullPaths' must be null, or contains the same items as 'listfiles', in full path format
	private boolean workWithHmExtsInfo(Path startPath, Map<String, FileCntSize> hmExtsInfoExcluded,
			Map<String, FileCntSize> hmExtsInfo, List<String> listFiles, List<File> listFullPaths) {
		List<String> extNewWithCount = getExtNewWithCount(hmExtsInfo);
		List<String> noNeedExts = null;

		if (!extNewWithCount.isEmpty()) {
			try {
				FileDataBase.showFrameInfo("Extension frame");
				var ft = new FutureTask<>(
						new ExtensionFrame(null, false, extNewWithCount, extsNeedListSet, extsNoNeedListSet));
				new Thread(ft).start();
				noNeedExts = ft.get();
			} catch (Exception e) {
				System.out.println("Error show frame of extensions: " + e.getMessage());
				System.exit(1);
			}
		}

		if (notNullEmptyList(noNeedExts)) { // unchecked any in 'extNew' and saved
			var keys = hmExtsInfo.keySet();
			if (keys.size() == noNeedExts.size()) {
				System.out.println("UNCHECKED ALL FOUND EXTENSIONS, no found results");
				return false;
			}
			for (var ext : noNeedExts) {
				if (!keys.contains(ext)) {
					continue;
				}
				var fcs = hmExtsInfo.get(ext);
				hmExtsInfoExcluded.put(ext, fcs);
				hmExtsInfo.put(ext, null);

				var sFin = ext.equals(Const.EXTEMPTY) ? Const.BRACE_END : Const.BRACE_END + ext;
				for (int x = 0; x < listFiles.size(); x++) {
					var s = listFiles.get(x);
					if (!s.isEmpty() && s.endsWith(sFin)) { // 'sFin' not empty
						listFiles.set(x, "");
						if (listFullPaths != null && listFullPaths.size() == listFiles.size()) {
							listFullPaths.set(x, null);
						}
					}
				}
			}

			Map<String, FileCntSize> tmp_hmExtsInfo = new HashMap<String, FileCntSize>();

			for (var s : hmExtsInfo.keySet()) {
				if (hmExtsInfo.get(s) != null) {
					tmp_hmExtsInfo.put(s, hmExtsInfo.get(s));
				}
			}
			hmExtsInfo.clear();
			hmExtsInfo.putAll(tmp_hmExtsInfo);

			listFiles.removeIf(String::isEmpty);
			if (listFullPaths != null) {
				listFullPaths.removeIf(path -> path == null);
			}
			if (listFiles.isEmpty()) { // may be?
				System.out.println("error: NO FOUND FILES in " + startPath + " after unchecking extensions");
				return false;
			}
		}
		return true;
	}

	private List<String> getExtNewWithCount(Map<String, FileCntSize> hmExtsInfo) {
		List<String> list = new ArrayList<>();
		List<Map.Entry<String, FileCntSize>> sortedHmExtsList = FileDataBase.getSortedHmExt(hmExtsInfo);
		for (var e : sortedHmExtsList) {
			if (extsNeedListSet.contains(e.getKey())) {
				continue;
			}
			var v = e.getValue();
			var sb = new StringBuilder();
			sb.append(e.getKey()).append(Const.extSeparator).append(v.getCount()).append(" [")
					.append(bytesToKBMB(false, 2, v.getSize())).append("]");
			list.add(sb.toString());
		}
		return list;
	}

	/**
	 * @param startPath          must be exists and directory, and in 'ABSOLUTEPATH'
	 * @param hmExtsInfoIncluded result info about result extensions, may be null or
	 *                           create if need it
	 * @param hmExtsInfoExcluded the same as 'hmExtsInfoIncluded' for excluded
	 *                           extensions
	 * @param extsNeed           set of need extensions (without '.', in lower
	 *                           case), may be null
	 * @param extsNONeed         set of no need extensions, may be null
	 * @param listFullPaths      if not 'null', be filling full paths of result list
	 * @return list of files path
	 * @throws IOException
	 */
	private List<String> getListPathsToEndBin(Path startPath, Map<String, FileCntSize> hmExtsInfoIncluded,
			Map<String, FileCntSize> hmExtsInfoExcluded, List<File> listFullPaths) throws IOException {
		if (!startPath.toFile().isDirectory()) {
			throw new IllegalArgumentException("Path must be 'directory': " + startPath);
		}

		var s = startPath.toString();
		var len = s.endsWith(File.separator) ? s.length() : s.length() + File.separator.length();

		var ff = new FindFiles(listFullPaths != null, len, hmExtsInfoIncluded != null, hmExtsInfoExcluded != null, null,
				extsNoNeedListSet);
		Files.walkFileTree(startPath, ff);
		if (hmExtsInfoIncluded != null) {
			hmExtsInfoIncluded.clear();
			hmExtsInfoIncluded.putAll(ff.getHmExtsInfo());
		}

		if (hmExtsInfoExcluded != null) {
			hmExtsInfoExcluded.clear();
			hmExtsInfoExcluded.putAll(ff.getHmExtsInfoExcluded());
		}
		if (listFullPaths != null) {
			listFullPaths.addAll(ff.getListFullPaths());
		}
		return ff.getListPathsToEndBin();
	}

// returns name, which starts result files of empty if error
// 'binFolderPathToString' need only for checking trying 'full disk base' that is, if bin-file exists, no confirm
	private String getResultName(Path startPath, String binFolder, String binFolderPathToString) {
		var startPathString = fileSeparatorAddIfNeed(false, true, startPath.toString());

		if (startPathString.contains(Const.dirSeparator)) {
			System.out.println("error: contains " + Const.dirSeparator);
			return "";
		}

		var startPathRoot = startPath.getRoot();
		if (startPathRoot == null) {
			System.out.println("error root for " + startPathString);
			return "";
		}
		var startPathRootString = fileSeparatorAddIfNeed(false, true, startPathRoot.toString());
		String s = startPathString.substring(startPathRootString.length());
		if (s.isEmpty()) {
			var n = Const.dirSeparator.concat(Const.extensionBinList);
			Path binPath = Path.of(binFolderPathToString, n);

			if (binPath.toFile().exists() || pauseQueryOne("Do you want do base of FULL DISK? " + startPath)) {
				return Const.dirSeparator;// full disk
			}
		}

		s = fileSeparatorDeleteAll(true, true, s);
		if (s.isEmpty()) {
			return "";
		}
		if (s.contains(File.separator)) {
			s = s.replace(File.separator, Const.dirSeparator);
		}
		return s;
	}

	// all parameters is correct, 'listFullPaths' must be created, but check one
	private void confirmAndSavingSearchResults(String startPath, String nameForResultFiles,
			List<String> listPathsToEndBin, List<File> listFullPaths) {
		if (nullEmptyString(nameForResultFiles) || nullEmptyList(listPathsToEndBin) || nullEmptyList(listFullPaths)) {
			return;
		}

		List<String> list = new ArrayList<>();
		var confirm = options.contains(Const.OPTIONS_ONE_PATHSLIST_YES) ? 3 : -1;

		if (confirm != 3) {
			list.add("temporary list of '*.bin', count: " + listPathsToEndBin.size()); // 0
			list.add("full paths list, count: " + listFullPaths.size());// 1
			list.add("BOTH LISTS TO ONE FILE");// 2
			if (!listFullPaths.isEmpty()) {
				list.add("Set mode '" + Const.PATHSLIST_NAME + "' for full path list"); // 3
			}
			confirm = pauseQueryList(list, null);
		}

		if (confirm == 3) {
			initShowPathsListTable(null, listFullPaths);
			return;
		} else if (confirm < 0 || confirm > 3) {
			return;
		}

		list.clear();
		startPath = nullEmptyString(startPath) ? "" : fileSeparatorAddIfNeed(false, true, startPath);
		addLog(ADDLOG_DATE, false, list);
		addLog("Start path: " + startPath, false, list);
		addLog("Size row / result lists: " + listPathsToEndBin.size() + " / " + listFullPaths.size(), false, list);
		addLog(ADDLOG_SEP, false, list);
		boolean needListPathsToEndBin = confirm == 0 || confirm == 2;
		boolean needListFullPaths = confirm == 1 || confirm == 2;

		if (needListPathsToEndBin) {
			list.addAll(listPathsToEndBin);
		}

		if (needListFullPaths) {
			if (needListPathsToEndBin) {
				addLog(ADDLOG_SEP, false, list);
			}
			for (var path : listFullPaths) {
				if (path != null) {
					list.add(path.toString());
				}
			}
		}
		saveAndShowList(true, 1, FileDataBase.getTempPath(nameForResultFiles.concat(Const.extensionTxt)), list);
	}

	private int SetInfoMode(final int value) {
		String info = "";

		if (value == Const.MODE_EXTRACT) {
			info = "EXTRACT: list of command line arguments";
		} else if (value == Const.MODE_PATHSLIST) {
			info = Const.PATHSLIST_NAME;
		} else if (value == Const.MODE_VIEW) {
			info = "VIEW";
		} else if (value == Const.MODE_STOP_ONE) {
			info = "ONE, finds and saves files list";
		} else if (value == Const.MODE_STOP_TWO) {
			info = "TWO, creates *.bin file after 'ONE'";
		} else if (value == Const.MODE_STOP_THREE) {
			info = "THREE, finds and deletes duplicates after 'ONE,TWO'";
		} else if (value == Const.MODE_STOP_FOUR) {
			info = "FOUR, copies selected files to selected folder";
		} else {
			return Const.MODE_NO_DEFINED;
		}
		setInfo(2, "mode", info, null, null);
		return value;
	}

//logic: if 'listFullPaths' is null/empty, be created and filling from 'parameters' (this is parameters of command line)
//if both null : error; if both not null : not must be, but be work with 'listFullPaths' only
	private void initShowPathsListTable(List<String> parameters, List<File> listFullPaths) {
		setInfo(2, "mode", Const.PATHSLIST_NAME, null, null);
		final String errorMessage = "error reading list of files";

		if (nullEmptyList(listFullPaths)) { // filling if empty
			if (notNullEmptyList(parameters)) {
				listFullPaths = new ArrayList<File>();
				for (int i = 0; i < parameters.size(); i++) { // need Canonical files, because remaining may be
					File f = getCorrectFileOrNull(true, 1, SIGN_FILE_OR_FOLDER, parameters.get(i));
					if (f == null) {
						continue;
					}
					listFullPaths.add(f);
				}
			}
		}

		if (nullEmptyList(listFullPaths)) {
			System.out.println(errorMessage);
			return;
		}

		if (listFullPaths.size() == 1) {
// will return a changed, if necessary, list with canonical paths; if 'false': no matter, 'return' only
			if (!workWithOneParameterOfPathsList(errorMessage, listFullPaths)) {
				return;
			}
		}
		showPathsListTable(listFullPaths);
	}

// method returns 'true' for next working with list 'listFullPaths', that is reading to table, frame 'PathsListTable'; and also changes that list as needed
	private boolean workWithOneParameterOfPathsList(final String errorMessage, List<File> listFullPaths) {
		if (nullEmptyList(listFullPaths)) {
			return true;
		}
		File file = listFullPaths.get(0);
		if (!file.exists()) {
			return true;
		}
		boolean bIsFolder = file.isDirectory();
		boolean oneYes = bIsFolder ? options.contains(Const.OPTIONS_PATHSLIST_ONE_FOLDER_YES)
				: options.contains(Const.OPTIONS_PATHSLIST_ONE_FILE_YES);
		boolean oneNo = bIsFolder ? options.contains(Const.OPTIONS_PATHSLIST_ONE_FOLDER_NO)
				: options.contains(Const.OPTIONS_PATHSLIST_ONE_FILE_NO);

		if (oneYes && oneNo) {
			oneYes = false;
			oneNo = false;
		}

		if (oneNo) {
			return true;
		}
		String s = bIsFolder ? "search in a folder " : "load paths from file: ";
		if (oneYes || pauseQueryOne("Mode " + Const.PATHSLIST_NAME + ". Confirm " + s.concat(file.toString()))) {
			if (bIsFolder) {
				if (!options.contains(Const.OPTIONS_ONE_PATHSLIST_YES)) {
					options += Const.OPTIONS_ONE_PATHSLIST_YES;
				}
				try {
					file = file.getAbsoluteFile().getCanonicalFile();
					createBin(Const.ID3_EXTRACT_NO, Const.MODE_STOP_ONE, file.toString(), null);
				} catch (Exception e) {
				}
				return false;
			} else {
				var tmp = getFileListFromFileOrNull(true, SIGN_FILE, file);
				if (nullEmptyList(tmp)) {
					System.out.println(errorMessage);
					return false;
				}
				listFullPaths.clear();
				listFullPaths.addAll(tmp);
			}
		}
		return true;
	}

	private void showPathsListTable(List<File> listFullPaths) {
		int size = nullEmptyList(listFullPaths) ? 0 : listFullPaths.size();
		if (size <= 0) {
			return;
		}
		boolean needCalculateCrc = false; // 'no' by default

		boolean crcYes = options.contains(Const.OPTIONS_PATHSLIST_CRC_YES);
		boolean crcNo = options.contains(Const.OPTIONS_PATHSLIST_CRC_NO);

		if (crcYes == crcNo) { // both defined or nothing
			if (pauseQueryOne("Confirm calculate 'crc'")) {
				needCalculateCrc = true;
			}
		} else {
			needCalculateCrc = crcYes;
		}

		setInfo(2, "calculate crc", needCalculateCrc ? "TRUE" : "FALSE", null, null);
		FileDataBase.showFrameInfo("Paths list table");
		var pathsListTable = new PathsListTable(options, needCalculateCrc, listFullPaths);
		var ft = new FutureTask<>(pathsListTable);
		new Thread(ft).start();
		try {
			var result = ft.get();
			if (result == Const.MR_DELETE) {
				deleteFiles(FileDataBase.getTempPath("deleteResult.txt"), pathsListTable.getResultStringPaths(), null);
			} else if (result == Const.MR_COPY_MOVE) {
				doCopyMoveNew();
			}
		} catch (Exception e) {
			System.out.println("Error 'PathsListTable': " + e.getMessage());
			return;
		}

	}

	private void fillPropertyList(Path path, Set<String> setGlobal) {
		var set = getSetFromFileOrEmptySet(1, path);
		setGlobal.clear();
		setGlobal.addAll(set);
	}

	private void showExtract(List<String> parameters) {
		if (nullEmptyList(parameters)) {
			return;
		}

		Set<File> files = new TreeSet<>();
		Set<File> folders = new TreeSet<>();
		List<String> errorList = new ArrayList<>();
		System.out.println("start extracting paths...");
		int totalCount = 0;
		for (int i = 0; i < parameters.size(); i++) {
			try {
				var file = Path.of(parameters.get(i)).toAbsolutePath().toFile().getCanonicalFile();
				if (!file.exists()) {
					errorArgument("no found: " + file);
				}
				if (file.isDirectory()) {
					if (!folders.add(file)) {
						errorArgument("folder path repeat: " + file);
					}
				} else {
					if (!files.add(file)) {
						errorArgument("file path repeat: " + file);
					}
				}
				totalCount++;
			} catch (Exception e) {
				addLog(e.getMessage(), true, errorList);
			}
		}

		if (totalCount == 0) {
			System.out.println("no result");
			return;
		}
		List<String> result = new ArrayList<String>();
		addLog(ADDLOG_DATE, false, result);
		addListToResult(false, "Folders", folders, result);
		addListToResult(true, "Files", files, result);
		addLog(ADDLOG_SEP, true, result);
		addLog("total paths: " + totalCount, true, result);
		if (!errorList.isEmpty()) {
			addLog(ADDLOG_SEP, false, result);
			addLog("=== Error information ===", false, result);
			result.addAll(errorList);
		}

		int needStartProcess = options.contains(Const.OPTIONS_EXTRACT_SAVE_YES) ? 1 : 3;
		saveAndShowList(false, needStartProcess, FileDataBase.getTempPath("extractResult.txt"), result);
	}

	private void addListToResult(boolean appendNamesWithoutExtensions, String caption, Set<File> paths,
			List<String> result) {
		if (nullEmptySet(paths)) {
			return;
		}
		addLog(ADDLOG_SEP, false, result);
		addLog("=== " + caption + ", size: " + paths.size() + " ===", false, result);
		for (var file : paths) {
			result.add(file.toString());
		}
		addLog(NEW_LINE_UNIX + "=== " + caption + " (names) ===", false, result);
		List<String> namesWithoutExtensions = appendNamesWithoutExtensions ? new ArrayList<String>() : null;
		for (var file : paths) {
			var name = file.getName().toString();
			if (nullEmptyString(name)) {
				continue;
			}
			result.add(name);
			if (namesWithoutExtensions != null) {
				name = getNameWithoutExtension(name);
				if (nullEmptyString(name)) {
					continue;
				}
				namesWithoutExtensions.add(name);
			}
		}
		if (namesWithoutExtensions == null) {
			return;
		}
		addLog(NEW_LINE_UNIX + "=== " + caption + " (names without extensions) ===", false, result);
		result.addAll(namesWithoutExtensions);
		return;
	}

	private String getNameWithoutExtension(String name) {
		if (nullEmptyString(name)) {
			return "";
		}
		var pos = name.lastIndexOf('.');
		return pos < 0 ? name : name.substring(0, pos);
	}

	private boolean checkRepositoryPath(boolean viewMode, boolean needInitDoubleRepo) {
		try {
			Path path = Path.of(FileDataBase.diskMain, Const.binFolderRepositorySignature);

			if (!path.toFile().exists()) {
				if (!pauseQueryOne("Confirm repository creating on disk " + FileDataBase.diskMain)) {
					return false;
				}
				Files.createDirectory(path);
			}

			if (FileDataBase.repositoryPathStandard == null) {
				FileDataBase.repositoryPathStandard = path.toString();

				String diskDoubleRepo = needInitDoubleRepo ? initDiskMainDoubleOrNull(FileDataBase.diskMain) : null;

				if (diskDoubleRepo != null) {
					Path doubleRepoPath = Path.of(diskDoubleRepo, Const.binFolderRepositorySignature);
					if (!doubleRepoPath.toFile().exists()) {
						try {
							Files.createDirectory(doubleRepoPath);
						} catch (Exception e) {
						}
					}
					if (doubleRepoPath.toFile().isDirectory()) {
						FileDataBase.repositoryPathStandardDouble = doubleRepoPath.toString();
					}
				}
			}

			Path pathTmp = null;
			if (FileDataBase.isTEMP) {
				pathTmp = Path.of(FileDataBase.diskMain, "~TMP" + Const.binFolderRepositorySignature);
				if (!pathTmp.toFile().exists()) {
					if (viewMode) {
						System.out.println("VIEW mode: no found temp repository " + pathTmp);
						return false;
					}
					Files.createDirectory(pathTmp);
				}
			}

			if (path.toFile().isDirectory()) {
				FileDataBase.repositoryPathCurrent = FileDataBase.isTEMP ? pathTmp.toString() : path.toString();
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	// finds disk for dublicate repository, confirm about, that must not be
	// 'diskMain'
	private String initDiskMainDoubleOrNull(String diskMain) {
		try {
			var disks = getEqualFoldersOnOtherDisks(true, diskMain, null);
			if (nullEmptyList(disks)) {
				return null;
			}
			List<String> list = new ArrayList<String>();
			for (var s : disks) {
				Path path = Path.of(s, Const.binFolderRepositorySignature);
				String exist = path.toFile().isDirectory() ? " exists: " + Const.binFolderRepositorySignature : "";
				list.add(s + exist);
			}

			if (list.size() != disks.size()) {
				return null;
			}
			System.out.println("Choose disk for DOUBLE REPO:");
			var confirm = pauseQueryList(list, null);
			if (confirm < 0) {
				return null;
			}

			return disks.get(confirm);
		} catch (Exception e) {
		}
		return null;
	}

	private int readGlobalOptions(boolean extractMode) {// for 'extractMode' all without confirm
		if (options.isEmpty()) {
			return 0;
		}

		boolean bId3No = options.contains(Const.OPTIONS_ID3_NO);
		int needCalcID3 = bId3No ? Const.ID3_EXTRACT_NO : Const.ID3_EXTRACT_NEW; // by default == Const.ID3_EXTRACT_NEW

		if (options.contains(Const.OPTIONS_TEMP) || options.contains(Const.OPTIONS_TEMP_YES)) {
			if (extractMode || options.contains(Const.OPTIONS_TEMP_YES) || pauseQueryOne("Confirm set option 'TEMP'."
					+ NEW_LINE_UNIX
					+ "Repository of result be set as '~TMP' prefix, and there will be saved 'need'/'no need' extensions"
					+ NEW_LINE_UNIX + "If 'cancel', remains standard repository")) {
				FileDataBase.isTEMP = true;
				setInfo(2, "option", "TEMP", null, null);
			}
		}

		if (extractMode) {
			return 0;
		}

		final String descrID3New = "Extract id3 for NEW files '*.mp3' while creating '*.bin'";
		final String descrID3All = "Extract id3 for ALL files '*.mp3', including allready existing in base, takes more time";
		final String descrID3No = "No extract id3 for '*.mp3'";

		boolean bId3Confirm = options.contains(Const.OPTIONS_ID3_CONFIRM);
		if (bId3Confirm) {
			List<String> list = new ArrayList<String>();
			list.add("ID3_NEW (by default). " + descrID3New);
			list.add("ID3_ALL. " + descrID3All);
			list.add("ID3_NO. " + descrID3No);

			int confirm = pauseQueryList(list, null);
			needCalcID3 = confirm == 2 ? Const.ID3_EXTRACT_NO
					: confirm == 1 ? Const.ID3_EXTRACT_ALL : Const.ID3_EXTRACT_NEW;
		}

		if (needCalcID3 == Const.ID3_EXTRACT_NO) {
			setInfo(2, "option", "ID3_NO", descrID3No, null);
		} else if (needCalcID3 == Const.ID3_EXTRACT_ALL) {
			setInfo(2, "option", "ID3_ALL", descrID3All, null);
		} else { // Const.ID3_EXTRACT_NEW
			setInfo(2, "option", "ID3_NEW", descrID3New, null);
		}

		return needCalcID3;
	}

// take existing folder and returns full exists path to *.bin (updated if need);
// returns null if error
	synchronized File getUpdatedBinFile(boolean tryCreateBin, File file) throws Exception {
		addLog(ADDLOG_SEP, true, null);
		System.out.println("start update '*.bin file'... " + file);
		File[] pathForReturn = new File[1];
		Arrays.fill(pathForReturn, null);
		createBin(Const.ID3_EXTRACT_NO, Const.MODE_STOP_TWO, file.toString(), pathForReturn);
		if (pathForReturn[0] == null) {
			return null;
		}
		if (pathForReturn[0].exists()) {
			return pathForReturn[0];
		}

		if (tryCreateBin) {
			saveToFile(true, 0, DeleteIfExists_OLD_DELETE, pathForReturn[0].toPath(), null, null);
			if (pathForReturn[0].exists()) {
				return pathForReturn[0];
			}
		}
		return null;
	}

	String getOptions() {
		return options;
	}

	/*
	 * 'listWithFilesPath' - path of text file with file path list to copying; user
	 * chooses root dest dir for copying, then bin has updated because paths from
	 * list be copied (or moved?) to dest dir
	 * 
	 * method called when be defined 'Const.MR_COPY_MOVE', on closing
	 * PathsListTable, ViewTable (->BeanViewTable)
	 */
	synchronized boolean doCopyMoveNew() throws Exception {
		Path listWithFilesPath = FileDataBase.getTempPathForCopyMove();
		if (listWithFilesPath == null || !listWithFilesPath.toFile().exists()) {
			System.out.println("list of files to copying no exists: " + listWithFilesPath);
			return false;
		}
		Path destFolder = getDestPathGUI(true);
		if (destFolder == null) {
			return false;
		}

		var rootDest = destFolder.getRoot();
		if (rootDest == null) {
			System.out.println("error: null root disk of " + destFolder);
			return false;
		}

		List<String> logList = new ArrayList<String>();
		addLog(ADDLOG_DATE, true, logList);
		addLog("Start copy filelist '" + listWithFilesPath + "' to folder: " + destFolder, true, logList);
		addLog(ADDLOG_SEP, false, logList);

		System.out.println("reading list of files to copying... " + listWithFilesPath);
		var listWithFiles = readFile(2, 0, listWithFilesPath);
		if (listWithFiles.isEmpty()) {
			System.out.println("NO FOUND ITEMS in " + listWithFilesPath);
			return false;
		}

		var destFolderLowerCase = destFolder.toString().toLowerCase();
		destFolderLowerCase = fileSeparatorAddIfNeed(false, true, destFolderLowerCase);

		System.out.println("start calculate crc for source list...");

// be not null sizes and with no equals signature ('size(crc)'); map contains <Signature, Canonical path>
		Set<String> needExtsInRealPath = new HashSet<String>();// be filling

		Map<String, Path> hashWithFilesRealPath = getListRealPath(destFolderLowerCase, listWithFiles, logList,
				needExtsInRealPath);

		if (hashWithFilesRealPath.isEmpty()) {
			System.out.println("no found files for copying");
			writeListToConsole(false, 0, 200, "log list", false, null, logList);
			return false;
		}

		if (!needExtsInRealPath.isEmpty()) {
			extsNeedListSet.addAll(needExtsInRealPath);
		}

		// 'getUpdatedBinFile' writes start info on console
		var binFileDestFolder = getUpdatedBinFile(true, destFolder.toFile());
		if (binFileDestFolder == null) { // 'empty' is norm, be that copied
			System.out.println("ERROR OF UPDATE bin file");
			return false;
		}

		System.out.println("reading bin of destination folder... " + binFileDestFolder);
		List<String> binFileListDestFolder = readFile(2, 0, binFileDestFolder.toPath());

		Set<String> hashFilesOfBin = ConverterBinFunc.getHashSetFromBin(binFileListDestFolder);

		addLog("Start copying of files (" + hashWithFilesRealPath.size() + " / " + listWithFiles.size() + ") to folder "
				+ destFolder, false, logList);
		addLog("*.bin this folder contains files: " + binFileListDestFolder.size() + "; original files: "
				+ hashFilesOfBin.size(), false, logList);
		addLog(ADDLOG_SEP, false, logList);

		List<Path> sourcePathListOtherDisk = new ArrayList<>();
		List<Path> sourcePathListSameDisk = new ArrayList<>();

		fillingListsFromHashMap(rootDest, hashWithFilesRealPath, hashFilesOfBin, sourcePathListOtherDisk,
				sourcePathListSameDisk, logList);

		if (sourcePathListOtherDisk.isEmpty() && sourcePathListSameDisk.isEmpty()) {
			System.out.println("no found files for copying");
			writeListToConsole(false, 0, 200, "log list", false, null, logList);
			return false;
		}

		addLog(ADDLOG_SEP, true, logList);
		var sb = new StringBuilder();
		sb.append("Files for copy/move:" + NEW_LINE_UNIX);

		if (!sourcePathListOtherDisk.isEmpty()) {
			sb.append("FROM_OTHER_DISK: ").append(sourcePathListOtherDisk.size()).append(NEW_LINE_UNIX);
		}
		if (!sourcePathListSameDisk.isEmpty()) {
			sb.append("FROM_THE_SAME_DISK: ").append(sourcePathListSameDisk.size()).append(NEW_LINE_UNIX);
		}

		addLog(sb.toString(), true, logList);

		List<String> list = new ArrayList<>();
		list.add("<COPY_ONLY>");// 0
		list.add("<MOVE_ONLY>");// 1
		list.add("<AUTO>: copy from other (" + sourcePathListOtherDisk.size() + "); move from the same ("
				+ sourcePathListSameDisk.size() + ")");// 2

		int confirm = pauseQueryList(list, logList);
		if (confirm < 0 || confirm > 2) {
			return false;
		}

		int countCopyTotal = copyingMovingNew(confirm, list.get(confirm), createNewFolder(destFolder, logList),
				sourcePathListOtherDisk, sourcePathListSameDisk, logList);

		String res = listWithFilesPath.toString().concat(Const.COPY_NEW_RESULT_POSTFIX);
		saveToFile(true, 0, DeleteIfExists_OLD_DELETE, Path.of(res), null, logList);
		if (countCopyTotal > 0) { // updating .*bin
			createBin(Const.ID3_EXTRACT_NO, Const.MODE_STOP_TWO, destFolder.toString(), null);
		}
		startProcess(false, Path.of(res));
		return countCopyTotal > 0;
	}

	// confirm: 0:copyOnly; 1:moveOnly; 2: auto: copy from other, move from same
	private int copyingMovingNew(int confirm, String caption, Path newDestFolder, List<Path> sourcePathListOtherDisk,
			List<Path> sourcePathListSameDisk, List<String> logList) {
		final String copyString = "Copying files";
		final String moveString = "MOVING files";

		var copying = confirm != 1;
		var moving = confirm != 0;

		var bCM = new CopyMove(DeleteIfExists_NEW_SAVE_WITH_OTHER_NAME, copying, moving);

		// other disk, copying if not 'moveOnly' defined
		int countOther = (sourcePathListOtherDisk.isEmpty()) ? 0
				: bCM.backUpCopyMoveFiles(QUERY_CONFIRM_LIST, 3, false, copying ? copyString : moveString, 0,
						newDestFolder, sourcePathListOtherDisk, logList);

		// the same disk, moving if not 'copyOnly' defined
		int countSame = 0;
		if (queryCopyMoveDefined(false) && !sourcePathListSameDisk.isEmpty()) {
			countSame = bCM.backUpCopyMoveFiles(getQueryCopyMove(), 3, false, moving ? moveString : copyString, 0,
					newDestFolder, sourcePathListSameDisk, logList);
		}

		int countCopyTotal = countSame + countOther;
		int sumSizes = sourcePathListSameDisk.size() + sourcePathListOtherDisk.size();
		System.out.println("Copy/move result: " + countCopyTotal + " from " + sumSizes);
		addLog(caption + ", RESULT: from the same " + countSame + "; from other " + countOther, false, logList);

		var errorOldDeleteOnMovingList = bCM.getErrorOldDeleteOnMovingList();
		if (notNullEmptyList(errorOldDeleteOnMovingList)) {
			errorOldDeleteOnMovingList.add(0, "<" + formatter.format(new Date())
					+ "> <ERROR MOVING LIST: files were copied but 'source files' not deleted>");
			saveAndShowList(true, 1, FileDataBase.getTempPath(Const.ERROR_OLD_DELETE_ON_MOVING),
					errorOldDeleteOnMovingList);
		}
		return countCopyTotal;
	}

	// map contains: <Signature, Canonical path>; 'needExtsInRealPath' filling by
	// exts in found paths, for updating 'dest' folder's bin
	private Map<String, Path> getListRealPath(String destFolderLowerCase, List<String> listWithFiles,
			List<String> logList, Set<String> needExtsInRealPath) {
		Map<String, Path> map = new HashMap<String, Path>();

		// itemToPath not empty, but may be any kind
		for (var itemToPath : listWithFiles) {
			try {
				var sTrimmed = removeEndStringAfterSpecificRestrictedWindowsChars(itemToPath);
				if (sTrimmed.length() < 3) {
					throw new IllegalArgumentException("string too short");
				}
				File file = Path.of(sTrimmed).toAbsolutePath().toFile().getCanonicalFile();

				var fLowerCase = file.toString().toLowerCase();
				if (fLowerCase.startsWith(destFolderLowerCase)) {
					addLog("error, destination folder contains this file: " + file, false, logList);
					continue;
				}

				if (file.isDirectory() || !file.exists()) {
					addLog("error (file not found or directory) " + file, false, logList);
					continue;
				}

				long fLen = file.length();
				long minSize = FileDataBase.skipEmpty ? 1 : 0;
				if (fLen < minSize) {
					addLog("error (null size) " + file, false, logList);
					continue;
				}

				var crc = new CalcCrc(1, "", file.toPath());
				// crcResult must be > 0 if fLen > 0; and crcResult == 0 if fLen == 0; and
				// crcResult < 0 if error
				if (crc.getCrcResult() < minSize) {
					addLog("error (no caclulated crc) " + file, false, logList);
					continue;
				}

				var signature = crc.getBinItemSignature();
				crc = null;

				if (map.containsKey(signature)) {
					addLog("error, file with equal signature (size,crc " + signature + ") with this list; " + file,
							false, logList);
					continue;
				}
				if (needExtsInRealPath != null) {
					var ar = ConverterBinFunc.dividePathToAll_Ext(0, file.toString());
					if (ar[0] == null) {
						addLog("error extract extension from " + file, false, logList);
						continue;
					}
					needExtsInRealPath.add(ar[0]);
				}
				map.put(signature, file.toPath());

			} catch (Exception e) {
				addLog("error (Exception) " + itemToPath + ", " + e.getMessage(), false, logList);
			}
		}
		return map;
	}

	private void fillingListsFromHashMap(Path rootDest, Map<String, Path> hashWithFilesRealPath,
			Set<String> hashFilesOfBin, List<Path> sourcePathListOtherDisk, List<Path> sourcePathListSameDisk,
			List<String> logList) {
		for (var hashPath : hashWithFilesRealPath.entrySet()) {
			var sign = hashPath.getKey();
			var path = hashPath.getValue();

			if (hashFilesOfBin.contains(sign)) {
				addLog("error copy, equal signature in destination *.bin (size, crc " + sign + ")) " + path, false,
						logList);
				continue;
			}

			var root = path.getRoot();
			if (root == null) {
				addLog("error: null root disk of " + path, false, logList);
				continue;
			}

			if (root.toString().equalsIgnoreCase(rootDest.toString())) {
				sourcePathListSameDisk.add(path);
			} else {
				sourcePathListOtherDisk.add(path);
			}
		}
	}

}
