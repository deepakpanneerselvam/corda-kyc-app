package com.biksen.kyc.contract;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionType;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;

import com.biksen.kyc.contract.KYCContract.Commands.Place;
import com.biksen.kyc.model.KYC;


public class KYCState implements DealState {
    private final KYC kyc;
    private final Party buyer;
    private final Party seller;
    private final KYCContract contract;
    private final UniqueIdentifier linearId;

    public KYCState(KYC kyc,
                    Party buyer,
                    Party seller,
                    KYCContract contract)
    {
        this.kyc = kyc;
        this.buyer = buyer;
        this.seller = seller;
        this.contract = contract;
        this.linearId = new UniqueIdentifier(
                Integer.toString(kyc.getKycId()),
                UUID.randomUUID());
    }

    public KYC getKYC() { return kyc; }
    public Party getBuyer() { return buyer; }
    public Party getSeller() { return seller; }
    @Override public KYCContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public String getRef() { return linearId.getExternalId(); }
    @Override public List<Party> getParties() { return Arrays.asList(buyer, seller); }
    @Override public List<CompositeKey> getParticipants() {
        return getParties()
                .stream()
                .map(Party::getOwningKey)
                .collect(toList());
    }

    
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        final List<PublicKey> partyKeys = getParties()
                .stream()
                .flatMap(party -> party.getOwningKey().getKeys().stream())
                .collect(toList());
        return ourKeys
                .stream()
                .anyMatch(partyKeys::contains);

    }

    
    @Override public TransactionBuilder generateAgreement(Party notary) {
        /*return new TransactionType.General().Builder(notary)
                .withItems(this, new Command(new Place(), getParticipants()));*/    	
    	
    	Class memberClasses[] = TransactionType.General.class.getDeclaredClasses();    	
    	
    	Class classDefinition = memberClasses[0];
    	
    	TransactionBuilder builder = null;
    	try{
    		Constructor cons = classDefinition.getConstructor(Party.class);    		
    		Object obj = cons.newInstance(notary);
    		
    		TransactionBuilder tempBuilder = (TransactionBuilder) obj;
    		builder = tempBuilder.withItems(this, new Command(new Place(), getParticipants()));
    		
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	} 
    	
    	return builder;    	
    	
    }    
    
}