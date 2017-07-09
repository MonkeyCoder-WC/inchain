package org.inchain.transaction.business;

import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * 资产交易
 */
public class AssetsTransferTransaction extends AssetsIssuedTransaction {

	public AssetsTransferTransaction(NetworkParams network) {
		super(network);
		type = Definition.TYPE_ASSETS_TRANSFER;
	}

	public AssetsTransferTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
	}

	public AssetsTransferTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	@Override
	public void verify() throws VerificationException {
		super.verify();
	}

	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
	}

	@Override
	protected void parse() throws ProtocolException {
		super.parse();
	}

}
