package org.inchain.wallet.controllers;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.core.AntifakeCode;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.Product;
import org.inchain.core.ProductKeyValue;
import org.inchain.core.exception.AccountEncryptedException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeBindTransaction;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.utils.Base58;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * 防伪测试控制器
 * @author ln
 *
 */
public class AntifakeController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(AntifakeController.class);
	
	public TextArea antifakeCodeId;				//防伪码内容
	public TextField antifakePasswordId;         //防伪密码
	
	public Button verifyButId;						//验证按钮
	public Button resetButId;						//重置按钮
	
	private List<String> successList = new ArrayList<String>();
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	antifakeCodeId.setBackground(Background.EMPTY);
    	Image reset = new Image(getClass().getResourceAsStream("/images/reset_icon.png"));
    	resetButId.setGraphic(new ImageView(reset));
    	Image verify = new Image(getClass().getResourceAsStream("/images/verify_icon.png"));
    	verifyButId.setGraphic(new ImageView(verify));
    	resetButId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				resetForms();
			}
		});
    	
    	verifyButId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				verify();
			}
		});
    }

	/**
     * 初始方法
     */
    public void initDatas() {
    	
    }

    /**
     * 验证防伪码
     */
    protected void verify() {

    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	
    	//防伪码内容
    	String antifakeCode = antifakeCodeId.getText().trim();
    	//防伪密码内容
    	String antifakePassword = antifakePasswordId.getText().trim();
    	//验证接收地址
    	if("".equals(antifakeCode)) {
    		antifakeCodeId.requestFocus();
    		DailogUtil.showTip("请输入防伪码");
    		return;
    	}
    	try {
    		byte[] bytes = Base58.decode(antifakeCode);
    		if(bytes.length == 20) {
    			if("".equals(antifakePassword)) {
    	    		antifakePasswordId.requestFocus();
    	    		DailogUtil.showTip("请输入防伪密码");
    	    		return;
    	    	} else {
    	    		try {
    	    			Long.parseLong(antifakePassword);
    	    		} catch (Exception e) {
    	        		antifakeCodeId.requestFocus();
    	        		DailogUtil.showTip("错误的密码");
    	        		return;
					}
    	    	}
    		}
    	} catch (Exception e) {
    		antifakeCodeId.requestFocus();
    		DailogUtil.showTip("错误的防伪码");
    		return;
		}
		//调用接口广播交易
    	//如果账户已加密，则需要先解密
		verifyDo(accountKit, antifakeCode,antifakePassword);
		
	}

    public void verifyDo(AccountKit accountKit, String antifakeCode, String antifakePassword) throws VerificationException {
    	try{
    		AntifakeCode base58AntifakeCode = null;
    		
    		byte[] bytes = Base58.decode(antifakeCode);
    		
    		//解析防伪码字符串
    		if(bytes.length > 20) {
    			base58AntifakeCode = AntifakeCode.base58Decode(antifakeCode);
    		} else {
    			base58AntifakeCode = new AntifakeCode(bytes, Long.parseLong(antifakePassword));
    		}
    		
			//判断验证码是否存在
			ChainstateStoreProvider chainstateStoreProvider = SpringContextUtils.getBean("chainstateStoreProvider");
			BlockStoreProvider blockStoreProvider = SpringContextUtils.getBean("blockStoreProvider");
			byte[] makebind = chainstateStoreProvider.getBytes(base58AntifakeCode.getAntifakeCode());
			if(makebind == null) {
				throw new VerificationException("防伪码不存在");
			}
			byte[] makebyte = new byte[Sha256Hash.LENGTH];
			byte[] bindbyte = new byte[Sha256Hash.LENGTH];
			System.arraycopy(makebind,0,makebyte,0,Sha256Hash.LENGTH);

			AntifakeCodeBindTransaction bindTx = null;
			TransactionStore bindStore = null;
			if(makebind.length == 2*Sha256Hash.LENGTH){
				System.arraycopy(makebind,Sha256Hash.LENGTH,bindbyte,0,Sha256Hash.LENGTH);
				bindStore = blockStoreProvider.getTransaction(bindbyte);
				bindTx = (AntifakeCodeBindTransaction) bindStore.getTransaction();
			}

			TransactionStore txStore = blockStoreProvider.getTransaction(makebyte);
			//必须存在
			if(txStore == null) {
				throw new VerificationException("防伪码生产交易不存在");
			}
			
			Transaction fromTx = txStore.getTransaction();
			//交易类型必须是防伪码生成交易
			if(fromTx.getType() != Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				throw new VerificationException("防伪码类型错误");
			}
			AntifakeCodeMakeTransaction codeMakeTx = (AntifakeCodeMakeTransaction) fromTx;
			
			//验证防伪码是否已经被验证了
			//保证该防伪码没有被验证
			byte[] txStatus = codeMakeTx.getHash().getBytes();
			byte[] txIndex = new byte[txStatus.length + 1];
			
			System.arraycopy(txStatus, 0, txIndex, 0, txStatus.length);
			txIndex[txIndex.length - 1] = 0;
			
			byte[] status = chainstateStoreProvider.getBytes(txIndex);
			if(status == null) {
				
				showProfuctInfo(blockStoreProvider, codeMakeTx,bindTx,"验证失败，该防伪码已被验证");
				return;
				//throw new VerificationException("验证失败，该防伪码已被验证");
				
			}
			//防伪码验证脚本
			//防伪码验证脚本
			long verifyCode = base58AntifakeCode.getVerifyCode();
			byte[] verifyCodeByte = new byte[8];
			Utils.uint64ToByteArrayLE(verifyCode, verifyCodeByte, 0);
			//把随机数sha256之后和防伪码再次sha256作为验证依据
			byte[] antifakePasswordSha256 = Sha256Hash.hashTwice(verifyCodeByte);
			byte[] verifyContent = new byte[Sha256Hash.LENGTH + 40];
			System.arraycopy(antifakePasswordSha256, 0, verifyContent, 0, Sha256Hash.LENGTH);
			System.arraycopy(base58AntifakeCode.getAntifakeCode(), 0, verifyContent, Sha256Hash.LENGTH, 20);
			System.arraycopy(codeMakeTx.getHash160(), 0, verifyContent, Sha256Hash.LENGTH + 20, 20);
			
			Script inputSig = ScriptBuilder.createAntifakeInputScript(Sha256Hash.hash(verifyContent));
			
			TransactionInput input = new TransactionInput((TransactionOutput) codeMakeTx.getOutput(0));
			input.setScriptSig(inputSig);
			NetworkParams network =SpringContextUtils.getBean("network");
			AntifakeCodeVerifyTransaction tx = new AntifakeCodeVerifyTransaction(network, input, base58AntifakeCode.getAntifakeCode());
			
			//验证账户，不能是认证账户
			Account systemAccount = accountKit.getSystemAccount();
			if(systemAccount == null) {
				throw new VerificationException("账户不存在，不能验证");
			}
			
			//添加奖励输出
			Coin rewardCoin = codeMakeTx.getRewardCoin();
			if(rewardCoin != null && rewardCoin.isGreaterThan(Coin.ZERO)) {
				tx.addOutput(rewardCoin, systemAccount.getAddress());
			}
			
			//签名即将广播的信息
			tx.sign(systemAccount);
			
			//验证成功才广播
			tx.verify();
			tx.verifyScript();
			
    		if(successList.contains(Hex.encode(base58AntifakeCode.getAntifakeCode()))) {
    			showProfuctInfo(blockStoreProvider, codeMakeTx,bindTx, "验证失败，该防伪码已被验证");
    			return;
    		}
    		
			//验证交易合法才广播
			//这里面同时会判断是否被验证过了
			TransactionValidator transactionValidator = SpringContextUtils.getBean("transactionValidator");
			TransactionValidatorResult rs = transactionValidator.valDo(tx, null).getResult();
			if(!rs.isSuccess()) {
				if(rs.getErrorCode() == TransactionValidatorResult.ERROR_CODE_USED) {
					showProfuctInfo(blockStoreProvider, codeMakeTx,bindTx,"验证失败，该防伪码已被验证");
					return;
				}
			}

			//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
			boolean success = MempoolContainer.getInstace().add(tx);
			if(!success) {
				showProfuctInfo(blockStoreProvider, codeMakeTx,bindTx,"验证失败，该防伪码已被验证");
				return;
			}		
			try {
				PeerKit peerKit = SpringContextUtils.getBean("peerKit");
				BroadcastResult result = peerKit.broadcast(tx).get();
				
				//等待广播回应
				if(result.isSuccess()) {
					successList.add(Hex.encode(base58AntifakeCode.getAntifakeCode()));
					resetForms();
					//更新交易记录
					TransactionStoreProvider transactionStoreProvider = SpringContextUtils.getBean("transactionStoreProvider");
					transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
					showProfuctInfo(blockStoreProvider, codeMakeTx,bindTx,"恭喜您，验证通过");
				}
			} catch (Exception e) {
				log.debug("广播失败，失败信息：" + e.getMessage(), e);
			}
			
    	} catch (Exception e) {
    		if(e instanceof AccountEncryptedException) {
    			new Thread() {
    				public void run() {
    					Platform.runLater(new Runnable() {
    					    @Override
    					    public void run() {
    					    	decryptWallet(accountKit, antifakeCode,antifakePassword);
    					    }
    					});
    				};
    			}.start();
    			return;
    		}
    		e.printStackTrace();
    		DailogUtil.showTip(e.getMessage(), 3000);
    		if(log.isDebugEnabled()) {
    			log.debug("验证出错：{}", e.getMessage());
    		}
    	}
    	
	}
    /**
     *显示验证商品信息 
     * */
	private void showProfuctInfo(BlockStoreProvider blockStoreProvider, AntifakeCodeMakeTransaction codeMakeTx, AntifakeCodeBindTransaction codeBindTx,String message) {
		VBox content = new VBox();
		VBox body = new VBox();
		body.setId("show_product_info_id");
		HBox result = new HBox();
		Label name,value;
		value = new Label(message);
		if(message.equals("恭喜您，验证通过")) {
			value.setStyle("-fx-text-fill:#27d454;-fx-font-size:16;");
		}else{
			value.setStyle("-fx-text-fill:red;-fx-font-size:16;");
		}
		result.getChildren().add(value);
		
		result.setStyle("-fx-padding:0 0 0 100;");
	
		body.getChildren().add(result);

		//获取产品信息
		TransactionStore productTxStore = null;
		if(codeMakeTx.getHasProduct()==0) {
			productTxStore = blockStoreProvider.getTransaction(codeBindTx.getProductTx().getBytes());
		}else {
			productTxStore = blockStoreProvider.getTransaction(codeBindTx.getProductTx().getBytes());
		}
		ProductTransaction productTransaction = (ProductTransaction) productTxStore.getTransaction();
		Product product = productTransaction.getProduct();
		List<ProductKeyValue> bodyContents = product.getContents();
		
		byte[] img = null;
		
		for (ProductKeyValue keyValuePair : bodyContents) {
			
			if(ProductKeyValue.LOGO.getCode().equals(keyValuePair.getCode()) || ProductKeyValue.IMG.getCode().equals(keyValuePair.getCode())) {
				img = keyValuePair.getValue();
				continue;
			}
			
			HBox item= new HBox();
			name = new Label(keyValuePair.getName()+":");
			item.getChildren().add(name);
			value.setMaxWidth(300);
			
			if(ProductKeyValue.CREATE_TIME.getCode().equals(keyValuePair.getCode())) {
				value = new Label(DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0))));
			}else{
				value = new Label(keyValuePair.getValueToString());
			}
			Tooltip tooltip = new Tooltip(value.getText());
			tooltip.setFont(Font.font(14));
			tooltip.setMaxWidth(480);
			tooltip.setWrapText(true);
			tooltip.setStyle("-fx-padding:10;");
			value.setTooltip(tooltip);
			item.getChildren().add(value);
			
			item.setStyle("-fx-padding:0 0 10 10;");
			content.getChildren().add(item);
		}
		content.setStyle("-fx-padding:20 0 0 120;");
		
		if(img != null) {
			ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(img)));
			
			imageView.setFitWidth(30);
			imageView.setFitHeight(30);
			imageView.setStyle("-fx-padding:0 0 10 10;");
			content.getChildren().add(imageView);
		}
		body.getChildren().add(content);
		DailogUtil.showDailog(body, "验证结果");
	}

	private void decryptWallet(AccountKit accountKit, String antifakeCode,String antifakePassword) {
		//解密账户
		URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
		FXMLLoader loader = new FXMLLoader(location);
		final AccountKit accountKitTemp = accountKit;
		DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
			@Override
			public void ok(Object param) {
				if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
					new Thread() {
	    				public void run() {
	    					Platform.runLater(new Runnable() {
	    					    @Override
	    					    public void run() {
	    					    	try {
	    					    		verifyDo(accountKitTemp, antifakeCode,antifakePassword);
	    					    	} finally {
	    					    		accountKitTemp.resetKeys();
	    					    	}
	    					    }
	    					});
	    				};
	    			}.start();
				}
			}
		});
	}
    
    /**
     * 重置表单
     */
	public void resetForms() {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	antifakeCodeId.setText("");
		    	antifakePasswordId.setText("");
		    }
		});
	}
	
	@Override
	public void onShow() {
		
	}

	@Override
	public void onHide() {
	}

	@Override
	public boolean refreshData() {
		return false;
	}

	@Override
	public boolean startupInit() {
		return false;
	}
}
