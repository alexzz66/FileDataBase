package alexnick.filedatabase;

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

//SHOW VIEWTABLE		
		var viewTable = new ViewTable(program, app.concat("View all saved *.bin"), beans0);
		var ft = new FutureTask<>(viewTable);
		new Thread(ft).start();
		try {
			showViewResult = ft.get();
			if (showViewResult == Const.MR_NEED_UPDATE_BASE) {
				pathForUpdate = viewTable.getPathForUpdate();
			}

		} catch (Exception e) {
			System.out.println("error of view...");
		}
	}

	int getShowViewResult() {
		return showViewResult;
	}

	private Path pathForUpdate = null;

	public Path getPathForUpdate() {
		return pathForUpdate;
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
		var fDat = FileDataBase.getDatPathForBinOrNull(path);
		if (fDat == null) {
			return null;
		}
// columns: binFolder, startPath, date, result -> all, except 'binfolder', be taken from '*.dat'
// 'x1': name of parent folder, example bin~data~results~F-GB-14,43-1805ca431af-flash
		final String parentName = path.getParent().toFile().getName();

		if (!parentName.startsWith(Const.binFolderStartSignature)) {
			return null;
		}

// example 'realBinDir': 'key':~bin~data~results~F-GB-14,43-1805ca431af-flash and 'value':'<K:\> '
		String realDiskInBraceOrEmpty = realBinDir.getOrDefault(parentName.toLowerCase(), "");

// ~bin~data~results~F-GB-14,43-1805ca431af-flash
// => will be after: F-GB-14,43-1805ca431af-flash
// result for exist disk (column 'BinFolder'): <K:\> F-GB-14,43-1805ca431af-flash
		final String x1 = realDiskInBraceOrEmpty.concat(parentName.replace(Const.binFolderStartSignature, ""));
		String[] stuff = new String[4]; //minimum 4
		Map<String, Integer> mapCountExt = new HashMap<String, Integer>();

		var countBinItems = FileDataBase.getCountBinItemsOrNil(realDiskInBraceOrEmpty, fDat, mapCountExt, stuff);
		if (countBinItems <= 0) {
			return null;
		}

// serviceIntTwo uses for 'id' in 'ViewTable'; fourApp uses for total count 'mark' in base
		var bean = new MyBean(x1, stuff[0], stuff[1], stuff[2], "");
		bean.serviceIntOne = countBinItems;
		bean.binPath = path;
		bean.mapCountExt = mapCountExt;
		return bean;
	}

}
