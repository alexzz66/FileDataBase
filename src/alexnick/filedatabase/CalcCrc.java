package alexnick.filedatabase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Adler32;

import com.mpatric.mp3agic.Mp3File;

import alexnick.CommonLib;

public class CalcCrc {
	private long crcResult;
	private String binItem = "";

// 'binItemSignature' the same as 'binItem', but shortly (for checking signature)
	private String binItemSignature = "";

	/**
	 * @param needCalcStartBinItemAndID3 if '> 0', be calculate start of bin item;
	 *                                   there be added 'endBinItem', if not empty';
	 *                                   if '2', also be calculate ID3property for
	 *                                   'mp3'; otherwise (if '0'), 'binItem' and
	 *                                   'signature' be empty;
	 * @param endBinItem
	 * @param path
	 */
	CalcCrc(int needCalcStartBinItemAndID3, String endBinItem, Path path) {
		crcResult = 0;
		final int szMax = 65535;// 65535==0xff_ff
		final int szChunk = (szMax / 3);
		final Adler32 crcAdler = new Adler32();

		try (var stream = Files.newInputStream(path, StandardOpenOption.READ)) {
			Long fLenLong = path.toFile().length();
			int fLen = fLenLong >= Integer.MAX_VALUE ? Integer.MAX_VALUE : fLenLong.intValue();

			if (fLen <= 0) {
				return;
			}

			boolean crcByChunk = fLen > szMax;

			byte[] buffer = stream.readNBytes(crcByChunk ? szChunk : fLen);
			crcAdler.update(buffer);

			if (crcByChunk) {
				int fLenDiv3 = fLen / 3;
				stream.skipNBytes(fLenDiv3 - szChunk);
				buffer = stream.readNBytes(szChunk);
				crcAdler.update(buffer);
				stream.skipNBytes(fLen - fLenDiv3 - szChunk - szChunk);
				buffer = stream.readNBytes(szChunk);
				crcAdler.update(buffer);
			}

			crcResult = crcAdler.getValue();

			if (needCalcStartBinItemAndID3 > 0) {
				setBinItem(fLen, path.toFile(), endBinItem);
				// Here must be init 'binItemSignature'
				if (needCalcStartBinItemAndID3 == 2) {
					boolean isMp3 = false;
					if (endBinItem.isEmpty()) {
						String end = path.toString().toLowerCase();
						if (end.endsWith(Const.extensionForCalcId3)) {
							isMp3 = true;
						}
					} else if (endBinItem.endsWith(Const.extensionForCalcId3Check)) {
						isMp3 = true;
					}

					if (isMp3) {
						updateID3Property(path);
					}
				}
			}
		} catch (Exception e) {
			crcResult = -1;
		}
	}

	private void updateID3Property(Path path) {
		FileDataBase.initId3IsProperty();
		var signature = getBinItemSignature();
		final String id3LengthStartValue = "length ";
		try {
			String id3Value = FileDataBase.id3IsProperty.containsKey(signature)
					? FileDataBase.id3IsProperty.getProperty(signature)
					: "";

			if (id3Value.startsWith(id3LengthStartValue)) {
				return;
			}

			File file = path.toFile();
			var id3 = new Mp3File(file);
			String id3LengthValue = CommonLib.secondsToString(false, (int) id3.getLengthInSeconds(),
					id3LengthStartValue, "; ");

			String res = "";
			if (id3Value.isEmpty()) {
				var tag2 = id3.getId3v2Tag();
				String artist = null;
				String title = null;

				if (tag2 != null) {
					var a = tag2.getArtist();
					if (a != null && !a.isEmpty()) {
						artist = a;
					}
					var t = tag2.getTitle();
					if (t != null && !t.isEmpty()) {
						title = t;
					}
				}

				if (artist == null || title == null) {
					var tag1 = id3.getId3v1Tag();
					if (tag1 != null) {
						if (artist == null) {
							var a = tag1.getArtist();
							if (a != null && !a.isEmpty()) {
								artist = a;
							}
						}
						if (title == null) {
							var t = tag1.getTitle();
							if (t != null && !t.isEmpty()) {
								title = t;
							}
						}
					}
				}

				final String sep = " || ";
				final String sepRep = " | ";

				res = (artist == null) ? "" : artist;
				if (!res.isEmpty()) {
					while (res.contains(sep)) {
						res = res.replace(sep, sepRep);
					}
				}
				if (title != null && !title.isEmpty()) {
					if (!res.isEmpty()) {
						while (title.contains(sep)) {
							title = title.replace(sep, sepRep);
						}
						res = res.concat(sep).concat(title);
					}
				}
			}

			if (!res.isEmpty() && res.contains(Const.BRACE_MARK)) { // '**' mark, writes after 'binfolder','id3'
				res = res.replace(Const.BRACE_MARK, "^^");
			}

			FileDataBase.id3IsProperty.put(signature, id3LengthValue.concat(res));
			FileDataBase.id3IsPropertyChanged = true;

		} catch (Exception e) {
		}
	}

	public long getCrcResult() {
		return crcResult;
	}

	/**
	 * For not empty 'endBinItem' only<br>
	 * if calculation error of 'crcResult', this method will not be called
	 * 
	 * @param fLen       must be correct
	 * @param file       must be exists
	 * @param endBinItem
	 */
	private void setBinItem(long fLen, File file, String endBinItem) {
		String[] arBinSign = ConverterBinFunc.getStartBinSignature(fLen, getCrcResult(), file);
		if (arBinSign == null) {
			return;
		}
		binItemSignature = arBinSign[0];
		this.binItem = arBinSign[1].concat(endBinItem);
	}

	// for not empty 'endBinItem' only
	public String getBinItem() {
		return binItem;
	}

	public String getBinItemSignature() {
		return binItemSignature;
	}

}
