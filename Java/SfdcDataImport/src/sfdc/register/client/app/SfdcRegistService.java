package sfdc.register.client.app;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

import sfdc.client.cmn.BaseRegistService;
import sfdc.client.cmn.CSVAccess;
import sfdc.client.util.CommonUtils;
import sfdc.db.dao.MappingDao;
import sfdc.db.entity.ConvertEntity;
import sfdc.db.entity.MappingEntity;
import sfdc.db.entity.SettingEntity;

public class SfdcRegistService extends BaseRegistService {
	
	/** ログ部品 */
	private static Logger logger = Logger.getLogger(SfdcRegistService.class);
	
	public SfdcRegistService() {
		super();
	}
	
	@Override
	protected boolean isActionValid(String linkMethod) {
		boolean isValid = false;
		if (StringUtils.isNotEmpty(linkMethod)) {
			linkMethod = linkMethod.toLowerCase();
			isValid = SettingEntity.LINK_METHOD_CREATE.equals(linkMethod)
					|| SettingEntity.LINK_METHOD_UPDATE.equals(linkMethod)
					|| SettingEntity.LINK_METHOD_UPSERT.equals(linkMethod)
					|| SettingEntity.LINK_METHOD_DELETE.equals(linkMethod);
		}
		return isValid;
	}

	@Override
	protected int regist(SettingEntity setting, List<MappingEntity> mappingList, String dataFileName) throws Exception {
		int retCode = RegistConstant.RETURN_OK;
		// CSVファイル読込
		CSVAccess access = null;
		ResultSet resultSet = null;
		try {
			int count = 0;
			File csvFile = new File(dataFileName);
			String sql = getCsvSql(csvFile.getName().replaceAll("\\..*", ""));
			Properties properties = getCsvProperties();
			access = new CSVAccess(dataFileName.replace(csvFile.getName(), ""), properties);
			resultSet = access.select(sql);
		    List<SObject> sObjectList = new ArrayList<SObject>();
		    List<String> columnList = new MappingDao().getColumns(mappingList.get(0).getIfId());
		    
		    // 1行ずつCSVファイルを読み込む
		    Map<String, List<ConvertEntity>> convertInfo =  getConvertInfo(mappingList);
		    while (resultSet.next()) {
				Map<String, String> valueMap = this.dataMapping(columnList, mappingList, convertInfo, resultSet);
		    	count++;
		    	if (valueMap != null && valueMap.size() > 0) {
		    		SObject sObject = this.getSObject(setting.getLinkObj(), mappingList, valueMap);
		    		sObjectList.add(sObject);
		    	} else {
		    		// マッピングなし、またはマッピング不正
		    		retCode = RegistConstant.RETURN_NG;
		    		logger.error(String.format(RegistConstant.MESSAGE_E013, count));
		    	}
			}

		    logger.info(String.format(RegistConstant.MESSAGE_I010, count));
		    
		    if (retCode == RegistConstant.RETURN_OK) {
		    	// Salesforceへ登録
		    	retCode = registToSalesforce(setting, sObjectList);
		    }

	    } catch (IOException e) {
	    	retCode = RegistConstant.RETURN_NG;
	    	logger.error("IO異常が発生しました", e);
	    } finally {
	    	try {
	    		if (resultSet != null) {
		    		resultSet.close();
		    	}
	    		
	    		if (access != null) {
	    			access.close();
	    		}
	    	} catch(Exception e) {
	    		logger.error("CSVファイル操作異常が発生しました", e);
	    	}
	    }
		
		return retCode;
	}
	
	/**
	 * データマッピング情報を取得する。
	 * @param columnList コラム情報
	 * @param mappingList マッピング設定情報
	 * @param data CSVデータ
	 * @return 処理結果
	 * @throws Exception 異常情報
	 */
	private Map<String, String> dataMapping(List<String> columnList, List<MappingEntity> mappingList, Map<String, List<ConvertEntity>> convertInfo, ResultSet data) throws Exception {
		Map<String, String> valueMap = new HashMap<String, String>();
		for(String column : columnList) {
			valueMap.put(column, this.getImportValue(mappingList, convertInfo, data, column));
		}
		
		return valueMap;
	}
	
	@Override
	protected String getDateStr(String input, String format) {
		return CommonUtils.dateFormat(input, CommonUtils.getStandardFormat(format));
	}

	/**
	 * SObjectを取得する。
	 * @param objectName Object名
	 * @param mappingList マッピング設定情報
	 * @param valueMap 値情報
	 * @return SObject
	 * @throws Exception 
	 */
	private SObject getSObject(String objectName, List<MappingEntity> mappingList, Map<String, String> valueMap) throws Exception {
		String column;
		SObject sObject = new SObject();
		sObject.setType(objectName);
		for(MappingEntity mapping : mappingList) {
			column = mapping.getoColP();
			String tVal = valueMap.get(column);
			if (!StringUtils.isEmpty(mapping.getDtFormat())) {
				// 日付
				if (StringUtils.isNotEmpty(tVal)) {
					Calendar c = Calendar.getInstance();
					String format = CommonUtils.getStandardFormat(mapping.getDtFormat());
					SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.JAPAN);
					sdf.setTimeZone(TimeZone.getTimeZone("JST"));
					sdf.setLenient(false);
					c.setTime(sdf.parse(tVal));
					sObject.setField(column, c);
				} else {
					sObject.setField(column, null);
				}
 			} else if (column.contains(":")) {
 				// 外部ID
 				String[] externals = column.split(":");
 				if (externals.length == 3) {
 					SObject pSObject = new SObject();
 	 				pSObject.setType(externals[1]);
 	 				pSObject.setField(externals[2], valueMap.get(column));
 	 				sObject.setField(externals[0], pSObject);
 				} else {
 					throw new Exception("関連項目の外部ID設定が不正です。");
 				}
			} else {
				if (StringUtils.isNotEmpty(tVal)) {
					if ("true".equals(tVal.toLowerCase()) || "false".equals(tVal.toLowerCase())) {
						sObject.setField(column, new Boolean(valueMap.get(column)));
					} else {
						sObject.setField(column, tVal);
					}
				} else {
					sObject.setField(column, valueMap.get(column));
				}
			}
		}
		
		return sObject;
	}

	/**
	 * Salesforceへ登録・更新・削除を行う。
	 * @param setting 設定情報
	 * @param sObjectList データ情報
	 * @return 処理結果
	 */
	private int registToSalesforce(SettingEntity setting, List<SObject> sObjectList) {
		int status = RegistConstant.RETURN_OK;
		try {
    		switch (setting.getLinkMethod().toLowerCase()) {
	    		case SettingEntity.LINK_METHOD_CREATE:
	    			// 新規登録
	    			status = this.create(sObjectList);
	    			break;
	    		case SettingEntity.LINK_METHOD_UPDATE:
	    			// 更新
	    			status = this.update(sObjectList);
	    			break;
	    		case SettingEntity.LINK_METHOD_UPSERT:
	    			// 登録・更新
	    			status = this.upsert(sObjectList);
	    			break;
	    		case SettingEntity.LINK_METHOD_DELETE:
	    			// 削除
	    			status = this.delete(sObjectList);
	    			break;
	    		default:
	    			status = RegistConstant.RETURN_NG;
	    			break;
			}
		} catch (ConnectionException e) {
			status = RegistConstant.RETURN_NG;
			logger.error(e.getMessage(), e);
		}
		
		return status;
	}

	/**
	 * 新規登録を行う。
	 * @param objList 処理対象情報
	 * @return 処理結果
	 * @throws ConnectionException
	 */
	private int create(List<SObject> objList) throws ConnectionException {
		int status = RegistConstant.RETURN_OK;
		if (objList != null && objList.size() > 0) {
			int[] result = this.registToSforce(objList, SettingEntity.LINK_METHOD_CREATE);
			status = result[1] > 0 ? RegistConstant.RETURN_NG : RegistConstant.RETURN_OK;
			logger.info(String.format(RegistConstant.MESSAGE_I009, "登録（Create）", this.setting.getIfId(), result[0], result[1]));
		}

		return status;
	}

	/**
	 * 情報更新を行う。
	 * @param objList 処理対象情報
	 * @return 処理結果
	 * @throws ConnectionException
	 */
	private int update(List<SObject> objList) throws ConnectionException {
		int status = RegistConstant.RETURN_OK;
		if (objList != null && objList.size() > 0) {
			int[] result = this.registToSforce(objList, SettingEntity.LINK_METHOD_UPDATE);
			status = result[1] > 0 ? RegistConstant.RETURN_NG : RegistConstant.RETURN_OK;
			logger.info(String.format(RegistConstant.MESSAGE_I009, "更新（Update）", this.setting.getIfId(), result[0], result[1]));
		}

		return status;
	}

	/**
	 * 情報登録・更新を行う。
	 * @param objList 処理対象情報
	 * @return 処理結果
	 * @throws ConnectionException
	 */
	private int upsert(List<SObject> objList) throws ConnectionException {
		int status = RegistConstant.RETURN_OK;
		if (objList != null && objList.size() > 0) {
			int[] result = this.registToSforce(objList, SettingEntity.LINK_METHOD_UPSERT);
			status = result[1] > 0 ? RegistConstant.RETURN_NG : RegistConstant.RETURN_OK;
			logger.info(String.format(RegistConstant.MESSAGE_I009, "登録・更新（Upsert）", this.setting.getIfId(), result[0], result[1]));
		}

		return status;
	}

	/**
	 * 情報削除を行う。
	 * @param objList 処理対象情報
	 * @return 処理結果
	 * @throws ConnectionException
	 */
	private int delete(List<SObject> objList) throws ConnectionException {
		int status = RegistConstant.RETURN_OK;
		if (objList != null && objList.size() > 0) {
			SObject[] list = new SObject[objList.size()];
			list = objList.toArray(list);
			int[] result = this.deleteFromSforce(list);
			status = result[1] > 0 ? RegistConstant.RETURN_NG : status;
			logger.info(String.format(RegistConstant.MESSAGE_I009, "削除（Delete）", this.setting.getIfId(), result[0], result[1]));
		}

		return status;
	}

	/**
	 * Salesforceへデータ登録・更新を行う。
	 * @param objList 処理対象
	 * @param linkMethod 処理方法
	 * @return 処理結果
	 * @throws ConnectionException
	 */
	private int[] registToSforce(List<SObject> objList, String linkMethod) throws ConnectionException {
		int result[] = new int[] { 0, 0 };
 		int size = objList.size();
		// ２００件毎に処理を行なう
		int times = size % 200 == 0 ? size / 200 : (size / 200 + 1);
		for(int i = 0; i < times; i++) {
			List<SObject> subList = new ArrayList<SObject>();
			if (i + 1 < times) {
				for(int j = i * 200; j < (i + 1) * 200; j++) {
					subList.add(objList.get(j));
				}
			} else {
				for(int j = i * 200; j < size; j++) {
					subList.add(objList.get(j));
				}
			}
			
			int tmpResult[] = new int[] { 0, 0 };
			SObject[] list = new SObject[subList.size()];
			list = subList.toArray(list);
			
			switch(linkMethod.toLowerCase()) {
				case SettingEntity.LINK_METHOD_CREATE:
					tmpResult = this.getResult(connection.create(list));
					break;
				case SettingEntity.LINK_METHOD_UPDATE:
					tmpResult = this.getResult(connection.update(list));
					break;
				case SettingEntity.LINK_METHOD_UPSERT:
					String idCol = setting.getExternalIdCol();
					idCol = StringUtils.isEmpty(idCol) ? "Id" : idCol;
					tmpResult = this.getUpsertStatus(connection.upsert(idCol, list));
					break;
			}
			
			// 結果合計
			result[0] += tmpResult[0];
			result[1] += tmpResult[1];
		}
		
		return result;
	}
	
	/**
	 * Salesforceから対象情報を削除する。
	 * @param objList 処理対象情報
	 * @return 処理結果
	 * @throws ConnectionException
	 */
	private int[] deleteFromSforce(SObject[] objList) throws ConnectionException {
		int delCount = 0;
		int delErrorCount = 0;
		int size = objList.length;
		// ２００件毎に処理を行なう
		int times = size % 200 == 0 ? size / 200 : (size / 200 + 1);
		for(int i = 0; i < times; i++) {
			List<String> subList = new ArrayList<String>();
			if (i + 1 < times) {
				for(int j = i * 200; j < (i + 1) * 200; j++) {
					subList.add(objList[j].getId());
				}
			} else {
				for(int j = i * 200; j < size; j++) {
					subList.add(objList[j].getId());
				}
			}
			
			String[] list = new String[subList.size()];
			list = subList.toArray(list);
			
			DeleteResult[] dResultList = connection.delete(list);
			if (dResultList != null && dResultList.length > 0) {
				for (DeleteResult result : dResultList) {
					if (result.isSuccess()) {
						delCount++;
					} else {
						delErrorCount++;
						logger.error(String.format(RegistConstant.MESSAGE_E012,
								result.getId(), result.getErrors()[0].getStatusCode(), result.getErrors()[0].getMessage()));
					}
				}
			}
		}
		
		return new int [] { delCount, delErrorCount };
	}
	
	/**
	 * 登録または更新処理結果を取得する。
	 * @param sResultList　処理情報
	 * @return 処理結果コード
	 */
	private int[] getResult(SaveResult[] sResultList) {
		int count = 0;
		int errorCount = 0;
		if (sResultList != null && sResultList.length > 0) {
			for(SaveResult sResult : sResultList) {
				
				if (sResult.isSuccess()) {
					count++;
				} else {
					errorCount++;
					for (com.sforce.soap.partner.Error e : sResult.getErrors()) {
						logger.error(e.getMessage());
					}
				}
			}
		}
		
		return new int [] { count, errorCount };
	}
	
	/**
	 * 登録・更新処理結果を取得する。
	 * @param uResultList　処理情報
	 * @return 処理結果コード
	 */
	private int[] getUpsertStatus(UpsertResult[] uResultList) {
		int count = 0;
		int errorCount = 0;
		if (uResultList != null && uResultList.length > 0) {
			for(UpsertResult uResult : uResultList) {
				if (uResult.isSuccess()) {
					count++;
				} else {
					errorCount++;
					for (com.sforce.soap.partner.Error e : uResult.getErrors()) {
						logger.error(e.getMessage());
					}
				}
			}
		}
		
		return new int [] { count, errorCount };
	}
}
