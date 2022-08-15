package alexnick.filedatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import alexnick.CommonLib;

public class BinFinder {
	private List<MyBean> beans = null;

	public BinFinder(Path repo) {
		if (repo == null || !repo.toFile().isDirectory()) {
			CommonLib.errorArgument("Error: undefined search repository");
		}

		beans = findAllCorrectBin(repo);
	}

	private List<MyBean> findAllCorrectBin(Path repo) {
		try {
// 'realBinDir', format <folder in repository; disk, where it found, be in '< >'>
			Map<String, String> realBinDir = getRealBinDir(true);
			if (realBinDir.isEmpty()) {
				return null;
			}
			int maxDepth = 2; // must be 2 (0: root; 1: repositoryfolder; 2:subfolders in that
			return Files.walk(repo, maxDepth).filter(FileDataBase::isCorrectBin).map(e -> fillMyBean(e, realBinDir))
					.filter(Objects::nonNull).toList();
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

	private MyBean fillMyBean(final Path path, Map<String, String> realBinDir) {
		var fDat = FileDataBase.getDatPathForBinOrNull(path);
		if (fDat == null) {
			return null;
		}

// columns: binFolder, startPath, date, result -> all, except 'binfolder', will be taken from '*.dat'
// 'x1': name of parent folder, example bin~data~results~F-GB-14,43-1805ca431af-flash
		final String parentName = path.getParent().toFile().getName();

		if (!parentName.startsWith(Const.binFolderStartSignature)) {
			return null;
		}

// example 'realBinDir': 'key':~bin~data~results~F-GB-14,43-1805ca431af-flash and 'value':'<K:\> '
		String realDiskInBraceOrEmpty = realBinDir.getOrDefault(parentName.toLowerCase(), "");

// ~bin~data~results~F-GB-14,43-1805ca431af-flash => will be after: F-GB-14,43-1805ca431af-flash
		String keyForSyncBin = parentName.replace(Const.binFolderStartSignature, ""); // begin init 'key'

// result for exist disk (column 'BinFolder'): <K:\> F-GB-14,43-1805ca431af-flash
		final String x1 = realDiskInBraceOrEmpty.concat(keyForSyncBin);

		// end init 'key'
		keyForSyncBin = keyForSyncBin.concat(File.separator).concat(path.toFile().getName());

		String[] stuff = new String[6]; // minimum 6
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
		bean.serviceStringOne = keyForSyncBin;
		return bean;
	}

	public List<MyBean> getBeansOrNull() {
		return beans;
	}

}
