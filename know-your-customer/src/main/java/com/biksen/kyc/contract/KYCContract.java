package com.biksen.kyc.contract;

import com.biksen.kyc.model.KYC;
import kotlin.Unit;
import net.corda.core.Utils;
import net.corda.core.contracts.*;
import net.corda.core.contracts.TransactionForContract.InOutGroup;
import net.corda.core.contracts.clauses.*;
import net.corda.core.crypto.SecureHash;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static kotlin.collections.CollectionsKt.single;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class KYCContract implements Contract {
    /**
     * This is a reference to the underlying legal contract template and associated parameters.
     */
    private final SecureHash legalContractReference = SecureHash.sha256("kyc template and params");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }

    /**
     * Filters the command list by type, party and public key all at once.
     */
    private List<AuthenticatedObject<Commands>> extractCommands(TransactionForContract tx) {
        return tx.getCommands()
                .stream()
                .filter(command -> command.getValue() instanceof Commands)
                .map(command -> new AuthenticatedObject<>(
                        command.getSigners(),
                        command.getSigningParties(),
                        (Commands) command.getValue()))
                .collect(toList());
    }

    /**
     * The AllComposition() clause mandates that all specified clauses clauses (in this case [Timestamped] and [Group])
     * must be executed and valid for a transaction involving this type of contract to be valid.
     */
    @Override
    public void verify(TransactionForContract tx) {    	
    	/*List<AuthenticatedObject<Commands>> comm = extractCommands(tx);
    	ClauseVerifier.verifyClause(tx, new Clauses.Timestamp(), comm);
    	ClauseVerifier.verifyClause(tx, new Clauses.Group(), comm);*/
    	
    	AllComposition com = new AllComposition<>(new Clauses.Timestamp(), new Clauses.Group());
        ClauseVerifier.verifyClause(tx,com,extractCommands(tx));
    }

    /**
     * Currently this contract only implements one command. 
     */
    public interface Commands extends CommandData {
        class Place implements IssueCommand, Commands {
            private final long nonce = Utils.random63BitValue();
            @Override public long getNonce() { return nonce; }
        }
    }

    /**
     * This is where we implement our clauses.
     */
    public interface Clauses {
        /**
         * Checks for the existence of a timestamp.
         */
        class Timestamp extends Clause<ContractState, Commands, Unit> {
            @Override public Set<Commands> verify(TransactionForContract tx,
                List<? extends ContractState> inputs,
                List<? extends ContractState> outputs,
                List<? extends AuthenticatedObject<? extends Commands>> commands,
                Unit groupingKey) {

                requireNonNull(tx.getTimestamp(), "must be timestamped");

                // We return an empty set because we don't process any commands
                return Collections.emptySet();
            }
        }

        // If you add additional clauses, make sure to reference them within the 'FirstComposition()' clause.
        class Group extends GroupClauseVerifier<KYCState, Commands, UniqueIdentifier> {
            public Group() { super(new FirstComposition<>(new Clauses.Place())); }

            @Override public List<InOutGroup<KYCState, UniqueIdentifier>> groupStates(TransactionForContract tx) {
                // Group by purchase order linearId for in/out states.
                return tx.groupStates(KYCState.class, KYCState::getLinearId);
            }
        }

        /**
         * Checks various requirements for the placement of a purchase order.
         */
        class Place extends Clause<KYCState, Commands, UniqueIdentifier> {
            @Override public Set<Class<? extends CommandData>> getRequiredCommands() {
                return Collections.singleton(Commands.Place.class);
            }

            @Override public Set<Commands> verify(TransactionForContract tx,
                List<? extends KYCState> inputs,
                List<? extends KYCState> outputs,
                List<? extends AuthenticatedObject<? extends Commands>> commands,
                UniqueIdentifier groupingKey)
            {
                final AuthenticatedObject<Commands.Place> command = requireSingleCommand(tx.getCommands(), Commands.Place.class);
                final KYCState out = single(outputs);
                final Instant time = tx.getTimestamp().getMidpoint();

                requireThat(require -> {
                    // Generic constraints around generation of the issue purchase order transaction.
                    require.by("No inputs should be consumed when issuing a kyc.",
                            inputs.isEmpty());
                    require.by("Only one output state should be created for each group.",
                            outputs.size() == 1);
                    require.by("The buyer and the seller cannot be the same entity.",
                            out.getBuyer() != out.getSeller());
                    require.by("All of the participants must be signers.",
                            command.getSigners().containsAll(out.getParticipants()));

                    // Purchase order specific constraints.
                    /*require.by("We only deliver to the UK.", out.getKYC().getKycId() == 111);
                    require.by("You must order at least one type of item.",
                            !out.getPurchaseOrder().getItems().isEmpty());
                    require.by("You cannot order zero or negative amounts of an item.",
                            out.getPurchaseOrder().getItems().stream().allMatch(item -> item.getAmount() > 0));
                    require.by("You can only order up to 100 items in total.",
                            out.getPurchaseOrder().getItems().stream().mapToInt(PurchaseOrder.Item::getAmount).sum() <= 100);
                    require.by("The delivery date must be in the future.",
                            out.getPurchaseOrder().getDeliveryDate().toInstant().isAfter(time));*/

                    return null;
                });

                return Collections.singleton(command.getValue());
            }
        }
    }
}