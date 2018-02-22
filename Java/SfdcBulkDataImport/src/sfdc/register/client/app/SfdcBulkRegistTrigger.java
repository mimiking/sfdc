package sfdc.register.client.app;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import sfdc.client.cmn.BaseTrigger;
import sfdc.client.cmn.CmdAgent;
import sfdc.client.cmn.CmdProcess;
import sfdc.client.cmn.ICmdService;
import sfdc.client.cmn.SfdcAgent;
import sfdc.client.util.Constant;
import sfdc.client.util.ServiceConfig;

public class SfdcBulkRegistTrigger extends BaseTrigger {
	/** ログ部品 */
	private static Logger logger = Logger.getLogger(SfdcBulkRegistTrigger.class);
    /**
	 * コンストラクタ
	 * @param ifid IFID
	 * @param fileName CSVファイル名
     * @throws Exception 
	 */
	public SfdcBulkRegistTrigger() {}
	
	public static void main(String[] args) {
		int retCode = Constant.RETURN_OK;
		// 引数チェック
		if (checkParam(args)) {
			logConfig(args[2]);
			ServiceConfig config = ServiceConfig.getConfig();
			if (config.validate()) {
				CmdAgent agent = new SfdcAgent(args[0], args[1], args[2]);
				ICmdService service = new SfdcBulkRegistService();
				if(CmdProcess.run(service, agent)){
					retCode = ((SfdcAgent)agent).getRetCode();
				}
				else{
					retCode = Constant.RETURN_NG;
				}
				if (retCode == Constant.RETURN_OK) {
					logger.info(String.format(RegistConstant.MESSAGE_I002, RegistConstant.MESSAGE_R002, retCode));
				} else {
					logger.info(String.format(RegistConstant.MESSAGE_I002, RegistConstant.MESSAGE_R001, retCode));
				}
				
			}
		} else {
			retCode = Constant.RETURN_NG;
			logger.info(String.format(RegistConstant.MESSAGE_I002, RegistConstant.MESSAGE_R001,  retCode));
		}
		
		System.exit(retCode);
	}
	
	/**
	 * 引数チェック
	 * @return 
	 */
	private static boolean checkParam(String[] args) {
		if (args.length != 3) {
			return false;
		}
		if (StringUtils.isEmpty(args[0]) || StringUtils.isEmpty(args[1]) || StringUtils.isEmpty(args[2])) {
			return false;
		}
		
		if (!FileUtils.getFile(args[2]).isDirectory()) {
			logger.error(String.format("パラメータ指定が不正です。%s", args[2]));
			return false;
		}
		
		return true;
	}
}
