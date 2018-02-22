package sfdc.client.cmn;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

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
import sfdc.db.entity.SettingEntity;

public abstract class CommonService implements ICmdService {
	/** ログ部品 */
	private static Logger logger = Logger.getLogger(CommonService.class);
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

}
