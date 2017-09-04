/**
 * 
 */
package salesforce.mail.cmn;

import org.apache.log4j.Logger;

import com.sforce.ws.ConnectorConfig;

/**
 * 共通実行クラス
 * 
 * @author M.Yi　2017/03/08
 *
 */
public class CmdProcess {
	/** ログ部品*/
	private static Logger logger =Logger.getLogger(CmdProcess.class);

	/**
	 * メイン関数
	 * @param args　コマンド文字列
	 */
	public static boolean run(ICmdService service, CmdAgent cmdAgent) {
		boolean ret=true;
		//ログイン情報を取得する。
		ConnectorConfig config = ServiceConfig.getConnectorConfig();
		if(null != config){

			//SalesForceへ接続できる場合
			if(service.connect(config)){
				//コマンドを実行
				if(service.execute(cmdAgent)){
					//Debugログ取得
					service.getDebugLog();
					//実行結果をログに出力する。
					cmdAgent.dumpExecuteInfo();
				}
				//実行失敗の場合
				else{
					logger.error("コマンドの実行が失敗になりました");
					ret=false;
				}
			}
			//接続が失敗になった場合
			else{
				logger.error("SalesForceサービスに接続できません。設定ファイル「" +ServiceConfig.settingFile + "]を確認ください。");
				ret=false;
			}

		}
		//ログイン情報を取得失敗の場合、異常終了
		else{
			logger.error("「" +ServiceConfig.settingFile + "]を読み込むに失敗しました。");
			ret=false;
		}
		return ret;
	}
	
	

}
