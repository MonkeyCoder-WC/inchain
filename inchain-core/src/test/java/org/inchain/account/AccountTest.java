package org.inchain.account;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.math.BigInteger;
import java.net.InetSocketAddress;

import org.inchain.Configure;
import org.inchain.UnitBaseTestCase;
import org.inchain.core.AccountKeyValue;
import org.inchain.core.Product;
import org.inchain.core.ProductKeyValue;
import org.inchain.crypto.ECKey;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.utils.Base58;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AccountTest {

	//	private Logger log = LoggerFactory.getLogger(getClass());
//	@Autowired
//	private NetworkParams network;
	public static void main(String[] args) {
		//@Test
//	public void testUser(){
		AccountKeyValue[] avalues = {new AccountKeyValue("name", "名称", "阿尼斯服装"),
				new AccountKeyValue("address", "地址", "重庆永川"),
				new AccountKeyValue("descript", "描述", "韩版女士服装"),
		};
		AccountBody body = new AccountBody(avalues);
		String ahexString = Base58.encode(body.serialize());
		System.out.println(ahexString);


		ProductKeyValue[] pvalues = {new ProductKeyValue("name", "名称", "正宗普绿茶"),
				//new ProductKeyValue("website", "官网", "https://www.inchain.or"),
				new ProductKeyValue("descript", "描述", "西湖龙井")
		};

		Product p = new Product(pvalues);
		String phexString = Base58.encode(p.serialize());
		System.out.println(phexString);
/*
	@Test
	public void testAddress() {
		
		Address address = Address.fromP2PKHash(network, network.getSystemAccountVersion(), 
				Utils.sha256hash160(ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"))
						.getPubKey(false)));
		
		assertEquals(address.getBase58(), "uMRDgrtfDvG5qkWBs1cHoTt8YbxFf7cDch");
		
		address = AccountTool.newAddressFromPrikey(network, network.getSystemAccountVersion(), new BigInteger(Hex.decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")));
		assertEquals(address.getBase58(), "uK2twT5bjB7WMknf1inMN73ZaktkGMSMnP");
		
	}
	
	@Test
	public void testAccountManager() throws Exception {
		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 6888), true, 25000));
		
		//测试前先清空帐户目录
		File dir = new File(Configure.DATA_ACCOUNT);
		if(dir.listFiles() != null) {
			for (File file : dir.listFiles()) {
				file.delete();
			}
		}
		
		PeerKit peerKit = getBean(PeerKit.class);
		peerKit.startSyn();
		
		AccountKit accountKit = getBean(AccountKit.class);
		try {
			Thread.sleep(2000l);
			if(accountKit.getAccountList().isEmpty()) {
				accountKit.createNewCertAccount("123456", "0123456", AccountBody.empty(), "");
			}
		} finally {
			accountKit.close();
			peerKit.stop();
			
			log.info("test end");
		}
	}*/
	}
}
