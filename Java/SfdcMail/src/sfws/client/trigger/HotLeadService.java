package sfws.client.trigger;

import org.apache.log4j.Logger;

//import com.sforce.soap.HotLeadReflect.Connector;
//import com.sforce.soap.HotLeadReflect.DebuggingInfo_element;
//import com.sforce.soap.HotLeadReflect.LogCategory;
//import com.sforce.soap.HotLeadReflect.LogCategoryLevel;
//import com.sforce.soap.HotLeadReflect.LogInfo;
//import com.sforce.soap.HotLeadReflect.LogType;
//import com.sforce.soap.HotLeadReflect.SoapConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import sfws.client.cmn.CmdAgent;
import sfws.client.cmn.ICmdService;

/**
 * Apexクラスのリモート呼び出しクラス
 * @author m.yi 2017/03/08
 *
 */
public class HotLeadService implements ICmdService{
	
	/** ログイン接続　*/
	static PartnerConnection loginCon;
	/** サービス接続　*/
//	static SoapConnection connection;

	/** ログ部品　*/
	static Logger logger =Logger.getLogger(HotLeadService.class);
	
	
	
	/**
	 * コンストラクタ
	 */
	public HotLeadService() {
		// TODO 自動生成されたコンストラクター・スタブ
	}
	
	/**
	 * サービス接続
	 * @param config
	 * @return
	 */
	@Override
	public boolean connect(ConnectorConfig config){
		boolean ret=true;
//		if(loginCon==null || connection==null){
//			try {
//				loginCon= new PartnerConnection(config);
//				//Debug log 設定
//				LogInfo[] logs = new LogInfo[1];
//				logs[0] = new LogInfo();
//				logs[0].setCategory(LogCategory.Apex_code);
//				logs[0].setLevel(LogCategoryLevel.Debug);
//				//WSサービスのコネクションを取得
//				config.setServiceEndpoint(Connector.END_POINT);
//				connection = Connector.newConnection(config);
//				connection.setDebuggingHeader(logs, LogType.Profiling);
//				
//			} catch (ConnectionException e) {
//				logger.error(e.getMessage());
//				ret=false;
//			}
//
//		}

		return ret;
	}
	
	/**
	 * HotLeadReflect処理を行う
	 * @param cmdAgent
	 * @return
	 */
	@Override
	public boolean execute(CmdAgent cmdAgent){
		
		HotLeadReflectAgent agent=(HotLeadReflectAgent)cmdAgent;

		// 登録・更新を行う。
        int status = doUpSertProcess(agent);
        // 正常終了の場合
        if(status == HotLeadReflectAgent.SUCCESS){
        	//削除処理を行う。
        	status=this.doDeleteProcess(agent);
        }
        //　結果コードを設定する。
        agent.setStatusCode(status);
		
		return status == HotLeadReflectAgent.SUCCESS;
	}
	/**
	 * 処理回数を計算する。
	 * @param agent　処理情報管理オブジェクト
	 * @param isDel　処理種別フラグ（true:削除、false:登録、更新）
	 * @return
	 */
	protected int getProcessTimes(HotLeadReflectAgent agent, boolean isDel){
		int times=0;
		try{
//			int count=0;
//			int records=0;
//			//削除処理の場合、
//			if(isDel){
//				//WebServiceを呼んで削除対象件数を取得する
//				count=connection.getLeadDelCount();
//				//削除処理の単位件数を取得する
//				records=agent.getDelRecordUnit();
//			}
//			//登録・更新処理の場合
//			else{
//				//WebServiceを呼んで登録・更新の対象件数を取得する
//				count=connection.getEloquaLeadCount();
//				//登録・更新の単位件数を取得する。
//				records=agent.getUpSertRecordUnit();
//			}
//			//SaleForceのログを取得する。
//			getDebugLog();
//			//処理回数を計算して返す。
//			times = count % records == 0 ? count / records : count / records + 1;
		}
		catch(Exception e){
			logger.error(e.getStackTrace());
			agent.setStatusCode(HotLeadReflectAgent.FAILURE);
		}
		agent.setStatusCode(HotLeadReflectAgent.SUCCESS);
		return times;
	}
	
	/**
	 * 登録・更新処理
	 * @param agent　処理情報管理オブジェクト
	 * @return　処理結果（0：正常、9：警告、-1：エラー）
	 */
	int doUpSertProcess(HotLeadReflectAgent agent){
		
		int status = HotLeadReflectAgent.SUCCESS;
//		//開始ログ出力
//		logger.info(agent.getMessage("I008"));
//		
//		int processTimes= getProcessTimes(agent, false);
//		//回数が1以上の場合
//		if(processTimes >0){
//			int records= agent.getUpSertRecordUnit();
//			for(int i=0; i< processTimes; i++){
//				// リード登録・更新WEBサービスを呼び出す
//				try {
//					status=connection.insertAndUpdate(agent.getLeadRecordType(), records);
//					//SaleForceのログを取得する。
//					getDebugLog();
//					//処理異常の場合、ループを中止する。
//					if(status==HotLeadReflectAgent.FAILURE){
//							break;
//					}
//				} catch (ConnectionException e) {
//					logger.error(agent.getMessage("E003", "登録・更新"));
//					logger.error(e.getStackTrace());
//					status=HotLeadReflectAgent.FAILURE;
//				}
//			}
//		}
//		//回数が0の場合
//		else{
//			if(HotLeadReflectAgent.SUCCESS==agent.getStatusCode()){
//				// 登録・更新データがありません。
//				logger.info(agent.getMessage("I007", "登録・更新"));
//			}
//			else{
//				logger.error(agent.getMessage("E003", "処理回数"));
//			}
//		}
//		agent.setStatusCode(status);
//		//終了ログ出力
//		logger.info(agent.getMessage("I009"));

		return status;
	}
	
	/**
	 * 削除処理
	 * @param agent
	 * @return　処理結果（0：正常、9：警告、-1：エラー）
	 */
	int doDeleteProcess(HotLeadReflectAgent agent){
		int status = HotLeadReflectAgent.SUCCESS;
//		//開始ログ出力
//		logger.info(agent.getMessage("I008"));
//		//処理回数を取得する。
//		int processTimes= getProcessTimes(agent, true);
//		//回数が1以上の場合
//		if(processTimes >0){
//			int records= agent.getDelRecordUnit();
//			for(int i=0; i< processTimes; i++){
//				// リード削除WEBサービスを呼び出す
//				try {
//					status=connection.deleteLead(agent.getLeadRecordType(), records);
//					//SaleForceのログを取得する。
//					getDebugLog();
//					//処理異常の場合、ループを中止する。
//					if(status==HotLeadReflectAgent.FAILURE){
//							break;
//					}
//				} catch (ConnectionException e) {
//					logger.error(agent.getMessage("E003", "削除"));
//					logger.error(e.getStackTrace());
//					status=HotLeadReflectAgent.FAILURE;
//				}
//			}
//		}
//		//回数が0の場合
//		else{
//			if(HotLeadReflectAgent.SUCCESS==agent.getStatusCode()){
//				// 削除データがありません。
//				logger.info(agent.getMessage("I007", "削除"));
//			}
//			else{
//				logger.error(agent.getMessage("E003", "処理回数"));
//			}
//		}
//		agent.setStatusCode(status);
//		//終了ログ出力
//		logger.info(agent.getMessage("I009"));

		return status;	
	}
	
	/**
	 * Apexの実行ログを出力する
	 */
	@Override
	public void getDebugLog(){
//		String start="\n**************Apex Debug log Start*****************\n";
//		String end  ="\n**************Apex Debug log End  *****************\n";
//		DebuggingInfo_element e= connection.getDebuggingInfo();
//		if(e!=null)
//			logger.info((start + e.getDebugLog() + end).replaceAll("\n", "\r\n"));
//		
	}
	
}
