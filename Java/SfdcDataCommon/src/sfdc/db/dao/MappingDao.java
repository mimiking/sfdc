package sfdc.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sfdc.client.cmn.DBAccess;
import sfdc.client.util.Constant;
import sfdc.db.entity.MappingEntity;

/**
 * マッピングデータを検索するクラス
 *
 */
public class MappingDao {
	/** ログ部品　*/
	static Logger logger = Logger.getLogger(MappingDao.class);
	/**
	 * コンストラクタ
	 * @param conn　DB接続オブジェクト
	 * @throws Exception 
	 */
	public MappingDao() {
	}

	/**
	 * マッピングデータを取得する。
	 * @param ifid IFID
	 * @return MappingEntity
	 * @throws Exception 
	 */
	public List<MappingEntity> select(String ifId) throws SQLException, Exception {
		String sql = getSelectSql();
		ResultSet rs = DBAccess.getInstance().select(sql, new Object[] { ifId });
        // 入力データトレースログ出力
		List<MappingEntity> mappingList= new ArrayList<MappingEntity>();
		while (rs.next()) {
			MappingEntity data= new MappingEntity();
			data.setIfId(rs.getString(1));
			data.setNo(rs.getInt(2));
			data.setiColL(rs.getString(3));
			data.setiColP(rs.getString(4));
			data.setoColL(rs.getString(5));
			data.setoColP(rs.getString(6));
			data.setFixedVal(rs.getString(7));
			data.setMultiSelect(rs.getInt(8));
			data.setMidStart(rs.getInt(9));
			data.setMidCount(rs.getInt(10));
			data.setDtFormat(rs.getString(11));
			mappingList.add(data);
			logger.debug(data.dumpInfo());
		}
		
		if (mappingList.size() == 0) {
			logger.info(String.format(Constant.MESSAGE_E004, ifId) );
		}
		
		return mappingList;
	}
	
	/**
	 * 対象コラムを取得する。
	 * @param ifId IFID
	 * @return 対象コラム情報
	 * @throws SQLException
	 * @throws Exception
	 */
	public List<String> getColumns(String ifId) throws SQLException, Exception {
		StringBuilder sql = new StringBuilder("SELECT DISTINCT O_COL_P FROM ZUC_M_MAPPINNG ");
		sql.append(this.getWhereClause());
		ResultSet rs = DBAccess.getInstance().select(sql.toString(), new Object[] { ifId });
        // 入力データトレースログ出力
		List<String> columns= new ArrayList<String>();
		while (rs.next()) {
			columns.add(rs.getString(1));
		}
		
		if (columns.size() == 0) {
			logger.info(String.format(Constant.MESSAGE_E004, ifId) );
		}
		
		return columns;
	}
	
	public List<String> getInputColumns(String ifId) throws SQLException, Exception {
		StringBuilder sql = new StringBuilder("SELECT DISTINCT I_COL_P FROM ZUC_M_MAPPINNG ");
		sql.append(this.getWhereClause());
		ResultSet rs = DBAccess.getInstance().select(sql.toString(), new Object[] { ifId });
        // 入力データトレースログ出力
		List<String> columns= new ArrayList<String>();
		while (rs.next()) {
			columns.add(rs.getString(1));
		}
		
		if (columns.size() == 0) {
			logger.info(String.format(Constant.MESSAGE_E004, ifId) );
		}
		
		return columns;
	}
	
	public List<String> getInputIndexList(String ifId) throws SQLException, Exception {
		StringBuilder sql = new StringBuilder("SELECT NO FROM ZUC_M_MAPPINNG ");
		sql.append(this.getWhereClause());
		ResultSet rs = DBAccess.getInstance().select(sql.toString(), new Object[] { ifId });
        // 入力データトレースログ出力
		List<String> columns= new ArrayList<String>();
		while (rs.next()) {
			columns.add(rs.getString(1));
		}
		
		if (columns.size() == 0) {
			logger.info(String.format(Constant.MESSAGE_E004, ifId) );
		}
		
		return columns;
	}
	
    /**
     * マッピング情報取得SQL文を取得する。
     * @return　検索SQL
     */
    protected String getSelectSql() {
	    	StringBuilder sqlCmd = new StringBuilder();
	    	sqlCmd.append("SELECT");
	    	sqlCmd.append(" IFID, NO AS C_NO, I_COL_L, I_COL_P, O_COL_L, O_COL_P, FIXED_VAL");
	    	sqlCmd.append(" , MULTI_SELECT, MID_START, MID_COUNT, DT_FORMAT");
	    	sqlCmd.append(" FROM ZUC_M_MAPPINNG");
	    	sqlCmd.append(this.getWhereClause());
	    	sqlCmd.append(" ORDER BY C_NO ASC ");
	    	return sqlCmd.toString();
    }
    
    private String getWhereClause() {
    	return " WHERE (DEL_FLG IS NULL OR DEL_FLG <> 'X') AND IFID = ?  AND O_COL_P IS NOT NULL";
    }
}
