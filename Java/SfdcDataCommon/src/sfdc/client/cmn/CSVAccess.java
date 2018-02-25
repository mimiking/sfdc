package sfdc.client.cmn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

public class CSVAccess {
	/** ログ部品　*/
	static Logger logger = Logger.getLogger(CSVAccess.class);
	
	/** DB接続オブジェクト　*/
	private Connection connection = null;
	
	public CSVAccess(String src, Properties properties) throws Exception {
		this.connection = ConnectionManager.getCsvConnection(src, properties);
	}
	
	public ResultSet select(String sql) throws SQLException {
		Statement stmt = this.connection.createStatement();
		
		ResultSet rs;
		try {
			logger.debug(String.format("SQL: %s", sql));
			rs = stmt.executeQuery(sql);
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
				logger.error("Connection close exception.", e);
			} finally {
				this.connection = null;
			}
		}
    }
}
