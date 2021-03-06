﻿package sfdc.register.client.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.CSVReader;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import sfdc.client.cmn.BaseRegistService;
import sfdc.client.cmn.CSVAccess;
import sfdc.client.cmn.SfdcConnectorConfig;
import sfdc.client.util.CSVFileWriter;
import sfdc.client.util.CommonUtils;
import sfdc.client.util.Constant;
import sfdc.db.dao.MappingDao;
import sfdc.db.entity.ConvertEntity;
import sfdc.db.entity.MappingEntity;
import sfdc.db.entity.SettingEntity;

public class SfdcBulkRegistService extends BaseRegistService {
	
	/** ログ部品 */
	private static Logger logger = Logger.getLogger(SfdcBulkRegistService.class);
	
	private BulkConnection bConn = null;
	
	public SfdcBulkRegistService() {
		super();
	}

	/**
	 * 接続を行う。
	 * @param config 設定情報
	 */
	@Override
	public boolean connect(ConnectorConfig config) {
		boolean ret=true;
		if (bConn == null) {
			SfdcConnectorConfig rConfig = (SfdcConnectorConfig)config;
			for (int i = 0; i < this.config.getRetryCnt(); i++) {
				try {
					new PartnerConnection(rConfig);
				    ConnectorConfig conf = new ConnectorConfig();
				    conf.setSessionId(rConfig.getSessionId());
				    conf.setProxy(rConfig.getProxy());
				
				    String soapEndpoint = rConfig.getServiceEndpoint();
				    String apiVersion = getApiVersion(rConfig.getEndPointUrl());
				    String restEndpoint = String.format("%sasync/%s", soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/")), apiVersion);
				    conf.setRestEndpoint(restEndpoint);
				    conf.setCompression(true);
				
				    conf.setTraceMessage(false);
				
				    bConn = new BulkConnection(conf );
					
					break;
				} catch (AsyncApiException | ConnectionException e) {
					logger.error(Constant.MESSAGE_E008, e);
					connection = null;
					if (i < this.config.getRetryCnt() - 1) {
						logger.info(String.format(Constant.MESSAGE_I018, this.config.getWaitSeconds()));
						try {
							Thread.sleep(this.config.getWaitSeconds() * 1000L);
						} catch (InterruptedException ie) {
							logger.error(ie);
						}
					}
					ret = false;
				}
			}
		}

		return ret;
	}
	
	@Override
	protected boolean isActionValid(String linkMethod) {
		boolean isValid = false;
		if (StringUtils.isNotEmpty(linkMethod)) {
			linkMethod = linkMethod.toLowerCase();
			isValid =  SettingEntity.LINK_METHOD_BULK_CREATE.equals(linkMethod)
					|| SettingEntity.LINK_METHOD_BULK_UPDATE.equals(linkMethod)
					|| SettingEntity.LINK_METHOD_BULK_UPSERT.equals(linkMethod)
					|| SettingEntity.LINK_METHOD_BULK_DELETE.equals(linkMethod);
		}
		return isValid;
	}
	
	@Override
	protected int regist(SettingEntity setting, List<MappingEntity> mappingList, String dataFileName) throws Exception {
		int retCode = Constant.RETURN_OK;
		CSVFileWriter csvWriter = null;
		CSVAccess access = null;
		ResultSet resultSet = null;
		File csvFile = new File(dataFileName);
		String workFolder = dataFileName.replace(csvFile.getName(), "");

	    String timestamp = CommonUtils.getSysTime("yyyyMMddHHmmssSSS");
	    String importFile = String.format("%s%s_convert.csv",  workFolder, timestamp);
		// CSVファイル読込
		try {
			int lines = 0, totals = 0;
		    
		    List<String> columnList = new MappingDao().getColumns(mappingList.get(0).getIfId());
		    List<String> headers = new ArrayList<String>();
		    for (String column : columnList) {
		    	if (column.contains(":")) {
		    		String[] cols = column.split(":");
		    		if (cols.length == 3) {
		    			headers.add(String.format("%s.%s", cols[0], cols[2]));
		    		} else {
		    			logger.error(String.format("関連項目の外部ID設定不正です。(%s)", column));
		    			return  Constant.RETURN_NG;
		    		}
		    	} else {
		    		headers.add(column);
		    	}
		    }

		    csvWriter = new CSVFileWriter(new File(importFile), "UTF-8", ",");
		    csvWriter.open();
		    csvWriter.writeLine(headers);
		    
		    String sql = getCsvSql(csvFile.getName().replaceAll("\\..*", ""));
			Properties properties = getCsvProperties();
			access = new CSVAccess(dataFileName.replace(csvFile.getName(), ""), properties);
			resultSet = access.select(sql);
		  
		    Map<String, List<ConvertEntity>> convertInfo =  getConvertInfo(mappingList);
		    while (resultSet.next()) {		    	
		    	List<String> valueList = this.dataMapping(columnList, mappingList, convertInfo, resultSet);
		    	lines++;
		    	if (valueList != null && valueList.size() > 0) {
		    		totals++;
		    		csvWriter.writeLine(valueList);
		    	} else {
		    		// マッピングなし、またはマッピング不正
		    		retCode = Constant.RETURN_NG;
		    		logger.error(String.format(RegistConstant.MESSAGE_E013, lines));
		    	}
		    }

		    csvWriter.close();
		    logger.info(String.format(RegistConstant.MESSAGE_I010, lines));
		    
		    if (retCode == Constant.RETURN_OK) {
		    	// Salesforceへ登録
		    	if (totals > 0) {
		    		// データインポートする
		    		retCode = this.csvImport(importFile, workFolder, timestamp);
		    	} else {
		    		// 処理対象データがありません。
		    		logger.error("処理対象データがありません。");
		    	}
		    }

	    } catch (Exception e) {
	    	if (csvWriter != null) {
	    		try {
	    			csvWriter.close();
	    		} catch (Exception ex) {
	    			logger.error("IO異常が発生しました。", e);
	    		}
	    	}
	    	retCode = Constant.RETURN_NG;
	    	logger.error("ファイル処理異常が発生しました。", e);
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
 	    	// 一時ファイルを削除する。
 	    	File file = new File(importFile);
 	    	if (file.exists()) {
 	    		FileUtils.forceDelete(file);
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
	private List<String> dataMapping(List<String> columnList, List<MappingEntity> mappingList, Map<String, List<ConvertEntity>> convertInfo, ResultSet data) throws Exception {
		List<String> valueList = new ArrayList<String>();
		for(String column : columnList) {
			valueList.add(this.getImportValue(mappingList, convertInfo, data, column));
		}
		
		return valueList;
	}
	
	@Override
	protected String getDateStr(String input, String format) {
		String dateStr = CommonUtils.dateFormat(input, CommonUtils.getStandardFormat(format));
		return dateStr.replace(" ", "T");
	}

	/**
	 * 変更されたCSVファイルをインポートする。
	 * @param fileName CSVファイル名
	 * @return 処理結果
	 * @throws IOException
	 */
	private int csvImport(String fileName, String workFolder, String timestamp) throws IOException {
		int status = Constant.RETURN_NG;
		try {
			switch (setting.getLinkMethod().toLowerCase()) {
				case SettingEntity.LINK_METHOD_BULK_CREATE:
					// 登録
					status = this.create(fileName, workFolder, timestamp);
					break;
				case SettingEntity.LINK_METHOD_BULK_UPDATE:
					// 更新
					status = this.update(fileName, workFolder, timestamp);
					break;
				case SettingEntity.LINK_METHOD_BULK_UPSERT:
					// 登録・更新
					status = this.upsert(fileName, workFolder, timestamp);
					break;
				case SettingEntity.LINK_METHOD_BULK_DELETE:
					// 削除
					status = this.delete(fileName, workFolder, timestamp);
					break;
			}
		} catch (AsyncApiException e) {
			logger.error("処理失敗しました。", e);
		}
		
		return status;
	}
	
	/**
	 * CSVファイルデータを登録する。
	 * @param fileName ファイル名
	 * @return 処理結果
	 * @throws AsyncApiException
	 * @throws IOException
	 */
	private int create(String fileName, String workFolder, String timestamp) throws AsyncApiException, IOException {
		JobInfo jobInfo = this.createJob(this.setting.getLinkObj(), OperationEnum.insert);
		return this.jobExecute(jobInfo, fileName, workFolder, timestamp);
	}
	
	/**
	 * CSVファイルデータを更新する。
	 * @param fileName ファイル名
	 * @return 処理結果
	 * @throws AsyncApiException
	 * @throws IOException
	 */
	private int update(String fileName, String workFolder, String timestamp) throws AsyncApiException, IOException {
		JobInfo jobInfo = this.createJob(this.setting.getLinkObj(), OperationEnum.update);
		return this.jobExecute(jobInfo, fileName, workFolder, timestamp);
	}
	
	/**
	 * CSVファイルデータを登録・更新する。
	 * @param fileName ファイル名
	 * @return 処理結果
	 * @throws AsyncApiException
	 * @throws IOException
	 */
	private int upsert(String fileName, String workFolder, String timestamp) throws AsyncApiException, IOException {
		JobInfo jobInfo = this.createJob(this.setting.getLinkObj(), OperationEnum.upsert);
		return this.jobExecute(jobInfo, fileName, workFolder, timestamp);
	}
	
	/**
	 * CSVファイルデータを削除する。
	 * @param fileName ファイル名
	 * @return 処理結果
	 * @throws AsyncApiException
	 * @throws IOException
	 */
	private int delete(String fileName, String workFolder, String timestamp) throws AsyncApiException, IOException {
		JobInfo jobInfo = this.createJob(this.setting.getLinkObj(), OperationEnum.delete);
		return this.jobExecute(jobInfo, fileName, workFolder, timestamp);
	}
	
	/**
	 * ジョブ実行を行う。
	 * @param jobInfo ジョブ情報
	 * @param fileName ファイル名
	 * @return 処理結果
	 * @throws IOException
	 * @throws AsyncApiException
	 */
	private int jobExecute(JobInfo jobInfo, String fileName, String workFolder, String timestamp) throws IOException, AsyncApiException {
		List<BatchInfo> batchList = this.createBatches(jobInfo, fileName, workFolder, timestamp);
		closeJob(jobInfo.getId());
	    awaitCompletion(jobInfo, batchList);
	    return checkResults(jobInfo, batchList);
	}

	/**
	 * ジョブを生成する。
	 * @param sObjectName SObject名
	 * @param action アクション
	 * @return ジョブ
	 * @throws AsyncApiException
	 */
	private JobInfo createJob(String sObjectName, OperationEnum action) throws AsyncApiException {
	    JobInfo job = new JobInfo();
	    job.setObject(sObjectName);
	    job.setOperation(action);
	    job.setContentType(ContentType.CSV);
	    if (OperationEnum.upsert  == action) {
	    	String idCol = this.setting.getExternalIdCol();
			idCol = StringUtils.isEmpty(idCol) ? "Id" : idCol;
	    	job.setExternalIdFieldName(idCol);
	    }
	    job = bConn.createJob(job);
	    
	    logger.info(job);
	
	    return job;
	}
	
	/**
	 * バッチを生成する。
	 * @param jobInfo ジョブ情報
	 * @param csvFileName ファイル名
	 * @return バッチ情報
	 * @throws IOException
	 * @throws AsyncApiException
	 */
	private List<BatchInfo> createBatches(JobInfo jobInfo, String csvFileName, String workFolder, String timeStamp) throws IOException, AsyncApiException {
		List<BatchInfo> batchInfos = new ArrayList<BatchInfo>();
		BufferedReader bufferReader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFileName)));
	    // ヘッダ
	    byte[] headerBytes = (bufferReader.readLine() + "\n").getBytes("UTF-8");
	    int headerBytesLength = headerBytes.length;
	    FileOutputStream tmpOut = null;
	    File tmpFile = new File(String.format("%s%s_bulk.csv", workFolder, timeStamp));
	    try {
	        tmpOut = new FileOutputStream(tmpFile);
	        int maxBytesPerBatch = this.config.getMaxBytesPerBatch();
	        int maxRowsPerBatch = this.config.getMaxRowsPerBatch();
	        int currentBytes = 0;
	        int currentLines = 0;
	        String nextLine;
	        while ((nextLine = bufferReader.readLine()) != null) {
	            byte[] bytes = (nextLine + "\n").getBytes("UTF-8");
	            if (currentBytes + bytes.length > maxBytesPerBatch
	              || currentLines > maxRowsPerBatch) {
	                createBatch(tmpOut, tmpFile, batchInfos, jobInfo);
	                currentBytes = 0;
	                currentLines = 0;
	            }
	            if (currentBytes == 0) {
	            	tmpOut.flush();
	            	tmpOut.close();
	                tmpOut = new FileOutputStream(tmpFile);
	                tmpOut.write(headerBytes);
	                currentBytes = headerBytesLength;
	                currentLines = 1;
	            }
	            tmpOut.write(bytes);
	            currentBytes += bytes.length;
	            currentLines++;
	        }
	        if (currentLines > 1) {
	            createBatch(tmpOut, tmpFile, batchInfos, jobInfo);
	        }
	        
	        tmpOut.flush();
		    tmpOut.close();
	        
	    } finally {
	        try {
	        	bufferReader.close();
	        	boolean isDel = tmpFile.delete();
	        	if (!isDel) {
	        		logger.error(String.format("一時ファイル削除失敗しました。（%s）", tmpFile.getAbsolutePath()));
	        	}
	        } catch (IOException e) {
	        	logger.error("ファイル処理異常が発生しました。", e);
	        }
	    }
	    return batchInfos;
	}
	

	/**
	 * バッチを作成する。
	 * @param tmpOut 一時出力オブジェクト
	 * @param tmpFile 一時ファイル
	 * @param batchInfos バッチ情報
	 * @param jobInfo ジョブ情報
	 * @throws IOException
	 * @throws AsyncApiException
	 */
	private void createBatch(FileOutputStream tmpOut, File tmpFile, List<BatchInfo> batchInfos, JobInfo jobInfo) throws IOException, AsyncApiException {
	    tmpOut.flush();
	    tmpOut.close();
	    FileInputStream tmpInputStream = new FileInputStream(tmpFile);
	    try {
	        BatchInfo batchInfo = this.bConn.createBatchFromStream(jobInfo, tmpInputStream);
	        logger.info(batchInfo);
	        batchInfos.add(batchInfo);
	    } finally {
	        tmpInputStream.close();
	    }
	}
		
	/**
	 * ジョブをクロースする。
	 * @param jobId ジョブID
	 * @throws AsyncApiException
	 */
	private void closeJob(String jobId) throws AsyncApiException {
	    JobInfo job = new JobInfo();
	    job.setId(jobId);
	    job.setState(JobStateEnum.Closed);
	    this.bConn.updateJob(job);
	}
		
	/**
	 * 処理を監視する
	 * @param job ジョブ情報
	 * @param batchInfoList バッチ情報
	 * @throws AsyncApiException
	 */
	private void awaitCompletion(JobInfo job, List<BatchInfo> batchInfoList) throws AsyncApiException {
	    long sleepTime = 0L;
	    Set<String> incomplete = new HashSet<String>();
	    for (BatchInfo bi : batchInfoList) {
	        incomplete.add(bi.getId());
	    }
	    while (!incomplete.isEmpty()) {
	        try {
	            Thread.sleep(sleepTime);
	        } catch (InterruptedException e) {}
	        logger.info(String.format("処理結果を待っています。（件数：%d）", incomplete.size()));
	        sleepTime = 10000L;
	        BatchInfo[] statusList = this.bConn.getBatchInfoList(job.getId()).getBatchInfo();
	        for (BatchInfo b : statusList) {
	            if (b.getState() == BatchStateEnum.Completed
	              || b.getState() == BatchStateEnum.Failed) {
	                if (incomplete.remove(b.getId())) {
	                	logger.info(String.format("バッチ状態：%s", b));
	                }
	            }
	        }
	    }
	}
		
	/**
	 * 処理結果をチェックする。
	 * @param job ジョブ
	 * @param batchInfoList バッチ情報
	 * @return 処理結果
	 * @throws AsyncApiException
	 * @throws IOException
	 */
	private int checkResults(JobInfo job, List<BatchInfo> batchInfoList) throws AsyncApiException, IOException {
	    int status = Constant.RETURN_OK;
		CSVReader reader = null;
		for (BatchInfo b : batchInfoList) {
			reader = new CSVReader(this.bConn.getBatchResultStream(job.getId(), b.getId()));
	        List<String> resultHeader = reader.nextRecord();
	        int resultCols = resultHeader.size();
	 
	        List<String> row;
	        while ((row = reader.nextRecord()) != null) {
	            Map<String, String> resultInfo = new HashMap<String, String>();
	            for (int i = 0; i < resultCols; i++) {
	                resultInfo.put(resultHeader.get(i), row.get(i));
	            }
	            boolean success = Boolean.valueOf(resultInfo.get("Success"));
	            if (!success) {
	            	status = Constant.RETURN_NG;
	            	logger.error(String.format("処理失敗したデータがあります。（エラー: %s）", resultInfo.get("Error")));
	            }
	        }
	    }
		
		return status;
	}
		
	/**
	 * APIバージョンを取得する。
	 * @param endPointUrl ENDPOINT　URL
	 * @return バージョン
	 */
	private String getApiVersion(String endPointUrl) {
		String[] list = endPointUrl.split("/");
		return list[list.length - 1];
	}

}
