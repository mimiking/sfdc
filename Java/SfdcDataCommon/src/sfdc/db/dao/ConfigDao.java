package sfdc.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sfdc.client.cmn.DBAccess;
import sfdc.client.util.CommonUtils;
import sfdc.db.entity.SettingEntity;

/**
 * 設定データを検索するクラス
 *
 */
public class ConfigDao {
	/** ログ部品　*/
	static Logger logger = Logger.getLogger(ConfigDao.class);
	/**
	 * コンストラクタ
	 */
	public ConfigDao() {}

	/**
	 * 設定データを取得する。
	 * @param ifid IFID
	 * @return ConfigEntity
	 * @throws SQLException
	 */
	public SettingEntity select(String ifid) throws SQLException, Exception {
		String sql = this.getSelectSql();
		String sysdate = CommonUtils.getSysDate();
		ResultSet rs = DBAccess.getInstance().select(sql, new Object[] { ifid, sysdate, sysdate, sysdate, sysdate });

        // 入力データトレースログ出力
		List<SettingEntity> configList= new ArrayList<SettingEntity>();
		while (rs.next()) {
			SettingEntity data= new SettingEntity();
			data.setIfId(rs.getString(1));
			data.setConnectId(rs.getInt(2));
			data.setFileHeader(rs.getString(3));
			data.setFileEncoding(rs.getString(4));
			data.setFileSplitter(rs.getString(5));
			data.setLinkMethod(rs.getString(6));
			data.setLinkObj(rs.getString(7));
			data.setExternalIdCol(rs.getString(8));
			data.setCondition(rs.getString(9));
			data.setEndpointUrl(rs.getString(10));
			data.setUserId(rs.getString(11));
			data.setPassword(rs.getString(12));
			configList.add(data);
			logger.debug(data.dumpInfo());
		}
		if (configList.isEmpty()) {
			return null;
		}
		return configList.get(0);
	}
	
    /**
     * 接続先TBL(ZUC_M_IF,ZUC_M_SFDC_CONNECT)を検索するSQLを取得
     * @return　検索SQL
     */
    protected String getSelectSql() {   	
	    	StringBuilder sqlCmd = new StringBuilder();
	    	sqlCmd.append(" SELECT IFID ");
	    	sqlCmd.append(", CONNECT_ID");
	    	sqlCmd.append(", FILE_HEADER");
	    	sqlCmd.append(", FILE_CHAR_CD");
	    	sqlCmd.append(", FILE_SPLITTER ");
	    	sqlCmd.append(", LINK_METHOD");
	    	sqlCmd.append(", LINK_OBJ");
	    	sqlCmd.append(", EXTERNALID_COL");
	    	sqlCmd.append(", CONDITION");
	    	sqlCmd.append(", ENDPOINT_URL");
	    	sqlCmd.append(", USER_ID");
	    	sqlCmd.append(", PASSWORD");
	    	sqlCmd.append(" FROM ZUC_M_IF MIF");
	    	sqlCmd.append(" INNER JOIN ZUC_M_SFDC_CONNECT CON");
	    	sqlCmd.append(" ON MIF.CONNECT_ID = CON.ID");
	    	sqlCmd.append(" WHERE MIF.IFID = ?");
	    	sqlCmd.append(" AND MIF.START_DT <= ?");
	    	sqlCmd.append(" AND MIF.END_DT >= ?");
	    	sqlCmd.append(" AND CON.START_DT <= ?");
	    	sqlCmd.append(" AND CON.END_DT >= ?");
	    	return sqlCmd.toString();
    }
}
