package com.example.muhammad.usd_converter;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;
public class CryptoUnitsTest {
 private Asset a(String code){return new Asset(code,code,"",Asset.Kind.CRYPTO);}
 private BigDecimal n(String s){return new BigDecimal(s);}
 @Test public void satoshiMetadata(){assertEquals("BTC",a("SAT").rateCode());assertEquals(n("100000000"),a("SAT").unitsPerCoin());assertTrue(a("SAT").isSubunit());assertEquals(0,Conversion.digits("SAT"));}
 @Test public void dropsMetadata(){assertEquals("XRP",a("DROP").rateCode());assertEquals(n("1000000"),a("DROP").unitsPerCoin());assertTrue(a("DROP").isSubunit());assertEquals(0,Conversion.digits("DROP"));}
 @Test public void coinsUnchanged(){assertEquals("BTC",a("BTC").rateCode());assertEquals(BigDecimal.ONE,a("BTC").unitsPerCoin());assertFalse(a("ETH").isSubunit());}
 @Test public void bitcoinToSatoshi(){assertEquals(n("100000000"),Conversion.cross(BigDecimal.ONE,a("BTC").unitsPerCoin(),a("SAT").unitsPerCoin(),"SAT"));}
 @Test public void singleSatoshiToBitcoin(){assertEquals(n("0.00000001"),Conversion.cross(BigDecimal.ONE,a("SAT").unitsPerCoin(),a("BTC").unitsPerCoin(),"BTC"));}
 @Test public void xrpToDrops(){assertEquals(n("1000000"),Conversion.cross(BigDecimal.ONE,a("XRP").unitsPerCoin(),a("DROP").unitsPerCoin(),"DROP"));}
 @Test public void singleDropToXrp(){assertEquals(n("0.000001"),Conversion.cross(BigDecimal.ONE,a("DROP").unitsPerCoin(),a("XRP").unitsPerCoin(),"XRP"));}
 @Test public void fiatToSatoshi(){assertEquals(n("100000"),Conversion.cross(n("100"),BigDecimal.ONE,n("0.00001").multiply(a("SAT").unitsPerCoin()),"SAT"));}
 @Test public void satoshiToDrops(){assertEquals(n("50000"),Conversion.cross(n("100"),n("1000"),n("500000"),"DROP"));}
 @Test public void wholeUnitOutput(){assertEquals(n("1"),Conversion.cross(n("1"),n("2"),BigDecimal.ONE,"SAT"));}
}
