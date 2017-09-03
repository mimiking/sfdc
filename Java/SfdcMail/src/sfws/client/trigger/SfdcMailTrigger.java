package sfws.client.trigger;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import sfws.client.cmn.CmdProcess;

public class SfdcMailTrigger {
	private static Logger logger =Logger.getLogger(SfdcMailTrigger.class);

	public static void main(String[] args) {
		int exitCode = SfdcMailAgent.SUCCESS;
		//log4jを初期化する。
		DOMConfigurator.configure("./config/log4j.xml"); 
		SfdcMailAgent agent = new SfdcMailAgent("./config/setting.config");
		
		if(!agent.validate()){
			//コマンドのフォーマットが不正の場合、異常終了
			logger.error("AppConfigの取得に失敗しました。");
			exitCode = SfdcMailAgent.FAILURE;;
		} else{
			// AppConfigを正常に取り込む場合、HotLeadReflect処理を行う。
			SfdcMailService service = new SfdcMailService();
			//正常終了の場合
			if(CmdProcess.run(service, agent)){
				exitCode = agent.getStatusCode();
			}
			else{
				exitCode = SfdcMailAgent.FAILURE;
			}
		}
		System.exit(exitCode);
	}
}
