package com.biksen.kyc.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.corda.core.crypto.Party;
import net.corda.core.flows.IllegalFlowLogicException;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.CordaPluginRegistry;
import net.corda.core.node.PluginServiceHub;
import net.corda.core.transactions.SignedTransaction;

import com.biksen.kyc.api.KYCApi;
import com.biksen.kyc.contract.KYCContract;
import com.biksen.kyc.contract.KYCState;
import com.biksen.kyc.flow.AttachmentFlow;
import com.biksen.kyc.flow.KYCFlow;
import com.biksen.kyc.model.KYC;
import com.biksen.kyc.service.KYCService;
import com.esotericsoftware.kryo.Kryo;

public class KYCPlugin extends CordaPluginRegistry {	
	
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = Collections.singletonList(KYCApi::new);

    /**
     * A list of flows required for this CorDapp. Any flow which is invoked from from the web API needs to be
     * registered as an entry into this map. The map takes the form:
     *
     * Name of the flow to be invoked -> Set of the parameter types passed into the flow.
     *
     * E.g. In the case of this CorDapp:
     *
     * "ExampleFlow.Initiator" -> Set(PurchaseOrderState, Party)
     *
     * This map also acts as a white list. If a flow is invoked via the API and not registered correctly
     * here, then the flow state machine will _not_ invoke the flow. Instead, an exception will be raised.
     */
    /*private final Map<String, Set<String>> requiredFlows = Collections.singletonMap(
            KYCFlow.Initiator.class.getName(),
            new HashSet<>(Arrays.asList(
                    KYCState.class.getName(),
                    Party.class.getName()
            )));*/
    
    /*private final Map<String, Set<String>> requiredFlows = Collections.singletonMap(
            AttachmentFlow.Initiator.class.getName(), 
            new HashSet<>(Arrays.asList(SignedTransaction.class.getName(), Party.class.getName())));*/
	
   //Putting the above two flows into a single map
    Map<String, Set<String>> requiredFlows = new HashMap<String, Set<String>>();
   //instance block
    {
    	requiredFlows.put(KYCFlow.Initiator.class.getName(), new HashSet<>(Arrays.asList(
                KYCState.class.getName(),
                Party.class.getName()
        )));
    	requiredFlows.put(AttachmentFlow.Initiator.class.getName(), new HashSet<>(Arrays.asList(SignedTransaction.class.getName(), Party.class.getName())));
    	
    }
   

    /**
     * A list of long lived services to be hosted within the node. Typically you would use these to register flow
     * factories that would be used when an initiating party attempts to communicate with our node using a particular
     * flow. See the [ExampleService.Service] class for an implementation.
     */
    private final List<Function<PluginServiceHub, ?>> servicePlugins = Collections.singletonList(KYCService::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    private final Map<String, String> staticServeDirs = Collections.singletonMap(
            // This will serve the exampleWeb directory in resources to /web/example
            "kyc", getClass().getClassLoader().getResource("kycWeb").toExternalForm()
    );

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, Set<String>> getRequiredFlows() { return requiredFlows; }
    @Override public List<Function<PluginServiceHub, ?>> getServicePlugins() { return servicePlugins; }
    @Override public Map<String, String> getStaticServeDirs() { return staticServeDirs; }

    /**
     * Register required types with Kryo (our serialisation framework).
     */
    @Override public boolean registerRPCKryoTypes(Kryo kryo) {
        kryo.register(KYCState.class);
        kryo.register(KYCContract.class);
        kryo.register(KYC.class);
        //kryo.register(PurchaseOrder.Address.class);
        kryo.register(Date.class);
        //kryo.register(PurchaseOrder.Item.class);
        kryo.register(KYCFlow.KYCFlowResult.Success.class);
        kryo.register(KYCFlow.KYCFlowResult.Failure.class);
        kryo.register(IllegalArgumentException.class);
        kryo.register(IllegalFlowLogicException.class);
        kryo.register(AttachmentFlow.AttachmentFlowResult.Success.class);
        kryo.register(AttachmentFlow.AttachmentFlowResult.Failure.class);
        return true;
    }
}
