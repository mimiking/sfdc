package sfws.client.trigger;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import sfws.client.cmn.CmdProcess;

/*************************************************************************************
 * Process： ホットリード反映トリガー
 * 
 * Class：   HotLeadReflectTrigger
 * 
 * 更新履歴
 * 項番      更新日付    担当者      更新内容
 *  1.0      2017/03/10  NIS M.Yi  新規作成
 *
**************************************************************************************/
public class HotLeadReflectTrigger {

	private static Logger logger =Logger.getLogger(HotLeadReflectTrigger.class);

	public static void main(String[] args) {
		int exitCode=HotLeadReflectAgent.SUCCESS;
		//log4jを初期化する。
		DOMConfigurator.configure("./config/log4j.xml"); 
		HotLeadReflectAgent agent= new HotLeadReflectAgent("./config/App.config");
		//コマンドのフォーマットが不正の場合、異常終了
		if(!agent.validate()){
			logger.error("AppConfigの取得に失敗しました。");
			exitCode=HotLeadReflectAgent.FAILURE;;
		}
		//AppConfigを正常に取り込む場合、HotLeadReflect処理を行う。
		else{
			HotLeadService service = new HotLeadService();
			//正常終了の場合
			if(CmdProcess.run(service, agent)){
				exitCode=agent.getStatusCode();
			}
			else{
				exitCode=HotLeadReflectAgent.FAILURE;
			}
		}
		System.exit(exitCode);
	}

}
