package sfdc.client.cmn;

public class SfdcAgent extends CmdAgent {
	
	private int retCode;
	private String ifId;
	private String fileName;
	private String logFolder;
	
	public SfdcAgent(String ifId, String fileName, String logFolder) {
		this.ifId = ifId;
		this.fileName = fileName;
		this.logFolder = logFolder;
	}
	

	@Override
	protected String[] parseCmd(String cmds) {
		return null;
	}

	@Override
	protected boolean validate() {
		return true;
	}

	@Override
	protected void dumpExecuteInfo() {}
	
	public int getRetCode() {
		return retCode;
	}

	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public String getIfId() {
		return ifId;
	}

	public void setIfId(String ifId) {
		this.ifId = ifId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getLogFolder() {
		return logFolder;
	}

	public void setLogFolder(String logFolder) {
		this.logFolder = logFolder;
	}
}
