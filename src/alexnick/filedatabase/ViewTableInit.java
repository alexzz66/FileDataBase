package alexnick.filedatabase;

import java.nio.file.Path;
import java.util.List;

import java.util.concurrent.FutureTask;

import alexnick.CommonLib;

public class ViewTableInit {
	private int showViewResult = Const.MR_NO_CHOOSED;

	public ViewTableInit(Program program) {
		var binFinder = new BinFinder(Path.of(FileDataBase.repositoryPathCurrent));

		List<MyBean> beans0 = binFinder.getBeansOrNull();
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

}
