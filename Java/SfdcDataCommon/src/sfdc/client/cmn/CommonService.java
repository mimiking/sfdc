package sfdc.client.cmn;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.DebugLevel;
import com.sforce.soap.partner.LogCategory;
import com.sforce.soap.partner.LogCategoryLevel;
import com.sforce.soap.partner.LogInfo;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import sfdc.client.util.Constant;
import sfdc.client.util.ServiceConfig;
import sfdc.db.dao.ConfigDao;
import sfdc.db.dao.MappingDao;
import sfdc.db.entity.ConvertEntity;
import sfdc.db.entity.SettingEntity;

public abstract class CommonService implements ICmdService {
	/** ログ部品 */
	private static Logger logger = Logger.getLogger(CommonService.class);
	/** 変換方式：完全一致 */
	public static final String PERFECT = "Perfect";
	/** 変換方式：部分一致 */
	public static final String PARTIAL = "Partial";
	/** 変換方式：正規表現 */
	public static final String REGEX = "Regex";
	/** クォーテーション **/
	public static final String QUOTE_YES = "1";
	/** サービス接続Soapオブジェクト　*/
	protected PartnerConnection connection;
	/** サービス接続情報　*/
	protected ServiceConfig config = null;
	/** 取り込み設定情報*/
	protected SettingEntity setting = null;
	
	public CommonService() {
		this.config = ServiceConfig.getConfig();
	}
	
	/**
	 * 接続情報オブジェクト取得
	 */
	@Override
	public ConnectorConfig getConfig(CmdAgent agent) {
		//SalesForceの接続情報を作成する。
		SfdcConnectorConfig config = null;
		SfdcAgent rAgent = (SfdcAgent)agent;
		try {
			config = new SfdcConnectorConfig();
			//Proxyの設定
			System.setProperty("java.net.useSystemProxies", "true");
			List<Proxy> proxyList = null;
			proxyList = ProxySelector.getDefault().select(URI.create(Connector.END_POINT));
			if (proxyList != null && proxyList.size() > 0) {
				Proxy proxy = proxyList.get(0);
				InetSocketAddress addr = (InetSocketAddress) proxy.address();

				if (addr != null) {
					logger.debug("use system proxy: " + addr.getHostName() + ":" + addr.getPort());
					config.setProxy(addr.getHostName(), addr.getPort());
				} else {
					logger.debug("No Proxy");
				}
			}
			setting = this.getSettings(rAgent.getIfId());
			if (setting != null) {
				if (isActionValid(setting.getLinkMethod())) {
					// 接続情報取得
					String userId = setting.getUserId();
					String password = setting.getPassword();
					String endPointUrl = setting.getEndpointUrl();
					if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(password) || StringUtils.isEmpty(endPointUrl)) {
						//　認証情報不備
						logger.error(String.format(Constant.MESSAGE_E015, rAgent.getIfId()));
						config = null;
					}
					
					config.setUsername(setting.getUserId());
					
					// 暗号化された値を復号する
					password = this.config.decrypt(this.config.getDecryptTool(), setting.getPassword());
					config.setPassword(password);
					// config.setPassword(setting.getPassword());
					
					config.setAuthEndpoint(setting.getEndpointUrl());
					config.setEndPointUrl(setting.getEndpointUrl());
				} else {
					config = null;
					logger.error(String.format("LinkMethod不正です、処理中止します。（%s）", this.setting.getLinkMethod()));
				}
			} else {
				config = null;
			}
		} catch (Exception e) {
			config = null;
		}
		return config;
	}

	@Override
	public boolean connect(ConnectorConfig config) {
		boolean ret=true;
		if (connection == null) {
			SfdcConnectorConfig rConfig = (SfdcConnectorConfig)config;
			for (int i = 0; i <= this.config.getRetryCnt(); i++) {
				try {
					//Debug log 設定
					LogInfo[] logs = new LogInfo[1];
					logs[0] = new LogInfo();
					logs[0].setCategory(LogCategory.All);
					if(logger.isDebugEnabled()){
						logs[0].setLevel(LogCategoryLevel.Debug);
					} else{
						logs[0].setLevel(LogCategoryLevel.Info);
					}
					// WSサービスのコネクションを取得
					config.setServiceEndpoint(rConfig.getEndPointUrl());
					connection = Connector.newConnection(config);
					connection.setDebuggingHeader(logs, DebugLevel.Profiling);
					break;
				} catch (ConnectionException e) {
					logger.error(Constant.MESSAGE_E008, e);
					connection = null;
					if (i < this.config.getRetryCnt()) {
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
	public void getDebugLog() {
		// 本機能ではApexは使用していないので出力しない
		// Do Nothing
	}

	@Override
	public void close() {
		try {
			DBAccess.getInstance().close();
		} catch (Exception e) {
			logger.error(e);
		}
		
		try {
			if (connection != null) {
				connection.logout();
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		} finally {
			connection = null;
		}
	}
	
	/**
	 * 設定情報取得
	 * @return 
	 * @throws Exception 
	 */
	private SettingEntity getSettings(String ifId) throws Exception {
		SettingEntity config = null;
		try {
			ConfigDao configDAO = new ConfigDao();
			config = configDAO.select(ifId);
		} catch (SQLException e) {
			logger.error(String.format(Constant.MESSAGE_E001, Constant.MESSAGE_R004), e);
		}
		
		return config;
	}
	
	protected boolean isActionValid(String linkMethod) {
		return true;
	}
	
	protected Properties getCsvProperties() {
		Properties properties = new Properties();
		String suppressHeaders = SettingEntity.FILE_HEADER_YES.equals(setting.getFileHeader()) ? "false" : "true";
		String encoding = setting.getFileEncoding();
		encoding = StringUtils.isEmpty(encoding) ? "SJIS" : encoding;
		properties.setProperty("separator", setting.getFileSplitter());
		properties.setProperty("charset", encoding);
		properties.setProperty("suppressHeaders", suppressHeaders);
		properties.setProperty("quotechar", QUOTE_YES.equals(setting.getQuotationMark()) ? "" : "\"" );
		
		return properties;
	}
	
	protected String getCsvSql(String tableName) throws SQLException, Exception {
		MappingDao dao = new MappingDao();
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT ");
		if (SettingEntity.FILE_HEADER_YES.equals(setting.getFileHeader())) {
			// ヘッダあり、マッピング情報からコラムを取得する。
			List<String> columnList = dao.getInputColumns(setting.getIfId());
			for(int i = 0; i < columnList.size(); i++) {
				if (i == 0) {
					sql.append(columnList.get(i));
				} else {
					sql.append(", ").append(columnList.get(i));
				}
			}
		} else {
			// ヘッダなし
			List<String> indexList = dao.getInputIndexList(setting.getIfId());
			for(int i = 0; i < indexList.size(); i++) {
				if (i == 0) {
					sql.append("COLUMN").append(indexList.get(i));
				} else {
					sql.append(", ").append("COLUMN").append(indexList.get(i));
				}
			}
		}
		
		sql.append(" FROM ").append(tableName);
		if (StringUtils.isNotEmpty(setting.getCondition())) {
			sql.append(" WHERE ").append(setting.getCondition());
		}
		
		logger.info(String.format("CSV SQL: %s", sql));
		
		return sql.toString();
	}
	
	/**
	 * データ切り出しを行う。
	 * @param input 処理対象
	 * @param start 開始INDEX
	 * @param length 長さ
	 * @return 切り出し結果
	 */
	protected String split(String input, int start, int length) {
		if (StringUtils.isEmpty(input)) {
			return StringUtils.EMPTY;
		} else if (start >= 0) {
			if (start >= input.length()) {
				return StringUtils.EMPTY;
			} else if (length > 0) {
				if (start + length > input.length()) {
					return input.substring(start, input.length());
				} else {
					return input.substring(start, start + length);
				}
			} else {
				return input.substring(start);
			}
			
		} else if (length > 0) {
			return input.substring(0, length);
		} else {
			return input;
		}
	}
	
	/**
	 * データ変換を行う。
	 * @param convertList 変換設定情報
	 * @param input 変換対象
	 * @return 変換結果
	 */
	protected String convert(List<ConvertEntity> convertList, String input) {
		if (convertList != null && convertList.size() > 0) {
			logger.info(String.format("データ変換開始します。（変換前：%s）", input));
			for(ConvertEntity conv: convertList) {
				if (PERFECT.equalsIgnoreCase(conv.getFixedVal())) {
					// 完全一致
					if (input.equals(conv.getBefore())) {
						input = conv.getAfter();
						break;
					}
				} else if (PARTIAL.equalsIgnoreCase(conv.getFixedVal())) {
					// 部分一致
					if (input.contains(conv.getBefore())) {
						input = input.replaceAll(conv.getBefore(), conv.getAfter());
						break;
					}
				} else if (REGEX.equalsIgnoreCase(conv.getFixedVal())) {
					Pattern p = Pattern.compile("(" + conv.getBefore() + ")");
					Matcher m = p.matcher(input);
					if (m.find()){
						input = input.replaceAll(m.group(1), conv.getAfter());
					}
				}
			}
			logger.info(String.format("データ変換終了しました。（変換後：%s）", input));
		}
		
		return input;
	}

}
