package alexnick.filedatabase;

import static alexnick.CommonLib.readFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import alexnick.CommonLib;

public class BinCreator {
	private List<String> binList = new ArrayList<String>();
	volatile private int countForInfCount = 0;

	public BinCreator(int needCalcID3, Path startPath, Path binPath, List<String> listPathsToEndBin) {
		createBinList(needCalcID3, startPath.toString(), binPath, listPathsToEndBin);
	}

	public List<String> getBinList() {
		return binList;
	}

// 'binPath' folder name for saving result, need for checking, if '*.bin' be saved before; 'listFiles' not null and no empty
// if 'needCalcID3 != Const.ID3_EXTRACT_NO' -> after calling this method, need saving/null id3Options; 
	synchronized private void createBinList(int needCalcID3, String startPath, Path binPath, List<String> listFiles) {
		List<String> oldBinList = readFile(2, 0, binPath); // read anyway, let's 'no exists'
		List<String> listFilesNewForBin = new ArrayList<String>();
		listFilesNewForBin.addAll(listFiles); // be new paths only

		if (!oldBinList.isEmpty()) {
			// get all items from 'listFiles'(that is '<path>exe' only) to lower case
			var hashMap = CommonLib.getMapFromList(true, listFilesNewForBin);
			if (!hashMap.isEmpty()) {
				var keys = hashMap.keySet();
				for (int i = 0; i < oldBinList.size(); i++) {
					var binItem = oldBinList.get(i);
					var pos = binItem.lastIndexOf('<');
					if (pos <= 11) { // min: 01234567(9)1
						continue;
					}
					var sLow = binItem.substring(pos).toLowerCase();
					// this file found in '*.bin' to lower case
					if (keys.contains(sLow)) {
						// for all 'mp3', this file not be added to 'binList', that is be updated
						if (needCalcID3 == Const.ID3_EXTRACT_ALL && sLow.endsWith(Const.extensionForCalcId3Check)) {
							continue;
						}
						// checking for changing, comparing length and date modified (crc no updated, if
						// equals)
						Path path = ConverterBinFunc.getPathFromBinItemOrNull(startPath, binItem);
						if (path == null || !ConverterBinFunc.equalsPathOnLengthAndDate(path, binItem)) {
							continue;
						}
						binList.add(binItem);
						oldBinList.set(i, "");
						var ind = hashMap.get(sLow);
						listFilesNewForBin.set(ind, "");
					}
				}
				listFilesNewForBin.removeIf(String::isEmpty);
				if (listFilesNewForBin.isEmpty()) { // no new items, but need save bin?
					oldBinList.removeIf(String::isEmpty);
					if (oldBinList.isEmpty()) {
						// binList.clear() => forbidden
						binList.add(0, Const.EXTEMPTY); // need for show duplicate table
					}
				}
			}
		}

		if (!listFilesNewForBin.isEmpty()) {
			System.out.println("start calculate of crc, files: " + listFilesNewForBin.size());
			countForInfCount = 0;
			var newBinList = listFilesNewForBin.stream().unordered() // instead of 'parallel()'
					// .parallel() >>> search stopping on 'parallel()' WTF???
					.map(s -> getBinItem(needCalcID3, startPath, s)).filter(s -> !s.isEmpty()).toList();
			if (!newBinList.isEmpty()) {
				binList.addAll(newBinList);
			}
		}
	}

	synchronized private String getBinItem(int needCalcID3, String startPath, String s) {
		infCount();
		Path path = ConverterBinFunc.getPathFromBinItemOrNull(startPath, s);
		if (path == null) {
			return "";
		}
		// THERE INIT FOR ID3
		var crc = new CalcCrc(needCalcID3 == Const.ID3_EXTRACT_NO ? 1 : 2, s, path);
		var binItem = crc.getBinItem();
		return binItem;
	}

	synchronized private void infCount() {
		countForInfCount++;
		if ((countForInfCount & 1023) == 0) {
			System.out.println("...processed files:" + countForInfCount);
		}
	}

}
