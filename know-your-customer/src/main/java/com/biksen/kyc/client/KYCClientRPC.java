package com.biksen.kyc.client;

import com.biksen.kyc.contract.KYCState;
import com.google.common.net.HostAndPort;
import kotlin.Pair;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.node.services.config.ConfigUtilities;
import net.corda.node.services.messaging.CordaRPCClient;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node and
 * steam some State data from the node.
 */
public class KYCClientRPC {
    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleClientRPC <node address>");
        }

        final Logger logger = LoggerFactory.getLogger(KYCClientRPC.class);
        final HostAndPort nodeAddress = HostAndPort.fromString(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, ConfigUtilities.configureTestSSL());

        // Can be amended in the com.example.Main file.
        client.start("user1", "test");
        final CordaRPCOps proxy = client.proxy();

        // Grab all signed transactions and all future signed transactions.
        final Pair<List<SignedTransaction>, Observable<SignedTransaction>> txsAndFutureTxs =
            proxy.verifiedTransactions();
        final List<SignedTransaction> txs = txsAndFutureTxs.getFirst();
        final Observable<SignedTransaction> futureTxs = txsAndFutureTxs.getSecond();

        // Log the 'placed' purchase order states and listen for new ones.
        futureTxs.startWith(txs).toBlocking().subscribe(
                transaction ->
                        transaction.getTx().getOutputs().forEach(
                                output -> {
                                    final KYCState poState = (KYCState) output.getData();
                                    logger.info(poState.getKYC().toString());
                                })
        );
    }
}