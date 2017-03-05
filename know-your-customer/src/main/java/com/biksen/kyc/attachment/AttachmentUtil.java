package com.biksen.kyc.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import net.corda.core.contracts.TransactionType;
import net.corda.core.crypto.Party;
import net.corda.core.crypto.SecureHash;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.Emoji;
import net.corda.node.services.config.ConfigUtilities;
import net.corda.node.services.messaging.CordaRPCClient;

import org.apache.activemq.artemis.api.core.ActiveMQException;

import rx.Observable;

import com.biksen.kyc.flow.AttachmentFlow;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;

public class AttachmentUtil {
	
	public static void main(String args[]) throws ActiveMQException, IOException{		
		
		if(args[0].equalsIgnoreCase("RECIPIENT"))
		{
			final HostAndPort nodeAddress = HostAndPort.fromString("localhost:10006");
			System.out.println("Connecting to the recipient node "+ nodeAddress);
			final CordaRPCClient client = new CordaRPCClient(nodeAddress, ConfigUtilities.configureTestSSL());
			client.start("user1", "test");
	                final CordaRPCOps proxy = client.proxy();
	                recipient(proxy);
		}
		
		if(args[0].equalsIgnoreCase("SENDER"))
		{
			final HostAndPort nodeAddress = HostAndPort.fromString("localhost:10004");
			System.out.println("Connecting to sender node "+ nodeAddress);
			final CordaRPCClient client = new CordaRPCClient(nodeAddress, ConfigUtilities.configureTestSSL());
			client.start("user1", "test");
	                final CordaRPCOps proxy = client.proxy();
			sender(proxy);  
	        
		}
		
	}

	private static final net.corda.core.crypto.SecureHash.SHA256 PROSPECTUS_HASH;

	static {
		PROSPECTUS_HASH = SecureHash.Companion.parse("decd098666b9657314870e192ced0c3519c2c9d395507a238338f8d003929de9");
	}

	public static final void sender(CordaRPCOps rpc) throws IOException{
		Party otherSide = rpc.partyFromName("HDFC");

		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("bank-of-london-cp.jar");
		//To add attachments the file must first be uploaded to the node, which returns a unique ID that can be added using TransactionBuilder.addAttachment()
		SecureHash id = rpc.uploadAttachment(in);
		
		Class memberClasses[] = TransactionType.General.class.getDeclaredClasses();     	
    	        Class classDefinition = memberClasses[0];    	
    	        TransactionBuilder builder = null;
    	        try{
    		     Constructor cons = classDefinition.getConstructor(Party.class);    		
    		     Object obj = cons.newInstance(otherSide);    		
    		     builder = (TransactionBuilder) obj;   		
    	        }catch(Exception e){
    		     e.printStackTrace();
    	        } 
    	        if(!rpc.attachmentExists(PROSPECTUS_HASH)){
    		      builder.addAttachment(PROSPECTUS_HASH);    	
    	        }
		//Attaching id returned by rpc.uploadAttachment() call
    	        builder.addAttachment(id);
    	        builder.signWith(net.corda.testing.CoreTestUtils.getALICE_KEY());
    	
    	        SignedTransaction stx = builder.toSignedTransaction(true);
    	
    	        System.out.println("Sending attachment......"+stx.getId());     	
    	
    	        final Set<Party> participants = ImmutableSet.of(otherSide);        
    	        final AttachmentFlow.AttachmentFlowResult result = rpc.startFlowDynamic(AttachmentFlow.Initiator.class, stx, otherSide).getReturnValue().toBlocking().first();  	
    	        System.out.println("Got result in sender :::: "+result.toString());    	

	}

	public static final void recipient(CordaRPCOps rpc) {		
		
                System.out.println("Waiting to receive transaction ...");
                SignedTransaction stx = (SignedTransaction)((Observable)rpc.verifiedTransactions().getSecond()).toBlocking().first();
                System.out.println("Received transaction....");
                WireTransaction wtx = stx.getTx();
                List collection = (List)wtx.getAttachments();
                System.out.println("Collection is = "+collection.size());
                if(!collection.isEmpty())
                {            
                      boolean flag = rpc.attachmentExists((SecureHash)PROSPECTUS_HASH);
                      if(!flag)
                      {
                          String s2 = "Failed requirement.";                
                      }
                      String s = (new StringBuilder()).append("File received - we're happy!").append("\n").append("\n").append("Final transaction is:").append("\n").append("\n").append(Emoji.INSTANCE.renderIfSupported(wtx)).toString();
                      System.out.println(s);
                 } else{
                      String s1 = (new StringBuilder()).append("Error: no attachments found in ").append(wtx.getId()).toString();
                      System.out.println(s1);
                 }
	}

}
