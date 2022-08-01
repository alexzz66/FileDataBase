package alexnick.filedatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import alexnick.CommonLib;

public class FindFiles extends SimpleFileVisitor<Path> {
	volatile private List<String> listPathsToEndBin; // '<'+path+'>'+ext
	volatile private List<File> listFullPaths = null;
	volatile private Map<String, FileCntSize> hmExtsInfoIncluded;
	volatile private Map<String, FileCntSize> hmExtsInfoExcluded;

	private Set<String> setExts = null;
	private int needSetExts = 0; // 0: all exts; 1: 'need' in setExts; 2: 'no need' in setExts

	private boolean skipEmpty;
	private int lengthStartPath;

	/**
	 * @param needListFullPaths      if true, will be created and filling by full
	 *                               path 'listFullPaths'; this are the same paths,
	 *                               as 'listPathsToEndBin', but in full path format
	 * @param lengthStartPath        length, will be removed with start each path
	 *                               string
	 * @param needHmExtsInfoIncluded result info (count,size) about extensions,
	 *                               'false' if no need calculate
	 * @param extsNeed               set of extensions, in lower case, without
	 *                               starts '.', '<empty>' for empty extension
	 * @param extsNONeed             the same, that 'extsNeed' (differs: need|no
	 *                               need); both or any may be 'null'; if both no
	 *                               empty, from 'need' be excluded 'noNeed'
	 */
	public FindFiles(boolean needListFullPaths, int lengthStartPath, boolean needHmExtsInfoIncluded,
			boolean needHmExtsInfoExcluded, Set<String> extsNeed, Set<String> extsNONeed) {
		this.lengthStartPath = lengthStartPath;
		this.skipEmpty = FileDataBase.skipEmpty;
		this.listPathsToEndBin = new ArrayList<String>();
		if (needListFullPaths) {
			listFullPaths = new ArrayList<File>();
		}
		this.hmExtsInfoIncluded = needHmExtsInfoIncluded ? new HashMap<String, FileCntSize>() : null;
		this.hmExtsInfoExcluded = needHmExtsInfoExcluded ? new HashMap<String, FileCntSize>() : null;
		setNeedOrNoNeedExtensions(extsNeed, extsNONeed);
	}

	private void setNeedOrNoNeedExtensions(Set<String> extsNeed, Set<String> extsNONeed) {
		boolean bIsNeed = CommonLib.notNullEmptySet(extsNeed);
		boolean bIsNoNeed = CommonLib.notNullEmptySet(extsNONeed);
		if (!bIsNeed && !bIsNoNeed) {
			return;
		}
		setExts = new HashSet<String>();
		if (!bIsNeed || !bIsNoNeed) {
			needSetExts = bIsNeed ? 1 : 2;
			if (bIsNeed) {
				setExts.addAll(extsNeed);
			} else { // bIsNoNeed is 'true' here
				setExts.addAll(extsNONeed);
			}
			return;
		}
		// both set's no empty; equals 'ext' deletes
		setExts.addAll(extsNeed);
		Set<String> setTmp = new HashSet<>();
		setTmp.addAll(extsNONeed);
		for (var s : extsNONeed) {
			if (extsNeed.contains(s)) {
				setExts.remove(s);
				setTmp.remove(s);
			}
		}
		if (!setExts.isEmpty()) {
			needSetExts = 1;
			return;
		}
		if (!setTmp.isEmpty()) {
			setExts.clear();
			setExts.addAll(setTmp);
			needSetExts = 2;
			return;
		}
		needSetExts = 0;
		setExts = null;
	}

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
		if (addFileToListFiles(path.toFile())) {
			if ((listPathsToEndBin.size() & 2047) == 0) // each 2048 (2047:111_1111_1111)
				System.out.println("...added files: " + listPathsToEndBin.size());
		}
		return FileVisitResult.CONTINUE;
	}

	private boolean addFileToListFiles(File file) {
		var ar = ConverterBinFunc.dividePathToAll_Ext(lengthStartPath, file.toString());
		if (ar[0] == null) {
			return false;
		}
		String ext = ar[0];
		long fLen = 0;
		if (hmExtsInfoIncluded != null || hmExtsInfoExcluded != null || skipEmpty) {
			fLen = file.length();
			if (skipEmpty && fLen <= 0) {
				return false;
			}
		}

		if (needSetExts != 0 && !checkSetExts(ext)) {
			if (hmExtsInfoExcluded != null) {
				addExtsInfo(fLen, ext, hmExtsInfoExcluded);
			}
			return false;
		}

		if (hmExtsInfoIncluded != null) {
			addExtsInfo(fLen, ext, hmExtsInfoIncluded);
		}

		String binEnd = ConverterBinFunc.getBinEndFromPathOrEmpty(lengthStartPath, ar, null);
		if (binEnd.isEmpty()) {
			System.out.println("empty");
		}
		if (!binEnd.isEmpty()) {
			listPathsToEndBin.add(binEnd);
			if (listFullPaths != null) {
				listFullPaths.add(file);
			}
		}
		return true;
	}

	// no check 'fLen' and 'ext' -> both be correct
	private void addExtsInfo(long fLen, String ext, Map<String, FileCntSize> hmExts) {
		hmExts.compute(ext, (t, r) -> (r == null) ? new FileCntSize(1, fLen) : r.addF(fLen));
	}

	// no check (needSetExts must be != 0 and setExts != null)
	private boolean checkSetExts(String ext) {
		var b = setExts.contains(ext);
		return (needSetExts == 1) ? b : !b;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public List<String> getListPathsToEndBin() {
		return listPathsToEndBin;
	}

	public List<File> getListFullPaths() {
		return listFullPaths;
	}

	public Map<String, FileCntSize> getHmExtsInfo() {
		return hmExtsInfoIncluded;
	}

	public Map<String, FileCntSize> getHmExtsInfoExcluded() {
		return hmExtsInfoExcluded;
	}

}
