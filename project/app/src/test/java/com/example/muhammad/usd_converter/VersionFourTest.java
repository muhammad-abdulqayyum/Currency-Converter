package com.example.muhammad.usd_converter;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;
public class VersionFourTest {
 private BigDecimal n(String s){return new BigDecimal(s);}
 @Test public void usdGold(){assertEquals(n("0.02500000"),Conversion.cross(n("100"),n("1"),n("0.00025"),"XAU"));}
 @Test public void goldUsd(){assertEquals(n("4000.00"),Conversion.cross(n("1"),n("0.00025"),n("1"),"USD"));}
 @Test public void goldSilver(){assertEquals(n("100.00000000"),Conversion.cross(n("1"),n("0.00025"),n("0.025"),"XAG"));}
 @Test public void btcGold(){assertEquals(n("25.00000000"),Conversion.cross(n("1"),n("0.00001"),n("0.00025"),"XAU"));}
 @Test public void xrpBtc(){assertEquals(n("0.00200000"),Conversion.cross(n("100"),n("0.5"),n("0.00001"),"BTC"));}
 @Test public void brlPhp(){assertEquals(n("1200.00"),Conversion.cross(n("100"),n("5"),n("60"),"PHP"));}
 @Test public void fractionalCoin(){assertEquals(n("0.00000001"),Conversion.assetAmount("0.00000001"));}
 @Test public void finalRounding(){assertEquals(n("0.33"),Conversion.cross(n("1"),n("3"),n("1"),"USD"));}
 @Test public void zero(){assertEquals(n("0.00"),Conversion.cross(n("0"),n("3"),n("1"),"USD"));}
 @Test public void dinar(){assertEquals(n("0.308"),Conversion.cross(n("1"),n("1"),n("0.3076"),"KWD"));}
 @Test(expected=IllegalArgumentException.class)public void zeroSource(){Conversion.cross(n("1"),n("0"),n("1"),"USD");}
 @Test(expected=IllegalArgumentException.class)public void negativeTarget(){Conversion.cross(n("1"),n("1"),n("-1"),"USD");}
 @Test(expected=IllegalArgumentException.class)public void precision(){Conversion.assetAmount("0.000000001");}
 @Test(expected=IllegalArgumentException.class)public void exponent(){Conversion.assetAmount("1e4");}
 @Test(expected=IllegalArgumentException.class)public void tooLarge(){Conversion.assetAmount("1000000000001");}
}
