package alexnick.filedatabase;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class DirInfoSimple {
	int totalId = 0;
	int countFilesOwn = 0;
	long sizeFilesOwn = 0L;
//	int countEmptySubFolders = 0;

	int countFilesSubfolders = 0;
	long sizeFilesSubfolders = 0L;
	long lastModified;
	String name;
	Set<Integer> emptySubFoldersTotalIdSet;

//'totalId' will be set, in case > 0; if 0 -> not defined
	public DirInfoSimple(Path dir, int totalId) {
		this.lastModified = dir.toFile().lastModified();
		name = dir.toFile().getName();
		if (totalId > 0) {
			this.totalId = totalId;
		}
		emptySubFoldersTotalIdSet = new HashSet<Integer>();
	}

}
