package com.biksen.kyc.api;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.crypto.SecureHash;
import net.corda.core.messaging.CordaRPCOps;

import com.biksen.kyc.contract.KYCContract;
import com.biksen.kyc.contract.KYCState;
import com.biksen.kyc.flow.KYCFlow;
import com.biksen.kyc.model.KYC;

// This API is accessible from /api/kyc. All paths specified below are relative to it.
@Path("kyc")
public class KYCApi {
    private final CordaRPCOps services;
    private final String myLegalName;

    public KYCApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /*
     * Returns the name of the node providing this end-point.
     * GET Request::
     * http://localhost:10007/api/kyc/me
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() { return singletonMap("me", myLegalName); }

    /**
     * Returns all parties registered with the [NetworkMapService]. The names can be used to look up identities by
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        final String NOTARY_NAME = "Controller";
        return singletonMap(
                "peers",
                services.networkMapUpdates().getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !name.equals(NOTARY_NAME))
                        .collect(toList()));
    }
    
    /*
     * Returns all kycs
     * GET Request::
     * http://localhost:10007/api/kyc/get-kycs
     */
    @GET
    @Path("get-kycs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<ContractState>> getKYCs() {
        return services.vaultAndUpdates().getFirst();
    }

    /*
     * Search matching kycs based on user id
     * GET Request::
     * http://localhost:10007/api/kyc/<user_id>/get-kycs-by-userid
     */
    @GET
    @Path("{userId}/get-kycs-by-userid")
    @Produces(MediaType.APPLICATION_JSON)
    public List<KYC> getKYCsByUserId(@PathParam("userId") String userId) {
    	
    	List<KYC> returnRecords = new ArrayList<KYC>();
    	
    	List<StateAndRef<ContractState>> allRecords = services.vaultAndUpdates().getFirst();
    	
    	for(int i=0; i<allRecords.size();i++){
    		
    		StateAndRef<ContractState> singleRecord = (StateAndRef<ContractState>) allRecords.get(i);
    		
    		KYCState state = (KYCState) singleRecord.getState().getData();
    		
    		if(state.getKYC().getUserId().equalsIgnoreCase(userId)){
    			returnRecords.add(state.getKYC());
    		}
    	}
    	// return only one record based on kycDate which is created last
    	KYC lastKYC = Collections.max(returnRecords, Comparator.comparing(KYC::getKycDate));
    	
    	returnRecords.clear();
    	returnRecords.add(lastKYC);
    	
        return returnRecords;
    }
    
    /*
     * Single party
     * http://localhost:10005/api/kyc/<HDFC>/create-kyc
     * PUT Request::
       {
    		"kycId": 111, "userId": "biksen", "userName": "Jiya Sen", "kycDate": "2017-02-09", "kycValidDate": "2019-09-15", "docId": "A001"
	   }
    */
   @PUT
   @Path("{party1}/create-kyc")
   public Response createKYC(KYC kyc, @PathParam("party1") String partyName1) throws InterruptedException, ExecutionException {
       final Party otherParty = services.partyFromName(partyName1);      
       
       System.out.println("Party1............"+otherParty);       

       if (otherParty == null) {
           return Response.status(Response.Status.BAD_REQUEST).build();
       }
       
       System.out.println("Request received............"+kyc);

       final KYCState state = new KYCState(
               kyc,
               services.nodeIdentity().getLegalIdentity(),
               otherParty,
               new KYCContract());

       // Initiate flow here. The line below blocks and waits for the flow to return.
       final KYCFlow.KYCFlowResult result = services
               .startFlowDynamic(KYCFlow.Initiator.class, state, otherParty)
               .getReturnValue()
               .toBlocking()
               .first();

       final Response.Status status;
       if (result instanceof KYCFlow.KYCFlowResult.Success) {
           status = Response.Status.CREATED;
       } else {
           status = Response.Status.BAD_REQUEST;
       }

       return Response
               .status(status)
               .entity(result.toString())
               .build();
   }
   
   @PUT
   @Path("{otherParty}/create-kyc-with-attachment")
   public Response createKYCWithAttachment(KYC kyc, @PathParam("otherParty") String otherPartyName) throws InterruptedException, ExecutionException {
       final Party otherParty = services.partyFromName(otherPartyName);      
       
       System.out.println("Party1............"+otherParty);       

       if (otherParty == null) {
           return Response.status(Response.Status.BAD_REQUEST).build();
       }
       
       System.out.println("Request received............"+kyc);

       final KYCState state = new KYCState(
               kyc,
               services.nodeIdentity().getLegalIdentity(),
               otherParty,
               new KYCContract());
       
       // Add attachment - Added attachment logic into KYCFlow.java
       /*
        * The code within this comment block is to build the JAR file from base64 string
        import java.util.Base64;

		byte[] bytes = "Hello, World!".getBytes("UTF-8");
		String encoded = Base64.getEncoder().encodeToString(bytes);
		byte[] decoded = Base64.getDecoder().decode(encoded);
		File file = new File("c:/newfile.pdf");;
		FileOutputStream fop = new FileOutputStream(file);

		fop.write(decoded);
		fop.flush();
		fop.close();
        */
       InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("bank-of-london-cp.jar");
       SecureHash id =  services.uploadAttachment(in);
       // End attachment

       // Initiate flow here. The line below blocks and waits for the flow to return.
       final KYCFlow.KYCFlowResult result = services
               .startFlowDynamic(KYCFlow.Initiator.class, state, otherParty)
               .getReturnValue()
               .toBlocking()
               .first();

       final Response.Status status;
       if (result instanceof KYCFlow.KYCFlowResult.Success) {
           status = Response.Status.CREATED;
       } else {
           status = Response.Status.BAD_REQUEST;
       }

       return Response
               .status(status)
               .entity(result.toString())
               .build();
   }

   
}
