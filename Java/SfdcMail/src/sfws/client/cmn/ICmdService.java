package sfws.client.cmn;

import com.sforce.ws.ConnectorConfig;

public interface ICmdService {
	
	public boolean connect(ConnectorConfig config);

	public boolean execute(CmdAgent cmdAgent);
	
	public void getDebugLog();
}
