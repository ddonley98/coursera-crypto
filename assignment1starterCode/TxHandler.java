import java.util.ArrayList;


public class TxHandler {

    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, (1) all UTXO's used as inputs by tx are valid
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // 1)
        // variable used to keep track of value totals to validate #5 at end
        double outputTotal = 0;
        double inputTotal = 0;
        // this list will keep track of all the UTXO items that are looked at.
        ArrayList<UTXO> currentUTXOList = new ArrayList<UTXO>();

        if(tx == null) return false;

        //
        // validate output transactions first #4, #5
        //
        for (Transaction.Output output : tx.getOutputs()) {
              // transaction not valid if value is negative
              if (output.value < 0 ) return false;

              outputTotal += output.value;
        }


        //
        // Validate input on transaction
        //
        int index = 0;
        for (Transaction.Input input : tx.getInputs() ) {

          // make sure get a valid input object
          if(input == null) return false;

            //
            //  Make a UTOX out of the transaction to use to validate if it is in the pool already
            //
            UTXO currentUTXO = new UTXO(input.prevTxHash, input.outputIndex);

            // need to validate rule #1; utxo must already exist in the pool
            if (!this.pool.contains(currentUTXO)) return false;

            // **** validate rule #2; signature DED::TODO
            if(input.signature == null || input.signature.length == 0) return false;

            // need to get the previous transaction output

            // verify rule #2
            Transaction.Output previousTxOutput = pool.getTxOutput(currentUTXO);
           if (!Crypto.verifySignature(previousTxOutput.address, tx.getRawDataToSign(index), input.signature)) return false;



            // validate rule #3; no UTXO can be claimed multiple times
            if(currentUTXOList.contains(currentUTXO)) return false;
            // not in list so add it for future checks
            currentUTXOList.add(currentUTXO);


            // *** assist in validating #5 at end  DED::TODO this could be bug
            // inputTotal += previousTxOutput.value;
            
            inputTotal += pool.getTxOutput(currentUTXO).value;
            index ++;
        }


        // validate rule #5
        if( inputTotal < outputTotal ) return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
      // Go through each transaction to validate it
      ArrayList<Transaction> passedTranList = new ArrayList<Transaction>();

      //  look at each transaction in the possible transactions array
      for (int i=0; i < possibleTxs.length; i++) {
        //
        //  Validate the transaction then add to the pool
        //
        if (possibleTxs[i] != null && isValidTx(possibleTxs[i])) {

            // add to the pool the outputs
            for (int outputs = 0; outputs < possibleTxs[i].getOutputs().size() ; outputs++ ) {
              // create new entry
              UTXO newUTXO = new UTXO(possibleTxs[i].getHash(), outputs);
              // add new entry to pool
              pool.addUTXO(newUTXO, possibleTxs[i].getOutputs().get(outputs));

            }

            //
            //  remove the inputs from the pool
            // iterate through all the inputs
            int inputsIndex = 0; 
            for (Transaction.Input input : possibleTxs[i].getInputs()) {

            // Bug with removing the input transactions
//   pass 71/100           UTXO newUTXO = new UTXO(input.prevTxHash, inputsIndex);
            // this got all 15 tests to pass
              UTXO newUTXO = new UTXO(input.prevTxHash, input.outputIndex);
              
              pool.removeUTXO(newUTXO);
              inputsIndex++;
            }

            // transaction is valid so add to the passed list
            passedTranList.add(possibleTxs[i]);
            possibleTxs[i] = null;
        }

      }

      // return array of valid transactions
      Transaction[] trans = new Transaction[passedTranList.size()];
      passedTranList.toArray(trans);
      return trans;
    }

}
