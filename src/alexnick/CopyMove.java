package alexnick;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CopyMove {
	public static final int QUERY_NO_TO_ALL = -1;
	public static final int QUERY_YES_TO_ALL = 0;
	public static final int QUERY_CONFIRM_EACH = 1;
	public static final int QUERY_CONFIRM_LIST = 2;

	private static int queryCopyMove = QUERY_CONFIRM_LIST;

	private static final String BACK_EXT = ".bak";
	private static final String PREFIX_FDB = "~FDB~";
	private static final String CONFIRM_YES = "<YES>";
	private static final String CONFIRM_YES_TO_ALL = "<YES_TO_ALL>";
	private static final String CONFIRM_NO = "<NO>";
	private static final String CONFIRM_NO_TO_ALL = "<NO_TO_ALL>";
	private static final String FILE_LENGTH_APPEND = " <file_length>";
	private static final String CANCELLED_BY_USER = "CANCELLED_BY_USER";

	public static final String ERROR_BRACE_START = "<"; // not must be empty
	public static final String ERROR_BRACE_END = "/> ";

	public static final int DeleteIfExists_ERROR = 0;
	public static final int DeleteIfExists_OLD_DELETE = 1;
	public static final int DeleteIfExists_OLD_RENAME_TO_BAK = 2;
	public static final int DeleteIfExists_NEW_SAVE_WITH_OTHER_NAME = 3;

	public static final int CopyMove_ERROR_VALUE = -1;
	public static final int CopyMove_COPY_ONLY = 0;
	public static final int CopyMove_AUTO_ATOMIC = 1;
	public static final int CopyMove_AUTO_NO_ATOMIC = 2;
	public static final int CopyMove_MOVE_ONLY_ATOMIC = 3;
	public static final int CopyMove_MOVE_ONLY_NO_ATOMIC = 4;

	private int deleteIfExistsMode = DeleteIfExists_ERROR;
	private int copyMoveMode = CopyMove_ERROR_VALUE;
	private List<String> errorOldDeleteOnMovingList;

//STATIC METHODS		

	/**
	 * Defines confirmation QUERY_CONFIRM_EACH' or 'QUERY_CONFIRM_LIST' before
	 * operation copy/move/delete
	 * 
	 * @param confirmOnly if 'true': check only need confirmation; if 'false', check
	 *                    also defined 'QUERY_YES_TO_ALL'
	 * @return
	 */
	synchronized public static boolean queryCopyMoveDefined(boolean confirmOnly) {
		var confirm = queryCopyMove == QUERY_CONFIRM_EACH || queryCopyMove == QUERY_CONFIRM_LIST;
		var result = confirmOnly ? confirm : (confirm || queryCopyMove == QUERY_YES_TO_ALL);
		return result;
	}

	public static int getQueryCopyMove() {
		return queryCopyMove;
	}

	public static void setQueryCopyMove(int queryCopyMoveInit) {// QUERY_NO_TO_ALL not set
		queryCopyMove = queryCopyMoveInit;
		if (!queryCopyMoveDefined(false)) {
			queryCopyMove = QUERY_CONFIRM_LIST;
		}
	}

	synchronized public static boolean checkQueryResultYesOrYesToAll(String queryResult) {
		return queryResult.equals(CONFIRM_YES) || queryResult.equals(CONFIRM_YES_TO_ALL);
	}

	/**
	 * Confirmation before operation, if user has chosen
	 * 'CONFIRM_YES_TO_ALL'/'CONFIRM_NO_TO_ALL': BE SET GLOBAL 'queryCopyMove' to
	 * 'QUERY_YES_TO_ALL'/'QUERY_NO_TO_ALL'
	 * 
	 * @param confirmCapt be print on console before confirm
	 * @return
	 */
	synchronized public static String getQueryResult(String confirmCapt) {
		if (queryCopyMove == QUERY_CONFIRM_EACH) {
			return CommonLib.pauseQueryOne(confirmCapt) ? CONFIRM_YES : CONFIRM_NO;
		}
		System.out.println(confirmCapt);
		List<String> confirmList = new ArrayList<String>();
		confirmList.add(CONFIRM_YES); // 0
		confirmList.add(CONFIRM_YES_TO_ALL);// 1
		confirmList.add(CONFIRM_NO);// 2
		confirmList.add(CONFIRM_NO_TO_ALL);// 3

		int confirm = CommonLib.pauseQueryList(confirmList, null);
		if (confirm < 0) {
			confirm = confirmList.size() - 1;
		}
		if (confirm == 1) {
			queryCopyMove = QUERY_YES_TO_ALL; // no more confirm
		} else if (confirm == 3) {
			queryCopyMove = QUERY_NO_TO_ALL;
		}
		return confirmList.get(confirm);
	}

	/**
	 * Creates prefix for name file or directory; from now date
	 * 
	 * @param withSec if 'false', result ends on '-', if 'true', be added sec
	 * @return prefix with starts '~FDB~
	 */
	synchronized public static String getPrefixName(boolean withSec) {
		var s = "yyyy-MM-dd-HH-mm-";
		if (withSec) {
			s = s + "ss";
		}
		SimpleDateFormat frm = new SimpleDateFormat(s);
		return PREFIX_FDB + frm.format(new Date());
	}

	/**
	 * Renamed 'path' with append 'appendString';<br>
	 * will be error if path already ends on 'appendString';<br>
	 * old back file 'path' + 'appendString' will be deleted, if exists
	 * 
	 * @param path         if no exists: error; otherwise be renamed with
	 * @param appendString be TRIMMED, after it, not must be empty, ends on '.' and
	 *                     contain illegal file name symbols.<br>
	 *                     Recommended starts on '.', example: '.bak';<br>
	 *                     This string be append with file name
	 * @throws Exception if error operation
	 */
	synchronized public static void renameToBack(Path path, String appendString) throws Exception {
		if (!path.toFile().exists() || CommonLib.nullEmptyString(appendString)) {
			throw new IllegalArgumentException("Path must be exists and back extension not must be empty");
		}
		appendString = appendString.trim();
		if (appendString.isEmpty() || appendString.endsWith(".")) {
			throw new IllegalArgumentException("Trimmed append string not must be empty and ends on '.'");
		}

		String s = path.toString();
		if (s.toLowerCase().endsWith(appendString.toLowerCase())) {
			throw new IllegalArgumentException("Path must not end with '" + appendString + "'");
		}
		Path back = Path.of(s.concat(appendString));
		if (back.toFile().exists()) {
			Files.delete(back);
		}
		Files.move(path, back, StandardCopyOption.ATOMIC_MOVE);
	}

	/**
	 * Append to 'path' string '.bak'; if this file exists, will be deleted;<br>
	 * NB: 'path' must not end with '.bak'; character case does not matter
	 * 
	 * @param path
	 * @throws Exception
	 */
	synchronized public static void renameToBack(Path path) throws Exception {
		renameToBack(path, BACK_EXT);
	}

	/**
	 * Extracts root directory (disk) from path
	 * 
	 * @param path where be extracted 'root'
	 * @return empty string if error, or root with UPPER CASE, with '\' on end
	 */
	synchronized public static String getRootUpperCaseWithFileSeparator(Path path) {
		String res = path.getRoot().toString().toUpperCase();
		return (CommonLib.nullEmptyString(res)) ? "" : CommonLib.fileSeparatorAddIfNeed(false, true, res);
	}

	/**
	 * Creates after confirm new sub folder in 'oldFolder' with prefix '~FDB~' +
	 * 'date-time' with second; OR remains 'oldFolder', if cancelled
	 * 
	 * @param oldFolder must be exists and must be directory, no checking here
	 * @param log       if not null, and be created new folder: be added to
	 *                  information about it
	 * @return new path or remains old path
	 */
	public static Path createNewFolder(Path oldFolder, List<String> log) {
		Path newFolder = Path.of(oldFolder.toString(), getPrefixName(true));
		if (CommonLib.pauseQueryOne(
				"Create new directory? " + newFolder + CommonLib.NEW_LINE_UNIX + " if 'cancel', be " + oldFolder)) {
			CommonLib.addLog("Be copy/move to folder " + newFolder, false, log);
		} else {
			newFolder = oldFolder;
		}
		return newFolder;
	}

//NO STATIC METHODS BELOW	

	/**
	 * @param deleteIfExistsMode be called 'copyFile' method with one;
	 * 
	 *                           if 'destination' file is exists:
	 * 
	 *                           DeleteIfExists_ERROR (by default) :error operation;
	 * 
	 *                           DeleteIfExists_OLD_DELETE:deletes 'destination'
	 *                           file before operation;
	 * 
	 *                           DeleteIfExists_OLD_RENAME_TO_BAK:old file of
	 *                           'destination' be renamed with add '.bak';
	 *                           WARNING:be error if 'destination' ends on '.bak';
	 * 
	 *                           DeleteIfExists_NEW_SAVE_WITH_OTHER_NAME:'destination'
	 *                           file not changes, but 'source' file get new name
	 *                           with prefix ("~FDB~" + current date-time)
	 * 
	 * @param copying            set 'true' for copy file;
	 * @param moving             set 'true' for move file;
	 * 
	 * @return NB: if 'copying' and 'moving' set as 'true' be auto define:<br>
	 *         case the same dik:'moving' (atomic method); case other disk:'copying'
	 *         <p>
	 * 
	 *         NB: private string list 'errorOldDeleteOnMovingList': if was been
	 *         moving 'no atomic', and 'old file' not been deleted, path of not
	 *         deleted file be added to
	 * 
	 */
	public CopyMove(int deleteIfExistsMode, boolean copying, boolean moving) {
		this.errorOldDeleteOnMovingList = new ArrayList<String>();
		setCopyMoveModeDefault(copying, moving);
		setDeleteIfExistsMode(deleteIfExistsMode);
	}

	public void setCopyMoveModeDefault(boolean copying, boolean moving) {
		setCopyMoveMode(!copying && !moving ? CopyMove_ERROR_VALUE
				: copying && moving ? CopyMove_AUTO_ATOMIC : copying ? CopyMove_COPY_ONLY : CopyMove_MOVE_ONLY_ATOMIC);
		return;
	}

	/**
	 * @param copyMoveMode Recommended values: only copy:COPY_ONLY;<br>
	 *                     only move:MOVE_ONLY_ATOMIC;<br>
	 *                     auto(copy or move):AUTO_ATOMIC.
	 *                     <p>
	 * 
	 *                     0:COPY_ONLY: 'COPY' all files<br>
	 *                     1:AUTO_ATOMIC (quickly): 'COPY' to other disk, 'MOVE'
	 *                     atomic to the same disk;<br>
	 *                     2:AUTO_NO_ATOMIC (longer):'COPY' to other disk, 'MOVE' no
	 *                     atomic to the same disk; <br>
	 *                     3:MOVE_ONLY_ATOMIC (quickly):to other disk - no atomic,
	 *                     to the same - atomic;<br>
	 *                     4:MOVE_ONLY_NO_ATOMIC (longer):to other disk and to the
	 *                     same disk - no atomic move.
	 */
	public void setCopyMoveMode(int value) {
		if (value == CopyMove_COPY_ONLY || value == CopyMove_AUTO_ATOMIC || value == CopyMove_AUTO_NO_ATOMIC
				|| value == CopyMove_MOVE_ONLY_ATOMIC || value == CopyMove_MOVE_ONLY_NO_ATOMIC) {
			copyMoveMode = value;
		} else {
			throw new IllegalArgumentException("choose copy or move mode");
		}
	}

	public void setDeleteIfExistsMode(int value) {
		if (value == DeleteIfExists_OLD_DELETE || value == DeleteIfExists_OLD_RENAME_TO_BAK
				|| value == DeleteIfExists_NEW_SAVE_WITH_OTHER_NAME) {
			deleteIfExistsMode = value;
		} else {
			deleteIfExistsMode = DeleteIfExists_ERROR;
		}
	}

	/**
	 * Copy/move 'source' to 'dest', BOTH must be not directory.
	 * 
	 * @param queryCopyMoveInit Set global 'queryCopyMove':<br>
	 *                          QUERY_YES_TO_ALL: no confirm before copying;<br>
	 *                          QUERY_CONFIRM_EACH: simple confirm ('1' for
	 *                          'YES');<br>
	 *                          QUERY_CONFIRM_LIST: list confirm
	 *                          (YES,YES_TO_ALL,NO,NO_TO_ALL), user select will be
	 *                          in result string in '< >';<br>
	 *                          for 'YES_TO_ALL', will be set as QUERY_YES_TO_ALL
	 *                          ,<br>
	 *                          for 'NO_TO_ALL' will be set as QUERY_NO_TO_ALL;
	 * @param source            path of source file, must exists
	 * @param dest              path of destination file. If path to 'dest' no
	 *                          exists, will be created directories
	 * @return string for writing log about result copying
	 */
	synchronized public String copyMoveFile(int queryCopyMoveInit, Path source, Path dest) {
		StringBuilder sb = new StringBuilder();
		sb.append(source.toString()).append(" >> ");
		String prefix = "";
		setQueryCopyMove(queryCopyMoveInit);
		String queryResult = ""; // if not empty, be added in result string;
		boolean isMove = false; // false: copying; true: moving
		boolean equalsSourceDestRoots = source.getRoot().toString().equalsIgnoreCase(dest.getRoot().toString());
		try {
			if (copyMoveMode == CopyMove_COPY_ONLY) {
				isMove = false;
			} else if (copyMoveMode == CopyMove_MOVE_ONLY_ATOMIC || copyMoveMode == CopyMove_MOVE_ONLY_NO_ATOMIC) {
				isMove = true;
			} else if (copyMoveMode == CopyMove_AUTO_ATOMIC || copyMoveMode == CopyMove_AUTO_NO_ATOMIC) {
				isMove = equalsSourceDestRoots;
			} else {
				throw new IllegalArgumentException("not defined copy/move mode");
			}

			if (!source.toFile().exists()) {
				throw new IllegalArgumentException("source file must exists");
			}
			if (source.toFile().isDirectory()) {
				throw new IllegalArgumentException("source file's directory");
			}
			if (dest.toFile().isDirectory()) {
				throw new IllegalArgumentException("destination file's directory");
			}

			if (dest.toFile().exists()) {
				var nullLength = dest.toFile().length() == 0;
				if (deleteIfExistsMode == DeleteIfExists_OLD_DELETE || nullLength) {
					tryDelete(nullLength, true, dest, sb);
				} else if (deleteIfExistsMode == DeleteIfExists_OLD_RENAME_TO_BAK) {
					renameToBack(dest); // after 'dest' not be exists, or throw
					sb.append("[OLD_RENAMED_TO_BAK] ");
				} else if (deleteIfExistsMode == DeleteIfExists_NEW_SAVE_WITH_OTHER_NAME) {
					if (prefix.isEmpty()) {
						prefix = getPrefixName(false);
					}
					dest = Path.of(dest.getParent().toString(), prefix + dest.toFile().getName());
					sb.append("[PREFIX_NEW_NAME] ");
				} else {
					throw new IOException("destination file is exists");
				}
			}

			if (queryCopyMoveDefined(true)) {
				System.out.println(CommonLib.NEW_LINE_UNIX + CommonLib.PRINTDELIMITER);
				var capt = isMove ? " [MOVE_TO] " : " [COPY_TO] ";
				var confirmCapt = CommonLib.NEW_LINE_UNIX + "CONFIRM, size: "
						+ CommonLib.bytesToKBMB(false, 2, source.toFile().length()) + "; " + source.toString() + capt
						+ dest.toString();
				queryResult = getQueryResult(confirmCapt);
				if (!checkQueryResultYesOrYesToAll(queryResult)) {
					throw new IOException(CANCELLED_BY_USER);
				}
			}
			CommonLib.CreateDirectoriesIfNeed(dest);

			Path resPath = null;
			long fileLengthSource = source.toFile().length(); // atomic for 'copyMoveMode'
			if (isMove && equalsSourceDestRoots
					&& (copyMoveMode == CopyMove_AUTO_ATOMIC || copyMoveMode == CopyMove_MOVE_ONLY_ATOMIC)) {
				resPath = Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
			} else {// copy or no atomic move: no equals 'source' and 'dest';
					// CopyMove_MOVE_ONLY_NO_ATOMIC or CopyMove_AUTO_NO_ATOMIC
				resPath = Files.copy(source, dest, StandardCopyOption.COPY_ATTRIBUTES);
			}
			if (resPath == null) { // not must be so
				throw new Exception("result operation's null");
			}

			if (resPath.toFile().length() != fileLengthSource) {
				throw new Exception("different file length 'source' and result 'dest' files");
			}

			sb.append(isMove ? "[MOVE_TO" : "[COPY_TO");

			if (!queryResult.isEmpty()) {
				sb.append(" : ").append(queryResult);
			}
			sb.append("] ");

			if (isMove && source.toFile().exists()) {
				if (!tryDelete(false, false, source, sb) && errorOldDeleteOnMovingList != null) {
					errorOldDeleteOnMovingList.add(source.toString());
				}
			}
			sb.append(resPath.toString()).append(FILE_LENGTH_APPEND).append(String.valueOf(fileLengthSource));
			return sb.toString();
		} catch (Exception e) { // starts with BRACE_START is define of error operation
			sb.append(dest);
			StringBuilder err = new StringBuilder();
			err.append(ERROR_BRACE_START).append("ERROR ").append(isMove ? "MOVE" : "COPY").append(ERROR_BRACE_END)
					.append(" [").append(e.getMessage());
			if (!queryResult.isEmpty()) {
				err.append(" : ").append(queryResult);
			}
			err.append("] ").append(sb.toString());
			return err.toString();
		}
	}

	private boolean tryDelete(boolean nullLength, boolean newThrowOnCatch, Path path, StringBuilder sb)
			throws Exception {
		var deleted = true;
		try {
			Files.delete(path);
		} catch (Exception e) {
			deleted = false;
			if (newThrowOnCatch) {
				throw new Exception("can't delete 'old' file" + (nullLength ? " ('old' length is 0)" : ""));
			}
		}
		if (sb != null) {
			sb.append(!deleted ? "[ERROR_OLD_DELETE] " : nullLength ? "[OLD_NULL_LENGTH_DELETED] " : "[OLD_DELETED] ");
		}
		return deleted;
	}

	/**
	 * Copies/moves files (no folders) from 'sourceList' to 'destFolder'; creates
	 * directories, if need;
	 * 
	 * @param queryCopyMoveInit             Set global 'queryCopyMove'<br>
	 *                                      QUERY_YES_TO_ALL: no confirm before
	 *                                      copying;<br>
	 *                                      QUERY_CONFIRM_EACH: simple confirm ('1'
	 *                                      for 'YES');<br>
	 *                                      QUERY_CONFIRM_LIST: list confirm
	 *                                      (YES,YES_TO_ALL,NO,NO_TO_ALL), user
	 *                                      select be in result string in '< >';<br>
	 *                                      for 'YES_TO_ALL', be set as
	 *                                      QUERY_YES_TO_ALL ,<br>
	 *                                      for 'NO_TO_ALL' be set as
	 *                                      QUERY_NO_TO_ALL;
	 * 
	 * @param writeSourceListBeforeCopying  0 (by default): no write
	 *                                      'sourceList';<br>
	 *                                      1:write on console only;<br>
	 *                                      2: write in log (if log != null);<br>
	 *                                      3: write on console and log;<br>
	 *                                      NB: 'sourceList' be sorted;
	 * 
	 *                                      Recommended values:<br>
	 *                                      0:only copy;<br>
	 *                                      3:only move;<br>
	 *                                      1:auto(copy or move).
	 * @param checkDestEqualPathIfOtherDisk example for 'destFolder' "d:/1" and
	 *                                      source file:<br>
	 *                                      "c:/1/2.txt" -><br>
	 *                                      if false:"d:/1/1/2.txt";<br>
	 *                                      if true: "d:/1/2.txt"
	 * @param caption                       short info about copying, example "New
	 *                                      files";
	 * @param removeFromSourceForExchange   if >= 3, be defined EXCHANGE method:
	 *                                      from start each path in 'sourceList'
	 *                                      will be removed specified count of
	 *                                      symbols;<br>
	 *                                      and end of this path, be added to
	 *                                      'destFolder';<br>
	 *                                      in other words, that's length of
	 *                                      'sourceFolder' with '\';<br>
	 *                                      NB: on EXCHANGE method,
	 *                                      'checkDestEqualPathIfOtherDisk' does not
	 *                                      matter
	 * @param destFolder                    folder, where been copying files, if not
	 *                                      exists, be created; must be root, in
	 *                                      that case be confirm
	 * @param sourceList                    must be created and no empty; must
	 *                                      contains paths files only
	 * @param log                           if not null, be wrote all details of
	 *                                      this method
	 * @return count copied files, 0 or more
	 */
	synchronized public int backUpCopyMoveFiles(int queryCopyMoveInit, int writeSourceListBeforeCopying,
			boolean checkDestEqualPathIfOtherDisk, String caption, int removeFromSourceForExchange, Path destFolder,
			List<Path> sourceList, List<String> log) {
		int copyCount = 0;
		long copyTotalSize = 0;
		try {
			if (sourceList.isEmpty()) {
				throw new IllegalArgumentException("source list is empty");
			}
			destFolder = destFolder.toAbsolutePath();
			if (!destFolder.toFile().exists()) {
				Files.createDirectories(destFolder);
			} else if (!destFolder.toFile().isDirectory()) {
				throw new IllegalArgumentException("destination folder is file, but required directory");
			}

			String destFolderString = destFolder.toString();
			destFolderString = CommonLib.fileSeparatorAddIfNeed(false, true, destFolderString);

			String rootDestFolderUpperCase = getRootUpperCaseWithFileSeparator(destFolder);

			if (rootDestFolderUpperCase.isEmpty()) {
				throw new IllegalArgumentException("error of define 'root' folder");
			}

			if (rootDestFolderUpperCase.equalsIgnoreCase(destFolderString)) {
				if (!CommonLib.pauseQueryOne("Do you REALLY WANT do backup on DISK instead of FOLDER? Choosed disk: "
						+ rootDestFolderUpperCase)) {
					throw new IllegalArgumentException("Cancelled by USER: destination folder is 'root'");
				}
			}

			if (CommonLib.notNullEmptyList(log)) {
				CommonLib.addLog(CommonLib.ADDLOG_SEP, false, log);
			}

			CommonLib.addLog(caption + " ===>> start copy/move, count: " + sourceList.size(), true, log);
			CommonLib.addLog("Destination folder: " + destFolder, true, log);

			// 'sort' need for checking equals paths
			sourceList.sort(Comparator.comparing(path -> path.toString().toLowerCase()));
			if (writeSourceListBeforeCopying < 0 || writeSourceListBeforeCopying > 3) {
				writeSourceListBeforeCopying = 0;
			}
			if (writeSourceListBeforeCopying == 2 && log == null) {
				writeSourceListBeforeCopying = 0;
			}

			if (writeSourceListBeforeCopying > 0) {
				var onConsole = writeSourceListBeforeCopying != 2;
				var tmpLog = writeSourceListBeforeCopying > 1 ? log : null;
				CommonLib.addLog(CommonLib.ADDLOG_SEP, onConsole, tmpLog);
				CommonLib.addLog("Files in path list:" + CommonLib.NEW_LINE_UNIX, onConsole, tmpLog);
				for (var path : sourceList) {
					CommonLib.addLog(path.toString(), onConsole, tmpLog);
				}
				CommonLib.addLog(CommonLib.ADDLOG_SEP, onConsole, tmpLog);
			}

			String equalReplaceLowerCase = destFolderString.substring(rootDestFolderUpperCase.length()).toLowerCase();

			String previous = "";
			int lenDelete = 0;
			final String resultEndInfo = "/" + sourceList.size() + ")]";

			setQueryCopyMove(queryCopyMoveInit); // first initialization 'queryCopyMove'

			for (int i = 0; i < sourceList.size(); i++) {
				Path sourceFile = sourceList.get(i).toAbsolutePath();
				var sourceFileString = sourceFile.toString();

				// exclude equals (by ignore case string) paths
				boolean bEquals = i > 0 && sourceFileString.equalsIgnoreCase(previous);
				previous = sourceFileString;
				if (bEquals) {
					CommonLib.addLog("ERROR, file has equal path, that be copied in this list early ' " + sourceFile,
							true, log);
					continue;
				}

// 'rootSourceFolderUpperCase' no need for 'exchange method', but set on that place, for check 'root disk'
				String rootSourceFolderUpperCase = getRootUpperCaseWithFileSeparator(sourceFile);
				if (rootSourceFolderUpperCase.isEmpty()) {
					CommonLib.addLog("ERROR, not defined 'root disk' " + sourceFile, true, log);
					continue;
				}

				if (removeFromSourceForExchange >= 3) { // exchange method
					lenDelete = removeFromSourceForExchange;
				} else {
					// 'rootSourceFolderUpperCase' need here, no for 'exchange method'
					var sourceFolderString = (sourceFileString.length() < rootSourceFolderUpperCase.length()
							+ equalReplaceLowerCase.length()) ? ""
									: sourceFileString.substring(0,
											rootSourceFolderUpperCase.length() + equalReplaceLowerCase.length());

					if (sourceFolderString.equalsIgnoreCase(destFolderString)) {
						CommonLib.addLog(
								"ERROR,  source file not must be copied/moved in the same folder " + sourceFile, true,
								log);
						continue;
					}

					lenDelete = (checkDestEqualPathIfOtherDisk && !equalReplaceLowerCase.isEmpty()
							&& !rootSourceFolderUpperCase.equals(rootDestFolderUpperCase)
							&& sourceFolderString.toLowerCase().endsWith(equalReplaceLowerCase))
									? sourceFolderString.length()
									: rootSourceFolderUpperCase.length();
				}

				var s0 = copyMoveFile(queryCopyMove, sourceFile,
						Path.of(destFolderString, sourceFileString.substring(lenDelete)));

				if (!s0.startsWith(ERROR_BRACE_START)) {
					copyCount++;
					var posFileLength = s0.indexOf(FILE_LENGTH_APPEND);
					if (posFileLength > 0) {
						try {
							posFileLength += FILE_LENGTH_APPEND.length();
							var sub = s0.substring(posFileLength);
							copyTotalSize += Long.valueOf(sub);
							s0 += " [N:" + (i + 1) + " (" + copyCount + resultEndInfo;
						} catch (Exception e) {
						}
					}
				}
				CommonLib.addLog(s0, true, log);
				if (queryCopyMove == QUERY_NO_TO_ALL) {
					throw new Exception("ALL_CANCELLED_BY_USER");
				}
			}
		} catch (Exception e) {
			CommonLib.addLog("ERROR of BackUp Copying method: " + e.getMessage(), true, log);
		}
		CommonLib.addLog(CommonLib.ADDLOG_SEP, true, log);
		CommonLib.addLog("Result: " + copyCount + " of " + sourceList.size() + ", total size: "
				+ CommonLib.bytesToKBMB(false, 0, copyTotalSize), true, log);
		return copyCount;
	}

	public List<String> getErrorOldDeleteOnMovingList() {
		return errorOldDeleteOnMovingList;
	}
}
