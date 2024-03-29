package alexnick.filedatabase;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import alexnick.CopyMove;

import static alexnick.CommonLib.*;

public class FileDataBase {

	private static Charset definedCharSet = Charset.defaultCharset();// in 'Options'

	static boolean isShiftDown = false;

	static boolean id3IsPropertyChanged = false;
	volatile static Properties id3IsProperty = null;

	private static boolean markIsPropertyChanged = false;
//!!! both must be set 'null' together; and 'markPropertySet' updated after changing 'markIsProperty'
	private static Properties markIsProperty = null;
	static Set<String> markPropertySet = null;

	// always placed in STANDARD repository, no matter of 'globTEMP';
	static String repositoryPathStandard = null; // full path to repository, init on start
	static String repositoryPathCurrent = null; // if isTEMP, repository set in 'repositoryPathStandard'
	static String diskMain = null;
	static String binFolderMainREPO_ID = "";// writes as repoID to '.dat'; empty as default

	static String repositoryPathStandardDouble = null;

// 'true' by default; if true - files with '0' length, be skipped, while creating '*.bin';'true' recommended
	static final boolean skipEmpty = true;

	// will be taken from 'Const.OPTIONS...';
	static boolean isTEMP = false;
	static int sizeTextField = Const.textFieldStardardSize;

// 'tryDoubleRepo' if true, return null for 'TEMP' or 'repositoryPathStandardDouble'== null
	static Path getPathInRepo(boolean repoCurrent, String fileName, boolean tryDoubleRepo) {
		if (tryDoubleRepo) {
			return ((repoCurrent && isTEMP) || repositoryPathStandardDouble == null) ? null
					: Path.of(repositoryPathStandardDouble, fileName);
		}
		return Path.of(repoCurrent ? repositoryPathCurrent : repositoryPathStandard, fileName);
	}

// 'tryDoubleRepo' if true, return null for 'TEMP' or 'repositoryPathStandardDouble'== null
	static Path getPathInPropertyFolder(boolean repoCurrent, String fileName, boolean tryDoubleRepo) {
		if (tryDoubleRepo) {
			return ((repoCurrent && isTEMP) || repositoryPathStandardDouble == null) ? null
					: Path.of(repositoryPathStandardDouble, Const.FOLDER_NAME_PROPERTY, fileName);
		}
		return Path.of(repoCurrent ? repositoryPathCurrent : repositoryPathStandard, Const.FOLDER_NAME_PROPERTY,
				fileName);
	}

	static Path getTempPath(String fileName) {
		return Path.of(isTEMP ? repositoryPathCurrent : repositoryPathStandard, "TEMP", fileName);
	}

	static Path getPathPropertyExtsNeed(boolean tryDoubleRepo) {
		return getPathInPropertyFolder(isTEMP, Const.propertyExtsNeed, tryDoubleRepo);
	}

	static Path getPathPropertyExtsNoNeed(boolean tryDoubleRepo) {
		return getPathInPropertyFolder(isTEMP, Const.propertyExtsNoNeed, tryDoubleRepo);
	}

	static Path getTempPathForCopyMove() {
		return getTempPath(Const.COPY_MOVE_NEW_LIST);
	}

//init 'mode','options','parameters'
	public static void main(String[] args) {
		var sbOptions = new StringBuilder();
		int mode = Const.MODE_NO_DEFINED;

		try {
			System.out.print(NEW_LINE_UNIX + "Start FileDataBase, ");
			addLog(ADDLOG_DATE, true, null);
			System.out.println("Folder: " + Path.of("").toAbsolutePath());
			System.out.println("Command line:");
			System.out.println(Arrays.toString(args));
			addLog(ADDLOG_SEP, true, null);

//'mode','options' init						
			mode = initModeOptions(args, sbOptions);

			List<String> parameters = new ArrayList<String>();

			if (mode == Const.MODE_NO_DEFINED) {
				var confirm = usage(true);
				if (confirm == Const.MODE_VIEW || confirm == Const.MODE_VIEW_TEMP) {
					mode = Const.MODE_VIEW;
					sbOptions = new StringBuilder();
					if (confirm == Const.MODE_VIEW_TEMP) {
						sbOptions.append(Const.OPTIONS_TEMP_YES);
					}
				} else {
					finalProgramExit(0, false);
					return;
				}
			} else {
				for (int i = 1; i < args.length; i++) {
					parameters.add(args[i]);
				}
			}

			sizeTextField = sbOptions.toString().contains(Const.OPTIONS_BIG_SIZE_TEXTFIELD) ? Const.textFieldBigSize
					: Const.textFieldStardardSize;

			setDefinedCharSet(sbOptions.toString());

			if (sbOptions.toString().contains(Const.OPTIONS_TEST_FDB)) { // test methods
				System.out.println("start 'test_fdb' mode...");

			} else { // normal program start
				new Program(mode, sbOptions.toString(), parameters);
			}
		} catch (Exception e) {
			if (e.getMessage().equals("exit")) {
				sbOptions = new StringBuilder(); // to avoid 'final pause'
			} else {
				System.out.println("ERROR: " + e);
				usage(false);
			}
		}
		finalProgramExit(0, sbOptions.toString().contains(Const.OPTIONS_FINAL_PAUSE));
	}

	private static void setDefinedCharSet(final String options) {
		var pos = options.indexOf(Const.OPTIONS_CMD_CHARSET_PREFIX);
		if (pos < 0) {
			return;
		}

		var s = options.substring(pos + Const.OPTIONS_CMD_CHARSET_PREFIX.length());
		pos = s.indexOf(";");
		if (pos <= 0) { // must be at least 1 symbol
			return;
		}

		try {
			definedCharSet = Charset.forName(s.substring(0, pos));
			setInfo(2, "option", "CMD_CODEPAGE: " + definedCharSet, null, null);
		} catch (Exception e) {
			return;
		}
	}

	private static int usage(boolean appendConfirmViewMode) {
		addLog(ADDLOG_SEP, true, null);
		System.out.println("error of program parameters initialization; choose:");
		List<String> list = new ArrayList<String>();
		list.add("show console 'help'"); // 0
		list.add("try find and show file 'help.txt'"); // 1
		list.add("both options: console + file");// 2

		if (appendConfirmViewMode) {
			list.add("set mode 'VIEW', standard repository");// 3 optional
			list.add("set modes 'VIEW', 'TEMP': temporary repository"); // 4 optional
		}

		list.add("cancel : close program");// size-1
		int confirm = (pauseQueryList(list, null));
		boolean needFinalPause = false;

		if (appendConfirmViewMode && (confirm == 3 || confirm == 4)) {
			return confirm == 3 ? Const.MODE_VIEW : Const.MODE_VIEW_TEMP;
		}

		if (confirm == 0 || confirm == 2) {
			needFinalPause = true;
			showConsoleHelp();
		}
		if (confirm == 1 || confirm == 2) {
			needFinalPause = true;
			tryOpenHelpFile();
		}

		finalProgramExit(0, needFinalPause);
		return confirm;
	}

//!!!can be called from 'catch()' -> so 'try-catch' required here	
	private static void tryOpenHelpFile() {
		final String helpName = "help.txt";
		try {
			System.out.println("...attempting to run a file " + helpName);

			Path path = Path.of(FileDataBase.class.getResource("/").toURI());
			if (path == null) {
				errorArgument("no found program 'root' folder");
				return;
			}
			path = path.getParent();
			path = Path.of(path.toString(), "run", helpName);
			if (!path.toFile().exists()) {
				System.out.println("Error, not found file: " + path);
				return;
			}
			System.out.println("...start file: " + path);
			startProcess(false, path);

		} catch (Exception e) {
			System.out.println(NEW_LINE_UNIX + "Error opening file " + helpName);
		}
	}

	private static void showConsoleHelp() {
		addLog(ADDLOG_SEP, true, null);
		console("Short help of 'FileDataBase':");
		console("");
		console("First argument must starts with '-', then symbol of 'mode'.");
		console("If need parameters, next after 'mode' symbol must be ':' then options by separator ';'");
		console("'Mode' may be -1..-4, -v, -p, -e, -a");
		console("Modes -1, -2, -3 and -4 follow each other; means if defined '-3', first be start '-1', then '-2', then '-3'");
		console("For this modes must be first parameter - exists directory, then be files search. Other parameters don't matter.");
		console("");
		console("Example command line:");
		console("-4:finalPause D:/needFolder");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-1' do files search only. Program will ask, when be new files extension to save them in settings.");
		console("Note: if been set option 'TEMP', program be work with temporary repository and without load extensions settings, means will ask about ALL found extensions and result be saved to ~TMP~ repository");
		console("---");
		console("Mode '-2' get chosen files paths from '-1', then creates and saves '*.bin' file and '*.dat' file with service information");
		console("Note: for new files in '*.bin' will be calculated 'CRC' and information about equals files be added to '*.dat' file");
		console("---");
		console("Mode '-3' get '*bin' from '-2', then shows window, where can choose and delete files with equal 'CRC'");
		console("---");
		console("Mode '-4' starts 'comparing folder', means you can choose either equal folder on OTHER disk; or any folder on your operating system ('Windows')");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-p' need to Rename, Delete, Copy/Move files, or save information of files/folders. Will be showed window with table.");
		console("Example command line:");
		console("-p:finalPause D:/anyFolder C:/anyFolder2 D:/anyFile.txt C:/folder/anyFile2.doc [and so on...]");
		console("");
		console("If first parameter is file and one parameter only, will be offer to load paths from this file, it must be text file in Unicode, one string = one path");
		console("Note: information in end each string after restricted symbols  '*', '?', '\"', '<', '>', '|' will be removed");
		console("");
		console("If first parameter is folder and one parameter only, will be offer search if this folder, then show 'Paths list table'. Analog mode '-1' with set option 'modeOnePlYes'");
		console("");
		console("This mode has many settings, see 'help.txt'");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-v' shows window with all created '*.bin' files. You can search by file names even without connecting disks, from where been created '*.bin'");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-e' just saves to text file information about folders/files command line and shows this file");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-a', '-auto'. Selects mode as: no parameters: '-v'; one parameter: file '-p', folder '-4'. More parameters: '-e' or '-p', if defined option 'autoNoExtract'");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-cb', '-compareBin'. Must be TWO *.bin files for comparing; not renaming, not copy/move. Text result only");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-dd', '-deleteDirectories'. Must be ONE exists folder; will be found and removed EMPTY subfolders");
		addLog(ADDLOG_SEP, true, null);
		console("Mode '-s', '-sync', '-syncBin'. Synchronization own repository '~bin~data~repository' with selected on other disk with the same name");
		console("");
	}

	private static void console(String s) {
		System.out.println(s);
	}

	private static int initModeOptions(String[] args, StringBuilder sbOptions) throws IOException {
		if (args.length == 0) {
			return Const.MODE_NO_DEFINED;
		}

		var firstArg = args[0].toLowerCase();
		if (firstArg.contains(" ")) {
			firstArg = firstArg.replace(" ", "");
		}

		if (!firstArg.startsWith("-")) {
			return Const.MODE_NO_DEFINED;
		}

		firstArg = firstArg.substring(1);
		var pos = firstArg.lastIndexOf(":");
		if (pos > 0) { // firstArg must be at least 1 symbol
			if (pos < firstArg.length() - 1) {
				sbOptions.append(";").append(firstArg.substring(pos + 1)).append(";");
			}
			firstArg = firstArg.substring(0, pos);
		}

		if (firstArg.equals("cb") || firstArg.equals("comparebin")) {
			return Const.MODE_COMPARE_BIN;
		}

		if (firstArg.equals("dd") || firstArg.equals("deletedirectories")) {
			return Const.MODE_DELETE_EMPTY_DIR;
		}

		if (firstArg.equals("s") || firstArg.equals("sync") || firstArg.equals("syncbin")) {
			return Const.MODE_SYNC_BIN;
		}

		final boolean bAutoMode = firstArg.equals("a") || firstArg.equals("auto");

//first arg: 'a','v', '1'..'4','e','p'
		if (firstArg.equals("v") || firstArg.equals("view") || (bAutoMode && args.length == 1)) {
			return Const.MODE_VIEW;
		}

		if (args.length < 2) {
			return Const.MODE_NO_DEFINED;
		}

		if (firstArg.equals("e") || firstArg.equals("extract") || (bAutoMode && (args.length > 2))) {
			if (bAutoMode && sbOptions.toString().contains(Const.OPTIONS_AUTO_NO_EXTRACT)) {
				setInfo(2, "option", "AUTO_NO_EXTRACT",
						"Setting 'paths list mode' for multiple parameters in 'auto mode'", null);
				return Const.MODE_PATHSLIST;
			}
			return Const.MODE_EXTRACT;
		}

		try {
			String secondArg = args[1];
			if (secondArg.endsWith(":")) { // windows
				secondArg += File.separator;
			}

// for 'bAutoMode' will be 'true' if 'args.length' == 2 and second parameter is not directory (may be not exists)
			if (firstArg.equals("p") || firstArg.equals("pl") || firstArg.equals("pathslist")
					|| (bAutoMode && !Path.of(secondArg).toFile().isDirectory())) {
				return Const.MODE_PATHSLIST;
			}

			File startPath = Path.of(secondArg).toRealPath().toFile();
			if (!startPath.isDirectory()) {
				return Const.MODE_NO_DEFINED;
			}

			var keyOptions = bAutoMode ? Const.MODE_STOP_FOUR : Const.checkModeStop(Integer.valueOf(firstArg));
			if (keyOptions == Const.MODE_NO_DEFINED) {
				System.out.println("first parameter must be from '-1' to '-4'");
			}
			return keyOptions;

		} catch (Exception e) {
		}
		return Const.MODE_NO_DEFINED;
	}

	static ArrayList<Map.Entry<String, FileCntSize>> getSortedHmExt(Map<String, FileCntSize> hmExt) {
		ArrayList<Map.Entry<String, FileCntSize>> sortedHmExtsList = new ArrayList<>(hmExt.entrySet());
		if (!sortedHmExtsList.isEmpty()) {
			sortedHmExtsList.sort((e1, e2) -> {
				var o1 = e1.getValue();
				var o2 = e2.getValue();

				if (o1.getCount() > o2.getCount())
					return -1;
				if (o1.getCount() < o2.getCount())
					return 1;
				if (o1.getSize() > o2.getSize())
					return -1;
				if (o1.getSize() < o2.getSize())
					return 1;
				return e1.getKey().compareTo(e2.getKey());
			});
		}
		return sortedHmExtsList;
	}

	/**
	 * @param whereIsPath if 0 path will be from 'binPath';<br>
	 *                    if 1 from 'getFour' with 'fourApp'<br>
	 *                    if 2 from 'getFour' withOut 'fourApp'<br>
	 *                    if 3 from 'getThree'
	 * @param isShiftDown if 'true' run file; if 'false' open parent directory
	 * @param myTable     must not be null/empty
	 * @param beans       must not be null/empty
	 */
	static void openDirectory(int whereIsPath, boolean isShiftDown, BeansFourTableDefault myTable, List<MyBean> beans) {
		if (myTable.getSelectedRowCount() != 1 || whereIsPath < 0 || whereIsPath > 3) {
			return;
		}
		var y = myTable.getSelectedRow();
		if (y < 0) {
			return;
		}

		var b = beans.get(y);

		if (b.isFourPrefixNoExists()) {
			return;
		}

		Path path = whereIsPath == 0 ? b.binPath
				: whereIsPath == 3 ? Path.of(b.getThree()) : Path.of(b.getFour(false, whereIsPath == 1));

		if (path == null) {
			return;
		}

		Path pathStart = isShiftDown ? path : path.getParent();

		if (pathStart == null) { // example, getParent for 'C:/' will be null
			pathStart = path;
		}
		if (pathStart.toFile().exists()) {
			startProcess(false, pathStart);
		}
	}

	/**
	 * @param noCheckPathInFour if 'true', 'b.four' will means the same as prefix
	 *                          'no exists'
	 * @param sortType          0 (by default):no sort list;<br>
	 *                          1:sort;<br>
	 *                          2: sort ignore case;<br>
	 *                          3: the same as '2' + mode: PATHS_NO_ROOT
	 * @param set               if null, to list will be saved checked items from
	 *                          'beans'; else 'beans' indices will be taken from
	 *                          'set'
	 * @param beans
	 */
	static void beansToList(boolean noCheckPathInFour, int sortType, Set<Integer> set, List<MyBean> beans) {
		if (nullEmptyList(beans)) {
			return;
		}
		List<String> listExists = new ArrayList<>();
		List<String> listNoExists = new ArrayList<>();
		List<String> listNoFoundDiskOrStartPath = new ArrayList<>();

		// EQUALS list with full information
		List<String> listExistsFullInform = new ArrayList<>();
		List<String> listNoExistsFullInform = new ArrayList<>();
		List<String> listNoFoundDiskOrStartPathFullInform = new ArrayList<>();

		Set<String> pathsNoRootSet = null;

		boolean pathsNoRoot = sortType == 3; // mode: PATHS_NO_ROOT
		if (pathsNoRoot) {
			sortType = 2;
			noCheckPathInFour = true;
			pathsNoRootSet = new HashSet<String>();
		}

		int count = set == null ? 0 : set.size();
		int errorCount = 0;

		if (set == null) {
			for (var b : beans) {
				if (!b.check) {
					continue;
				}

				if (fillLists(noCheckPathInFour, b, listExists, listNoExists, listNoFoundDiskOrStartPath,
						listExistsFullInform, listNoExistsFullInform, listNoFoundDiskOrStartPathFullInform,
						pathsNoRootSet)) {
					count++;
				} else {
					errorCount++;
				}
			}
		} else {
			for (var i : set) {
				fillLists(noCheckPathInFour, beans.get(i), listExists, listNoExists, listNoFoundDiskOrStartPath,
						listExistsFullInform, listNoExistsFullInform, listNoFoundDiskOrStartPathFullInform,
						pathsNoRootSet);
			}
		}

		if (count == 0) {
			JOptionPane.showMessageDialog(null, "No found checked items for saving to list");
			return;
		}

		Path resPath = getTempPath("toListResult.txt");
		var sb = new StringBuilder();
		sb.append("Chosen items: ").append(count).append("; errors: ").append(errorCount).append(".")
				.append(NEW_LINE_UNIX).append("Found result:").append(NEW_LINE_UNIX);

		if (!listExists.isEmpty()) {
			sb.append("<EXISTS>: ").append(listExists.size()).append(NEW_LINE_UNIX);
		}

		if (!listNoExists.isEmpty()) {
			sb.append("<NO EXISTS>: ").append(listNoExists.size()).append(NEW_LINE_UNIX);
		}

		final String listNoFoundCaption = noCheckPathInFour ? "WITHOUT CHECKING ON EXISTING PATHS"
				: "NO FOUND DISK OR START PATH";

		if (!listNoFoundDiskOrStartPath.isEmpty()) {
			sb.append("<").append(listNoFoundCaption).append(">: ").append(listNoFoundDiskOrStartPath.size())
					.append(NEW_LINE_UNIX);
		}

		if (sortType < 0 || sortType > 2) {
			sortType = 0;
		}

		sb.append(NEW_LINE_UNIX + "List (").append(sortType == 0 ? "no " : "").append("sorted) will be saved to ")
				.append(resPath).append(NEW_LINE_UNIX);

		if (pathsNoRoot) {
			sb.append("Paths without root. Continue?");
		} else {
			sb.append("Choose <YES> to save ALL information; <NO>: paths only");
		}

		var res = JOptionPane.showConfirmDialog(null, sb.toString(), "Save checked to list",
				pathsNoRoot ? JOptionPane.YES_NO_OPTION : JOptionPane.YES_NO_CANCEL_OPTION);

		if (res == JOptionPane.YES_OPTION) { // full inform or without root
			if (!pathsNoRoot) {
				listExists = listExistsFullInform;
				listNoExists = listNoExistsFullInform;
				listNoFoundDiskOrStartPath = listNoFoundDiskOrStartPathFullInform;
			}
		} else if (res != JOptionPane.NO_OPTION || pathsNoRoot) {
			return;
		}

		sortFillingList(sortType, "EXISTS", listExists, null);
		sortFillingList(sortType, "NO EXISTS", listNoExists, listExists);
		sortFillingList(sortType, listNoFoundCaption, listNoFoundDiskOrStartPath, listExists);

		listExists.add(0, formatter.format(new Date()));
		if (saveToFile(false, true, 0, CopyMove.DeleteIfExists_OLD_DELETE, resPath, null, listExists)) {
			startProcess(false, resPath);
		}
	}

	private static boolean fillLists(boolean noCheckPathInFour, MyBean b, List<String> listExists,
			List<String> listNoExists, List<String> listNoFoundDiskOrStartPath, List<String> listExistsFullInform,
			List<String> listNoExistsFullInform, List<String> listNoFoundDiskOrStartPathFullInform,
			Set<String> pathsNoRootSet) {

		var bIsPrefix = noCheckPathInFour || b.isFourPrefixNoExists() || pathsNoRootSet != null;
		var s = b.getFour(bIsPrefix, true);

		if (bIsPrefix) {
			if (pathsNoRootSet != null) {
				int pos = s.indexOf(Const.ROOT_SEPARATOR_WINDOWS);
				if (pos >= 0) {
					s = s.substring(pos + Const.ROOT_SEPARATOR_WINDOWS.length());
				}

				if (!pathsNoRootSet.add(s.toLowerCase())) {
					return false;
				}
			}

			listNoFoundDiskOrStartPath.add(s);

			if (pathsNoRootSet == null) {
				listNoFoundDiskOrStartPathFullInform.add(fullInform(s, b));
			}

		} else {
			var p = Path.of(s);
			if (p.toFile().exists()) {
				listExists.add(s);
				listExistsFullInform.add(fullInform(s, b));
			} else {
				listNoExists.add(s);
				listNoExistsFullInform.add(fullInform(s, b));
			}
		}

		return true;
	}

	private static String fullInform(String s, MyBean b) {
		var sb = new StringBuilder();
		sb.append(s).append(Const.BRACE_START_FIRST_SPACE);
		if (!b.getOne().isEmpty()) {
			sb.append(" [ ").append(b.getOne()).append(" ] ");
		}
		if (!b.getTwo().isEmpty()) {
			sb.append(" [ ").append(b.getTwo()).append(" ] ");
		}
		if (!b.getThree().isEmpty()) {
			sb.append(" [ ").append(b.getThree()).append(" ] ");
		}
		sb.append(Const.BRACE_END);
		return sb.toString();
	}

	static boolean isCorrectBin(Path path) {
		var f = path.toFile();
		if (!f.exists() || f.isDirectory()) {
			return false;
		}

		// 'f.length() < 20' suppose like that: 'aabbccdd(0)123456789'
		return f.length() > 20 && path.toString().endsWith(Const.extensionBinList);
	}

	static Path getDatPathForBinOrNull(Path path) {
		if (path == null) {
			return null;
		}

		String sDat = changeEndOfStringOrEmpty(path.toString(), Const.extensionBinList, Const.extensionBinData);
		if (sDat.isEmpty()) {
			return null;
		}

		File f = Path.of(sDat).toFile();
		if (!f.exists() || f.isDirectory()) {
			return null;
		}

		return f.toPath();
	}

//'stuffForReturn' must be created, size minimum 4, be filled 'two', 'three', 'four', 'two' AS_IS_IN_DAT for MyBean	
//'realDiskInBraceOrEmpty' if not null/empty, must be like example '<E:\> '; otherwise must be empty
//'mapCountExtForReturn' if created, will be cleared and filled extension info; ALSO MEANS defining checkSum
	static int getCountBinItemsOrNil(String realDiskInBraceOrEmpty, Path fDat,
			Map<String, Integer> mapCountExtForReturn, String[] stuffForReturn) {
		if (stuffForReturn == null || stuffForReturn.length < 6) {
			return 0;
		}

		if (realDiskInBraceOrEmpty == null) {
			realDiskInBraceOrEmpty = "";
		}
// 'fdat': D:\~bin~data~repository\~bin~data~results~F-GB-14,43-1805ca431af-flash\~$$~.dat
		var datList = readFile(0, 0, fDat); // be empty if no exists 'fDat'
		final String e = "error";
		String x2 = e;
		String x3 = e;
		String x4 = e;
		String dateId = ""; // dateID optional, empty if no found; no counted
		String repoID = ""; // repoID optional, empty if no found; no counted
		String realStartPath = null;

		int count = 0;
		int indexExtBegin = 0;
		int indexExtEnd = 0;
		for (int i = 0; i < datList.size() - 1; i++) {// 'i < size - 1', for safe bounds
			if (count >= 4) {
				break;
			}

			var s = datList.get(i);
			/*
			 * last of stuff (4,5) ALIAS_DATE_ID = "<dateID>" and ALIAS_REPO_ID = "<repoID>"
			 * may be not defined (old versions); placed in '.dat' below ALIAS_DATE
			 */
			switch (s) {
			case Const.ALIAS_START_SEARCH -> {
				if (x2.equals(e)) {
					// 'x2' is start path, may be folder 'F:\test' or full disk, as 'F:\'
					x2 = datList.get(i + 1);
					realStartPath = x2;
					// set real start path
					// !!! 'x2' for existing disks, MUST START WITH '<' and ENDS on '> '
					if (realDiskInBraceOrEmpty.length() < 4 || !correctWindowsStartPath(x2)) { // means_empty
						x2 = Const.NO_DISK_PLUS + x2;// "<NO_DISK> "
					} else { // 'realDiskInBraceOrEmpty' == '<E:\> '

						// first be one letter from real (exist) disk
						var startPathFormatted = realDiskInBraceOrEmpty.substring(1, 2);

						if (x2.startsWith(startPathFormatted)) { // the same disk, as start path
							startPathFormatted = x2;
						} else {

							// create new start path with other (real disk) first letter
							startPathFormatted += x2.substring(1); // for whole disk example: 'K:\'

// !!! start path from '.dat' file, must be in ' <' , '>', if current disk different now
							x2 = startPathFormatted + Const.BRACE_START_FIRST_SPACE + x2 + Const.BRACE_END;// for_whole_disk_example:
																											// K:\ <F:\>
						}

						if (!Path.of(startPathFormatted).toFile().exists()) {
							x2 = Const.NO_FOUND_PLUS + x2;// "<NO_FOUND> "
						}
					}
					count++;
				}
			}

			case Const.ALIAS_DATE -> {
				if (x3.equals(e)) {
					// 'x3' set as 2022.04.24_17:36:37 (sun)
					x3 = datList.get(i + 1);
					count++;
				}
			}

			case Const.ALIAS_DATE_ID -> {
				if (dateId.isEmpty()) {
					dateId = datList.get(i + 1);
				}
			}

			case Const.ALIAS_REPO_ID -> {
				if (repoID.isEmpty()) {
					repoID = datList.get(i + 1);
				}
			}

			case Const.ALIAS_FOUND_EXT -> {
				if (indexExtBegin == 0) {
					indexExtBegin = i + 2;
					count++;
				}
			}

			case Const.ALIAS_FOUND_FILES -> {
				if (x4.equals(e)) {
					x4 = datList.get(i + 1);
					indexExtEnd = i;
					count++;
				}
			}
			} // of switch
		}

		if (indexExtBegin == 0 || indexExtBegin >= indexExtEnd) {
			return 0;
		}
		var ps = x4.indexOf(',');
		if (ps <= 0 || x4.equals(e)) {
			return 0;
		}

		int countBinItems = 0;

		try {
			countBinItems = Integer.valueOf(x4.substring(0, ps));
		} catch (NumberFormatException e1) {
			return 0;
		}

		if (countBinItems <= 0) {
			return 0;
		}

//FILL STUFF >> CORRECT VALUES	
		stuffForReturn[0] = x2;
		stuffForReturn[1] = x3;
		stuffForReturn[2] = x4;
		stuffForReturn[3] = realStartPath;
		stuffForReturn[4] = dateId;
		stuffForReturn[5] = repoID;

//CHECKSUM
		if (mapCountExtForReturn == null) {
			return countBinItems;
		}

		int cntCheckSum = 0;
		mapCountExtForReturn.clear();

		for (int x = indexExtBegin; x < indexExtEnd; x++) {
			var s = datList.get(x);
			var pos = s.indexOf(Const.extSeparator);
			if (pos < 1) {
				continue;
			}
			var ext = s.substring(0, pos);
			s = s.substring(pos + 3);
			pos = s.indexOf(',');
			if (pos < 1) {
				continue;
			}
			try {
				int cnt = Integer.valueOf(s.substring(0, pos));
				if (cnt <= 0) {
					continue;
				}

				cntCheckSum += cnt;
				if (mapCountExtForReturn.containsKey(ext)) {
					return 0;
				}

				mapCountExtForReturn.put(ext, cnt);
			} catch (NumberFormatException e1) {
				continue;
			}
		}

		if (cntCheckSum != countBinItems) {
			return 0;
		}

		return countBinItems;
	}

	static List<Entry<String, Integer>> fillSortedListExtCountOrNull(List<MyBean> beans) {
		Map<String, Integer> mapExtCount = new HashMap<String, Integer>();
		for (var b : beans) {
			var ext = b.getFourApp(true, true);
			mapExtCount.compute(ext, (k, v) -> v == null ? 1 : v + 1);
		}

		if (mapExtCount.isEmpty()) {
			return null;
		}
		return getSortedListFromMap(mapExtCount);
	}

//=========START PROPERTY=========	

//'bIsMarkProp' if true, need change global 'markIsPropertyChanged'; else 'id3IsPropertyChanged'  	
	private static boolean initProperty(boolean bIsMarkProp, String fileName) {
		Properties prop = bIsMarkProp ? markIsProperty : id3IsProperty;

		if (prop != null) {
			return true;
		}

		if (bIsMarkProp) {
			markIsPropertyChanged = false;
			markIsProperty = new Properties();
			markPropertySet = null;
		} else {
			id3IsPropertyChanged = false;
			id3IsProperty = new Properties();
		}
		prop = bIsMarkProp ? markIsProperty : id3IsProperty;
		// !!! properties in standard folder only
		loadOrStoreProperties(false, "", getPathInPropertyFolder(false, fileName, false),
				getPathInPropertyFolder(false, fileName, true), prop);
		return prop != null;
	}

//'bIsMarkProp' if true, need change global 'markIsPropertyChanged'; else 'id3IsPropertyChanged'  	
	private static void savePropertyAndNull(boolean bIsMarkProp, String caption, String fileName) {
		boolean valueOfPropertyChanged = bIsMarkProp ? markIsPropertyChanged : id3IsPropertyChanged;
		Properties prop = bIsMarkProp ? markIsProperty : id3IsProperty;
		try {
			if (prop != null && valueOfPropertyChanged) {
				loadOrStoreProperties(true, caption, getPathInPropertyFolder(false, fileName, false),
						getPathInPropertyFolder(false, fileName, true), prop);
			}
		} finally { // prop = null; not work so...
			if (bIsMarkProp) {
				markIsPropertyChanged = false;
				markIsProperty = null;
				markPropertySet = null;
			} else {
				id3IsPropertyChanged = false;
				id3IsProperty = null;
			}
		}
	}

	static void initId3IsProperty() {
		initProperty(false, Const.id3IsPropertyName);
	}

	static void saveID3Property() {
		savePropertyAndNull(false, "Properties ID3 tags for *" + Const.extensionForCalcId3, Const.id3IsPropertyName);
	}

	/**
	 * @return true, if 'markIsProperty' and 'markPropertySet' not null
	 */
	static boolean initMarkIsProperty() {
		if (!initProperty(true, Const.markIsPropertyName)) {
			return false;
		}
		if (markPropertySet != null) {
			return true;
		}

		markPropertySet = new HashSet<String>();
		for (var value : markIsProperty.values()) {
			String s = formatMark((String) value, false);
			if (s.isEmpty()) {
				continue;
			}
			markPropertySet.add(s);
		}
		return true;
	}

	/**
	 * @return 'true', if been changed 'markIsProperty'
	 */
	static boolean saveMarkProperty() {
		if (nullEmptyProperties(markIsProperty)) {
			return false;
		}
		Properties tmp = new Properties();
		tmp.putAll(markIsProperty);
		for (var key : tmp.keySet()) {
			String value = tmp.getProperty((String) key);
			if (value != null && value.isEmpty()) {
				markIsProperty.remove(key);
				markIsPropertyChanged = true;
			}
		}
		boolean result = markIsPropertyChanged;
		savePropertyAndNull(true, "Mark properties", Const.markIsPropertyName);
		return result;
	}

	/**
	 * Formats 'mark' according to the rules: result will be trimmed and will be set
	 * to lower case; and all '*' will be replaced on ''; if result empty, returns
	 * empty; max length result no more 100 symbols
	 * 
	 * 
	 * @param mark
	 * @param needMarkBraces if 'true', to start result be added MARK_BRACE;
	 * @return if result is empty, returns empty (that is error)
	 */
	static String formatMark(String mark, boolean needMarkBraces) {
		if (mark.isEmpty()) {
			return "";
		}
		final int maxLength = 100;
		mark = mark.toLowerCase();
		if (mark.contains("*")) {
			mark = mark.replace("*", "");
		}

		while (mark.contains("  ")) {
			mark = mark.replace("  ", " ");
		}

		if (mark.length() > maxLength) {
			mark = mark.substring(0, maxLength);
		}

		mark = mark.trim();
		return (!needMarkBraces || mark.isEmpty()) ? mark : Const.BRACE_MARK + mark;
	}

	static boolean addMarkToProperties(boolean needFormat, String signature, String mark) {
		if (!initMarkIsProperty()) {
			return false;
		}

		if (mark.equals(Const.REMOVE_MARK)) {
			mark = "";
		} else {
			if (needFormat) {
				mark = formatMark(mark, false);
			}
			if (mark.isEmpty()) {
				return false;
			}
		}

		markIsPropertyChanged = true; // save on close 'this'
		FileDataBase.markIsProperty.put(signature, mark);
		markPropertySet.add(mark);
		return true;
	}

	static String getMarkFromPropertiesOrEmpty(String signature) {
		if (!initMarkIsProperty() || nullEmptySet(markPropertySet)) {
			return "";
		}
		return markIsProperty.getProperty(signature, "");
	}

//=========END PROPERTY=========

// if index incorrect, example '-1', will not be changed value of 'sortedListExtSkipped'
	static void setSkipInfo(int index, boolean[] sortedListExtSkipped, String[] sortedListExts, JTextField tfSkipInfo) { // press
																															// 'skip'
		if (sortedListExtSkipped == null || sortedListExts == null
				|| sortedListExtSkipped.length != sortedListExts.length) {
			return;
		}
		if (index >= 0 && index < sortedListExtSkipped.length) {
			sortedListExtSkipped[index] = !sortedListExtSkipped[index];
		}
		var sb = new StringBuilder();
		sb.append(Const.skipInfoPrefix);
		for (int i = 0; i < sortedListExtSkipped.length; i++) {
			if (sortedListExtSkipped[i]) {
				sb.append(sortedListExts[i]).append(";");
			}
		}
		tfSkipInfo.setText(sb.toString());
		return;
	}

	static boolean getSkipExt(MyBean b, boolean[] sortedListExtSkipped, String[] sortedListExts) {
		if (sortedListExtSkipped == null) {
			return false;
		}
		var ext = b.getFourApp(true, true);
		for (int i = 0; i < sortedListExts.length; i++) {
			if (sortedListExts[i].equals(ext)) {
				return sortedListExtSkipped[i];
			}
		}
		return false; // no must be here return
	}

	static final KeyListener keyListenerShiftDown = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent e) {
			isShiftDown = (e.getKeyCode() == KeyEvent.VK_SHIFT);
		}

		@Override
		public void keyReleased(KeyEvent e) {
			isShiftDown = false;
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	};

	static boolean dragging(int rowDest, BeansFourTableDefault myTable, List<MyBean> beans) {
		if (beans.size() <= 1 || rowDest < 0 || rowDest >= beans.size()) {
			return false;
		}
		var arSelectedRows = myTable.getSelectedRows();
		if (arSelectedRows.length == 0) {
			return false;
		}
		var selectedRowFirst = beans.size();
		for (var i : arSelectedRows) {// 'arSelectedRows' is ordered
			if (i == rowDest) {
				return false;
			}
			if (i < selectedRowFirst) {
				selectedRowFirst = i;
			}
		}
		if (selectedRowFirst >= beans.size() || selectedRowFirst == rowDest) {
			return false;
		}
		myTable.clearSelection();
		List<MyBean> tmp = new ArrayList<MyBean>();
		for (var i : arSelectedRows) {
			tmp.add(beans.get(i));
			beans.set(i, null);
		}
		beans.removeIf(Objects::isNull);
		if (rowDest > beans.size()) {
			rowDest = beans.size();
		}
		beans.addAll(rowDest, tmp);

		myTable.updateUI();
		myTable.setRowSelectionInterval(rowDest, rowDest + tmp.size() - 1);
		return true;
	}

	/**
	 * Call before creating GUI frame, for print information about it
	 * 
	 * @param frameName will be showed in quotes
	 */
	static void showFrameInfo(final String frameName) {
		System.out.println("show '" + frameName + "'...");
	}

	// finally init b.one; after all other parameters init
	static void formatBeanOneForEqualTable(String prefix, MyBean b) {
		StringBuilder sb = new StringBuilder();
		appendNotNullEmpty(prefix, sb);

		sb.append(Const.BRACE_START);

		sb.append(formatInt(b.serviceIntThree, 3, null, null));
		if (b.serviceIntTwo > 0) { // equalSignIdDestCount
			sb.append("--" + b.serviceIntTwo);
		}

		sb.append("; ").append(b.serviceStringOne);

		if (b.getTwo().equals(b.getThree())) {// sourceName.equals(destName)
			sb.append("; equals");
		} else if (b.getTwo().equalsIgnoreCase(b.getThree())) {
			sb.append("; registerOnly");
		}

		sb.append(Const.BRACE_END_WITH_SPACE).append(bytesToKBMB(false, 3, b.serviceLong));
		b.setOne(sb.toString());
	}

	/**
	 * @param test                  if true, no message for 'arCodePointsForReturn',
	 *                              confirm set as 'yes'
	 * @param needANDexcluding      if 'true', will be found
	 *                              Const.textFieldLastANDSeparator on end of
	 *                              'rowString', then will be set as result<br>
	 *                              if false, return empty result string
	 * @param toLowerCase           if true, ALL result strings will be set to lower
	 *                              case
	 * @param rowString             will be divided by
	 *                              Const.textFieldFindORSeparator after excluding,
	 *                              if defined 'needANDexcluding'
	 * @param substringsOrForReturn must not be null; will be clear and filling
	 *                              result substrings (by OR separator)<br>
	 *                              If error, returns EMPTY list
	 * @param arCodePointsForReturn if not null and length == 2 and rowString in
	 *                              specified format, this array will be filled
	 *                              after confirmation; if cancelled/error, will be
	 *                              set as [0,0]
	 * @return null if not defined 'needANDexcluding' or error; else will be AND
	 *         substrings, not EMPTY
	 */
	static List<String> getSubstringsAND_DivideByOR_NullIfError(boolean test, boolean needANDexcluding,
			boolean toLowerCase, String rowString, List<String> substringsOrForReturn, int[] arCodePointsForReturn) {
		if (nullEmptyString(rowString) || rowString.equals(Const.textFieldFindORSeparator)
				|| substringsOrForReturn == null) {
			return null;
		}

		substringsOrForReturn.clear();
		if (arCodePointsForReturn != null) {
			for (int i = 0; i < arCodePointsForReturn.length; i++) {
				arCodePointsForReturn[i] = 0;
			}
		}

		if (rowString.equals(Const.textFieldFindORSeparator)) {
			return null;
		}

		if (arCodePointsForReturn != null) {
			if (arCodePointsForReturn.length == 2 && rowString.startsWith(Const.textFieldFindORSeparator)) {
				var result = divideStringOrNull(rowString.substring(Const.textFieldFindORSeparator.length()),
						arCodePointsForReturn);

				if (result) {
					int confirm = test ? JOptionPane.YES_OPTION : JOptionPane.CANCEL_OPTION;

					if (confirm != JOptionPane.YES_OPTION) {
						var s = formatConfirmYesNoMessage("Select search type:",
								"by char from " + arCodePointsForReturn[0] + " to " + arCodePointsForReturn[1],
								"text search as is", null);
						confirm = JOptionPane.showConfirmDialog(null, s, "Search by char codes",
								JOptionPane.YES_NO_CANCEL_OPTION);
					}

					if (confirm == JOptionPane.YES_OPTION) { // must be correct return 'OR' anyway (need for testInfo)
						substringsOrForReturn.add(rowString.substring(Const.textFieldFindORSeparator.length()));
						return null;
					}

					if (confirm != JOptionPane.NO_OPTION) { // means'cancel', empty 'OR' substrings, cancel search
						return null;
					}
				}
				// 'no option'
				arCodePointsForReturn[0] = 0;
				arCodePointsForReturn[1] = 0; // checking by that index, if '0' - no search by char codes
			}
		}

		String sAND = "";

		if (needANDexcluding) {
			if (rowString.equals(Const.textFieldLastANDSeparator)) {
				return null;
			}

			int lastIndexAND = rowString.lastIndexOf(Const.textFieldLastANDSeparator);

			if (lastIndexAND > 0) {// 'lastIndexAND' must be > 0 (s in result must not be empty)
				sAND = rowString.substring(lastIndexAND + Const.textFieldLastANDSeparator.length());
				rowString = rowString.substring(0, lastIndexAND);
			}

			if (rowString.isEmpty()) {
				return null;
			}
		}

		var substringsOr = splitStringBySeparatorOrNull(true, true, Const.textFieldFindORSeparator, rowString,
				toLowerCase);

		if (nullEmptyList(substringsOr)) {
			return null;
		}

		substringsOrForReturn.addAll(substringsOr);
		return sAND.isEmpty() ? null
				: splitStringBySeparatorOrNull(true, true, Const.textFieldFindORSeparator, sAND, toLowerCase);
	}

//s may be as int:int; may be :int; 'arCodePointsForReturn' must be created with length == 2
	private static boolean divideStringOrNull(String s, int[] arCodePointsForReturn) {
		try {
			if (s.length() < 2 || arCodePointsForReturn.length != 2) { // 's' example '0:' or ':1'
				return false;
			}

			int pos = s.indexOf(":");
			String s0 = "";
			String s1 = "";

			if (pos >= 0) {
				s0 = s.substring(0, pos);
				s1 = s.substring(pos + 1);
			}

			if (!s0.isEmpty() || !s1.isEmpty()) {
				arCodePointsForReturn[0] = s0.isEmpty() ? 0 : Integer.valueOf(s0);
				arCodePointsForReturn[1] = s1.isEmpty() ? Integer.MAX_VALUE : Integer.valueOf(s1);
				if (arCodePointsForReturn[0] <= arCodePointsForReturn[1] && arCodePointsForReturn[1] > 0) { // max_must_be>0
					return true;
				}
			}
		} catch (Exception e) {
		}

		arCodePointsForReturn[0] = 0;
		arCodePointsForReturn[1] = 0;
		return false;
	}

	/**
	 * Finds 'subStrings' in 'string'
	 * 
	 * @param findPosition 1:find in starts; 2:find in ends; else (example 0): any
	 *                     place 'stringInLowerCase'
	 * @param toLowerCase  1: 'string' will be set to lower case<br>
	 *                     2: 'each from 'substrings' will be set to lower case<br>
	 *                     3: '1' and '2': all strings will be set to lower case<br>
	 *                     else (example 0): no action, comparing as is
	 * @param string       string for finding, must not be null/empty
	 * @param subStrings   substrings for finding, must not be null/empty
	 * @return 'true' if found at least one 'subString' in 'string'
	 */
	static boolean findSubStringsInString(int findPosition, int toLowerCase, String string, List<String> subStrings) {
		if (nullEmptyString(string) || nullEmptyList(subStrings)) {
			return false;
		}

		if (toLowerCase < 0 || toLowerCase > 3) {
			toLowerCase = 0;
		}

		if (toLowerCase == 1 || toLowerCase == 3) {
			string = string.toLowerCase();
		}

		for (var subString : subStrings) {
			if (nullEmptyString(subString)) {
				continue;
			}

			if (toLowerCase >= 2) { // means 2 or 3
				subString = subString.toLowerCase();
			}

			if (findPosition == 1) {
				if (string.startsWith(subString)) {
					return true;
				}
			} else if (findPosition == 2) {
				if (string.endsWith(subString)) {
					return true;
				}
			} else if (string.contains(subString)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Finds 'subStrings' in path by name 'rowStringForPath'; if file length > 0 and
	 * < 10_000_000; strings must be in UTF-8
	 * 
	 * @param test             if true, returns total count of strings without
	 *                         search substrings
	 * @param toLowerCase      1: 'string' will be set to lower case<br>
	 *                         2: 'each from 'substrings' will be set to lower
	 *                         case<br>
	 *                         3: '1' and '2': all strings will be set to lower
	 *                         case<br>
	 *                         else (example 0): no action, comparing as is
	 * @param rowStringForPath string of path, where will be searching; path file
	 *                         must be exists and length no more 10 MB
	 * @param subStringsAND    substrings for finding, if not null/empty, will be
	 *                         FIRST search
	 * @param subStringsOr     substrings for finding, if search 'FIRST' without
	 *                         result, will be SECOND search<br>
	 *                         This parameter MUST NOT BE null/empty
	 * @param arCodePoints     if not null: search by chars codes, rules:<br>
	 *                         length must be 2 (minimal char code, maximal char
	 *                         code)<br>
	 *                         maximal must be >= minimal and > 0<br>
	 *                         parameter 'toLowerCase' may be 1 or 3 for set file
	 *                         strings to lower case<br>
	 *                         parameter 'subStringsOr' must not be null/empty
	 * @return if test == false: '-1' case error; '0': not found; '1': found at
	 *         least one 'subString' in 'string'<br>
	 *         if test == true: -1: case error, or count of lines
	 */

	static int getTextSearchResult(boolean test, int toLowerCase, String rowStringForPath, List<String> subStringsAND,
			List<String> subStringsOr, int[] arCodePoints) {
		if (!test && nullEmptyList(subStringsOr)) {
			return -1;
		}

		File file = null;
		try {
			file = Path.of(rowStringForPath).toFile();
			if (!file.exists() || file.isDirectory() || file.length() == 0L || file.length() > 10_000_000L) {
				return -1;
			}

		} catch (Exception e) {
			return -1;
		}

		boolean searchByChars = false;

		if (!test && arCodePoints != null) { // for 'test' not used
			if ((arCodePoints.length == 2) && (arCodePoints[0] <= arCodePoints[1]) && arCodePoints[1] > 0) {
				searchByChars = true;
			}
		}

		try (var br = Files.newBufferedReader(file.toPath())) {

			if (test) {
				return (int) br.lines().count();
			}

			String string;

			boolean needAnd = !searchByChars && notNullEmptyList(subStringsAND);

			while ((string = br.readLine()) != null) {

				if (searchByChars) {
					for (int i = 0; i < string.length(); i++) {
						int c = string.codePointAt(i);
						if (c >= arCodePoints[0] && c <= arCodePoints[1]) {
							return 1;
						}
					}
					continue;
				}

				boolean res = needAnd ? FileDataBase.findSubStringsInString(0, toLowerCase, string, subStringsAND)
						: true;

				if (res && FileDataBase.findSubStringsInString(0, toLowerCase, string, subStringsOr)) {
					return 1;
				}
			}

		} catch (Exception e) {
			return -1;
		}

		return 0;
	}

	/**
	 * Text search in each bean, by getFour(false, true)
	 * 
	 * @param beans
	 * @return null if error or correct 'addedInfo'
	 */
	static int[] testTextSearchOrNull(List<MyBean> beans) {
		if (nullEmptyList(beans)) {
			return null;
		}

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		for (var b : beans) {
			var res = false;
			var one = b.getOne();

			if (one.startsWith(Const.BRACE_TEST_ERROR_FULL)) {
				// res == false
			} else if (one.startsWith(Const.BRACE_START_TEST)) {
				res = true; // was done test and it not a error
			} else { // set TEST to 'one'
				var count = getTextSearchResult(true, 0, b.getFour(false, true), null, null, null);
				res = count > 0;

				String onePrefix = res ? Const.BRACE_START_TEST + count + Const.BRACE_END_WITH_SPACE
						: Const.BRACE_TEST_ERROR_FULL;
				b.setOne(onePrefix.concat(one));
			}

			if (b.check != res) {
				if (res) {
					addedInfo[0]++;
				} else {
					addedInfo[1]++;
				}
				b.check = res;
			}
		}

		return addedInfo;
	}

	// onlyAddSub: 0:"only", 1:"add", 2:"sub"
	static int[] selectedToCheckedOrNull(int onlyAddSub, BeansFourTableDefault myTable, List<MyBean> beans) {
		if (nullEmptyList(beans) || onlyAddSub < 0 || onlyAddSub > 2 || myTable == null) {
			return null;
		}

		int[] selectedRows = myTable.getSelectedRows();
		if (selectedRows.length == 0) {
			return null;
		}

		final boolean bOnly = onlyAddSub == 0;
		final boolean bSub = onlyAddSub == 2;

		Set<Integer> indexesSelectedSet = new HashSet<Integer>();
		for (int row : selectedRows) {
			indexesSelectedSet.add(row); // this 'set' must not be empty
		}

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			var selected = indexesSelectedSet.contains(i);

			var res = false;
			if (bOnly) { // for 'only': selected ==> checked
				res = selected;
			} else { // 'add' or 'sub'
				if (!selected) {
					continue;
				}
				res = !bSub;
			}

			if (b.check != res) {
				if (res) {
					addedInfo[0]++;
				} else {
					addedInfo[1]++;
				}
				b.check = res;
			}
		}

		return addedInfo;
	}

	static void testInfo(Component parentComponent, List<String> substringsAND, List<String> substringsOr,
			int[] arCodePoints) {
		var sb = new StringBuilder();
		sb.append("TEST need for 'textSearch'").append(NEW_LINE_UNIX)
				.append("To first column will be added '<test:' and lines count in file").append(NEW_LINE_UNIX)
				.append("If not error, that row will be checked.").append(NEW_LINE_UNIX).append(NEW_LINE_UNIX)
				.append("Information for current search:");

		sb.append(NEW_LINE_UNIX).append("Substrings AND");
		if (nullEmptyList(substringsAND)) {
			sb.append(": not defined");
		} else {
			sb.append(NEW_LINE_UNIX).append(substringsAND.toString());
		}

		sb.append(NEW_LINE_UNIX).append(NEW_LINE_UNIX).append("Substrings OR");
		if (nullEmptyList(substringsOr)) {
			sb.append(": not defined");
		} else {
			sb.append(NEW_LINE_UNIX).append(substringsOr.toString());
			if (arCodePoints != null && arCodePoints.length == 2 && arCodePoints[1] > 0) {
				sb.append(NEW_LINE_UNIX).append("Char search from ").append(arCodePoints[0]).append(" to ")
						.append(arCodePoints[1]);
			}
		}

		JOptionPane.showMessageDialog(parentComponent, sb.toString());
	}

	static String getTypeInfoString(int typeInfo) {
		return typeInfo == 1 ? "Files" : typeInfo == 2 ? "Folders" : "FilesFolders";
	}

	/**
	 * Generates one string from 'numbers' of 'beans'; in quotes with 'space'
	 * separator, opens result file.<br>
	 * NB: for Windows command line, the length of result string must be no more
	 * 8191 symbols
	 * 
	 * @param parentComponent may be null or frame, from be called that method; need
	 *                        for show 'message'
	 * @param behaviour       if result (exist files/folders) not empty:<br>
	 *                        0 (by default): do not offer open OpenWithTable (to
	 *                        string only)<br>
	 *                        1: offer open OpenWithTable<br>
	 *                        2: open OpenWithTable without confirm
	 * @param typeInfo        3 (by default): files, folders <br>
	 *                        1: files only<br>
	 *                        2: folders only<br>
	 * @param message         0 (by default): message anyway (except no correct
	 *                        'numbers' or 'beans')<br>
	 *                        1: message if found errors (no exists files or
	 *                        any)<br>
	 *                        2: show result file only<br>
	 *                        for 0,1,2: if empty result, will be message about it
	 *                        anyway<br>
	 *                        3: quiet mode; get result only
	 * @param numbers         correct numbers, path will be of 'getFour(true, true)'
	 * @param beans           correct beans
	 * @return generated string list of EXISTS files or null
	 */
	static List<File> toCommandLine(JDialog parentComponent, int behaviour, int typeInfo, int message,
			Set<Integer> numbers, List<MyBean> beans) {
		if (nullEmptyList(beans) || nullEmptySet(numbers)) {
			return null;
		}

		if (message < 0 || message > 3) {
			message = 0;
		}

		if (behaviour < 0 || behaviour > 2) {
			behaviour = 0;
		}

		final int idQuiet = 3;

		if (typeInfo < 1 || typeInfo > 3) {
			typeInfo = 3;
		}
		// 'typeInfoString' uses for file name and information
		final String typeInfoString = getTypeInfoString(typeInfo);

		int errorCount = 0;
		int existsCount = 0;
		int noExistsCount = 0;

		var sbExists = new StringBuilder();
		var sbNoExists = new StringBuilder();
		List<File> result = new ArrayList<>();

		for (var i : numbers) {
			if (i < 0 || i >= beans.size()) {
				errorCount++;
				continue;
			}

			try {
				var b = beans.get(i);
				boolean noExistsPrefix = b.isFourPrefixNoExists();

				var file = Path.of(b.getFour(noExistsPrefix, true)).toFile();

				if (file == null) {
					throw new NullPointerException();
				}

				StringBuilder sb;

				if (noExistsPrefix || !file.exists()) {
					noExistsCount++;
					sb = sbNoExists;
				} else {
					existsCount++;
					sb = sbExists;
					result.add(file);
				}

				if (!sb.isEmpty()) {
					sb.append(" ");
				}

				sb.append("\"").append(file.toString()).append("\"");

			} catch (Exception e) {
				errorCount++;
				continue;
			}
		}

		boolean foundErrors = (errorCount > 0) || (noExistsCount > 0);

		final String exists = sbExists.toString();

		if (message == idQuiet) {
			return result;
		}

		var sbPrefix = new StringBuilder();
		sbPrefix.append(typeInfoString).append(". Total count of chosen: ").append(numbers.size())
				.append("; of which errors: ").append(errorCount).append(", exists: ").append(existsCount)
				.append(", no exists: ").append(noExistsCount).append(NEW_LINE_UNIX).append(NEW_LINE_UNIX);

		if (behaviour > 0 && !result.isEmpty()) {
			int confirm = (behaviour == 2) ? JOptionPane.YES_OPTION : JOptionPane.CANCEL_OPTION;

			if (confirm != JOptionPane.YES_OPTION) {
				StringBuilder sbOfferConfirm = new StringBuilder(sbPrefix);
				sbOfferConfirm.append("<YES> open with table").append(NEW_LINE_UNIX);
				sbOfferConfirm.append("<NO> to string method");

				confirm = JOptionPane.showConfirmDialog(parentComponent, sbOfferConfirm.toString(), "Open with",
						JOptionPane.YES_NO_CANCEL_OPTION); // for 'no' - continue;
			}

			if (confirm != JOptionPane.NO_OPTION) {
				if (confirm == JOptionPane.YES_OPTION) { // call from table? no need 'callable' frame
					new OpenWithTable(parentComponent, typeInfo, result);
				}
				return result;
			}
		}

		final String noExists = sbNoExists.toString();

		boolean isResults = !exists.isEmpty() || !noExists.isEmpty();

		boolean needMessage = (message == 2) && (isResults) ? false : (message == 1) && (!foundErrors) ? false : true;
		var confirm = JOptionPane.CANCEL_OPTION;

		if (needMessage) {
			StringBuilder sbToCmdConfirm = new StringBuilder(sbPrefix);
			if (!isResults) {
				sbToCmdConfirm.append("Result is empty");
				JOptionPane.showMessageDialog(parentComponent, sbToCmdConfirm.toString());
				return result;
			}

			sbToCmdConfirm.append("<YES> exists only, string length: ").append(exists.length()).append(NEW_LINE_UNIX);
			sbToCmdConfirm.append("<NO> exists / no exists, with string length info: ").append(exists.length())
					.append(" / ").append(noExists.length());

			sbToCmdConfirm.append(NEW_LINE_UNIX).append(NEW_LINE_UNIX)
					.append("[NB: for Windows command line, the length of result string must be no more 8191 symbols]");
			confirm = JOptionPane.showConfirmDialog(parentComponent, sbToCmdConfirm.toString(), "to Command line",
					JOptionPane.YES_NO_CANCEL_OPTION);

		} else if (noExists.isEmpty()) {
			confirm = JOptionPane.YES_OPTION;
		} else {
			confirm = JOptionPane.NO_OPTION;
		}

		var sbResult = new StringBuilder();
		final String prefix = "commandLine" + File.separator;
		String name = null;

		StringBuilder sbCmd = null;

		if (confirm == JOptionPane.NO_OPTION) { // with info; 1 file creates

			sbResult.append(typeInfoString).append(" <EXISTS, count: ").append(existsCount).append("; length: ")
					.append(exists.length()).append(">").append(NEW_LINE_UNIX).append(exists).append(NEW_LINE_UNIX);
			sbResult.append("<NO EXISTS, count: ").append(noExistsCount).append("; length: ").append(noExists.length())
					.append(">").append(NEW_LINE_UNIX).append(noExists).append(NEW_LINE_UNIX);

			name = prefix.concat(typeInfoString).concat("_fullInfo");

		} else if (confirm == JOptionPane.YES_OPTION) { // 2 files creates

			if (exists.isEmpty()) {
				JOptionPane.showMessageDialog(parentComponent, "Result is empty");
				return result;
			}

			sbResult.append(exists);
			name = prefix.concat(typeInfoString);

			// create cmd file
			sbCmd = new StringBuilder();

			// need 'pause'?
			sbCmd.append("%1 ").append(exists);

		} else {
			return result;
		}

		saveAndShowList(false, true, 1, getTempPath(name + ".txt"), List.of(sbResult.toString()));

		if (sbCmd == null) {
			return result;
		}

		try {
			name = prefix.concat("dropProgramToOpen").concat(typeInfoString);
			var b = sbCmd.toString().getBytes(definedCharSet);
			saveAndShowList(true, false, 4, getTempPath(name + ".cmd"), List.of(new String(b)));
		} catch (Exception e) {
		}

		return result;
	}

} // of main class

class InputTextGUI {
	String result;

	/**
	 * Result will be in new 'class'.result <br>
	 * If user choose 'OK' - will be not null string from text field; else 'null'
	 * 
	 * @param parentComponent   may be null or 'this' for calling from Frame or
	 *                          Dialog
	 * @param appIsInitialValue if 'true', 'app' will be set to text field as
	 *                          'initial value'; if 'false', be written in caption
	 *                          dialog window
	 * @param description       will be written above text field
	 * @param app               if not null, will be set as empty string; can be
	 *                          either 'caption' or initial value; depends on
	 *                          'appIsInitialValue'
	 */
	public InputTextGUI(Component parentComponent, boolean appIsInitialValue, String description, String app) {
		if (app == null) {
			app = "";
		}
		this.result = appIsInitialValue ? JOptionPane.showInputDialog(description, app)
				: JOptionPane.showInputDialog(parentComponent, description, app, JOptionPane.INFORMATION_MESSAGE);
	}
}
