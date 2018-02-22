package sfdc.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sfdc.client.cmn.DBAccess;
import sfdc.db.entity.ConvertEntity;

public class ConvertDao {
	static Logger logger = Logger.getLogger(ConvertDao.class);
	
	public ConvertDao() {
	}
	
	public List<ConvertEntity> select(String ifId, int number) throws SQLException, Exception {
		List<ConvertEntity> convertList = new ArrayList<ConvertEntity>();
		
		String sql = this.getSqlCmd();
		ResultSet rs = DBAccess.getInstance().select(sql, new Object[] { ifId, new Integer(number)});
		
        // 入力データトレースログ出力
		while (rs.next()) {
			ConvertEntity data= new ConvertEntity();
			data.setIfId(rs.getString(1));
			data.setNo(rs.getInt(2));
			data.setConvNo(rs.getInt(3));
			data.setiColL(rs.getString(4));
			data.setiColP(rs.getString(5));
			data.setBefore(rs.getString(6));
			data.setAfter(rs.getString(7));
			data.setFixedVal(rs.getString(8));
			data.setMemo(rs.getString(9));
			data.setDelFlg(rs.getString(10));
			convertList.add(data);
			logger.debug(data.dumpInfo());
		}
		
		if (convertList.size() == 0) {
			logger.info(String.format("変換情報がありません。（IFID：%s, 順序No.：%d）", ifId, number));
		}

		return convertList;
	}
	
	private String getSqlCmd() {
		StringBuilder sqlCmd = new StringBuilder("SELECT");
		sqlCmd.append(" IFID, NO, CONV_NO, I_COL_L, I_COL_P,");
		sqlCmd.append(" BEFORE_CONV, AFTER_CONV, FIXED_VAL, MEMO, DEL_FLG");
		sqlCmd.append(" FROM ZUC_M_DATA_CONVERT");
		sqlCmd.append(" WHERE");
		sqlCmd.append(" (DEL_FLG IS NULL OR DEL_FLG <> 'X')");
		sqlCmd.append(" AND IFID = ?");
		sqlCmd.append(" AND NO = ?");
		sqlCmd.append(" ORDER BY CONV_NO ASC");												
		return sqlCmd.toString();
	}
}
