package sfdc.client.cmn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import sfdc.client.util.CommonUtils;
import sfdc.client.util.Constant;
import sfdc.db.dao.ConvertDao;
import sfdc.db.dao.MappingDao;
import sfdc.db.entity.ConvertEntity;
import sfdc.db.entity.MappingEntity;
import sfdc.db.entity.SettingEntity;

public abstract class BaseRegistService extends CommonService {

	/** ログ部品 */
	private static Logger logger = Logger.getLogger(BaseRegistService.class);
	
	public BaseRegistService() {
		super();
	}
	
	/**
	 * 処理を実行
	 * @cmdAgent
	 */
	@Override
	public boolean execute(CmdAgent cmdAgent) {
		int status = Constant.RETURN_NG;
		boolean isSuccess = false;
		SfdcAgent agent = (SfdcAgent) cmdAgent;
		try {
			String ifId = agent.getIfId();
			String fileName = agent.getFileName();
			String fileFullName = fileName;
			if (!CommonUtils.isFileExist(fileName)) {
				fileFullName = String.format("%s/%s", config.getDataFilePath(), fileFullName);
				if (!CommonUtils.isFileExist(fileFullName)) {
					// 指定されたファイルが存在しません
					logger.error(String.format(Constant.MESSAGE_E002, fileName));
					status = Constant.RETURN_NG;
					return isSuccess;
				}
			}
			
			// マッピング情報を取得する
			List<MappingEntity> mappingList = this.getMappingList(ifId);
			if (mappingList.size() > 0) {
				// 取込
				status = this.regist(this.setting, mappingList, fileFullName);
			}

			// 結果戻り
			agent.setRetCode(status);
			isSuccess = true;
		} catch (Exception e) {
			agent.setRetCode(Constant.RETURN_NG);
			logger.error(e);
		}

		return isSuccess;
	}
	
	protected abstract int regist(SettingEntity setting, List<MappingEntity> mappingList, String dataFileName) throws Exception;
	
	protected abstract String getDateStr(String input, String format);
	
	protected Map<String, List<ConvertEntity>> getConvertInfo(List<MappingEntity> mappingList) throws Exception {
		
		Map<String, List<ConvertEntity>> convertInfo = new HashMap<String, List<ConvertEntity>>();
		if (mappingList != null) {
			for (MappingEntity entity : mappingList) {
				List<ConvertEntity> convertList =  this.getConvertList(entity.getIfId(), entity.getNo());
				convertInfo.put(getConvertMapKey(entity.getIfId(), entity.getNo()), convertList);
			}
		}
		
		return convertInfo;
		
	}
	
	protected String getConvertMapKey(String ifId, int no) {
		return String.format("%s%s", ifId, no);
	}
	
	/**
	 * 値を取得する。
	 * @param mappingList マッピング設定情報
	 * @param data CSVデータ
	 * @param column コラム名
	 * @return 値
	 * @throws Exception 異常
	 */
	protected String getImportValue(List<MappingEntity> mappingList, Map<String, List<ConvertEntity>> convertInfo, ResultSet data, String column) throws Exception {
//		int index;
		List<ConvertEntity> convertList = null;
		StringBuilder value = new StringBuilder();
		List<MappingEntity> list = this.getColMappings(mappingList, column);
		for(MappingEntity entity : list) {
			convertList = convertInfo.get(getConvertMapKey(entity.getIfId(), entity.getNo()));
			if (!StringUtils.isEmpty(entity.getFixedVal()) || !StringUtils.isEmpty(entity.getDtFormat())) {
				// 固定値 または　日時フォーマットの場合
				if (entity.getMultiSelect() == 0 && entity.getMidStart() == 0 && entity.getMidCount() == 0 && convertList.size() == 0) {
					if (!StringUtils.isEmpty(entity.getFixedVal()) && StringUtils.isEmpty(entity.getDtFormat())) {
						// 固定値
						value.append(entity.getFixedVal());
					} else if (StringUtils.isEmpty(entity.getFixedVal()) && !StringUtils.isEmpty(entity.getDtFormat())) {
						// 日時フォーマット
						value.append(getDateStr(data.getString(entity.getiColP()), entity.getDtFormat()));
					} else {
						// エラー
						logger.error("固定値と日時フォーマットが同時に設定できません。");
						break;
					}
				} else {
					// 固定値または日時フォーマットが設定された場合、ほかのものが設定できません。
					logger.error("設定不正です。固定値または日時フォーマットが設定されているが、複数選択、切り出し、データ変更の情報も設定しています。");
					break;
				}
			} else {
				// 複数選択、切り出し、データ変換処理
				value.append(convert(convertList, this.split(data.getString(entity.getiColP()), entity.getMidStart(), entity.getMidCount())));
			}
		}
		
		logger.info(String.format("%s = %s", column, value));
		
		return value.toString();
	}
	
	/**
	 * 参照型項目のコラム名を取得する。
	 * @param column 参照型設定コラム
	 * @return コラム名
	 */
	protected String getColumn(String column) {
//		if (column.endsWith("_r")) {
//			String[] cols = column.split("\\.");
//			if (cols.length > 1) {
//				return cols[1].replace("__r", "__c");
//			} else {
//				return cols[0].replace("__r", "__c");
//			}
//		} else {
//			return column;
//		}
		String[] cols = column.split("\\.");
		if (cols.length > 1) {
			return cols[1].replace("__r", "__c");
		} else {
			return cols[0].replace("__r", "__c");
		}
	}
	
	/**
	 * マッピング情報を取得する。
	 * @return マッピング情報
	 * @throws Exception 
	 */
	private List<MappingEntity> getMappingList(String ifId) throws Exception {
		List<MappingEntity> mappingList = new ArrayList<MappingEntity>();
		try {
			MappingDao dao = new MappingDao();
			mappingList = dao.select(ifId);
		} catch (SQLException e) {
			logger.error(String.format(Constant.MESSAGE_E001, Constant.MESSAGE_R007), e);
		}
		
		return mappingList;
	}
	
	/**
	 * データ変換情報を取得する。
	 * @param ifId IFID
	 * @param no 順序No.
	 * @return データ変換情報
	 * @throws Exception 
	 */
	private List<ConvertEntity> getConvertList(String ifId, int no) throws Exception {
		
		logger.info("データ変換情報取得開始します。");
		
		List<ConvertEntity> convertList = new ArrayList<ConvertEntity>();
		try {
			ConvertDao dao = new ConvertDao();
			convertList = dao.select(ifId, no);
		} catch (SQLException e) {
			logger.error(String.format(Constant.MESSAGE_E001, Constant.MESSAGE_R008), e);
		}
		
		logger.info("データ変換情報取得完了しました。");
		
		return convertList;
	}
	
	/**
	 * コラムに対するマッピング情報を取得する。
	 * @param mappingList マッピング情報
	 * @param column コラム名
	 * @return マッピング情報
	 */
	private List<MappingEntity> getColMappings(List<MappingEntity> mappingList, String column) {
		List<MappingEntity> list = new ArrayList<MappingEntity>();
		if (mappingList != null && mappingList.size() > 0) {
			for(MappingEntity entity: mappingList) {
				if(column.equals(entity.getoColP())) {
					list.add(entity);
				}
			}
		}
		
		return list;
	}
}
