package tests;

public class SubSubTest21 extends SubTest2 {
	public String toString() {
		int i = 21;
		return "SubSubTest"+i;
	}
	
	int curChar;
	
	 public final int jjMoveStringLiteralDfa2_0(long old0, long active0)
	  {
	    if (((active0 &= old0)) == 0L)
	      return jjStartNfa_0(0, old0); 
	    try { curChar = System.in.read(); }
	    catch(java.io.IOException e) {
	      jjStopStringLiteralDfa_0(1, active0);
	      return 2;
	    }
	    switch(curChar)
	      {
	      case 97:
	        return jjMoveStringLiteralDfa3_0(active0, 0x1000L);
	      case 99:
	        return jjMoveStringLiteralDfa3_0(active0, 0x402000L);
	      case 100:
	        return jjMoveStringLiteralDfa3_0(active0, 0x800L);
	      case 102:
	        return jjMoveStringLiteralDfa3_0(active0, 0x200000L);
	      case 110:
	        return jjMoveStringLiteralDfa3_0(active0, 0x20000L);
	      case 111:
	        return jjMoveStringLiteralDfa3_0(active0, 0x48000L);
	      case 112:
	        if ((active0 & 0x800000L) != 0L)
	          return jjStartNfaWithStates_0(2, 23, 4);
	        break;
	      case 116:
	        if ((active0 & 0x10000L) != 0L)
	          return jjStartNfaWithStates_0(2, 16, 4);
	        return jjMoveStringLiteralDfa3_0(active0, 0x104000L);
	      case 117:
	        return jjMoveStringLiteralDfa3_0(active0, 0x80000L);
	      default :
	        break;
	      }
	    return jjStartNfa_0(1, active0);
	  }
	
	 private final int jjMoveStringLiteralDfa3_0(long old0, long active0) {
		 return (int)(old0+active0);
	 }
	 
	 private final int jjStopStringLiteralDfa_0(long old0, long active0) {
		 return (int)(old0+active0);
	 }
	 
	 private final int jjStartNfa_0(long old0, long active0) {
		 return (int)(old0+active0);
	 }
	 
	 private final int jjStartNfaWithStates_0(long old0, long active0, long other) {
		 return (int)(old0+active0+other);
	 }
}
