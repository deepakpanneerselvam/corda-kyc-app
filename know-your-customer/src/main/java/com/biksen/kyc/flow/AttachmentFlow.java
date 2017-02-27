package com.biksen.kyc.flow;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.flows.BroadcastTransactionFlow;
import co.paralleluniverse.fibers.Suspendable;

import com.biksen.kyc.contract.KYCState;
import com.google.common.collect.ImmutableSet;


public class AttachmentFlow {
    public static class Initiator extends FlowLogic<AttachmentFlowResult> {

    	private Party counterParty;
    	private SignedTransaction signedTx;       

        public Initiator(SignedTransaction signedTx, Party counterParty) {
            this.signedTx = signedTx;
            this.counterParty = counterParty;
        }       

        /**
         * This is the initiator flow. The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override 
		public AttachmentFlowResult call() {

			try {
				
				final Set<Party> participants = ImmutableSet.of(counterParty);
		        
		        subFlow(new BroadcastTransactionFlow(signedTx, participants),false);

				return new AttachmentFlowResult.Success("Transaction successfull at Initiator side.....");
			} catch (Exception ex) {

				return new AttachmentFlowResult.Failure("Transaction failed at Initiator side....."+ex.getMessage());
			}
		}
    }

    public static class Acceptor extends FlowLogic<AttachmentFlowResult> {

        private final Party otherParty;        

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }        

        @Suspendable
        @Override 
        public AttachmentFlowResult call() {
            try {
            	
            	final SignedTransaction ntx = this.sendAndReceive(SignedTransaction.class, otherParty, otherParty.getOwningKey())
                        .unwrap(data -> data);
                
                ntx.verifySignatures();
                
            	return new AttachmentFlowResult.Success("Transaction successfull at Acceptor side..........");
            } catch (Exception ex) {
            	return new AttachmentFlowResult.Failure("Transaction failed at Acceptor side....."+ex.getMessage());
            }
        }
    }

    public static class AttachmentFlowResult {
        public static class Success extends com.biksen.kyc.flow.AttachmentFlow.AttachmentFlowResult {
            private String message;

            private Success(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Success(%s)", message); }
        }

        public static class Failure extends com.biksen.kyc.flow.AttachmentFlow.AttachmentFlowResult {
            private String message;

            private Failure(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Failure(%s)", message); }
        }
    }
}