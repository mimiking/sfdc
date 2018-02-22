/**
 * 
 */
package sfdc.client.cmn;

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
		boolean ret = true;
		//ログイン情報を取得する。
		ConnectorConfig config = service.getConfig(cmdAgent);
		if (config != null) {
			//SalesForceへ接続できる場合
			if (service.connect(config)) {
				//コマンドを実行
				if (service.execute(cmdAgent)) {
					//Debugログ取得
					service.getDebugLog();
					// 接続切り
					service.close();
					//実行結果をログに出力する。
//					cmdAgent.dumpExecuteInfo();
				} else {
					//実行失敗の場合
					logger.error("コマンドの実行が失敗になりました");
					ret = false;
				}
			} else {
				//接続が失敗になった場合
				logger.error("SalesForceサービスに接続できません。接続情報をご確認ください。");
				ret = false;
			}
		} else {
			//ログイン情報を取得失敗の場合、異常終了
			ret = false;
		}
		return ret;
	}

}
