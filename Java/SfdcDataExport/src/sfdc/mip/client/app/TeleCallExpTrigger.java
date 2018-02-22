package sfdc.mip.client.app;

import org.apache.log4j.Logger;

import sfdc.client.cmn.BaseTrigger;
import sfdc.client.cmn.CmdAgent;
import sfdc.client.cmn.CmdProcess;
import sfdc.client.cmn.ICmdService;
import sfdc.client.cmn.SfdcAgent;
import sfdc.client.util.ServiceConfig;

/*************************************************************************************
 * Process：SFDC-MIP連携データ抽出処理
 * 
 * Class：   TeleCallExpTrigger
 * 
 * 更新履歴
 * 項番      更新日付    担当者      更新内容
 * 1.0      2018/1/29  M.Yi       新規作成
 *
**************************************************************************************/
public class TeleCallExpTrigger extends BaseTrigger {

	private static Logger logger = Logger.getLogger(TeleCallExpTrigger.class);

	public static void main(String[] args) {
		int retCode = TeleCallExpConstant.RETURN_OK;
		// 引数チェック
		if (validateParam(args)) {
			logConfig(args[2]);
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I001, TeleCallExpConstant.MESSAGE_R003));
			ServiceConfig config = ServiceConfig.getConfig();
			if (config.validate()) {
				CmdAgent agent = new SfdcAgent(args[0], args[1], args[2]);
				ICmdService service = new TeleCallExpService();
				if (CmdProcess.run(service, agent)) {
					retCode = ((SfdcAgent)agent).getRetCode();
					logger.info(String.format(TeleCallExpConstant.MESSAGE_I015, TeleCallExpConstant.MESSAGE_R002, retCode));
				} else {
					retCode = TeleCallExpConstant.RETURN_NG;
					logger.info(String.format(TeleCallExpConstant.MESSAGE_I015, TeleCallExpConstant.MESSAGE_R001, retCode));
				}
			} else {
				retCode = TeleCallExpConstant.RETURN_NG;
				logger.info(String.format(TeleCallExpConstant.MESSAGE_E005, TeleCallExpConstant.MESSAGE_R009));
			}
		} else {
			logger.info(TeleCallExpConstant.MESSAGE_E004);
			retCode = TeleCallExpConstant.RETURN_NG;
		}
		System.exit(retCode);
	}
	
	/**
	 * 引数チェック
	 * @param args 引数
	 * @return 
	 */
	private static boolean validateParam(String[] args) {
		if (args.length != 3) {
			return false;
		}
		return true;
	}
}
