package sfdc.client.cmn;

import com.sforce.ws.ConnectorConfig;

public interface ICmdService {
	
	public ConnectorConfig getConfig(CmdAgent cmdAgent);
	
	public boolean connect(ConnectorConfig config);

	public boolean execute(CmdAgent cmdAgent);
	
	public void getDebugLog();
	
	/**
	 * Connectionクロス
	 */
	public void close();
}
