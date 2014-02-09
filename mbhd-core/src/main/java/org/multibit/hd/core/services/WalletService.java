package org.multibit.hd.core.services;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.Wallet;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.multibit.hd.core.api.TransactionData;
import org.multibit.hd.core.api.WalletData;
import org.multibit.hd.core.managers.WalletManager;

import java.math.BigInteger;
import java.util.Date;
import java.util.Set;

/**
 *  <p>Service to provide the following to GUI classes:<br>
 *  <ul>
 *  <li>list Transactions in the current wallet</li>
 *  </ul>
 * <p/>
 * Most of the functionality is provided by WalletManager and BackupManager.
 * This service provides a single point of entry for GUI code and encapsulates things in a service thread. 
 */
public class WalletService extends AbstractService {

  @Override
  public void start() {
    this.requireSingleThreadExecutor();
  }

  /**
   * Get all the transactions in the current wallet
   */
  public Set<TransactionData> getTransactions() {
    // See if there is a current wallet
    WalletManager walletManager = WalletManager.INSTANCE;

    Optional<WalletData> walletDataOptional = walletManager.getCurrentWalletData();
    if (!walletDataOptional.isPresent()) {
      // No wallet is present
      return Sets.newHashSet();
    }

    // Wallet is present
    WalletData walletData = walletDataOptional.get();
    Wallet wallet = walletData.getWallet();

    // There should be a wallet
    Preconditions.checkNotNull(wallet);

    Set<Transaction> transactions = wallet.getTransactions(true);

    // Adapted transaction data to return
    Set<TransactionData> transactionDatas = Sets.newHashSet();

    if (transactions != null) {
      for (Transaction transaction : transactions) {
        TransactionData transactionData = adaptTransaction(wallet, transaction);
        transactionDatas.add(transactionData);
      }
    }
    return transactionDatas;
  }

  private TransactionData adaptTransaction(Wallet wallet, Transaction transaction) {
    String transactionId = transaction.getHashAsString();
    Date updateTime = transaction.getUpdateTime();
    BigInteger amountBTC = transaction.getValue(wallet);

    TransactionConfidence transactionConfidence = transaction.getConfidence();

    int depth = 0; // By default not in a block
    TransactionConfidence.ConfidenceType confidenceType = TransactionConfidence.ConfidenceType.UNKNOWN; // By default do not know the confidence

    if (transactionConfidence != null) {
      confidenceType = transaction.getConfidence().getConfidenceType();
      if (TransactionConfidence.ConfidenceType.BUILDING.equals(confidenceType)) {
        depth = transaction.getConfidence().getDepthInBlocks();
      }
    }

    // TODO- fee on send

    TransactionData transactionData = new TransactionData(transactionId, updateTime, amountBTC, Optional.<BigInteger>absent(), confidenceType, depth);

    return transactionData;
  }
}
