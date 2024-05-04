package ro.uaic.swqual;

import ro.uaic.swqual.operational.Add;

public class Main {

    public static void main(String[] args) {
        Processor processor = Processor.getInstance();
        processor.execute(new Add("AX", "BX"));
    }
}