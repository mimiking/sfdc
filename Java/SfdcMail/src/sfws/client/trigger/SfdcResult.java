package sfws.client.trigger;

public class SfdcResult {
	private int code;
	private int retryCount = 0;
	
	public SfdcResult() {
	}
	
	public SfdcResult(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
	
	public int getRetryCount() {
		return this.retryCount;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public void add() {
		retryCount++;
	}

}
