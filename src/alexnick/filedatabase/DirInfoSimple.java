package alexnick.filedatabase;

import java.nio.file.Path;

public class DirInfoSimple {
	int countFilesOwn = 0;
	long sizeFilesOwn = 0L;
	int countEmptySubFolders = 0;

	int countFilesSubfolders = 0;
	long sizeFilesSubfolders = 0L;
	long lastModified;
	String name;

	public DirInfoSimple(Path dir) {
		this.lastModified = dir.toFile().lastModified();
		name = dir.toFile().getName();
	}
	

}
