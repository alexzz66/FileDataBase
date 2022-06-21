package alexnick.filedatabase;

public class FileCntSize {
	private int count;
	private long size;

	public FileCntSize(int count, long size) {
		this.setCount(count);
		this.setSize(size);
	}

	public FileCntSize addF(long fLen) {
		this.setCount(this.getCount() + 1);
		this.setSize(this.getSize() + fLen);
		return this;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

}
