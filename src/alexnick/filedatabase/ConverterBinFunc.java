package alexnick.filedatabase;

import static alexnick.CommonLib.bytesToKBMB;
import static alexnick.CommonLib.dateModifiedToString;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alexnick.CommonLib;

/**
 * @author alexey
 *
 *         Created: 25.04.2022 14:54:16 Contains static methods for convert to
 *         *.bin format; rules:
 * 
 *         bin entry like so
 *         'sizeToHex(crc)datyModifiedToHex*<pathWithoutStartPathLength>extInLowerCase
 *         (or empty, if not extension) Example: file D:\test\Document.TXT with
 *         length = 431 bytes, crc = 255, date modified = 2012.08.01 01:08:17,
 *         if 'startPath' be 'D:\test' be created as:
 *         000001af(ff)138dfb9e347*<Document>txt
 * 
 *         Length of 'sizeToHex' not must be less that 8 symbols; NB: more then
 *         '8' symbols, in case file length more than 4GB
 * 
 *         between '*' and '<', that is between 'binStart' and 'binEnd', may be
 *         any comment, but usually it's empty (recommended)
 * 
 */
public class ConverterBinFunc {

	/**
	 * @param columnBinFolderId3Mark if not null, be info for table columns
	 *                               'BinFolder [ID3] **mark'| Size,Signature'
	 *                               instead of 'CRC | Size'; <br>
	 *                               in same time, initialized
	 *                               BinCreator.id3IsProperty
	 * @param startPath              disk/folder must be not empty for write inf
	 *                               about duplicates to binInf, else must be
	 *                               null/empty
	 * @param binItem                string from *.bin file, not must be empty
	 * @param appendInf              must be null/empty, but if length == 1 (example
	 *                               "1") then be written info about size and date
	 *                               modified (be taken from 'binItem'); if length >
	 *                               1 and correct 'appendInf', be also added after
	 *                               ' : ' info (be taken from 'appendInf')
	 * @param binPath                must be null, but if beans is filling, and
	 *                               binPath not null - be added
	 * @param setExts                initialized where need set extension only,
	 *                               empty as Const.EXTEMPTY
	 * @param beans                  if not null, be filling
	 * @return extracted path string from 'binItem', be added startPath, if no
	 *         empty, and appendInf if not null/empty
	 */
	synchronized static String getPathStringFromBinItem(String[] columnBinFolderId3Mark, String startPath,
			String binItem, String appendInf, Path binPath, Set<String> setExts, List<MyBean> beans) {
		if (binItem.isEmpty()) {
			return "";
		}
		var posBeginOfBinEnd = binItem.lastIndexOf(Const.BRACE_START);
		if (posBeginOfBinEnd < 0 || posBeginOfBinEnd == binItem.length() - 1) {
			return "";
		}
		var pathStringTmp = binItem.substring(posBeginOfBinEnd + 1);
		var posIndexOfExt = pathStringTmp.indexOf(Const.BRACE_END);
		if (posIndexOfExt < 0) {
			return "";
		}
		String ext = (posIndexOfExt == pathStringTmp.length() - 1) ? "" : pathStringTmp.substring(posIndexOfExt + 1);

		if (setExts != null) {
			var tmp = ext.isEmpty() ? Const.EXTEMPTY : ext;
			if (!setExts.contains(tmp)) {
				return "";
			}
		}

		if (!ext.isEmpty()) {
			ext = "." + ext;
		}

		var sbPathString = new StringBuilder();
		CommonLib.appendNotNullEmpty(startPath, sbPathString);
		sbPathString.append((ext.isEmpty()) ? pathStringTmp.substring(0, posIndexOfExt)
				: pathStringTmp.replace(Const.BRACE_END, "."));

		if (CommonLib.notNullEmptyString(appendInf)) {
			fillSbPathString(appendInf, binItem, sbPathString);
		}

		if (beans != null) {
			var name = pathStringTmp.substring(0, posIndexOfExt);
			if (CommonLib.notNullEmptyString(startPath)) {
				name = startPath.concat(name);
			}
			fillBeans(name, binItem, columnBinFolderId3Mark, ext, binPath, beans);
		}
		return sbPathString.toString();
	}

	private static void fillBeans(String name, String binItem, String[] columnBinFolderId3Mark, String ext,
			Path binPath, List<MyBean> beans) {
		long[] arrInf = getDecodeDateSizeCrc(binItem);
		String crc, size, modified;
		if (arrInf[0] == 0) {
			final String e = "error";
			size = e;
			crc = e;
			modified = e;
		} else {
			size = bytesToKBMB(false, 0, arrInf[1]);
			modified = dateModifiedToString(arrInf[0]);
			if (columnBinFolderId3Mark != null) { // that is size == 3:binFolder,viewNoId3,viewNoMark

				String signature = "";
				var ps = binItem.indexOf(')');
				if (ps > 10) { // 00000001(0)
					signature = binItem.substring(0, ps + 1);
					size = signature.concat(Const.columnSignatureSeparator).concat(size);
				}

				var sb = new StringBuilder();
				sb.append(columnBinFolderId3Mark[0]);
				if (!signature.isEmpty()) {
					if (columnBinFolderId3Mark[1].isEmpty() && ext.equals(Const.extensionForCalcId3)) {
						FileDataBase.initId3IsProperty();
						sb.append(" <ID3>"); // label, that id3 showed and extension's 'mp3'
						if (CommonLib.notNullEmptyProperties(FileDataBase.id3IsProperty)) {
							String id3 = FileDataBase.id3IsProperty.getProperty(signature, "");
							if (!id3.isEmpty()) {
								sb.append(" ").append(id3);
							}
						}
					}

					if (columnBinFolderId3Mark[2].isEmpty()) {
						String mark = FileDataBase.getMarkFromPropertiesOrEmpty(signature);
						if (!mark.isEmpty()) {
							sb.append(" ").append(FileDataBase.formatMark(mark, true));
						}
					}
				}
				crc = sb.toString();
			} else {
				crc = Long.toHexString(arrInf[2]);
			}
		}
		var bean = new MyBean(crc, size, modified, name, ext);
		bean.binPath = binPath;
		beans.add(bean);
	}

	private static void fillSbPathString(String appendInf, String binItem, StringBuilder sbPathString) {
		String appInf = getAppendInf(binItem);
		if (!appInf.isEmpty()) {
			sbPathString.append(" ").append(appInf);
			if (appendInf.length() > 1) { // minim length must be 20 :)
				appInf = getAppendInf(appendInf);
				if (!appInf.isEmpty()) {
					sbPathString.append(" : ").append(appInf);
				}
			}
		}
	}

	/**
	 * Takes 'rowList' with strings in bin format as '<file>txt', decodes to
	 * 'resultList'
	 * 
	 * @param startPath  if not null/empty, be added before each result string
	 * @param appendInf  must be null/empty, but if length == 1 (example "1") then
	 *                   be written info about size and date modified (be taken from
	 *                   'binItem'); if length > 1 and correct 'appendInf', be also
	 *                   added after ' : ' info (be taken from 'appendInf')
	 * @param rowList    not must be null/empty
	 * @param resultList must be not null, result be added to
	 */
	synchronized static void fillBinList(String startPath, String appendInf, List<String> rowList,
			List<String> resultList) {
		if (resultList == null || CommonLib.nullEmptyList(rowList)) {
			return;
		}
		for (var s : rowList) {
			if (!s.isEmpty()) {
				resultList.add(getPathStringFromBinItem(null, startPath, s, appendInf, null, null, null));
			}
		}
	}

	synchronized static Path getPathFromBinItemOrNull(String startPath, String binItem) {
		var pathString = getPathStringFromBinItem(null, "", binItem, "", null, null, null);
		return pathString.isEmpty() ? null : Path.of(startPath, pathString);
	}

	synchronized static String getBinEndFromBinItemOrEmpty(boolean toLowerCase, String binItem) {
		// 00000000(0)17d1dcdd58e*<1> -> 26 symbols
		// returns 'binEnd', example: '<1>'
		if (binItem.length() < 26 || binItem.startsWith("00000000")) {
			return "";
		}
		var posBegin = binItem.lastIndexOf(Const.BRACE_START);
		if (posBegin < 23) {
			return "";
		}
		var binEnd = toLowerCase ? binItem.substring(posBegin).toLowerCase() : binItem.substring(posBegin);
		var posIndexOfExt = binEnd.indexOf(Const.BRACE_END);
		// min 'sub': <1>
		return (binEnd.length() < 3 || posIndexOfExt < 2) ? "" : binEnd;
	}

	/**
	 * @param lengthStartPath index of symbol in 'path' to string, where be copying
	 *                        end of 'path'; if from begin, must be '0'; if '<0' or
	 *                        '> length of 'path' string, result be empty
	 * @param ar              if 'null', be filling from path; else (not 'null')
	 *                        'path' no required
	 * @param path            must be exists, from it be created 'binEnd' entry (
	 *                        '<' + path + '>' + ext )
	 * @return 'endBin' from 'path' or empty if error
	 */
	synchronized static String getBinEndFromPathOrEmpty(int lengthStartPath, String[] ar, Path path) {
		var sb = new StringBuilder();
		try {
			if (ar == null) {
				ar = ConverterBinFunc.dividePathToAll_Ext(lengthStartPath, path.toString());
			}
			sb.append(Const.BRACE_START).append(ar[1]).append(Const.BRACE_END).append(ar[2]);
		} catch (Exception e) {
			return "";
		}
		return sb.toString();
	}

	/**
	 * @param lengthStartPath length of 'start path', must be 0; but must not be
	 *                        more 'path' length
	 * @param pathString      string, be divided
	 * @return array[3] 0: extension without 'dot' in lower case ('EXTEMPTY' if
	 *         empty);<br>
	 *         1:substring without 'dot' and 'extension'; <br>
	 *         2: the same as 'array[0], but "" if empty;<br>
	 *         if error, arrays be filled with null; checks array[0] == null
	 */
	synchronized static String[] dividePathToAll_Ext(int lengthStartPath, String pathString) {
		String[] ar = new String[3];
		var s = lengthStartPath <= 0 ? pathString
				: lengthStartPath >= pathString.length() ? "" : pathString.substring(lengthStartPath);
		if (s.isEmpty()) {
			return ar; // returns 'null' in all array
		}
		var pos = s.lastIndexOf('.');
		if (pos >= 0 && pos < s.lastIndexOf(File.separator)) {
			pos = -1;
		}
		if (pos < 0 || (pos >= s.length() - 1)) {
			ar[0] = Const.EXTEMPTY;
			ar[1] = s;
			ar[2] = "";
		} else {
			ar[0] = s.substring(pos + 1).toLowerCase();// extension without dot
			ar[1] = s.substring(0, pos);
			ar[2] = ar[0];
		}
		return ar;
	}

	// takes string of kind "00000000(0)17d1dcdd58e"
	// returns decoded size and date modified, or empty
	synchronized private static String getAppendInf(String s) {
		long[] arrInf = getDecodeDateSizeCrc(s);
		return (arrInf[0] == 0) ? "" : getFileInf(true, arrInf[0], arrInf[1]);
	}

	/**
	 * @param needWriteSize append inform about file size
	 * @param lastModified  file date modified
	 * @param size          file length
	 * @return formatted string in braces '<' and '/> ' or only this braces if error
	 */
	synchronized static String getFileInf(boolean needWriteSize, long lastModified, long size) {
		String sInf;
		try {
			sInf = dateModifiedToString(lastModified);
			if (needWriteSize)
				sInf += "; " + bytesToKBMB(false, 0, size);
		} catch (Exception e) {
			sInf = "";
		}
		return Const.BRACE_START + sInf + Const.BRACE_END_WITH_SLASH_SPACE; // braces need for extract paths
	}

	/**
	 * @param fLen if '<0' be as 'file.length' ; else writes as is (already init);
	 *             anyway must be '>=0'
	 * @param crc  if '<0' : error
	 * @param file for calculate 'length' and 'date modified': if date in 'hex' < 9
	 *             symbols , be 9 symbols
	 * @return 'null' if error; or array, length == 2; [0]:signature; [1] start bin
	 *         signature
	 */
	synchronized static String[] getStartBinSignature(long fLen, long crc, File file) {
		if (crc < 0) { // no must be, but check anyway
			return null; // result be empty
		}
		var sb = new StringBuilder();
		if (fLen < 0) {
			fLen = file.length();
			if (fLen < 0) {
				return null;
			}
		}

		sb.append(Long.toHexString(fLen));
		while (sb.toString().length() < 8) {
			sb.insert(0, "0");
		}
		sb.append("(").append(Long.toHexString(crc)).append(")");
		var dateModified = file.lastModified();
//'dateModified' min length must be 9 symbols, that is MIN_DATE_MODIFIED_LONG must be = '0x111_111_111L'
		if (dateModified < Const.MIN_DATE_MODIFIED_LONG) {
			dateModified = Const.MIN_DATE_MODIFIED_LONG;
		}
		String[] arBinSign = new String[2];
		arBinSign[0] = sb.toString();
		sb.append(Long.toHexString(dateModified)).append("*");
		arBinSign[1] = sb.toString();
		return arBinSign;
	}

	// take 'binItem' in bin format '00000005(5fb01ff)1805ba1617c*<test>txt'
	// path must exists
	synchronized static boolean equalsPathOnLengthAndDate(Path path, String binItem) {
		try {
			String[] arrString = getSizeCrcDateInStringArrayOrNull(binItem);
			if (arrString == null) {
				return false;
			}
			File file = path.toFile();
			if (!file.exists()) {
				return false;
			}
			var fLen = file.length();
			var sb = new StringBuilder();
			sb.append(Long.toHexString(fLen));
			while (sb.toString().length() < 8) {
				sb.insert(0, "0");
			}
			if (!arrString[0].equals(sb.toString())) {
				return false;
			}
			var dtNew = Long.toHexString(file.lastModified());
			return arrString[2].equals(dtNew);
		} catch (Exception e) {
		}
		return false;
	}

	synchronized private static String[] getSizeCrcDateInStringArrayOrNull(String binStartOrFull) {
		String[] arrString = new String[3];
		Arrays.fill(arrString, "");

		var pos = binStartOrFull.indexOf('*');
		if (pos >= 0) {
			binStartOrFull = binStartOrFull.substring(0, pos);
		}
		var p1 = binStartOrFull.indexOf('(');
		var p2 = binStartOrFull.indexOf(')');
//suppose length min: 8 + 3 + 9, that is 00000000 + (0) + 111111111
		if (binStartOrFull.length() < 20 || p1 < 8 || p2 < 10) {
			return null;
		}
		// size,crc,dateModified
		arrString[0] = binStartOrFull.substring(0, p1);
		arrString[1] = binStartOrFull.substring(p1 + 1, p2);
		arrString[2] = binStartOrFull.substring(p2 + 1);
		return arrString;
	}

	/**
	 * takes bin item, extracts, if needed, string of kind "00000000(0)17d1dcdd58e",
	 * minim length suppose 20 symbols
	 *
	 * @param binStartOrFull string from *.bin file, be taken information about date
	 *                       and size
	 * @return not null array, length == 3, if array[0] == 0 - error; in array[0] -
	 *         date modified, in array[1] - size, in array[2] - crc
	 */
	synchronized static long[] getDecodeDateSizeCrc(String binStartOrFull) {
		long[] arr = new long[3];
		Arrays.fill(arr, 0);
		// size,crc,date_modified
		String[] arrString = getSizeCrcDateInStringArrayOrNull(binStartOrFull);
		if (arrString == null) {
			return arr;
		}
		try {
			// result, decoded: date_modified,size,crc
			var size = Long.valueOf(arrString[0], 16);
			var modified = Long.valueOf(arrString[2], 16);
			arr[0] = (modified == 0) ? 1 : modified; // both is correct modified (not '0')
			arr[1] = size;
			arr[2] = Long.valueOf(arrString[1], 16); // if error crc - no matters - be '0'
		} catch (Exception e) {
		}
		return arr;
	}

	synchronized public static Set<String> getHashSetFromBin(List<String> binFileListDestFolder) {
		Set<String> hashFilesOfBin = new HashSet<String>();
		for (var s : binFileListDestFolder) {
			var p = s.indexOf(')'); // need size and crc,(in hashWithFilesRealPath too)
			// minim 00000000(0)17f0ad3ad68 , that is 10
			if (p < 10) {
				continue;
			}
			hashFilesOfBin.add(s.substring(0, p + 1));
		}
		return hashFilesOfBin;
	}

	synchronized public static String getSignatureOrEmpty(String s) {
		if (s == null || s.length() < 11) {
			return "";
		}
		var ps = s.indexOf(')');
		return (ps < 11) ? "" : s.substring(0, ps + 1);// 00000001(0)

	}

}
