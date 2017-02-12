package com.biksen.kyc.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.biksen.kyc.contract.KYCState;
import com.biksen.kyc.model.KYC;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.CryptoUtilities;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.NotaryFlow;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static kotlin.collections.CollectionsKt.single;


public class KYCFlow {
    public static class Initiator extends FlowLogic<KYCFlowResult> {

        private final KYCState kycState;
        private final Party otherParty;
        
        private final ProgressTracker progressTracker = new ProgressTracker(
                CONSTRUCTING_OFFER,
                SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION,
                VERIFYING,
                SIGNING,
                NOTARY,
                RECORDING,
                SENDING_FINAL_TRANSACTION
        );

        private static final ProgressTracker.Step CONSTRUCTING_OFFER = new ProgressTracker.Step(
                "Constructing proposed kyc.");
        private static final ProgressTracker.Step SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION = new ProgressTracker.Step(
                "Sending kyc to other party for review, and receiving partially signed transaction from other party in return.");
        private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step NOTARY = new ProgressTracker.Step(
                "Obtaining notary signature.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");
        private static final ProgressTracker.Step SENDING_FINAL_TRANSACTION = new ProgressTracker.Step(
                "Sending fully signed transaction to other party.");

        public Initiator(KYCState kycState, Party otherParty) {
            this.kycState = kycState;
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override public KYCFlowResult call() {
            
            try {
                
                final KeyPair myKeyPair = getServiceHub().getLegalIdentityKey();                
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
                final CompositeKey notaryPubKey = notary.getOwningKey();

                // Stage 1.
                progressTracker.setCurrentStep(CONSTRUCTING_OFFER);
                
                final TransactionState offerMessage = new TransactionState<ContractState>(kycState, notary);

                // Stage 2.
                progressTracker.setCurrentStep(SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION);
                
                // -----------------------
                // Flow jumps to Acceptor.
                // -----------------------
               
                final SignedTransaction ptx = sendAndReceive(SignedTransaction.class, otherParty, offerMessage)
                        .unwrap(data -> data);

                // Stage 7.
                progressTracker.setCurrentStep(VERIFYING);
               
                final WireTransaction wtx = ptx.verifySignatures(CryptoUtilities.getComposite(myKeyPair.getPublic()), notaryPubKey);
                
                wtx.toLedgerTransaction(getServiceHub()).verify();

                // Stage 8.
                progressTracker.setCurrentStep(SIGNING);
                
                final DigitalSignature.WithKey mySig = CryptoUtilities.signWithECDSA(myKeyPair, ptx.getId().getBytes());
                final SignedTransaction vtx = ptx.plus(mySig);

                // Stage 9.
                progressTracker.setCurrentStep(NOTARY);
                
                final DigitalSignature.WithKey notarySignature = subFlow(new NotaryFlow.Client(vtx, NotaryFlow.Client.Companion.tracker()), false);
               
                final SignedTransaction ntx = vtx.plus(notarySignature);

                // Stage 10.
                progressTracker.setCurrentStep(RECORDING);
                
                getServiceHub().recordTransactions(Collections.singletonList(ntx));

                // Stage 11.
                progressTracker.setCurrentStep(SENDING_FINAL_TRANSACTION);
                
                send(otherParty, ntx);
                //This will return to REST service
                return new KYCFlowResult.Success(String.format("Transaction id %s committed to ledger.", ntx.getId()));
            } catch(Exception ex) {
                
                return new KYCFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class Acceptor extends FlowLogic<KYCFlowResult> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                WAIT_FOR_AND_RECEIVE_PROPOSAL,
                GENERATING_TRANSACTION,
                SIGNING,
                SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
                VERIFYING_TRANSACTION,
                RECORDING
        );

        private static final ProgressTracker.Step WAIT_FOR_AND_RECEIVE_PROPOSAL = new ProgressTracker.Step(
                "Receiving proposed kyc from initiator.");
        private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
                "Generating transaction based on proposed kyc.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing proposed transaction with our private key.");
        private static final ProgressTracker.Step SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE = new ProgressTracker.Step(
                "Sending partially signed transaction to initiator and wait for a response.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        @Suspendable
        @Override public KYCFlowResult call() {
            try {
                
                final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

                // Stage 3.
                progressTracker.setCurrentStep(WAIT_FOR_AND_RECEIVE_PROPOSAL);
                
                final TransactionState<DealState> message = this.receive(TransactionState.class, otherParty)
                        .unwrap(data -> (TransactionState<DealState>) data );
                
                System.out.println("Received data at receiver side............."+((KYCState)message.getData()).getKYC());

                // Stage 4.
                progressTracker.setCurrentStep(GENERATING_TRANSACTION);                
                
                // This will call "KYCState.generateAgreement()"
                final TransactionBuilder utx = message.getData().generateAgreement(message.getNotary());
                
                final Instant currentTime = getServiceHub().getClock().instant();
               
                utx.setTime(currentTime, Duration.ofSeconds(30));              

                // Stage 5.
                progressTracker.setCurrentStep(SIGNING);
                
                final SignedTransaction stx = utx.signWith(keyPair).toSignedTransaction(false);

                // Stage 6.
                progressTracker.setCurrentStep(SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE);
                // Send the state back across the wire to the designated counterparty.
                // ------------------------
                // Flow jumps to Initiator.
                // ------------------------
                // Receive the signed transaction off the wire from the other party.
                final SignedTransaction ntx = this.sendAndReceive(SignedTransaction.class, otherParty, stx)
                        .unwrap(data -> data);

                // Stage 12.
                progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
                
                ntx.verifySignatures();
                // Check it's valid.
                ntx.toLedgerTransaction(getServiceHub()).verify();

                // Record the transaction.
                progressTracker.setCurrentStep(RECORDING);
                getServiceHub().recordTransactions(Collections.singletonList(ntx));
                return new KYCFlowResult.Success(String.format("Transaction id %s committed to ledger.", ntx.getId()));
            } catch (Exception ex) {
                return new KYCFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class KYCFlowResult {
        public static class Success extends com.biksen.kyc.flow.KYCFlow.KYCFlowResult {
            private String message;

            private Success(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Success(%s)", message); }
        }

        public static class Failure extends com.biksen.kyc.flow.KYCFlow.KYCFlowResult {
            private String message;

            private Failure(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Failure(%s)", message); }
        }
    }
}