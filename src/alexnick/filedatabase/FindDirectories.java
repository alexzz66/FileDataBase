package alexnick.filedatabase;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import alexnick.CommonLib;

public class FindDirectories extends SimpleFileVisitor<Path> {
	private Map<Path, DirInfoSimple> resultMap = new HashMap<Path, DirInfoSimple>();
	private Path startFolder;
	private List<MyBean> beans = null;
	private int countError = 0;
	private int currentBeansId = 0;
	private int needResultBeans;
	private int totalId = 0;

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		try {
			totalId++; // from '1' and more correct 'totalId'
			resultMap.put(dir, new DirInfoSimple(dir, totalId));
		} catch (Exception e) {
			countError++;
			System.out.println("error read folder: " + e.getMessage() + "; folder: " + dir);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		fillResultMapOnVisitFile(file);
		return FileVisitResult.CONTINUE;
	}

	private void fillResultMapOnVisitFile(Path path) { // this is exists file (not directory)
		try {
			var parent = path.getParent();
			var dis = resultMap.get(parent);
			dis.countFilesOwn++;
			final long sizePath = path.toFile().length();
			dis.sizeFilesOwn += sizePath;

			while (parent != null && !parent.equals(startFolder)) {
				parent = parent.getParent();
				if (parent != null) {
					var dis2 = resultMap.get(parent); // 'dis2' must not be null
					dis2.countFilesSubfolders++;
					dis2.sizeFilesSubfolders += sizePath;
				}
			}

		} catch (Exception e) {
			countError++;
			System.out.println("error: " + e.getMessage() + "; file: " + path);
		}
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		fillBeansOnPostVisitDir(dir);
		return FileVisitResult.CONTINUE;
	}

	private void fillBeansOnPostVisitDir(Path dir) {
		if (beans == null || needResultBeans == 0) {
			return;
		}

		var dis = resultMap.get(dir);
		boolean dirIsEmpty = dis.countFilesOwn == 0 && dis.countFilesSubfolders == 0;

//needResultBeans 1: for empty folders; 2: for no empty folders; 3: for all folders
		if (needResultBeans == 1) { // empty
			if (!dirIsEmpty) {
				return;
			}
		} else if (needResultBeans == 2) { // no empty
			if (dirIsEmpty) {
				return;
			}
		}

		if (dirIsEmpty) { // empty or all, count or empty subfolders
			var parent = dir;
			var ownTotalId = dis.totalId;

			while (parent != null && !parent.equals(startFolder)) {
				parent = parent.getParent();
				if (parent != null) {
					var dis2 = resultMap.get(parent); // 'dis2' must not be null
					dis2.emptySubFoldersTotalIdSet.add(ownTotalId);
				}
			}
		}

		var bean = new MyBean("", dis.name, CommonLib.dateModifiedToString(dis.lastModified), dir.toString(), null);
		bean.serviceIntOne = ++currentBeansId;
		bean.serviceIntTwo = dis.totalId;
		bean.binPath = dir;
		bean.serviceSet = new TreeSet<Integer>(Comparator.comparingInt(e -> -e));
		bean.serviceSet.addAll(dis.emptySubFoldersTotalIdSet);

		beans.add(bean);
	}

//CONSTRUCTOR
	/**
	 * @param needResultBeans 0 (by default): 'beans' will not be created; 1: for
	 *                        empty folders; 2: for no empty folders; 3: for all
	 *                        folders
	 * @param startFolder     MUST BE Canonical and Absolute path for correct
	 *                        'Files.walkFileTree'
	 * @throws IOException
	 */
	public FindDirectories(int needResultBeans, Path startFolder) throws IOException {
		if (startFolder == null || !startFolder.toFile().isDirectory()) {
			return;
		}
		this.needResultBeans = needResultBeans < 0 || needResultBeans > 3 ? 3 : needResultBeans;
		this.startFolder = startFolder;
		totalId++; // first number; will be '1';
		resultMap.put(startFolder, new DirInfoSimple(startFolder, totalId));
		if (needResultBeans != 0) {
			this.beans = new ArrayList<MyBean>();
		}
	}

	Map<Path, DirInfoSimple> getResultMap() { // 'resultMap' not empty
		errorInfo();
		return resultMap;
	}

	public List<MyBean> getBeans() {
		if (beans == null) {
			return null;
		}
		errorInfo();
		return beans;
	}

	private void errorInfo() {
		if (countError > 0) {
			System.out.println();
			System.out.println("!!! ERRORS AT WALK FILES, count: " + countError);
		}
	}

	public int getCountError() {
		return countError;
	}

}
