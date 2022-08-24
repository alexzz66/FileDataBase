package alexnick.filedatabase;

import java.awt.Color;
import java.io.File;
import java.nio.charset.Charset;

import alexnick.CommonLib;

public class Const {

//charset for class 'BufferTools' in com.mpatric.mp3agic; by default: "ISO-8859-1"	
	private static Charset localCharset = Charset.forName(System.getProperty("file.encoding"));// Charset.forName("cp1251");_>>_for_Russian_Windows
	public static final String defaultCharsetName = localCharset.toString();// "ISO-8859-1"; >> default_id3_charSet

	static final String BRACE_START = "<";// CopyMove.ERROR_BRACE_START;
	static final String BRACE_START_FIRST_SPACE = " " + BRACE_START;
	static final String BRACE_START_TEST = BRACE_START + "test: ";

	static final String BRACE_END = ">";
	static final String BRACE_END_WITH_SLASH_SPACE = "/> "; // CopyMove.ERROR_BRACE_END;
	static final String BRACE_END_WITH_SPACE = "> ";

	static final String BRACE_TEST_ERROR_FULL = BRACE_START_TEST + "error" + BRACE_END_WITH_SPACE;

	static final String ROOT_SEPARATOR_WINDOWS = ":" + File.separator;
	static final String UPDATE_BIN_COMPARING_REMINDER = CommonLib.NEW_LINE_UNIX + CommonLib.NEW_LINE_UNIX
			+ "Recommended update *.bin files before comparing";

// constant for init Program options; must starts/ends with ';' and be in lowerCase,without ' ' and ':'
	static final String OPTIONS_TEMP = ";temp;";
	static final String OPTIONS_TEMP_YES = ";tempyes;"; // set 'isTemp' without confirm

//by default, id3 will be extracted for NEW added 'mp3' to '*.bin'
	static final String OPTIONS_ID3_CONFIRM = ";id3confirm;";
	static final String OPTIONS_ID3_NO = ";id3no;"; // no extract id3 for 'mp3'

	static final String OPTIONS_FINAL_PAUSE = ";finalpause;";
	static final String OPTIONS_DO_BAK_TO_COPY_MOVE = ";bak;";
	static final String OPTIONS_BIG_SIZE_TEXTFIELD = ";bigsize;";

	static final String OPTIONS_PATHSLIST_CRC_YES = ";plcrcyes;";
	static final String OPTIONS_PATHSLIST_CRC_NO = ";plcrcno;";

	static final String OPTIONS_PATHSLIST_ONE_FILE_YES = ";plonefileyes;";
	static final String OPTIONS_PATHSLIST_ONE_FILE_NO = ";plonefileno;";
	static final String OPTIONS_PATHSLIST_ONE_FOLDER_YES = ";plonefolderyes;";
	static final String OPTIONS_PATHSLIST_ONE_FOLDER_NO = ";plonefolderno;";

	static final String OPTIONS_PATHSLIST_SINGLE_ONLY = ";plsingleonly;";
	static final String OPTIONS_PATHSLIST_SET_MULTI = ";plsetmulti;";
	static final String OPTIONS_PATHSLIST_SET_DRAG = ";plsetdrag;";

	static final String OPTIONS_VIEW_NO_ID3 = ";viewnoid3;";
	static final String OPTIONS_VIEW_NO_MARK = ";viewnomark;";

	static final String OPTIONS_EXTRACT_SAVE_YES = ";extractsaveyes;";
	static final String OPTIONS_AUTO_NO_EXTRACT = ";autonoextract;";
	static final String OPTIONS_DOUBLE_REPO = ";doublerepo;";
	static final String OPTIONS_TEST_FDB = ";test_fdb;";

	static final String OPTIONS_COMPARETWOBIN_NOFULLPATHS = ";comparetwobinnofullpaths;";

//when select 'replaceSourceDest...' for rename file, and no found 'source' in 'name', be set as rename error
	static final String OPTIONS_RENAME_REPLACE_NO_SUBSTRING_ERROR = ";replacenosubstringerror;";

	static final String OPTIONS_ONE_PATHSLIST_YES = ";modeoneplyes;"; // query to open 'PathsListTable' for
																		// MODE_STOP_ONE
	static final String EXTEMPTY = "<empty>";

	// groups names in ExtensionFrame
	static final String GROUPS_NO_GROUP = "<no group>";
	static final String BRACE_MARK = "**"; // do not change, '**' be replaced in 'id3' on "^^"
	static final String REMOVE_MARK = "*";

	static final String binFolderRepositorySignature = "~bin~data~repository";
	static final String keepFolderEmpty = "FilesInRepository";

	static final String binFolderStartSignature = "~bin~data~results~"; // folder name starts with this + number
	static final String dirSeparator = "~$$~";

	static final String extSeparator = " : ";
	static final String countSeparator = "; ";
	static final String skipInfoPrefix = "Skip: ";

	// DEFINED EXTENSIONS:
	static final String extensionForCalcId3 = ".mp3";
	static final String extensionForCalcId3Check = ">mp3"; // for 'needCalcId3'==ID3_EXTRACT_ALL

	static final String extensionProperties = ".properties";

	// save to base:
	static final String extensionBinList = ".bin";
	static final String extensionBinData = ".dat";

	// save to temp folder:
	static final String extensionTxt = ".txt";
	static final String extensionLog = ".log";

	// 'COPY_NEW_RESULT_POSTFIX' writes after "path list files' for saving result
	static final String COPY_NEW_RESULT_POSTFIX = "~result.txt";

	static final String GENERATED_BIN_NAME = "generated.bin";

// Paths of exts 'propertyExtsNeedList','propertyExtsNoNeedList' depends of 'globTEMP'
	static final String propertyExtsNeed = "~prop~extsNeed" + extensionProperties;
	static final String propertyExtsNoNeed = "~prop~extsNoNeed" + extensionProperties;

	static final String groupsProperty = "~prop~groups" + extensionProperties;
	static final String id3IsPropertyName = "~prop~id3Is" + extensionProperties;
	static final String markIsPropertyName = "~prop~markIs" + extensionProperties;

	static final String FOLDER_NAME_PROPERTY = "property";
	static final String COPY_MOVE_NEW_LIST = "~tmp~copyMoveNewlist.txt";

	static final int MR_NO_CHOOSED = -1;// JOptionPane.DEFAULT_OPTION;
	static final int MR_OK = 0; // JOptionPane.YES_OPTION;
	static final int MR_CANCEL = 2; // JOptionPane.CANCEL_OPTION;
	static final int MR_DELETE = 2048;
	static final int MR_COPY_MOVE = 2049;
	static final int MR_RENAME = 2050;
	static final int MR_NEED_UPDATE_BASE = 2051;

	static final int ID3_EXTRACT_NO = 0;
	static final int ID3_EXTRACT_NEW = 1; // by default
	static final int ID3_EXTRACT_ALL = 2;

	static final int MODE_NO_DEFINED = -1;

	static final int MODE_STOP_ONE = 1;
	static final int MODE_STOP_TWO = 2;
	static final int MODE_STOP_THREE = 3;
	static final int MODE_STOP_FOUR = 4;

	static final int MODE_AUTO = 1024;
	static final int MODE_EXTRACT = 1025;
	static final int MODE_PATHSLIST = 1026;
	static final int MODE_VIEW = 2048;// must be more than 10 as minimum
	static final int MODE_VIEW_TEMP = 2049;// must be more than 10 as minimum
	static final int MODE_COMPARE_BIN = 2050;
	static final int MODE_DELETE_EMPTY_DIR = 2051;
	static final int MODE_SYNC_BIN = 2052;

	static final int textFieldStardardSize = 20;
	static final int textFieldBigSize = 32;

//return correct value of 'MODE_STOP...', or MODE_NO_DEFINED 	
	static int checkModeStop(int value) {
		return (value == MODE_STOP_ONE || value == MODE_STOP_TWO || value == MODE_STOP_THREE || value == MODE_STOP_FOUR)
				? value
				: MODE_NO_DEFINED;
	}

	/*
	 * method was not needed, as it turned out... public static int
	 * checkOptions(boolean needModeAuto, int value) { if (value == MODE_AUTO) {
	 * return needModeAuto ? value : MODE_NO_DEFINED; } return (value ==
	 * MODE_EXTRACT || value == MODE_PATHSLIST || value == MODE_VIEW || value ==
	 * MODE_VIEW_TEMP) ? value : checkModeStop(value); }
	 */

	static final String PATHSLIST_NAME = "PATHS_LIST";

	static final String ALIAS_DATE = "<date>";
	static final String ALIAS_START_SEARCH = "<startSearch>";

	static final String ALIAS_DATE_ID = "<dateID>";
	static final String ALIAS_REPO_ID = "<repoID>";

	static final String ALIAS_FOUND_FILES = "Found files:";
	static final String ALIAS_FOUND_EXT = "<ResultBinInfo>";

	// for EqualSignatureMoveTable
	static final String MOVE_PREFIX_FOR_COLUMN_RESULT = "$MOVED ";
	static final String MOVE_PREFIX_FOR_COLUMN_GENERATED_PATH = "<MOVED_FROM> ";

	static final String ERROR_FILE_EXISTS_NO_BRACES = "dest file exists";
	static final String ERROR_FILE_EXISTS = BRACE_START + "error:" + ERROR_FILE_EXISTS_NO_BRACES + BRACE_END_WITH_SPACE;
	static final String ERROR_FILE_NOT_MOVED = BRACE_START + "error:file not moved" + BRACE_END_WITH_SPACE;

	// contains ':' to have restricted symbols in file name OS (windows)
	static final String EQUAL_SIZE = ":equal_size:";

	static final String prefixInTableForNoExists = "!  ";

	static final long MIN_DATE_MODIFIED_LONG = 0x111111111L; // suppose 9 symbols at 'toString'
	static final String NO_FOUND_PLUS = BRACE_START + "NO_FOUND" + BRACE_END_WITH_SPACE;
	static final String NO_DISK_PLUS = BRACE_START + "NO_DISK" + BRACE_END_WITH_SPACE;

	static final String textFieldBinFolderClick = "findBinFolder";
	static final String textFieldFindPathClick = "findPath";
	static final String textFieldFindMarkClick = "findMark";

	static final String textFieldFindORSeparator = "??";
	static final String textFieldLastANDSeparator = ">>";

	private final static String tfTipPrefix = "find substring in column '";
	private final static String tfTipPostfix = "', for several items, use separator '" + textFieldFindORSeparator
			+ "', AND filter at the end after '" + textFieldLastANDSeparator + "'";

	static final String textFieldBinFolderToolTip = tfTipPrefix + "<set in combobox>" + tfTipPostfix;
	static final String textFieldPathToolTip = tfTipPrefix + "Path" + tfTipPostfix;
	static final String textFieldMarkToolTip = tfTipPrefix + "Mark" + tfTipPostfix;
	static final String cmbOnlyAddSubToolTip = "set 'only' checked / 'add' found to checked / 'sub' uncheck found. If ends with 'case', case sensitive";

	static final String butCheckToolTip = "check filter(if defined)/all/no";
	static final String butCheckToolTipFilesFolders = "check filter/files/folders(if defined)/all/no";
	static final String butSkipYesNoCaption = "skip";

	static final String DIRECTORY_ALIAS = "<directory>"; // not must be empty
	static final String CRC_ERROR_ALIAS = "<crc_error>";
	static final String CRC_NO_CALCULATED_ALIAS = "<crc_no_calculated>";

	static final String columnSignatureSeparator = "; ";// in column 'Size; signature'

	static final String OLD_NAME_START = "old: \"";
	static final String OLD_NAME_END = "\""; // need for write rename result in PathsListTable
	static final String ERROR_OLD_DELETE_ON_MOVING = "errorOldDeleteOnMoving.txt";
	static final String NOT_CHOSEN = "not chosen";
	static final Color LIGHT_COLOR = new Color(238, 238, 238);
}
