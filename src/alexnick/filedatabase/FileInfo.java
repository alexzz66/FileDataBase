package alexnick.filedatabase;

import alexnick.CommonLib;

/**
 * date, size, crc
 *
 */
public class FileInfo {

	final private long date;
	final private long size;
	final private long crc;
	final private String extForFourApp; // must not be null (there's sorting)
	final private String mark; // not must be null

	/**
	 * All parameters's correct
	 * 
	 * @param date          date modification
	 * @param size          >=0
	 * @param crc           >=0
	 * @param extForFourApp if not null/empty, be added 'dot' before, if need
	 */
	public FileInfo(long date, long size, long crc, String extForFourApp, String mark) {
		this.date = date;
		this.size = size <= 0 ? 0 : size;
		this.crc = crc <= 0 ? 0 : crc;
		this.extForFourApp = CommonLib.nullEmptyString(extForFourApp) ? ""
				: CommonLib.startOrEndAddIfNeed(".", true, false, extForFourApp);
		this.mark = CommonLib.nullEmptyString(mark) ? "" : mark;
	}

	long getDate() {
		return date;
	}

	long getSize() {
		return size;
	}

	long getCrc() {
		return crc;
	}

	String getExtForFourApp() {
		return extForFourApp;
	}

	String getMark() {
		return mark;
	}

}
