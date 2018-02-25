package sfdc.mip.client.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import sfdc.client.cmn.CmdAgent;
import sfdc.client.cmn.CommonService;
import sfdc.client.cmn.SfdcAgent;
import sfdc.client.util.CSVFileWriter;
import sfdc.client.util.CommonUtils;
import sfdc.db.dao.ConvertDao;
import sfdc.db.dao.MappingDao;
import sfdc.db.entity.ConvertEntity;
import sfdc.db.entity.MappingEntity;
import sfdc.db.entity.SettingEntity;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.XmlObject;

/**
 * SFDC-MIP連携を実行するクラス
 *
 * @author M.Yi 2018/1/29
 *
 */
public class TeleCallExpService extends CommonService {
	
	/** ログ部品　*/
	static Logger logger = Logger.getLogger(TeleCallExpService.class);
	
	List<MappingEntity> mappingList = null;
	
	String soql = null;
	
	Map<String, List<ConvertEntity>> convertListMap = new HashMap<String, List<ConvertEntity>>();
	
	/**
	 * コンストラクタ
	 */
	public TeleCallExpService() {}
	
	/**
	 * 抽出処理を行う
	 * @param cmdAgent
	 * @return
	 */
	@Override
	public boolean execute(CmdAgent cmdAgent) {
		//連携管理オブジェクトを初期化
		SfdcAgent agent = (SfdcAgent)cmdAgent;
		//処理結果コードを初期化
		int status = TeleCallExpConstant.RETURN_OK;
		try {
			//汎用抽出の設定情報を取得する。
			status = prepareSfdcDownload(agent);
			if (status == TeleCallExpConstant.RETURN_OK) {
				// SFDCからデータを抽出する。
				status = doSfdcDownload(agent);
			}
			// 結果コードを設定する。
	        agent.setRetCode(status);
		} catch (Exception e) {
			logger.error(e);
			status = TeleCallExpConstant.RETURN_NG;
		}
		//プロセスの処理結果を判定
		return status == TeleCallExpConstant.RETURN_OK;
	}
	
	/**
	 * 連携データ取得用検索条件を初期化する
	 * @param agent　連携処理管理クラスのオブジェクト
	 * @return　処理結果（0：正常、9：警告、-1：エラー）
	 */
	protected int prepareSfdcDownload(SfdcAgent agent) {
		if (StringUtils.equals(super.setting.getLinkMethod(), TeleCallExpConstant.STRING_QUERY)) {
			// マッピング情報取得
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I001, TeleCallExpConstant.MESSAGE_R008));
			List<MappingEntity> mappingList = getMappingData(agent.getIfId());
			if (!validateMappingData(agent.getIfId(), mappingList)) {
				return TeleCallExpConstant.RETURN_NG;
			}
			this.mappingList = mappingList;
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I002, TeleCallExpConstant.MESSAGE_R008));
			
			// SOQL文の生成
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I001, TeleCallExpConstant.MESSAGE_R007));
			this.soql = createQuerySoql(super.setting, this.mappingList);
			if (this.soql == null) {
				logger.info(String.format(TeleCallExpConstant.MESSAGE_E001, TeleCallExpConstant.MESSAGE_R007));
				return TeleCallExpConstant.RETURN_NG;
			}
			if (this.soql.length() > 20000) {
				logger.info(String.format(TeleCallExpConstant.MESSAGE_E001, TeleCallExpConstant.MESSAGE_R007));
				return TeleCallExpConstant.RETURN_NG;
			}
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I002, TeleCallExpConstant.MESSAGE_R007));
			return TeleCallExpConstant.RETURN_OK;
		} else {
			logger.info(TeleCallExpConstant.MESSAGE_E006);
			return TeleCallExpConstant.RETURN_NG;
		}
	}
	
	/**
	 * SFDCへデータ更新処理
	 * @param agent 処理情報管理オブジェクト
	 * @return　処理結果（0：正常、9：警告、-1：エラー）
	 */
	protected int doSfdcDownload(SfdcAgent agent){
		// SFDCデータ抽出
		List<Map<String, String>> sfdcDataList = getSfdcData(super.connection, this.soql);
		if (sfdcDataList == null) {
			logger.info(String.format(TeleCallExpConstant.MESSAGE_E001, TeleCallExpConstant.MESSAGE_R014));
			return TeleCallExpConstant.RETURN_NG;
		}
		if (sfdcDataList.isEmpty()) {
			return TeleCallExpConstant.RETURN_OK;
		}
		
		// CSV出力
		logger.info(TeleCallExpConstant.MESSAGE_I012);
		List<List<String>> csvFileData = prepareCSVFileData(agent.getIfId(), super.setting, this.mappingList, sfdcDataList);
		if (outputCSVFile(super.setting, csvFileData, agent.getFileName()) != TeleCallExpConstant.RETURN_OK) {
			return TeleCallExpConstant.RETURN_NG;
		}
		logger.info(String.format(TeleCallExpConstant.MESSAGE_I013, agent.getFileName()));
		
		return TeleCallExpConstant.RETURN_OK;
	}
	
	/**
	 * マッピング情報チェック
	 * @param ifid IFID
	 * @param mappingList マッピング情報リスト
	 * @return boolean
	 */
	private boolean validateMappingData(String ifid, List<MappingEntity> mappingList) {
		if (mappingList.isEmpty()) {
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I005, TeleCallExpConstant.MESSAGE_R010));
			return false;
		}
		for (int index = 0, length = mappingList.size(); index < length; index++) {
			MappingEntity mappingEntity = mappingList.get(index);
			if (!StringUtils.isEmpty(mappingEntity.getFixedVal()) ||
					!StringUtils.isEmpty(mappingEntity.getDtFormat())) {
				List<ConvertEntity> convertEntityList = getConvertData(ifid, mappingEntity.getNo());
				if (!convertEntityList.isEmpty()) {
					logger.info(String.format(TeleCallExpConstant.MESSAGE_E005, TeleCallExpConstant.MESSAGE_R010));
					logger.info("IFID = " + ifid + "; No. = " + mappingEntity.getNo());
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * マッピング情報取得
	 * @param ifid IFID
	 * @return List<MappingEntity>
	 */
	private List<MappingEntity> getMappingData(String ifid) {
		MappingDao mappingDAO = new MappingDao();
		List<MappingEntity> mappingList = new ArrayList<MappingEntity>();
		try {
			mappingList = mappingDAO.select(ifid);
		} catch (Exception e) {
			logger.error(String.format(TeleCallExpConstant.MESSAGE_E001, TeleCallExpConstant.MESSAGE_R008), e);
		}
		return mappingList;
	}

	/**
	 * SOQL文の生成
	 * @param configEntity 設定情報
	 * @param mappingList マッピング情報リスト
	 * @return String
	 */
	private String createQuerySoql(SettingEntity configEntity, List<MappingEntity> mappingList) {
        StringBuilder soql = new StringBuilder();
        String separator = " ";
        soql.append("SELECT");
        for (int index = 0, length = mappingList.size(); index < length; index++) {
    			String iColP = mappingList.get(index).getiColP();
    			if (StringUtils.isEmpty(iColP)) {
				continue;
			}
        		// 複数選択リスト
			if (StringUtils.equals(StringUtils.substring(iColP, 0, 1), "(")) {
				continue;
			}
			soql.append(separator);
			soql.append(iColP);
			separator = ", ";
		}
        separator = " ";
        soql.append(separator);
        
        if (StringUtils.containsIgnoreCase(configEntity.getCondition(), "FROM")) {
        		soql.append(configEntity.getCondition());
		} else if (!StringUtils.isEmpty(configEntity.getLinkObj())) {
			soql.append("FROM");
			soql.append(separator);
			soql.append(configEntity.getLinkObj());
			soql.append(separator);
			soql.append(StringUtils.trimToEmpty(configEntity.getCondition()));
		} else {
			logger.info(String.format(TeleCallExpConstant.MESSAGE_E005, TeleCallExpConstant.MESSAGE_R009));
			logger.info("LINK_OBJ: " + configEntity.getLinkObj() + "; CONDITION: " + configEntity.getCondition());
			return null;
		} 
        
        String soqlStr = soql.toString();
        logger.debug(String.format(TeleCallExpConstant.MESSAGE_I006, soqlStr));
		return soqlStr;
	}
	
	/**
	 * SFDCデータ抽出
	 * @param serviceConn SFDCサービス
	 * @param soql SOQL文
	 * @return List<Map<String, String>>
	 */
	private List<Map<String, String>> getSfdcData(PartnerConnection serviceConn, String soql) {
		List<Map<String, String>> sfdcDataList = new ArrayList<Map<String, String>>(); 
		try {
			logger.info(TeleCallExpConstant.MESSAGE_I009);
			QueryResult qr = serviceConn.query(soql);
			logger.info(String.format(TeleCallExpConstant.MESSAGE_I011, qr.getSize()));
			boolean done = false;
			int loopCount = 0;
			while (!done) {
				logger.debug("Records in results set " + loopCount++ + " - ");
				SObject[] records = qr.getRecords();
				for (int index = 0, length = records.length; index < length; index++) {
					logger.debug("Record " + (index + 1));
					Map<String, String> sfdcData = new HashMap<String, String>();
					SObject data = records[index];
					Iterator<XmlObject> children = data.getChildren();
					pickSfdcData(children, sfdcData, "");
					sfdcDataList.add(sfdcData);
				}
				if (qr.isDone()) {
					done = true;
				} else {
					qr = serviceConn.queryMore(qr.getQueryLocator());
				}
			}
			
		} catch (ConnectionException e) {
			logger.error(e);
			return null;
		}
		return sfdcDataList;
	}
	
	/**
	 * SFDCデータ整理
	 */
	private void pickSfdcData(Iterator<XmlObject> children, Map<String, String> sfdcData,
			String parentName) {
		while (children.hasNext()) {
			XmlObject xmlObject = (XmlObject) children.next();
			String key = xmlObject.getName().getLocalPart();
			if (!StringUtils.equals(key, "type") && !StringUtils.equals(key, "Id")) {
				if (xmlObject.hasChildren()) {
					pickSfdcData(xmlObject.getChildren(), sfdcData, key);
				} else {
					String value = (String) xmlObject.getValue();
					if (!StringUtils.isEmpty(parentName)) {
						key = parentName + "." + key;
					}
					sfdcData.put(key, value);
					logger.debug(key + " = " + value);
				}
			}
		}
	}
	
	/**
	 * データ変換情報取得
	 * @param ifid IFID
	 * @param no NO
	 * @return List<ConvertEntity>
	 */
	private List<ConvertEntity> getConvertData(String ifid, int no) {
		ConvertDao convertDAO = new ConvertDao();
		List<ConvertEntity> convertList = new ArrayList<ConvertEntity>();
		try {
			convertList = convertDAO.select(ifid, no);
		} catch (Exception e) {
			logger.error(String.format(TeleCallExpConstant.MESSAGE_E001, TeleCallExpConstant.MESSAGE_R015), e);
		}
		return convertList;
	}
	
	/**
	 * CSVファイル出力データ準備
	 * @param configEntity 設定情報
	 * @param mappingList マッピング情報リスト
	 * @param sfdcDataList SFDCデータリスト
	 * @return List<List<String>>
	 */
	protected List<List<String>> prepareCSVFileData(String ifid, SettingEntity configEntity, List<MappingEntity> mappingList,
			List<Map<String, String>> sfdcDataList) {
		// 出力データマトリクス生成
		List<List<String>> outputDataList = createOutputMatrix(configEntity, mappingList, sfdcDataList);
		// ヘッダ行
		List<String> header = new ArrayList<String>();
		// 出力データの編集
		for (int colIndex = 0, rowlength = mappingList.size(); colIndex < rowlength; colIndex++) {
			int rowNo = 0;
			if (StringUtils.equals(configEntity.getFileHeader(), "1")) {
				rowNo++;
			}
			MappingEntity mappingEntity = mappingList.get(colIndex);
			// 結合項目を考慮しデータをまとめる
			String colName = mappingEntity.getoColP();
			int colPIndex = header.indexOf(colName);
			if (colPIndex < 0) {
				// ヘッダ行編集
				header.add(colName);
				// 明細行編集
				for (int rowIndex = 0, length = sfdcDataList.size(); rowIndex < length; rowIndex++) {
					Map<String, String> sfdcData = sfdcDataList.get(rowIndex);
					String colData = editOutputData(ifid, sfdcData, mappingEntity);
					outputDataList.get(rowNo + rowIndex).add(colData);
				}
			} else {
				// 明細行編集
				for (int rowIndex = 0, length = sfdcDataList.size(); rowIndex < length; rowIndex++) {
					Map<String, String> sfdcData = sfdcDataList.get(rowIndex);
					String colData = editOutputData(ifid, sfdcData, mappingEntity);
					String colDataBefore = outputDataList.get(rowNo + rowIndex).get(colPIndex);
					outputDataList.get(rowNo + rowIndex).set(colPIndex, colDataBefore + colData);
				}
			}
		}
		if (StringUtils.equals(configEntity.getFileHeader(), "1")) {
			outputDataList.set(0, header);
		}
		logger.debug(outputDataList);
		return outputDataList;
	}
	
	/**
	 * 出力データマトリクス生成
	 * @param configEntity 設定情報
	 * @param mappingList マッピング情報リスト
	 * @param sfdcDataList SFDCデータリスト
	 * @return List<List<String>>
	 */
	private List<List<String>> createOutputMatrix(SettingEntity configEntity, List<MappingEntity> mappingList,
			List<Map<String, String>> sfdcDataList) {
		List<List<String>> outputDataList = new ArrayList<List<String>>();
		// ヘッダ行
		if (StringUtils.equals(configEntity.getFileHeader(), "1")) {
			outputDataList.add(new ArrayList<String>());
		}
		// 明細行
		for (int index = 0, length = sfdcDataList.size(); index < length; index++) {
			outputDataList.add(new ArrayList<String>());
		}
		return outputDataList;
	}
	
	/**
	 * 明細データ編集
	 * @param Map<String, String> SFDCデータ
	 * @param mappingEntity マッピング情報
	 * @return String
	 */
	private String editOutputData(String ifid, Map<String, String> sfdcData, MappingEntity mappingEntity) {
		// 複数選択データ処理
		String colData = processMultiSelect(sfdcData, mappingEntity);
		// 編集仕様の設定パターンによってデータを編集する
		if (!StringUtils.isEmpty(mappingEntity.getFixedVal())) { // 固定値
			return mappingEntity.getFixedVal();
		} else if (!StringUtils.isEmpty(mappingEntity.getDtFormat())) { // 日時フォーマット
			return CommonUtils.dateFormat(colData, mappingEntity.getDtFormat());
		} else { // データ変換あり
//			if (mappingEntity.getMidStart() != 0 || mappingEntity.getMidCount() != 0) { // 切出し
//				if (mappingEntity.getMidCount() == 0) {
//					colData = StringUtils.substring(colData, mappingEntity.getMidStart() - 1);
//				} else if (mappingEntity.getMidStart() == 0) {
//					colData = StringUtils.substring(colData, 0, mappingEntity.getMidCount());
//				} else {
//					int beginIndex = mappingEntity.getMidStart() - 1;
//					int endIndex = beginIndex + mappingEntity.getMidCount();
//					colData = StringUtils.substring(colData, beginIndex, endIndex);
//				}
//			} else { // 編集しない
//				colData = StringUtils.trimToEmpty(colData);
//			}
			colData = split(colData, mappingEntity.getMidStart(), mappingEntity.getMidCount());
			// データ変換
			String key = String.valueOf(mappingEntity.getNo());
			List<ConvertEntity> convertEntityList = null;
			if (convertListMap.containsKey(key)) {
				convertEntityList = convertListMap.get(key);
			} else {
				convertEntityList = getConvertData(ifid, mappingEntity.getNo());
				convertListMap.put(key, convertEntityList);
			}
			if (!convertEntityList.isEmpty()) {
				colData = convertData(ifid, colData, mappingEntity, convertEntityList);
			}
		}
		return colData;
	}
	
	/**
	 * 複数選択データ処理
	 * @param Map<String, String> SFDCデータ
	 * @param mappingEntity マッピング情報
	 * @return String
	 */
	private String processMultiSelect(Map<String, String> sfdcData, MappingEntity mappingEntity) {
		String colData = null;
		if (mappingEntity.getMultiSelect() != 0) {
			String iColP = mappingEntity.getiColP();
			iColP = StringUtils.replace(iColP, "(", "");
			iColP = StringUtils.replace(iColP, ")", "");
			mappingEntity.setiColP(iColP);
			colData = sfdcData.get(mappingEntity.getiColP());
			String[] colDataArray = StringUtils.split(StringUtils.trimToEmpty(colData), ";");
			int index = mappingEntity.getMultiSelect() - 1;
			if (index >= colDataArray.length) {
				colData = "";
			} else {
				colData = colDataArray[index];
			}
		} else {
			colData = StringUtils.trimToEmpty(sfdcData.get(mappingEntity.getiColP()));
		}
		return colData;
	}
	
	/**
	 * データ変換
	 * @param colData 明細データ
	 * @param mappingEntity マッピング情報
	 * @param convertEntityList データ変換情報リスト
	 * @return String
	 */
	private String convertData(String ifid, String colData, MappingEntity mappingEntity, List<ConvertEntity> convertEntityList) {
		// データ変換情報取得
		logger.info(String.format(TeleCallExpConstant.MESSAGE_I001, TeleCallExpConstant.MESSAGE_R015));
		if (convertEntityList.isEmpty()) {
			logger.info(String.format(TeleCallExpConstant.MESSAGE_E005, TeleCallExpConstant.MESSAGE_R016));
			return colData;
		}
		logger.info(String.format(TeleCallExpConstant.MESSAGE_I002, TeleCallExpConstant.MESSAGE_R015));
//		// データ変換
//		for (int index = 0, length = convertEntityList.size(); index < length; index++) {
//			ConvertEntity convertEntity = convertEntityList.get(index);
//			// 変換条件判定
//			String convCondition = convertEntity.getFixedVal();
//			if (StringUtils.equals(convCondition, TeleCallExpConstant.STRING_PERFECT)) {// 完全一致
//				if (StringUtils.equals(colData, convertEntity.getBefore())) {
//					colData = convertEntity.getAfter();
//					break;
//				}
//			} else if (StringUtils.equals(convCondition, TeleCallExpConstant.STRING_PARTIAL)) { // 部分一致
//				colData = StringUtils.replace(colData, convertEntity.getBefore(), convertEntity.getAfter());
//			} else if (StringUtils.equals(convCondition, TeleCallExpConstant.STRING_REGEX)) { // 正規表現
//				colData = colData.replaceAll(convertEntity.getBefore(), convertEntity.getAfter());
//			} else {
//				logger.info(String.format(TeleCallExpConstant.MESSAGE_E005, TeleCallExpConstant.MESSAGE_R016));
//				break;
//			}
//		}
		return convert(convertEntityList, colData);
	}
	
	/**
	 * MIP連携用CSVファイル出力
	 * @param configEntity 設定情報
	 * @param csvFileData CSVファイル出力データ
	 * @param csvFile CSVファイル
	 * @return int
	 */
	protected int outputCSVFile(SettingEntity configEntity, List<List<String>> csvFileData, String csvFile) {
		File file = new File(csvFile);
		File diertory = file.getParentFile();
		if (!diertory.exists()) {
			diertory.mkdirs();
		}
		CSVFileWriter csvFileWriter = new CSVFileWriter(file, configEntity.getFileEncoding(),
				configEntity.getFileSplitter());
		try {
			csvFileWriter.open();
			if (QUOTE_YES.equals(setting.getQuotationMark())) {
				for (int index = 0, length = csvFileData.size(); index < length; index++) {
					csvFileWriter.writeLine(csvFileData.get(index));
				}
			} else {
				for (int index = 0, length = csvFileData.size(); index < length; index++) {
					csvFileWriter.writeLineWithoutEscape(csvFileData.get(index));
				}
			}
			
			csvFileWriter.close();
		} catch (IOException e) {
			logger.error(String.format(TeleCallExpConstant.MESSAGE_E001, TeleCallExpConstant.MESSAGE_R017), e);
			return TeleCallExpConstant.RETURN_NG;
		}
		return TeleCallExpConstant.RETURN_OK;
	}
	
}
