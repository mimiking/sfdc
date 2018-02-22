package sfdc.client.cmn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class DBAccess {
	/** ログ部品　*/
	static Logger logger = Logger.getLogger(DBAccess.class);

	/** DB接続オブジェクト　*/
	private Connection connection = null;
	
	private static DBAccess dbAccess = null;
	
	private DBAccess() throws Exception {
		this.connection = ConnectionManager.getDBConnection();
	}
	
	public static DBAccess getInstance() throws Exception {
		if (dbAccess == null) {
			dbAccess = new DBAccess();
		}
		return dbAccess;
	}
	
	public ResultSet select(String sql, Object[] params) throws SQLException {
		PreparedStatement stmt = this.connection.prepareStatement(sql);
		if (params != null && params.length > 0) {
			for(int i = 1; i <= params.length; i++) {
				stmt.setObject(i, params[i - 1]);
			}
		}
		
		ResultSet rs;
		try {
			logger.debug(String.format("SQL: %s", sql));
			rs = stmt.executeQuery();
		} catch (SQLException e) {
			logger.error(String.format("SQLException: %s", sql));
			logger.error(e.getMessage());
			throw e;
		}
		
		return rs;
	}
	
	public void close() {
		if (this.connection != null) {
	    		try {
				this.connection.close();
			} catch (SQLException e) {
				logger.error("Connection close exception.");
				logger.error(e.getMessage());
			} finally {
				this.connection = null;
			}
		}
    }
}
