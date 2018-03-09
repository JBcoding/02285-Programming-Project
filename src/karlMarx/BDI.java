package karlMarx;

public class BDI {
    public static void main(String[] args) {
        /*
            Agent Control Loop Version 2
            1. B := B0                 // initial beliefs
            2. while true do
            3.   get next percept rho;
            4.   B := brf(B,rho);        // brf (B, rho) is your updated beliefs based on current beliefs B and percept rho.
            5.   I := deliberate(B);   // deliberate(B) is the set of intentions chosen based on current beliefs B.
            6.   pi := plan(B,I);       // plan(B,I) is the plan chosen based on current beliefs B and current intentions I.
            7.   execute(pi)            // the procedure executing pi
            8. end while
        */
        /*
            1. load level
            2. while true do
            5.   choose box and goal
            6.   plan a way from box to goal
            7.   execute plan
            8. end while
        */
    }
}
