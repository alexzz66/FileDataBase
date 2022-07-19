package alexnick.filedatabase;

import static alexnick.CommonLib.readFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.FutureTask;

import alexnick.CommonLib;

public class InitShowViewTable {
	private int showViewResult = Const.MR_NO_CHOOSED;

	public InitShowViewTable(Program program) {
		List<MyBean> beans0 = findAllCorrectBin(program);
		if (CommonLib.nullEmptyList(beans0)) {
			System.out.println("No found corrected *.bin for view");
			return;
		}
		FileDataBase.showFrameInfo("View *.bin table");
		String app = FileDataBase.isTEMP ? "[TEMP mode] " : "";
		var viewTable = new ViewTable(program, app.concat("View all saved *.bin"), beans0);
		var ft = new FutureTask<>(viewTable);
		new Thread(ft).start();
		try {
			showViewResult = ft.get();
		} catch (Exception e) {
			System.out.println("error of view...");
		}
	}

	int getShowViewResult() {
		return showViewResult;
	}

	private List<MyBean> findAllCorrectBin(Program program) {
		try {
// 'realBinDir', format <folder in repository; disk, where it found, be in '< >'>
			Map<String, String> realBinDir = getRealBinDir(true);
			if (realBinDir.isEmpty()) {
				return null;
			}
			int maxDepth = 2; // must be 2 (0: root; 1: repositoryfolder; 2:subfolders in that
			return Files.walk(Path.of(FileDataBase.repositoryPathCurrent), maxDepth).filter(FileDataBase::isCorrectBin)
					.map(e -> fillMyBean(e, realBinDir)).filter(Objects::nonNull).toList();
		} catch (IOException e) {
			System.out.println("ERROR search '*.bin' " + e);
		}
		return null;
	}

// 'realBinDir', format <folder in repository; disk, where it found, be in '< >'>
	private Map<String, String> getRealBinDir(boolean needWindowsDisk) throws IOException {
		Map<String, String> realBinDir = new HashMap<String, String>();
		for (var root : File.listRoots()) {
			if (needWindowsDisk && !CommonLib.correctWindowsStartPath(root.toString())) {
				continue;
			}
			Files.list(root.toPath()).filter(e -> fillRealBinDir(root, e.toFile(), realBinDir)).limit(1).count();
		}
		return realBinDir;
	}

	private boolean fillRealBinDir(File root, File file, Map<String, String> realBinDir) {
		if (!file.isDirectory()) {
			return false;
		}
		if (file.getName().startsWith(Const.binFolderStartSignature)) {
			realBinDir.put(file.getName().toLowerCase(),
					Const.BRACE_START + root.toString() + Const.BRACE_END_WITH_SPACE);
			return true;
		}
		return false;
	}

	private MyBean fillMyBean(Path path, Map<String, String> realBinDir) {
		var sDat = CommonLib.changeEndOfStringOrEmpty(path.toString(), Const.extensionBinList, Const.extensionBinData);
		if (sDat.isEmpty()) {
			return null;
		}
		var fDat = Path.of(sDat);
		if (!fDat.toFile().exists()) {
			return null;
		}

// columns: binFolder, startPath, date, result -> all, except 'binfolder', be taken from '*.dat'
// 'x1': name of parent folder, example bin~data~results~F-GB-14,43-1805ca431af-flash
		String x1 = path.getParent().toFile().getName();
		if (!x1.startsWith(Const.binFolderStartSignature))
			return null;

// example 'realBinDir': 'key':~bin~data~results~F-GB-14,43-1805ca431af-flash and 'value':'<K:\> '
		String realDiskInBraceOrEmpty = realBinDir.getOrDefault(x1.toLowerCase(), "");

// ~bin~data~results~F-GB-14,43-1805ca431af-flash
// => be after: F-GB-14,43-1805ca431af-flash
// result for exist disk (column 'BinFolder'): <K:\> F-GB-14,43-1805ca431af-flash
		x1 = realDiskInBraceOrEmpty.concat(x1.replace(Const.binFolderStartSignature, ""));

// 'fdat': D:\~bin~data~repository\~bin~data~results~F-GB-14,43-1805ca431af-flash\~$$~.dat		
		var datList = readFile(0, 0, fDat); // be empty if no exists 'fDat'
		final String e = "error";
		String x2 = e;
		String x3 = e;
		String x4 = e;
		int count = 0;
		int indexExtBegin = 0;
		int indexExtEnd = 0;
		for (int i = 0; i < datList.size(); i++) {
			var s = datList.get(i);
			if (count >= 4)
				break;
			switch (s) {
			case Const.ALIAS_START_SEARCH -> {
				if (x2.equals(e)) {
// 'x2' is start path, may be folder 'F:\test' or full disk, as 'F:\'
					x2 = datList.get(i + 1);
					// set real start path
// !!! 'x2' for existing disks, MUST START WITH '<' and ENDS on '> '					
					if (realDiskInBraceOrEmpty.length() < 4 || !CommonLib.correctWindowsStartPath(x2)) { // means_empty
						x2 = Const.NO_DISK_PLUS + x2;// "<NO_DISK> "
					} else { // 'realDiskInBraceOrEmpty' == '<E:\> '

						// first be one letter from real (exist) disk
						var startPathFormatted = realDiskInBraceOrEmpty.substring(1, 2);

						if (x2.startsWith(startPathFormatted)) { // the same disk, as start path
							startPathFormatted = x2;
						} else {

//create new start path with other (real disk) first letter	
							startPathFormatted += x2.substring(1); // for whole disk example: 'K:\'

//!!! start path from '.dat' file, must be in ' <' , '>', if current disk different now
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
					// 'x3' set as 2022.04.24_17:36:37 (вс)
					x3 = datList.get(i + 1);
					count++;
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
			}
		}

		if (indexExtBegin == 0 || indexExtBegin >= indexExtEnd) {
			return null;
		}
		var ps = x4.indexOf(',');
		if (ps <= 0 || x4.equals(e)) {
			return null;
		}

		int countBinItems = 0;

		try {
			countBinItems = Integer.valueOf(x4.substring(0, ps));
		} catch (NumberFormatException e1) {
			return null;
		}

		if (countBinItems <= 0) {
			return null;
		}

		int cntCheckSum = 0;
		Map<String, Integer> mapCountExt = new HashMap<String, Integer>();
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
				if (mapCountExt.containsKey(ext)) {
					return null;
				}
				mapCountExt.put(ext, cnt);
			} catch (NumberFormatException e1) {
				continue;
			}
		}
		if (cntCheckSum != countBinItems) {
			return null;
		}
// serviceIntTwo uses for 'id' in 'ViewTable'; fourApp uses for total count 'mark' in base
		var bean = new MyBean(x1, x2, x3, x4, "");
		bean.serviceIntOne = countBinItems;
		bean.binPath = path;
		bean.mapCountExt = mapCountExt;
		return bean;
	}

}
